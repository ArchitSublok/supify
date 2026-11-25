package com.supify.app;

import com.supify.concurrency.DecayMonitorThread;
import com.supify.concurrency.TaskBuffer;
import com.supify.dao.DatabaseConfig;
import com.supify.dao.InMemoryStore;
import com.supify.dao.Repository;
import com.supify.exception.SupifyException;
import com.supify.io.AuditFileIO;
import com.supify.io.CsvSupplierIO;
import com.supify.model.Claim;
import com.supify.model.Evidence;
import com.supify.model.Supplier;
import com.supify.model.VerificationCheck;
import com.supify.model.VerificationMethod;
import com.supify.service.SupplierService;
import com.supify.service.VerificationService;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

/** Clean interactive CLI for Supify Supplier Discovery & Verification. */
public class Main {

    public static void main(String[] args) {
        InMemoryStore store = new InMemoryStore();
        TaskBuffer<Claim> reviewQueue = new TaskBuffer<>(100);
        SupplierService supplierService = new SupplierService(store);
        VerificationService verificationService = new VerificationService(store, supplierService, reviewQueue);

        seedSuppliers(store, verificationService);

        DecayMonitorThread monitor = new DecayMonitorThread(store, supplierService, 4000);
        monitor.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> monitor.stopMonitor(), "Shutdown-Monitor"));

        Scanner sc = new Scanner(System.in);
        boolean running = true;

        while (running && sc.hasNextLine()) {
            printAlerts(monitor);
            printHeader("SUPIFY :: Supplier Discovery & Verification");
            System.out.println("  [1] Buyer Console");
            System.out.println("  [2] Supplier Console");
            System.out.println("  [3] Verifier Console");
            System.out.println("  [4] System & Admin");
            System.out.println("  [0] Exit");

            int choice = readInt(sc, "Select", 1);
            switch (choice) {
                case 1 -> buyerMenu(sc, store, supplierService, monitor);
                case 2 -> supplierMenu(sc, store, verificationService, monitor);
                case 3 -> verifierMenu(sc, store, verificationService, monitor);
                case 4 -> toolsMenu(sc, store, monitor);
                case 0 -> {
                    running = false;
                    monitor.stopMonitor();
                    reviewQueue.shutdown();
                    System.out.println("\nSession closed. Goodbye!");
                }
                default -> System.out.println("  Invalid option.");
            }
        }
    }

    private static void buyerMenu(Scanner sc, InMemoryStore store, SupplierService service, DecayMonitorThread monitor) {
        while (sc.hasNextLine()) {
            printAlerts(monitor);
            printHeader("BUYER CONSOLE");
            System.out.println("  [1] List Suppliers");
            System.out.println("  [2] Search Suppliers");
            System.out.println("  [3] Inspect Supplier Claims");
            System.out.println("  [4] Compare 2 Suppliers");
            System.out.println("  [0] Back");

            int ch = readInt(sc, "Choice", 0);
            if (ch == 0) break;

            switch (ch) {
                case 1 -> printSupplierTable(service.search("", "", null, null, "score", 1, 20));
                case 2 -> {
                    System.out.print("Keyword / city / category (ENTER for all): ");
                    String kw = sc.nextLine().trim();
                    int sortChoice = readInt(sc, "Sort by [1=Score, 2=Capacity, 3=Name]", 1);
                    String sort = switch (sortChoice) {
                        case 2 -> "capacity";
                        case 3 -> "name";
                        default -> "score";
                    };
                    printSupplierTable(service.search(kw, "", null, null, sort, 1, 20));
                }
                case 3 -> {
                    Supplier s = pickSupplier(sc, store, "Select supplier to inspect");
                    if (s == null) break;
                    System.out.printf("%n  %s (%s) | %s | Cap: %,d/mo | MOQ: %d | Lead: %s%n",
                            s.getName(), s.getId(), s.getCity(), s.getCapacityPerMonth(), s.getMoq(), s.getLeadTime());
                    System.out.printf("  Standing: %s (%.2f)%n", s.getTrustBand(), s.getTrustScore());
                    for (Claim c : store.getClaimsForSupplier(s.getId())) {
                        System.out.println("  - " + c);
                        for (Evidence e : store.getEvidenceForClaim(c.getId())) {
                            System.out.println("      Evidence : " + e);
                        }
                        for (VerificationCheck chk : store.getChecksForClaim(c.getId())) {
                            System.out.println("      Check    : " + chk);
                        }
                        store.getActiveAttestationForClaim(c.getId()).ifPresent(a ->
                                System.out.println("      Attest   : " + a.getId() + " (" + a.getMethod()
                                        + ", expires in " + a.secondsLeft() + "s)"));
                    }
                }
                case 4 -> {
                    Supplier a = pickSupplier(sc, store, "First supplier");
                    if (a == null) break;
                    Supplier b = pickSupplier(sc, store, "Second supplier");
                    if (b == null) break;
                    System.out.printf("%n  %-14s | %-28s | %-28s%n", "FIELD", a.getName(), b.getName());
                    System.out.println("  " + "-".repeat(76));
                    System.out.printf("  %-14s | %-28s | %-28s%n", "Trust Band", a.getTrustBand(), b.getTrustBand());
                    System.out.printf("  %-14s | %-28.2f | %-28.2f%n", "Trust Score", a.getTrustScore(), b.getTrustScore());
                    System.out.printf("  %-14s | %-28d | %-28d%n", "Capacity/mo", a.getCapacityPerMonth(), b.getCapacityPerMonth());
                    System.out.printf("  %-14s | %-28d | %-28d%n", "MOQ", a.getMoq(), b.getMoq());
                    System.out.printf("  %-14s | %-28s | %-28s%n", "Lead Time", a.getLeadTime(), b.getLeadTime());
                }
                default -> System.out.println("  Invalid option.");
            }
        }
    }

    private static void supplierMenu(Scanner sc, InMemoryStore store, VerificationService verificationService,
                                     DecayMonitorThread monitor) {
        String myId = "SUP-1";
        while (sc.hasNextLine()) {
            printAlerts(monitor);
            printHeader("SUPPLIER CONSOLE (" + myId + ")");
            System.out.println("  [1] My Standing");
            System.out.println("  [2] Submit Claim");
            System.out.println("  [3] Expiry Timers");
            System.out.println("  [0] Back");

            int ch = readInt(sc, "Choice", 0);
            if (ch == 0) break;

            switch (ch) {
                case 1 -> store.findById(myId).ifPresent(s ->
                        System.out.println("\n  " + s + " | Claims: " + store.getClaimsForSupplier(myId).size()));
                case 2 -> {
                    int p = readInt(sc, "Pillar [1=IDENTITY, 2=LEGAL, 3=QUALITY, 4=CAPABILITY]", 3);
                    String pillar = switch (p) {
                        case 1 -> "IDENTITY";
                        case 2 -> "LEGAL";
                        case 4 -> "CAPABILITY";
                        default -> "QUALITY";
                    };
                    System.out.print("Claim label (ENTER for 'ISO 45001 Safety'): ");
                    String label = sc.nextLine().trim();
                    if (label.isEmpty()) label = "ISO 45001 Safety";

                    System.out.print("Certificate value (ENTER for 'OHS-2026-01'): ");
                    String val = sc.nextLine().trim();
                    if (val.isEmpty()) val = "OHS-2026-01";

                    try {
                        Claim c = verificationService.submitClaim(myId, label.toLowerCase().replace(" ", "_"),
                                label, pillar, val, label + " PDF", "PDF", "docs/" + label + ".pdf");
                        System.out.println("  Submitted " + c.getId() + " with PDF evidence for verifier review.");
                    } catch (SupifyException e) {
                        System.out.println("  [ERROR] " + e.getMessage());
                    }
                }
                case 3 -> {
                    System.out.println("\n  Active Attestation Countdowns:");
                    for (Claim c : store.getClaimsForSupplier(myId)) {
                        store.getActiveAttestationForClaim(c.getId()).ifPresent(a ->
                                System.out.println("  - " + c.getLabel() + " -> " + a.getState()
                                        + " (" + a.secondsLeft() + "s left)"));
                    }
                }
                default -> System.out.println("  Invalid option.");
            }
        }
    }

    private static void verifierMenu(Scanner sc, InMemoryStore store, VerificationService verificationService,
                                     DecayMonitorThread monitor) {
        while (sc.hasNextLine()) {
            printAlerts(monitor);
            printHeader("VERIFIER CONSOLE");
            System.out.println("  [1] Pending Claims");
            System.out.println("  [2] Approve / Reject Claim");
            System.out.println("  [0] Back");

            int ch = readInt(sc, "Choice", 0);
            if (ch == 0) break;

            List<Claim> pending = store.getPendingClaims();
            if (ch == 1) {
                if (pending.isEmpty()) {
                    System.out.println("  No pending claims.");
                } else {
                    for (Claim c : pending) {
                        System.out.println("  - " + c + " | Supplier: " + c.getSupplierId());
                        for (Evidence e : store.getEvidenceForClaim(c.getId())) {
                            System.out.println("      Proof: " + e);
                        }
                    }
                }
            } else if (ch == 2) {
                if (pending.isEmpty()) {
                    System.out.println("  No pending claims.");
                    continue;
                }
                Claim target = pickPendingClaim(sc, pending);
                if (target == null) continue;

                int m = readInt(sc, "Method [1=DOC, 2=API, 3=AUDIT, 4=SITE]", 1);
                VerificationMethod method = switch (m) {
                    case 2 -> VerificationMethod.THIRD_PARTY_API;
                    case 3 -> VerificationMethod.HUMAN_AUDIT;
                    case 4 -> VerificationMethod.SITE_VISIT;
                    default -> VerificationMethod.DOCUMENT_REVIEW;
                };

                System.out.print("Approve? (Y/N) [Y]: ");
                String ans = sc.nextLine().trim();
                boolean approved = ans.isEmpty() || ans.equalsIgnoreCase("Y");

                System.out.print("Note (ENTER for default): ");
                String note = sc.nextLine().trim();
                if (note.isEmpty()) note = "Reviewed uploaded proof against registry.";

                try {
                    VerificationCheck check = verificationService.verifyClaim(
                            target.getId(), "Verifier-01", method, approved, note, 86400);
                    System.out.println("  Saved: " + check);
                    store.findById(target.getSupplierId()).ifPresent(s ->
                            System.out.printf("  Updated %s -> %s (%.2f)%n", s.getId(), s.getTrustBand(), s.getTrustScore()));
                } catch (SupifyException e) {
                    System.out.println("  [ERROR] " + e.getMessage());
                }
            } else {
                System.out.println("  Invalid option.");
            }
        }
    }

    private static void toolsMenu(Scanner sc, InMemoryStore store, DecayMonitorThread monitor) {
        while (sc.hasNextLine()) {
            printAlerts(monitor);
            printHeader("SYSTEM & ADMIN");
            System.out.println("  [1] Active Threads");
            System.out.println("  [2] Queue Worker Check");
            System.out.println("  [3] CSV Export / Import");
            System.out.println("  [4] Audit Log");
            System.out.println("  [5] Parse Rule");
            System.out.println("  [6] DB Status");
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
                        System.out.println("  Synced " + imported.size() + " suppliers: " + names);
                    } catch (IOException e) {
                        System.out.println("  [ERROR] CSV error: " + e.getMessage());
                    }
                }
                case 4 -> {
                    File logFile = new File("data/audit.log");
                    try {
                        AuditFileIO.appendAuditByteLog(logFile, "System checkpoint logged.");
                        System.out.println("\n--- data/audit.log ---\n" + AuditFileIO.readAuditByteLog(logFile));
                    } catch (IOException e) {
                        System.out.println("  [ERROR] Audit log error: " + e.getMessage());
                    }
                }
                case 5 -> {
                    System.out.print("Rule (ENTER for 'CAPACITY >= 50000 AND SCORE >= 0.80'): ");
                    String rule = sc.nextLine().trim();
                    if (rule.isEmpty()) rule = "CAPACITY >= 50000 AND SCORE >= 0.80";
                    try {
                        System.out.println("  Tokens: " + AuditFileIO.tokenizeRule(rule));
                    } catch (IOException e) {
                        System.out.println("  [ERROR] Rule parse error: " + e.getMessage());
                    }
                }
                case 6 -> {
                    System.out.println("  JDBC URL : " + DatabaseConfig.getUrl());
                    System.out.println("  Driver   : " + DatabaseConfig.isDriverAvailable());
                    System.out.println("  Connected: " + DatabaseConfig.testConnection());
                    if (!DatabaseConfig.getLastError().isBlank()) {
                        System.out.println("  Note     : " + DatabaseConfig.getLastError());
                    }
                }
                default -> System.out.println("  Invalid option.");
            }
        }
    }

    private static Supplier pickSupplier(Scanner sc, InMemoryStore store, String label) {
        List<Supplier> all = store.findAll();
        all.sort(Comparator.comparing(Supplier::getId));
        System.out.println();
        for (int i = 0; i < all.size(); i++) {
            Supplier s = all.get(i);
            System.out.printf("  [%d] %s | %s (%s)%n", i + 1, s.getId(), s.getName(), s.getTrustBand());
        }
        System.out.print(label + " [1]: ");
        if (!sc.hasNextLine()) return null;
        String input = sc.nextLine().trim();
        if (input.isEmpty()) return all.isEmpty() ? null : all.get(0);
        try {
            int idx = Integer.parseInt(input) - 1;
            if (idx >= 0 && idx < all.size()) return all.get(idx);
        } catch (NumberFormatException ignored) {
        }
        return store.findById(input.toUpperCase()).orElseGet(() -> {
            System.out.println("  Supplier not found: " + input);
            return null;
        });
    }

    private static Claim pickPendingClaim(Scanner sc, List<Claim> pending) {
        System.out.println();
        for (int i = 0; i < pending.size(); i++) {
            Claim c = pending.get(i);
            System.out.printf("  [%d] %s | %s | %s [%s]%n", i + 1, c.getId(), c.getSupplierId(), c.getLabel(), c.getPillar());
        }
        System.out.print("Select claim [1]: ");
        if (!sc.hasNextLine()) return null;
        String input = sc.nextLine().trim();
        if (input.isEmpty()) return pending.get(0);
        try {
            int idx = Integer.parseInt(input) - 1;
            if (idx >= 0 && idx < pending.size()) return pending.get(idx);
        } catch (NumberFormatException ignored) {
        }
        for (Claim c : pending) {
            if (c.getId().equalsIgnoreCase(input)) return c;
        }
        System.out.println("  Claim not found: " + input);
        return null;
    }

    private static void runProducerConsumerDemo() {
        TaskBuffer<String> buffer = new TaskBuffer<>(2);
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 4; i++) {
                    String job = "Verify-Job-" + i;
                    buffer.produce(job);
                    System.out.println("  [Queued]    " + job);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Queue-Producer");

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 4; i++) {
                    String job = buffer.consume();
                    System.out.println("  [Processed] " + job);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Queue-Consumer");

        producer.start();
        consumer.start();
        try {
            producer.join(3000);
            consumer.join(3000);
            buffer.shutdown();
            System.out.println("  Queue worker check complete.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void printSupplierTable(List<Supplier> list) {
        if (list.isEmpty()) {
            System.out.println("  No matching suppliers.");
            return;
        }
        System.out.printf("%n  %-2s | %-6s | %-28s | %-12s | %-16s | %-10s | %-5s%n",
                "#", "ID", "SUPPLIER", "CITY", "CATEGORY", "BAND", "SCORE");
        System.out.println("  " + "-".repeat(94));
        for (int i = 0; i < list.size(); i++) {
            Supplier s = list.get(i);
            System.out.printf("  %-2d | %-6s | %-28s | %-12s | %-16s | %-10s | %.2f%n",
                    i + 1, s.getId(), s.getName(), s.getCity(), s.getCategory(), s.getTrustBand(), s.getTrustScore());
        }
    }

    private static void printHeader(String title) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("  " + title);
        System.out.println("=".repeat(80));
    }

    private static void printAlerts(DecayMonitorThread monitor) {
        for (String alert : monitor.drainAlerts()) {
            System.out.println("\n  [ALERT] " + alert);
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
