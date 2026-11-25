package com.supify.test;

import com.supify.app.Main;
import com.supify.concurrency.DecayMonitorThread;
import com.supify.concurrency.TaskBuffer;
import com.supify.dao.DatabaseConfig;
import com.supify.dao.InMemoryStore;
import com.supify.dao.Repository;
import com.supify.exception.SupifyException;
import com.supify.io.AuditFileIO;
import com.supify.io.CsvSupplierIO;
import com.supify.model.Claim;
import com.supify.model.Supplier;
import com.supify.model.TrustBand;
import com.supify.model.VerificationCheck;
import com.supify.model.VerificationMethod;
import com.supify.service.SupplierService;
import com.supify.service.VerificationService;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/** Clean, modular test runner covering all core Supify backend workflows. */
public class SupifyTestRunner {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Supify Backend Automated Test Suite ===");

        testSeedDataAndGenerics();
        testStreamSearchSortAndPagination();
        testFourLayerVerificationSpine();
        testTrustBandCappingAndRejection();
        testProducerConsumerWaitNotify();
        testDaemonDecayAndAlert();
        testCsvCharacterStreams();
        testByteStreamsAndStreamTokenizer();
        testCheckedExceptionHandling();
        testJdbcConfigAndDriver();

        System.out.printf("%n=== TEST SUMMARY: %d PASSED, %d FAILED ===%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static InMemoryStore newSeededStore(TaskBuffer<Claim> queue) {
        InMemoryStore store = new InMemoryStore();
        SupplierService supplierService = new SupplierService(store);
        VerificationService verificationService = new VerificationService(store, supplierService, queue);
        Main.seedSuppliers(store, verificationService);
        return store;
    }

    private static void testSeedDataAndGenerics() {
        InMemoryStore store = newSeededStore(null);
        List<Supplier> filtered = store.findMatching(s -> s.getCapacityPerMonth() >= 25000);
        List<String> names = Repository.mapList(store.findAll(), Supplier::getName);

        Supplier temp = new Supplier("SUP-TMP", "Temp Vendor", "Delhi", "Packaging", 5000, 100, "7 days");
        store.save(temp);
        boolean found = store.findById("SUP-TMP").isPresent();
        boolean deleted = store.deleteById("SUP-TMP");

        check("1. Seed Data & Generics/Wildcards",
                store.count() == 5
                        && filtered.size() == 3
                        && names.contains("Aravind Fasteners Pvt Ltd")
                        && found && deleted);
    }

    private static void testStreamSearchSortAndPagination() {
        InMemoryStore store = newSeededStore(null);
        SupplierService service = new SupplierService(store);

        List<Supplier> filtered = service.search("Pune", "", TrustBand.VERIFIED, 10000, "capacity", 1, 5);
        List<Supplier> page1 = service.search("", "", null, null, "name", 1, 2);
        List<Supplier> page2 = service.search("", "", null, null, "name", 2, 2);
        List<String> categories = service.getAllCategories();

        check("2. Stream Search, Sort & Pagination",
                filtered.size() == 1
                        && filtered.get(0).getId().equals("SUP-2")
                        && page1.size() == 2 && page2.size() == 2
                        && !page1.get(0).getName().equals(page2.get(0).getName())
                        && categories.size() == 5);
    }

    private static void testFourLayerVerificationSpine() throws Exception {
        InMemoryStore store = new InMemoryStore();
        SupplierService supplierService = new SupplierService(store);
        VerificationService verificationService = new VerificationService(store, supplierService, null);

        Supplier sup = new Supplier("SUP-9", "Apex Precision", "Chennai", "Machining", 20000, 500, "12 days");
        store.save(sup);

        Claim c1 = verificationService.submitClaim("SUP-9", "gst", "GST Active", "IDENTITY", "33AAACA1111Z1", "GST PDF", "PDF", "gst.pdf");
        Claim c2 = verificationService.submitClaim("SUP-9", "legal", "MCA Certificate", "LEGAL", "CIN-991", "MCA PDF", "PDF", "mca.pdf");
        VerificationCheck chk1 = verificationService.verifyClaim(c1.getId(), "Ver-1", VerificationMethod.THIRD_PARTY_API, true, "GST verified", 3600);
        VerificationCheck chk2 = verificationService.verifyClaim(c2.getId(), "Ver-1", VerificationMethod.HUMAN_AUDIT, true, "Legal audit passed", 3600);

        check("3. 4-Layer Spine (Claim -> Evidence -> Check -> Attestation)",
                "PASS".equals(chk1.getOutcome())
                        && "PASS".equals(chk2.getOutcome())
                        && "VERIFIED".equals(c1.getStatus())
                        && store.getEvidenceForClaim(c1.getId()).size() == 1
                        && store.getChecksForClaim(c2.getId()).size() == 1
                        && store.getActiveAttestationForClaim(c1.getId()).isPresent()
                        && sup.getTrustBand() == TrustBand.VERIFIED);
    }

    private static void testTrustBandCappingAndRejection() throws Exception {
        InMemoryStore store = new InMemoryStore();
        SupplierService supplierService = new SupplierService(store);
        VerificationService verificationService = new VerificationService(store, supplierService, null);

        Supplier sup = new Supplier("SUP-CAP", "No-Identity Co", "Surat", "Textiles", 30000, 500, "10 days");
        store.save(sup);

        // Missing IDENTITY caps score at UNVERIFIED (<= 0.30) even with SITE_VISIT QUALITY proof
        Claim qualityClaim = verificationService.submitClaim("SUP-CAP", "iso", "ISO 9001", "QUALITY", "ISO-1", "ISO PDF", "PDF", "iso.pdf");
        verificationService.verifyClaim(qualityClaim.getId(), "Aud-1", VerificationMethod.SITE_VISIT, true, "Site visit passed", 3600);
        boolean cappedUnverified = sup.getTrustBand() == TrustBand.UNVERIFIED && sup.getTrustScore() <= 0.30;

        // Adding IDENTITY without LEGAL caps at BASIC (<= 0.65)
        Claim idClaim = verificationService.submitClaim("SUP-CAP", "gst", "GST", "IDENTITY", "24ABC", "GST PDF", "PDF", "gst.pdf");
        verificationService.verifyClaim(idClaim.getId(), "Ver-1", VerificationMethod.THIRD_PARTY_API, true, "GST ok", 3600);
        boolean cappedBasic = sup.getTrustBand() == TrustBand.BASIC && sup.getTrustScore() <= 0.65;

        // FAIL decision marks claim REJECTED and creates zero attestation
        Claim badClaim = verificationService.submitClaim("SUP-CAP", "legal", "MCA", "LEGAL", "BAD", "Blurry scan", "IMAGE", "bad.png");
        VerificationCheck failCheck = verificationService.verifyClaim(badClaim.getId(), "Ver-1", VerificationMethod.DOCUMENT_REVIEW, false, "Scan unreadable", 3600);
        boolean rejectedCleanly = "FAIL".equals(failCheck.getOutcome())
                && "REJECTED".equals(badClaim.getStatus())
                && store.getActiveAttestationForClaim(badClaim.getId()).isEmpty();

        check("4. Trust Band Capping & FAIL Rejection Rules",
                cappedUnverified && cappedBasic && rejectedCleanly);
    }

    private static void testProducerConsumerWaitNotify() throws InterruptedException {
        TaskBuffer<String> buffer = new TaskBuffer<>(2);
        List<String> consumed = new ArrayList<>();

        Thread producer = new Thread(() -> {
            try {
                buffer.produce("TASK-1");
                buffer.produce("TASK-2");
                buffer.produce("TASK-3");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Test-Producer");

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 3; i++) {
                    consumed.add(buffer.consume());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Test-Consumer");

        producer.start();
        consumer.start();
        producer.join(3000);
        consumer.join(3000);
        buffer.shutdown();

        check("5. TaskBuffer Producer-Consumer (wait / notifyAll)",
                consumed.equals(List.of("TASK-1", "TASK-2", "TASK-3")));
    }

    private static void testDaemonDecayAndAlert() throws Exception {
        InMemoryStore store = new InMemoryStore();
        SupplierService supplierService = new SupplierService(store);
        VerificationService verificationService = new VerificationService(store, supplierService, null);

        Supplier sup = new Supplier("SUP-DECAY", "Decay Demo Co", "Noida", "Electronics", 10000, 100, "7 days");
        store.save(sup);

        Claim c = verificationService.submitClaim("SUP-DECAY", "gst", "GST Short", "IDENTITY", "09ABC", "GST PDF", "PDF", "gst.pdf");
        verificationService.verifyClaim(c.getId(), "Ver-1", VerificationMethod.DOCUMENT_REVIEW, true, "1s expiry test", 1);

        DecayMonitorThread daemon = new DecayMonitorThread(store, supplierService, 200);
        boolean isDaemon = daemon.isDaemon();
        daemon.start();
        Thread.sleep(1500);
        daemon.stopMonitor();

        check("6. Daemon Auto-Decay & Non-Blocking Alert",
                isDaemon
                        && "EXPIRED".equals(c.getStatus())
                        && store.getActiveAttestationForClaim(c.getId()).isEmpty()
                        && sup.getTrustBand() == TrustBand.UNVERIFIED
                        && !daemon.drainAlerts().isEmpty());
    }

    private static void testCsvCharacterStreams() throws Exception {
        InMemoryStore store = newSeededStore(null);
        File csvFile = new File("data/test_suite_suppliers.csv");
        try {
            CsvSupplierIO.exportToCsv(csvFile, store.findAll());
            // Append one malformed line to verify import skips bad records cleanly
            try (FileWriter fw = new FileWriter(csvFile, true)) {
                fw.write("BAD,ROW,ONLY,4,COLS\n");
            }
            List<Supplier> imported = CsvSupplierIO.importFromCsv(csvFile);
            check("7. CSV Character Streams & Row Validation", imported.size() == 5);
        } finally {
            csvFile.delete();
        }
    }

    private static void testByteStreamsAndStreamTokenizer() throws Exception {
        File logFile = new File("data/test_suite_audit.log");
        try {
            AuditFileIO.appendAuditByteLog(logFile, "verifier.approved CLM-101");
            String contents = AuditFileIO.readAuditByteLog(logFile);
            List<String> tokens = AuditFileIO.tokenizeRule("CAPACITY >= 50000 AND SCORE >= 0.80");

            check("8. Byte Streams & StreamTokenizer",
                    contents.contains("verifier.approved CLM-101")
                            && tokens.size() == 9
                            && tokens.get(0).equals("WORD(CAPACITY)")
                            && tokens.get(3).equals("NUM(50000.0)"));
        } finally {
            logFile.delete();
        }
    }

    private static void testCheckedExceptionHandling() {
        InMemoryStore store = newSeededStore(null);
        SupplierService supplierService = new SupplierService(store);
        VerificationService verificationService = new VerificationService(store, supplierService, null);

        boolean missingSup = false;
        boolean blankLabel = false;
        boolean missingClaim = false;
        boolean blankNote = false;

        try {
            verificationService.submitClaim("MISSING-SUP", "k", "Label", "QUALITY", "val", "", "PDF", "");
        } catch (SupifyException e) { missingSup = true; }

        try {
            verificationService.submitClaim("SUP-1", "k", "   ", "QUALITY", "val", "", "PDF", "");
        } catch (SupifyException e) { blankLabel = true; }

        try {
            verificationService.verifyClaim("NO-SUCH-CLM", "Ver", VerificationMethod.DOCUMENT_REVIEW, true, "ok", 60);
        } catch (SupifyException e) { missingClaim = true; }

        try {
            verificationService.verifyClaim("CLM-129", "Ver", VerificationMethod.DOCUMENT_REVIEW, true, "  ", 60);
        } catch (SupifyException e) { blankNote = true; }

        check("9. Checked SupifyException Guards",
                missingSup && blankLabel && missingClaim && blankNote);
    }

    private static void testJdbcConfigAndDriver() {
        String url = DatabaseConfig.getUrl();
        boolean driverOk = DatabaseConfig.isDriverAvailable();
        check("10. JDBC Config & Driver Availability",
                url != null && url.startsWith("jdbc:mysql://") && driverOk);
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  [PASS] " + name);
        } else {
            failed++;
            System.err.println("  [FAIL] " + name);
        }
    }
}
