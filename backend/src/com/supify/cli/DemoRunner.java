package com.supify.cli;

import com.supify.concurrency.AlertBuffer;
import com.supify.concurrency.ThreadLifecycleMonitor;
import com.supify.concurrency.ThreadLifecycleMonitor.ThreadSnapshot;
import com.supify.dao.memory.InMemoryAuditRepository;
import com.supify.dao.memory.InMemorySupplierRepository;
import com.supify.dao.memory.InMemoryVerificationRepository;
import com.supify.io.RuleStreamTokenizer;
import com.supify.model.Attestation;
import com.supify.model.Claim;
import com.supify.model.Evidence;
import com.supify.model.Organisation;
import com.supify.model.PaginatedResult;
import com.supify.model.TrustSnapshot;
import com.supify.model.VerificationTask;
import com.supify.model.enums.CheckOutcome;
import com.supify.model.enums.EvidenceKind;
import com.supify.model.enums.Pillar;
import com.supify.model.enums.VerificationMethod;
import com.supify.service.SupplierSearchService;
import com.supify.service.SupplierSearchService.SupplierSearchResult;
import com.supify.service.TrustEvaluationService;
import com.supify.service.VerificationService;
import com.supify.util.SeedDataFactory;
import com.supify.util.TableFormatter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;

/** Repeatable 5-step guided walkthrough running on isolated in-memory repositories. */
public class DemoRunner {
    private final ConsoleView view;

    public DemoRunner(ConsoleView view) {
        this.view = view;
    }

    public void runGuidedDemo() {
        InMemorySupplierRepository supplierRepo = new InMemorySupplierRepository();
        InMemoryVerificationRepository verificationRepo = new InMemoryVerificationRepository();
        InMemoryAuditRepository auditRepo = new InMemoryAuditRepository();

        SeedDataFactory.populate(supplierRepo, verificationRepo);

        TrustEvaluationService trustService = new TrustEvaluationService(supplierRepo, verificationRepo);
        SupplierSearchService searchService = new SupplierSearchService(supplierRepo);
        VerificationService verificationService = new VerificationService(verificationRepo, trustService, auditRepo);

        view.printSection("GUIDED DEMO :: End-to-End Verification & Concurrency Walkthrough");

        // Step 1: Buyer Discovery
        System.out.println(ConsoleView.BOLD + "[1/5] BUYER DISCOVERY :: Top 3 Ranked Suppliers via Stream API" + ConsoleView.RESET);
        PaginatedResult<SupplierSearchResult> top = searchService.search("", "", null, null, "score", false, 1, 3);
        List<List<String>> supplierRows = new ArrayList<>();
        int rank = 1;
        for (SupplierSearchResult r : top.getItems()) {
            supplierRows.add(Arrays.asList(
                    String.valueOf(rank++),
                    r.legalName(),
                    r.category(),
                    r.trustBand().toFormattedString(),
                    String.format("%.3f", r.overallTrustScore())
            ));
        }
        System.out.println(TableFormatter.renderTable(
                Arrays.asList("#", "SUPPLIER", "CATEGORY", "TRUST BAND", "SCORE"),
                supplierRows
        ));

        // Step 2: Supplier Claim & Evidence Intake
        Organisation targetOrg = supplierRepo.findAllOrganisations().stream()
                .filter(o -> o.getLegalName().contains("Aravind"))
                .findFirst()
                .orElseThrow();
        System.out.println(ConsoleView.BOLD + "[2/5] SUPPLIER INTAKE :: Submitting Quality Claim + PDF Evidence" + ConsoleView.RESET);

        Evidence uploadedProof = new Evidence(
                UUID.randomUUID(),
                "storage://demo/iatf-16949-2026.pdf",
                EvidenceKind.PDF,
                "IATF 16949 Certificate (DNV Audited)",
                Instant.now().minus(Duration.ofDays(15)),
                Instant.now().plus(Duration.ofDays(365))
        );
        Claim submittedClaim = verificationService.submitClaimWithEvidence(
                targetOrg.getId(),
                targetOrg.getLegalName(),
                "iatf_16949_demo",
                "IATF 16949 Automotive Quality",
                Pillar.QUALITY,
                "CERT-IATF-2026-4401",
                uploadedProof
        );
        List<Evidence> linkedProofs = verificationRepo.findEvidenceByClaim(submittedClaim.getId());
        System.out.printf("  Supplier:        %s%n", targetOrg.getLegalName());
        System.out.printf("  Submitted Claim: %s (%s)%n", submittedClaim.getLabel(), submittedClaim.getStatus().getLabel());
        System.out.printf("  Linked Evidence: %d file(s) attached -> %s%n%n",
                linkedProofs.size(),
                linkedProofs.isEmpty() ? "None" : linkedProofs.get(0).getTitle());

        // Step 3: Verifier Approval -> Time-Bounded Attestation + Trust Update
        System.out.println(ConsoleView.BOLD + "[3/5] VERIFIER DECISION :: Approving Task & Recomputing Trust Score" + ConsoleView.RESET);
        VerificationTask demoTask = verificationRepo.findPendingTasks().stream()
                .filter(t -> t.getClaimId().equals(submittedClaim.getId()))
                .findFirst()
                .orElseThrow();
        verificationService.processVerifierDecision(
                demoTask.getId(),
                "Verifier #04 (Demo)",
                VerificationMethod.HUMAN_AUDIT,
                CheckOutcome.PASS,
                "Verified IATF certificate registry checksum and site scope.",
                Duration.ofDays(365)
        );

        Optional<Attestation> issuedAtt = verificationRepo.findActiveAttestationByClaim(submittedClaim.getId());
        TrustSnapshot updatedTrust = trustService.evaluateSupplier(targetOrg.getId());
        System.out.printf("  Attestation State: %s (%s)%n",
                issuedAtt.map(a -> a.getState().name()).orElse("MISSING"),
                issuedAtt.map(Attestation::getDecayCountdownString).orElse("N/A"));
        System.out.printf("  Updated Standing:  %s (Score: %.3f)%n%n",
                updatedTrust.getBand().toFormattedString(),
                updatedTrust.getOverallScore());

        // Step 4: Concurrency Priority Queue Re-Sort + Non-Blocking Alert Buffer
        System.out.println(ConsoleView.BOLD + "[4/5] CONCURRENCY :: Priority Queue Escalation & Buffered Alerts" + ConsoleView.RESET);
        PriorityBlockingQueue<VerificationTask> pq = verificationRepo.getTaskPriorityQueue();
        VerificationTask escalatedTarget = verificationRepo.findPendingTasks().stream()
                .filter(t -> t.getSupplierName().contains("Kavach"))
                .findFirst()
                .orElse(null);
        if (escalatedTarget != null) {
            int before = escalatedTarget.getPriority();
            pq.remove(escalatedTarget);
            escalatedTarget.escalate(200);
            pq.offer(escalatedTarget);
            VerificationTask head = pq.peek();
            System.out.printf("  Escalated Task: %s (%d -> %d) | New Heap Head: %s%n",
                    escalatedTarget.getClaimLabel(), before, escalatedTarget.getPriority(),
                    head != null ? head.getSupplierName() : "N/A");
        }
        AlertBuffer.getInstance().postAlert("Demo SLA escalation pushed Kavach CTO task to top of review queue.");
        view.checkAndPrintAlerts();

        // Step 5: StreamTokenizer Rule Parsing & Thread Lifecycle Snapshot
        System.out.println(ConsoleView.BOLD + "[5/5] I/O & THREADS :: StreamTokenizer Rules & Live Thread Count" + ConsoleView.RESET);
        String sampleRule = "MIN_CAPACITY >= 50000 AND TRUST_SCORE >= 0.70";
        try {
            List<RuleStreamTokenizer.Token> tokens = RuleStreamTokenizer.tokenizeCriteria(sampleRule);
            System.out.printf("  Rule Parsed: \"%s\" -> %d tokens%n", sampleRule, tokens.size());
        } catch (IOException e) {
            view.printError("Rule parsing error: " + e.getMessage());
        }
        List<ThreadSnapshot> threads = ThreadLifecycleMonitor.captureSupifyThreads();
        long daemonCount = threads.stream().filter(ThreadSnapshot::isDaemon).count();
        System.out.printf("  Active JVM Threads: %d total (%d background daemons)%n%n", threads.size(), daemonCount);

        view.printOk("Guided demo finished cleanly (live session state unchanged).");
        view.pressEnterToContinue();
    }
}
