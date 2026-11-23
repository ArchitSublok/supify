package com.supify.test;

import com.supify.concurrency.AlertBuffer;
import com.supify.concurrency.CustomTaskQueue;
import com.supify.dao.GenericRepository;
import com.supify.dao.memory.InMemoryAuditRepository;
import com.supify.dao.memory.InMemorySupplierRepository;
import com.supify.dao.memory.InMemoryVerificationRepository;
import com.supify.exception.InvalidDataException;
import com.supify.exception.SupplierNotFoundException;
import com.supify.exception.VerificationException;
import com.supify.io.AuditByteStreamHandler;
import com.supify.io.CsvDataExporterImporter;
import com.supify.io.RuleStreamTokenizer;
import com.supify.model.Attestation;
import com.supify.model.AuditEvent;
import com.supify.model.Claim;
import com.supify.model.Evidence;
import com.supify.model.Organisation;
import com.supify.model.PaginatedResult;
import com.supify.model.SupplierProfile;
import com.supify.model.TrustSnapshot;
import com.supify.model.VerificationTask;
import com.supify.model.enums.AttestationState;
import com.supify.model.enums.CheckOutcome;
import com.supify.model.enums.ClaimStatus;
import com.supify.model.enums.EvidenceKind;
import com.supify.model.enums.Pillar;
import com.supify.model.enums.TaskState;
import com.supify.model.enums.TrustBand;
import com.supify.model.enums.VerificationMethod;
import com.supify.service.SupplierSearchService;
import com.supify.service.TrustEvaluationService;
import com.supify.service.VerificationService;
import com.supify.util.SeedDataFactory;
import java.io.File;
import java.io.StreamTokenizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Predicate;

/**
 * Automated verification test suite for the Supify Java backend.
 * Zero external dependencies; validates domain spine, streams, concurrency, I/O, and exceptions.
 */
public class SupifyTestRunner {

    public static void main(String[] args) {
        System.out.println(">>> Starting Automated Supify Backend Verification Tests <<<\n");

        InMemorySupplierRepository supplierRepo = new InMemorySupplierRepository();
        InMemoryVerificationRepository verificationRepo = new InMemoryVerificationRepository();
        InMemoryAuditRepository auditRepo = new InMemoryAuditRepository();

        // Test 1: Seed data population
        SeedDataFactory.populate(supplierRepo, verificationRepo);
        List<SupplierProfile> profiles = supplierRepo.findAllProfiles();
        List<VerificationTask> pendingTasks = verificationRepo.findPendingTasks();
        assertCondition(profiles.size() == 5, "Test 1.1: SeedDataFactory populated 5 canonical suppliers");
        assertCondition(pendingTasks.size() == 3, "Test 1.2: SeedDataFactory queued 3 pending verification tasks");

        // Test 2: Stream API multi-facet search, sorting & pagination
        SupplierSearchService searchService = new SupplierSearchService(supplierRepo);
        PaginatedResult<SupplierSearchService.SupplierSearchResult> allSorted =
                searchService.search("", "", null, null, "score", false, 1, 3);
        assertCondition(allSorted.getTotalCount() == 5, "Test 2.1: Total matched suppliers = 5");
        assertCondition(allSorted.getItems().size() == 3, "Test 2.2: Pagination pageSize=3 returns 3 items");
        assertCondition(allSorted.getItems().get(0).overallTrustScore() >= allSorted.getItems().get(1).overallTrustScore(),
                "Test 2.3: Descending trust score sort verified");

        PaginatedResult<SupplierSearchService.SupplierSearchResult> keywordSearch =
                searchService.search("Fasteners", "", TrustBand.VERIFIED, 10000, "name", true, 1, 10);
        assertCondition(keywordSearch.getTotalCount() == 1
                        && keywordSearch.getItems().get(0).legalName().contains("Aravind"),
                "Test 2.4: Multi-facet keyword + band + capacity filter matched Aravind Fasteners");

        // Test 3: 4-layer verification spine & trust evaluation
        TrustEvaluationService trustService = new TrustEvaluationService(supplierRepo, verificationRepo);
        VerificationService verificationService = new VerificationService(verificationRepo, trustService, auditRepo);

        Organisation aravindOrg = supplierRepo.findAllOrganisations().stream()
                .filter(o -> o.getLegalName().contains("Aravind"))
                .findFirst()
                .orElseThrow();

        long auditBefore = auditRepo.count();
        Evidence sampleEvidence = new Evidence(
                UUID.randomUUID(),
                "storage://test/iatf-cert.pdf",
                EvidenceKind.PDF,
                "IATF 16949 Certificate",
                Instant.now().minus(Duration.ofDays(10)),
                Instant.now().plus(Duration.ofDays(365))
        );
        Claim submittedClaim = verificationService.submitClaimWithEvidence(
                aravindOrg.getId(),
                aravindOrg.getLegalName(),
                "iatf_16949_test",
                "IATF 16949 Certification",
                Pillar.QUALITY,
                "CERT-IATF-9901",
                sampleEvidence
        );
        assertCondition(submittedClaim.getStatus() == ClaimStatus.SUBMITTED,
                "Test 3.1: Claim transitioned to SUBMITTED on intake");

        VerificationTask newTask = verificationRepo.findPendingTasks().stream()
                .filter(t -> t.getClaimId().equals(submittedClaim.getId()))
                .findFirst()
                .orElse(null);
        assertCondition(newTask != null, "Test 3.2: VerificationTask automatically enqueued for submitted claim");

        boolean decided = verificationService.processVerifierDecision(
                newTask.getId(),
                "Verifier #01",
                VerificationMethod.HUMAN_AUDIT,
                CheckOutcome.PASS,
                "Verified IATF certificate registry checksum and facility scope.",
                Duration.ofDays(365)
        );
        assertCondition(decided, "Test 3.3: Verifier PASS decision recorded atomically");

        Claim updatedClaim = verificationRepo.findClaimById(submittedClaim.getId()).orElseThrow();
        Optional<Attestation> attestationOpt = verificationRepo.findActiveAttestationByClaim(submittedClaim.getId());
        assertCondition(updatedClaim.getStatus() == ClaimStatus.VERIFIED,
                "Test 3.4: Claim status updated to VERIFIED after PASS check");
        assertCondition(attestationOpt.isPresent() && attestationOpt.get().getState() == AttestationState.ACTIVE,
                "Test 3.5: Active time-bounded Attestation issued");

        TrustSnapshot recomputed = trustService.evaluateSupplier(aravindOrg.getId());
        assertCondition(recomputed.getOverallScore() > 0.0,
                "Test 3.6: TrustEvaluationService recomputed supplier trust snapshot");

        List<AuditEvent> recentAudit = auditRepo.findRecentEvents(5);
        assertCondition(auditRepo.count() >= auditBefore + 2
                        && recentAudit.get(0).getSequenceNumber() > recentAudit.get(1).getSequenceNumber(),
                "Test 3.7: Monotonic AuditEvent sequence numbers verified");

        // Test 4: Producer-consumer CustomTaskQueue (wait/notifyAll)
        CustomTaskQueue<String> pcQueue = new CustomTaskQueue<>(2);
        List<String> consumed = new ArrayList<>();
        Thread producer = new Thread(() -> {
            try {
                pcQueue.produce("item-1");
                pcQueue.produce("item-2");
                pcQueue.produce("item-3");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Test-Producer");

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 3; i++) {
                    String item = pcQueue.consume();
                    if (item != null) consumed.add(item);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Test-Consumer");

        try {
            producer.start();
            consumer.start();
            producer.join(3000);
            consumer.join(3000);
            pcQueue.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            assertCondition(false, "Test 4: Producer-Consumer interrupted unexpectedly");
        }
        assertCondition(consumed.size() == 3 && consumed.get(0).equals("item-1") && consumed.get(2).equals("item-3"),
                "Test 4: Bounded CustomTaskQueue producer-consumer coordination with wait/notifyAll passed");

        // Test 5: PriorityBlockingQueue heap re-sort escalation + AlertBuffer
        PriorityBlockingQueue<VerificationTask> pq = verificationRepo.getTaskPriorityQueue();
        VerificationTask lowPriorityTask = verificationRepo.findPendingTasks().stream()
                .filter(t -> t.getSupplierName().contains("Kavach"))
                .findFirst()
                .orElse(null);
        assertCondition(lowPriorityTask != null, "Test 5.1: Located pending task for heap re-sort test");
        pq.remove(lowPriorityTask);
        lowPriorityTask.escalate(200);
        pq.offer(lowPriorityTask);
        assertCondition(pq.peek() == lowPriorityTask && lowPriorityTask.getState() == TaskState.ESCALATED,
                "Test 5.2: Removing, escalating (+200), and re-offering restored PriorityBlockingQueue heap head");

        AlertBuffer.getInstance().drainAlerts();
        AlertBuffer.getInstance().postAlert("Test escalation alert");
        List<String> drainedAlerts = AlertBuffer.getInstance().drainAlerts();
        assertCondition(drainedAlerts.size() == 1 && drainedAlerts.get(0).contains("Test escalation alert"),
                "Test 5.3: AlertBuffer safely queued and drained background alert");

        // Prepare clean test directory under data/
        File dataDir = new File("data");
        if (!dataDir.exists()) dataDir.mkdirs();

        // Test 6: Character streams CSV export & import round-trip
        File tempCsv = new File(dataDir, "test_suppliers_roundtrip.csv");
        tempCsv.delete();
        try {
            Map<UUID, Organisation> orgMap = new HashMap<>();
            for (Organisation org : supplierRepo.findAllOrganisations()) {
                orgMap.put(org.getId(), org);
            }
            CsvDataExporterImporter.exportSuppliersToCsv(tempCsv, supplierRepo.findAllProfiles(), orgMap);
            List<CsvDataExporterImporter.ExportRecord> imported = CsvDataExporterImporter.importSuppliersFromCsv(tempCsv);
            assertCondition(imported.size() == 5,
                    "Test 6: Character-stream CSV export & import round-trip preserved all 5 suppliers");
        } catch (Exception e) {
            assertCondition(false, "Test 6 failed with exception: " + e.getMessage());
        } finally {
            tempCsv.delete();
        }

        // Test 7: Byte streams binary audit write & magic header read verification
        File tempBin = new File(dataDir, "test_audit_ledger.bin");
        tempBin.delete();
        try {
            AuditByteStreamHandler.appendBinaryRecord(tempBin, "test.action", "payload-alpha");
            AuditByteStreamHandler.appendBinaryRecord(tempBin, "test.verify", "payload-beta");
            List<AuditByteStreamHandler.BinaryAuditRecord> binRecords = AuditByteStreamHandler.readBinaryRecords(tempBin);
            assertCondition(binRecords.size() == 2 && binRecords.get(1).payload().equals("payload-beta"),
                    "Test 7: Byte-stream binary audit write & SPFY magic header read verified");
        } catch (Exception e) {
            assertCondition(false, "Test 7 failed with exception: " + e.getMessage());
        } finally {
            tempBin.delete();
        }

        // Test 8: StreamTokenizer rule parsing
        try {
            List<RuleStreamTokenizer.Token> tokens = RuleStreamTokenizer.tokenizeCriteria("MIN_SCORE >= 70 AND ACTIVE == 1");
            assertCondition(tokens.size() == 9
                            && tokens.get(0).type() == StreamTokenizer.TT_WORD
                            && tokens.get(0).sval().equals("MIN_SCORE")
                            && tokens.get(3).type() == StreamTokenizer.TT_NUMBER
                            && tokens.get(3).nval() == 70.0,
                    "Test 8: StreamTokenizer parsed rule words, numbers, and comparison operators");
        } catch (Exception e) {
            assertCondition(false, "Test 8 failed with exception: " + e.getMessage());
        }

        // Test 9: Custom exception hierarchy verification
        SupplierNotFoundException snfe = new SupplierNotFoundException("Supplier missing");
        VerificationException ve = new VerificationException("Verification blocked", new IllegalStateException("cause"));
        InvalidDataException ide = new InvalidDataException("Invalid field");
        assertCondition(snfe instanceof Exception && ve instanceof Exception && ide instanceof Exception,
                "Test 9: Custom exception classes extend java.lang.Exception cleanly");

        // Test 10: GenericRepository<T extends Identifiable<ID>, ID> type safety & wildcard predicate
        GenericRepository<Organisation, UUID> orgGenericRepo = new GenericRepository<>() {
            private final Map<UUID, Organisation> store = new HashMap<>();

            @Override
            public Organisation save(Organisation entity) {
                store.put(entity.getId(), entity);
                return entity;
            }

            @Override
            public Optional<Organisation> findById(UUID id) {
                return Optional.ofNullable(store.get(id));
            }

            @Override
            public List<Organisation> findAll() {
                return new ArrayList<>(store.values());
            }

            @Override
            public List<Organisation> findMatching(Predicate<? super Organisation> predicate) {
                List<Organisation> matched = new ArrayList<>();
                for (Organisation o : store.values()) {
                    if (predicate.test(o)) matched.add(o);
                }
                return matched;
            }

            @Override
            public boolean deleteById(UUID id) {
                return store.remove(id) != null;
            }

            @Override
            public long count() {
                return store.size();
            }
        };
        orgGenericRepo.save(aravindOrg);
        assertCondition(orgGenericRepo.count() == 1
                        && orgGenericRepo.findById(aravindOrg.getId()).isPresent()
                        && orgGenericRepo.findMatching(o -> o.getLocation().contains("Coimbatore")).size() == 1
                        && orgGenericRepo.deleteById(aravindOrg.getId())
                        && orgGenericRepo.count() == 0,
                "Test 10: GenericRepository<Organisation, UUID> CRUD and wildcard Predicate verified");

        System.out.println("\n>>> ALL SUPIFY BACKEND VERIFICATION TESTS PASSED SUCCESSFULLY! <<<");
    }

    private static void assertCondition(boolean condition, String message) {
        if (condition) {
            System.out.println("  [PASS] " + message);
        } else {
            System.err.println("  [FAIL] " + message);
            System.exit(1);
        }
    }
}
