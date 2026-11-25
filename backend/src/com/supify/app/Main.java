package com.supify.app;

import com.supify.concurrency.DecayMonitorThread;
import com.supify.concurrency.TaskBuffer;
import com.supify.dao.DatabaseConfig;
import com.supify.dao.InMemoryStore;
import com.supify.dao.Repository;
import com.supify.exception.SupifyException;
import com.supify.io.AuditFileIO;
import com.supify.io.CsvSupplierIO;
import com.supify.model.Attestation;
import com.supify.model.Claim;
import com.supify.model.Evidence;
import com.supify.model.Supplier;
import com.supify.model.TrustBand;
import com.supify.model.VerificationCheck;
import com.supify.model.VerificationMethod;
import com.supify.service.SupplierService;
import com.supify.service.VerificationService;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/** Simple interactive CLI for Supify Supplier Discovery & Evidence Verification. */
public class Main {

    public static void main(String[] args) {
        InMemoryStore store = new InMemoryStore();
        TaskBuffer<Claim> reviewQueue = new TaskBuffer<>(100);
        SupplierService supplierService = new SupplierService(store);
        VerificationService verificationService = new VerificationService(store, supplierService, reviewQueue);

        seedSuppliers(store, verificationService);

        DecayMonitorThread monitor = new DecayMonitorThread(store, supplierService, 4000);
        monitor.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> monitor.stopMonitor(), "Shutdown-MonitorStop"));

        Scanner sc = new Scanner(System.in);
        boolean running = true;

        while (running && sc.hasNextLine()) {
            printAlerts(monitor);
            System.out.println("\n=== SUPIFY :: Supplier Discovery & Verification CLI ===");
            System.out.println("  [1] Buyer Console    (List, Search, Claims, Compare)");
            System.out.println("  [2] Supplier Console (Submit Claim + Evidence, Expiry Timers)");
            System.out.println("  [3] Verifier Console (Pending Queue, Approve/Reject)");
            System.out.println("  [4] System & Admin   (Threads, Queue, CSV/Audit Files, Rules, DB)");
            System.out.println("  [0] Exit");

            int choice = readInt(sc, "Select option", 1);
            switch (choice) {
                case 1 -> buyerMenu(sc, store, supplierService, monitor);
                case 2 -> supplierMenu(sc, store, verificationService, monitor);
                case 3 -> verifierMenu(sc, store, verificationService, monitor);
                case 4 -> toolsMenu(sc, store, monitor);
                case 0 -> {
                    running = false;
                    monitor.stopMonitor();
                    reviewQueue.shutdown();
                    System.out.println("Stopped background monitor. Goodbye!");
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void buyerMenu(Scanner sc, InMemoryStore store, SupplierService service, DecayMonitorThread monitor) {
        while (sc.hasNextLine()) {
            printAlerts(monitor);
            System.out.println("\n--- BUYER CONSOLE ---");
            System.out.println("  [1] List All Suppliers (Sorted by Trust Score)");
            System.out.println("  [2] Search & Filter Suppliers");
            System.out.println("  [3] View Supplier Profile & 4-Layer Claims");
            System.out.println("  [4] Compare 2 Suppliers Side-by-Side");
            System.out.println("  [0] Back");

            int ch = readInt(sc, "Choice", 0);
            if (ch == 0) break;

            if (ch == 1) {
                printSupplierList(service.search("", "", null, null, "score", 1, 20));
            } else if (ch == 2) {
                System.out.print("Keyword (e.g. Fasteners, Pune, or ENTER): ");
                String kw = sc.nextLine();
                System.out.print("Category (available: " + service.getAllCategories() + " or ENTER): ");
                String cat = sc.nextLine();
                int minCap = readInt(sc, "Minimum monthly capacity (0 for any)", 0);
                System.out.print("Sort by [score / capacity / name]: ");
                String sort = sc.nextLine();
                printSupplierList(service.search(kw, cat, null, minCap, sort, 1, 20));
            } else if (ch == 3) {
                System.out.print("Enter Supplier ID (e.g. SUP-1): ");
                String id = sc.nextLine().trim();
                Optional<Supplier> supOpt = store.findById(id);
                if (supOpt.isEmpty()) {
                    System.out.println("Supplier not found: " + id);
                    continue;
                }
                Supplier s = supOpt.get();
                System.out.println("\n" + s + " | Capacity: " + s.getCapacityPerMonth()
                        + "/mo | MOQ: " + s.getMoq() + " | Lead: " + s.getLeadTime());
                for (Claim c : store.getClaimsForSupplier(id)) {
                    System.out.println("  Claim: " + c);
                    for (Evidence e : store.getEvidenceForClaim(c.getId())) {
                        System.out.println("    Evidence: " + e);
                    }
                    for (VerificationCheck chk : store.getChecksForClaim(c.getId())) {
                        System.out.println("    Check:    " + chk);
                    }
                    store.getActiveAttestationForClaim(c.getId()).ifPresent(a ->
                            System.out.println("    Attest:   " + a.getId() + " (" + a.getMethod()
                                    + ", expires in " + a.secondsLeft() + "s)"));
                }
            } else if (ch == 4) {
                System.out.print("First Supplier ID (e.g. SUP-1): ");
                String id1 = sc.nextLine().trim();
                System.out.print("Second Supplier ID (e.g. SUP-2): ");
                String id2 = sc.nextLine().trim();
                Optional<Supplier> s1 = store.findById(id1);
                Optional<Supplier> s2 = store.findById(id2);
                if (s1.isEmpty() || s2.isEmpty()) {
                    System.out.println("Both supplier IDs must exist.");
                } else {
                    Supplier a = s1.get(), b = s2.get();
                    System.out.printf("%-16s | %-28s | %-28s%n", "FIELD", a.getName(), b.getName());
                    System.out.println("-".repeat(78));
                    System.out.printf("%-16s | %-28s | %-28s%n", "Trust Band", a.getTrustBand(), b.getTrustBand());
                    System.out.printf("%-16s | %-28.2f | %-28.2f%n", "Trust Score", a.getTrustScore(), b.getTrustScore());
                    System.out.printf("%-16s | %-28d | %-28d%n", "Capacity/mo", a.getCapacityPerMonth(), b.getCapacityPerMonth());
                    System.out.printf("%-16s | %-28d | %-28d%n", "MOQ", a.getMoq(), b.getMoq());
                    System.out.printf("%-16s | %-28s | %-28s%n", "Lead Time", a.getLeadTime(), b.getLeadTime());
                }
            }
        }
    }

    private static void supplierMenu(Scanner sc, InMemoryStore store, VerificationService verificationService,
                                     DecayMonitorThread monitor) {
        String myId = "SUP-1";
        while (sc.hasNextLine()) {
            printAlerts(monitor);
            System.out.println("\n--- SUPPLIER CONSOLE (Logged in as " + myId + ") ---");
            System.out.println("  [1] View My Trust Standing");
            System.out.println("  [2] Submit New Claim with Evidence");
            System.out.println("  [3] Check Attestation Expiry Timers");
            System.out.println("  [0] Back");

            int ch = readInt(sc, "Choice", 0);
            if (ch == 0) break;

            if (ch == 1) {
                store.findById(myId).ifPresent(System.out::println);
            } else if (ch == 2) {
                System.out.print("Pillar [IDENTITY / LEGAL / QUALITY / CAPABILITY]: ");
                String pillar = sc.nextLine().trim();
                if (pillar.isEmpty()) pillar = "QUALITY";
                System.out.print("Claim label (e.g. ISO 45001 Safety): ");
                String label = sc.nextLine().trim();
                if (label.isEmpty()) label = "ISO 45001 Safety";
                System.out.print("Certificate value/number: ");
                String val = sc.nextLine().trim();
                if (val.isEmpty()) val = "OHS-2026-01";
                System.out.print("Document title: ");
                String docTitle = sc.nextLine().trim();
                if (docTitle.isEmpty()) docTitle = label + " PDF";

                try {
                    Claim c = verificationService.submitClaim(myId, label.toLowerCase().replace(" ", "_"),
                            label, pillar, val, docTitle, "PDF", "docs/" + docTitle + ".pdf");
                    System.out.println("Submitted claim " + c.getId() + " for verification review.");
                } catch (SupifyException e) {
                    System.out.println("[ERROR] " + e.getMessage());
                }
            } else if (ch == 3) {
                System.out.println("\nActive Attestation Countdowns:");
                for (Claim c : store.getClaimsForSupplier(myId)) {
                    store.getActiveAttestationForClaim(c.getId()).ifPresent(a ->
                            System.out.println("  " + c.getLabel() + " -> " + a.getState()
                                    + " (" + a.secondsLeft() + "s remaining)"));
                }
            }
        }
    }

    private static void verifierMenu(Scanner sc, InMemoryStore store, VerificationService verificationService,
                                     DecayMonitorThread monitor) {
        while (sc.hasNextLine()) {
            printAlerts(monitor);
            System.out.println("\n--- VERIFIER CONSOLE ---");
            System.out.println("  [1] View Pending Claims");
            System.out.println("  [2] Approve / Reject Claim");
            System.out.println("  [0] Back");

            int ch = readInt(sc, "Choice", 0);
            if (ch == 0) break;

            List<Claim> pending = store.getPendingClaims();
            if (ch == 1) {
                if (pending.isEmpty()) {
                    System.out.println("No pending claims.");
                } else {
                    for (Claim c : pending) {
                        System.out.println("  " + c + " | Supplier=" + c.getSupplierId());
                        for (Evidence e : store.getEvidenceForClaim(c.getId())) {
                            System.out.println("     Proof: " + e);
                        }
                    }
                }
            } else if (ch == 2) {
                System.out.print("Enter Claim ID to verify (e.g. CLM-104): ");
                String claimId = sc.nextLine().trim();
                System.out.print("Method [1=DOCUMENT_REVIEW, 2=THIRD_PARTY_API, 3=HUMAN_AUDIT, 4=SITE_VISIT]: ");
                int m = readInt(sc, "Method", 1);
                VerificationMethod method = switch (m) {
                    case 2 -> VerificationMethod.THIRD_PARTY_API;
                    case 3 -> VerificationMethod.HUMAN_AUDIT;
                    case 4 -> VerificationMethod.SITE_VISIT;
                    default -> VerificationMethod.DOCUMENT_REVIEW;
                };
                System.out.print("Approve? (Y/N): ");
                boolean approved = sc.nextLine().trim().equalsIgnoreCase("Y");
                System.out.print("Verification note: ");
                String note = sc.nextLine().trim();
                if (note.isEmpty()) note = "Reviewed uploaded proof against registry.";

                try {
                    VerificationCheck check = verificationService.verifyClaim(
                            claimId, "Verifier-01", method, approved, note, 86400);
                    System.out.println("Saved check: " + check);
                } catch (SupifyException e) {
                    System.out.println("[ERROR] " + e.getMessage());
                }
            }
        }
    }

    private static void toolsMenu(Scanner sc, InMemoryStore store, DecayMonitorThread monitor) {
        while (sc.hasNextLine()) {
            printAlerts(monitor);
            System.out.println("\n--- SYSTEM & ADMIN TOOLS ---");
            System.out.println("  [1] Active JVM Thread States");
            System.out.println("  [2] Verification Queue Worker Check");
            System.out.println("  [3] Export & Import Supplier CSV");
            System.out.println("  [4] Write & Read Audit Log");
            System.out.println("  [5] Parse Verification Rule");
            System.out.println("  [6] Database Connection Status");
            System.out.println("  [0] Back");

            int ch = readInt(sc, "Choice", 0);
            if (ch == 0) break;

            switch (ch) {
                case 1 -> DecayMonitorThread.printThreadStates();
                case 2 -> runProducerConsumerDemo();
                case 3 -> {
                    File csvFile = new File("data/suppliers.csv");
                    try {
                        CsvSupplierIO.exportToCsv(csvFile, store.findAll());
                        List<Supplier> imported = CsvSupplierIO.importFromCsv(csvFile);
                        List<String> names = Repository.mapList(imported, Supplier::getName);
                        System.out.println("Exported & imported " + imported.size() + " suppliers: " + names);
                    } catch (IOException e) {
                        System.out.println("[ERROR] CSV I/O failed: " + e.getMessage());
                    }
                }
                case 4 -> {
                    File logFile = new File("data/audit.log");
                    try {
                        AuditFileIO.appendAuditByteLog(logFile, "CLI byte stream check executed.");
                        System.out.println("--- data/audit.log ---\n" + AuditFileIO.readAuditByteLog(logFile));
                    } catch (IOException e) {
                        System.out.println("[ERROR] Byte stream failed: " + e.getMessage());
                    }
                }
                case 5 -> {
                    System.out.print("Enter rule (ENTER for 'CAPACITY >= 50000 AND SCORE >= 0.80'): ");
                    String rule = sc.nextLine().trim();
                    if (rule.isEmpty()) rule = "CAPACITY >= 50000 AND SCORE >= 0.80";
                    try {
                        System.out.println("Tokens: " + AuditFileIO.tokenizeRule(rule));
                    } catch (IOException e) {
                        System.out.println("[ERROR] Tokenizer failed: " + e.getMessage());
                    }
                }
                case 6 -> {
                    System.out.println("JDBC URL:         " + DatabaseConfig.getUrl());
                    System.out.println("Driver Available: " + DatabaseConfig.isDriverAvailable());
                    System.out.println("DB Connected:     " + DatabaseConfig.testConnection());
                    if (!DatabaseConfig.getLastError().isBlank()) {
                        System.out.println("Note:             " + DatabaseConfig.getLastError());
                    }
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private static void runProducerConsumerDemo() {
        TaskBuffer<String> buffer = new TaskBuffer<>(2);
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 4; i++) {
                    String job = "Verify-Job-" + i;
                    buffer.produce(job);
                    System.out.println("  [Producer] Added " + job);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Demo-Producer");

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 4; i++) {
                    String job = buffer.consume();
                    System.out.println("  [Consumer] Processed " + job);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Demo-Consumer");

        producer.start();
        consumer.start();
        try {
            producer.join(3000);
            consumer.join(3000);
            buffer.shutdown();
            System.out.println("Producer-Consumer coordination complete.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void printSupplierList(List<Supplier> list) {
        if (list.isEmpty()) {
            System.out.println("No matching suppliers.");
            return;
        }
        System.out.println();
        for (Supplier s : list) {
            System.out.println("  " + s);
        }
    }

    private static void printAlerts(DecayMonitorThread monitor) {
        for (String alert : monitor.drainAlerts()) {
            System.out.println("\n[ALERT] " + alert);
        }
    }

    private static int readInt(Scanner sc, String prompt, int fallback) {
        System.out.print(prompt + " [" + fallback + "]: ");
        if (!sc.hasNextLine()) return fallback;
        String input = sc.nextLine().trim();
        if (input.isEmpty()) return fallback;
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static void seedSuppliers(InMemoryStore store, VerificationService verificationService) {
        store.save(new Supplier("SUP-1", "Aravind Fasteners Pvt Ltd", "Coimbatore", "Fasteners", 50000, 1000, "14 days"));
        store.save(new Supplier("SUP-2", "Monarch Industrial Assemblies", "Pune", "Sheet Metal", 12000, 200, "21 days"));
        store.save(new Supplier("SUP-3", "Kavach Safety Systems", "Ahmedabad", "Safety Equipment", 25000, 500, "10 days"));
        store.save(new Supplier("SUP-4", "Suryodaya Solar Tech", "Jaipur", "Solar Structures", 80000, 2500, "18 days"));
        store.save(new Supplier("SUP-5", "Bharat Forgings Co", "Rajkot", "Forgings", 15000, 300, "15 days"));

        try {
            Claim c1 = verificationService.submitClaim("SUP-1", "gst", "GSTIN Active", "IDENTITY", "33AABCA1234F1Z8", "GST Cert", "PDF", "gst.pdf");
            Claim c2 = verificationService.submitClaim("SUP-1", "incorp", "MCA Corporate Filing", "LEGAL", "CIN-U2899TN", "COI Cert", "PDF", "coi.pdf");
            Claim c3 = verificationService.submitClaim("SUP-1", "iso_9001", "ISO 9001 Quality", "QUALITY", "QA-98421", "ISO PDF", "PDF", "iso.pdf");
            Claim c4 = verificationService.submitClaim("SUP-1", "capacity_cap", "50T/mo Cold Heading Capacity", "CAPABILITY", "50000 kg/mo", "Factory Report", "SITE_REPORT", "site.pdf");

            verificationService.verifyClaim(c1.getId(), "API-Adapter", VerificationMethod.THIRD_PARTY_API, true, "GST portal active", 86400);
            verificationService.verifyClaim(c2.getId(), "Verifier-01", VerificationMethod.DOCUMENT_REVIEW, true, "MCA verified", 86400);
            verificationService.verifyClaim(c3.getId(), "Auditor-01", VerificationMethod.HUMAN_AUDIT, true, "ISO audit passed", 86400);
            // Short 30-second expiry for live decay monitoring
            verificationService.verifyClaim(c4.getId(), "Auditor-Chennai", VerificationMethod.SITE_VISIT, true, "Inspected 4 headers", 30);

            Claim c5 = verificationService.submitClaim("SUP-2", "gst", "GSTIN Active", "IDENTITY", "27AABCM9911K1Z1", "GST Cert", "PDF", "m_gst.pdf");
            Claim c6 = verificationService.submitClaim("SUP-2", "incorp", "Factory License", "LEGAL", "MH-FAC-441", "License PDF", "PDF", "m_lic.pdf");
            Claim c7 = verificationService.submitClaim("SUP-2", "quality_audit", "AS9100 Audit", "QUALITY", "AS9100D", "Audit Report", "SITE_REPORT", "m_as.pdf");
            verificationService.verifyClaim(c5.getId(), "API-Adapter", VerificationMethod.THIRD_PARTY_API, true, "GST active", 86400);
            verificationService.verifyClaim(c6.getId(), "Auditor-02", VerificationMethod.HUMAN_AUDIT, true, "Factory license valid", 86400);
            verificationService.verifyClaim(c7.getId(), "Auditor-Pune", VerificationMethod.SITE_VISIT, true, "On-site audit passed", 86400);
            verificationService.submitClaim("SUP-2", "iso_14001", "ISO 14001 Environmental", "COMPLIANCE", "ENV-2026", "ISO 14001 PDF", "PDF", "iso14001.pdf");

            Claim c8 = verificationService.submitClaim("SUP-3", "gst", "GSTIN Active", "IDENTITY", "24AABCK1102M1Z3", "GST PDF", "PDF", "k_gst.pdf");
            Claim c9 = verificationService.submitClaim("SUP-3", "legal_reg", "Trade License", "LEGAL", "GJ-2026-88", "License PDF", "PDF", "k_lic.pdf");
            verificationService.verifyClaim(c8.getId(), "Verifier-01", VerificationMethod.DOCUMENT_REVIEW, true, "GST verified", 86400);
            verificationService.verifyClaim(c9.getId(), "Verifier-01", VerificationMethod.THIRD_PARTY_API, true, "Trade registry matched", 86400);

            Claim c10 = verificationService.submitClaim("SUP-4", "gst", "GSTIN Active", "IDENTITY", "08AABCS8821D1Z9", "GST PDF", "PDF", "s_gst.pdf");
            verificationService.verifyClaim(c10.getId(), "Verifier-01", VerificationMethod.DOCUMENT_REVIEW, true, "GST valid (Legal pending)", 86400);

            verificationService.submitClaim("SUP-5", "self_cap", "15T Forging Claim", "QUALITY", "Self Declared", "Brochure", "PDF", "b_brochure.pdf");
        } catch (SupifyException e) {
            System.err.println("Seed error: " + e.getMessage());
        }
    }
}
