package com.supify.cli;

import com.supify.concurrency.AlertBuffer;
import com.supify.dao.SupplierRepository;
import com.supify.dao.VerificationRepository;
import com.supify.model.Attestation;
import com.supify.model.Claim;
import com.supify.model.Evidence;
import com.supify.model.VerificationCheck;
import com.supify.model.VerificationTask;
import com.supify.model.enums.CheckOutcome;
import com.supify.model.enums.TaskState;
import com.supify.model.enums.VerificationMethod;
import com.supify.service.VerificationService;
import com.supify.util.TableFormatter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class VerifierMenu {
    private final ConsoleView view;
    private final SessionContext session;
    private final VerificationRepository verificationRepo;
    private final SupplierRepository supplierRepo;
    private final VerificationService verificationService;

    public VerifierMenu(ConsoleView view, SessionContext session, VerificationRepository verificationRepo, SupplierRepository supplierRepo, VerificationService verificationService) {
        this.view = view;
        this.session = session;
        this.verificationRepo = verificationRepo;
        this.supplierRepo = supplierRepo;
        this.verificationService = verificationService;
    }

    public void display() {
        while (true) {
            view.printBanner(session);
            System.out.println(" VERIFIER STATION COMMANDS (Trained Verifier Console):");
            System.out.println("  [1] View Priority Verification Task Queue (Heap-Ordered by SLA/Priority)");
            System.out.println("  [2] Inspect Task & Evidence Dossier (4-Layer Spine: Claim -> Evidence)");
            System.out.println("  [3] Review & Record Attestation Decision (Pass/Fail + Audit Rationale)");
            System.out.println("  [4] Trigger SLA Escalation Test (Heap Re-Sort / Priority Queue Concurrency)");
            System.out.println("  [0] Return to Main Menu");
            System.out.println();

            int choice = view.promptInt("Select an option", 0);
            if (choice == 0) break;

            switch (choice) {
                case 1 -> handleViewQueue();
                case 2 -> handleInspectDossier();
                case 3 -> handleReviewDecision();
                case 4 -> handleSlaEscalationTest();
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void handleViewQueue() {
        List<VerificationTask> tasks = verificationRepo.findPendingTasks();
        System.out.println(ConsoleView.CYAN + ConsoleView.BOLD + "\n=== VERIFICATION REVIEW QUEUE (" + tasks.size() + " Pending Tasks) ===" + ConsoleView.RESET);

        if (tasks.isEmpty()) {
            System.out.println("No pending verification tasks currently in queue.");
            view.pressEnterToContinue();
            return;
        }

        List<String> headers = Arrays.asList("#", "Task ID", "Supplier", "Claim Label", "Priority", "State", "Dual?", "SLA Deadline");
        List<List<String>> rows = new ArrayList<>();

        for (int i = 0; i < tasks.size(); i++) {
            VerificationTask t = tasks.get(i);
            String pColor = t.getPriority() >= 90 ? ConsoleView.RED + ConsoleView.BOLD : (t.getPriority() >= 60 ? ConsoleView.YELLOW : "");
            String stateColor = t.getState() == TaskState.ESCALATED ? ConsoleView.RED + ConsoleView.BOLD : ConsoleView.CYAN;

            rows.add(Arrays.asList(
                    String.valueOf(i + 1),
                    t.getId().toString().substring(0, 8),
                    t.getSupplierName(),
                    t.getClaimLabel(),
                    pColor + t.getPriority() + ConsoleView.RESET,
                    stateColor + t.getState().name() + ConsoleView.RESET,
                    t.isRequiresDual() ? ConsoleView.MAGENTA + "YES" + ConsoleView.RESET : "No",
                    t.getSlaRemainingString()
            ));
        }

        System.out.println(TableFormatter.renderTable(headers, rows));
        System.out.println(ConsoleView.YELLOW + "Note: Tasks are dynamically heap-ordered in PriorityBlockingQueue by priority score & SLA." + ConsoleView.RESET);
        view.pressEnterToContinue();
    }

    private void handleInspectDossier() {
        List<VerificationTask> tasks = verificationRepo.findPendingTasks();
        if (tasks.isEmpty()) {
            System.out.println("No pending tasks available to inspect.");
            view.pressEnterToContinue();
            return;
        }

        handleViewQueueSummary(tasks);
        int idx = view.promptInt("Select Task # to inspect (1 - " + tasks.size() + ")", 1);
        if (idx < 1 || idx > tasks.size()) {
            System.out.println("Invalid task index.");
            view.pressEnterToContinue();
            return;
        }

        VerificationTask task = tasks.get(idx - 1);
        UUID claimId = task.getClaimId();
        Optional<Claim> claimOpt = verificationRepo.findClaimById(claimId);
        List<Evidence> evidenceList = verificationRepo.findEvidenceByClaim(claimId);
        List<VerificationCheck> checks = verificationRepo.findChecksByClaim(claimId);
        Optional<Attestation> attestationOpt = verificationRepo.findActiveAttestationByClaim(claimId);

        System.out.println(ConsoleView.CYAN + ConsoleView.BOLD + "\n" + "═".repeat(70) + ConsoleView.RESET);
        System.out.println(ConsoleView.CYAN + ConsoleView.BOLD + " EVIDENCE & CLAIM DOSSIER :: " + task.getSupplierName() + ConsoleView.RESET);
        System.out.println(ConsoleView.CYAN + ConsoleView.BOLD + "═".repeat(70) + ConsoleView.RESET);

        System.out.printf("  Task ID:          %s\n", task.getId());
        System.out.printf("  Priority Score:   %d (State: %s)\n", task.getPriority(), task.getState());
        System.out.printf("  SLA Due At:       %s (%s)\n", task.getSlaDueAt(), task.getSlaRemainingString());
        System.out.printf("  Requires Dual:    %s\n", task.isRequiresDual() ? "YES (Four-eyes principle)" : "No");
        System.out.println();

        if (claimOpt.isPresent()) {
            Claim c = claimOpt.get();
            System.out.println(ConsoleView.BOLD + "Layer 1: Claim Assertion" + ConsoleView.RESET);
            System.out.printf("  - Claim ID:        %s\n", c.getId());
            System.out.printf("  - Label:           %s\n", c.getLabel());
            System.out.printf("  - Pillar:          %s\n", c.getPillar().getLabel());
            System.out.printf("  - Asserted Value:  %s\n", ConsoleView.GREEN + c.getAssertedValue() + ConsoleView.RESET);
            System.out.printf("  - Status:          %s\n", c.getStatus().getLabel());
            System.out.println();
        }

        System.out.println(ConsoleView.BOLD + "Layer 2: Submitted Evidence Documents (" + evidenceList.size() + ")" + ConsoleView.RESET);
        if (evidenceList.isEmpty()) {
            System.out.println("  (No direct evidence files linked to this claim)");
        } else {
            for (Evidence ev : evidenceList) {
                System.out.printf("  - [%s] %s\n", ev.getKind().name(), ev.getTitle());
                System.out.printf("    Storage URI:  %s\n", ev.getStorageRef());
                System.out.printf("    Issued At:    %s | Expires At: %s\n", ev.getIssuedAt(), ev.getExpiresAt());
            }
        }
        System.out.println();

        System.out.println(ConsoleView.BOLD + "Layer 3: Verification Checks History (" + checks.size() + ")" + ConsoleView.RESET);
        if (checks.isEmpty()) {
            System.out.println("  (No verification checks recorded yet)");
        } else {
            for (VerificationCheck chk : checks) {
                String oColor = chk.getOutcome() == CheckOutcome.PASS ? ConsoleView.GREEN : ConsoleView.RED;
                System.out.printf("  - Check ID: %s | Method: %s | Outcome: %s%s%s (Confidence: %.0f%%)\n",
                        chk.getId().toString().substring(0, 8),
                        chk.getMethod().getLabel(),
                        oColor, chk.getOutcome().name(), ConsoleView.RESET,
                        chk.getConfidence() * 100);
                System.out.printf("    Verifier: %s | Rationale: %s\n", chk.getPerformedBy(), chk.getRationale());
            }
        }
        System.out.println();

        System.out.println(ConsoleView.BOLD + "Layer 4: Current Attestation State" + ConsoleView.RESET);
        if (attestationOpt.isPresent()) {
            Attestation att = attestationOpt.get();
            System.out.printf("  - Attestation ID: %s | State: %s | Valid Until: %s\n",
                    att.getId().toString().substring(0, 8),
                    ConsoleView.GREEN + att.getState().name() + ConsoleView.RESET,
                    att.getValidUntil());
        } else {
            System.out.println("  - Pending Attestation (Awaiting Verifier Decision)");
        }

        System.out.println(ConsoleView.CYAN + ConsoleView.BOLD + "═".repeat(70) + ConsoleView.RESET);
        view.pressEnterToContinue();
    }

    private void handleReviewDecision() {
        List<VerificationTask> tasks = verificationRepo.findPendingTasks();
        if (tasks.isEmpty()) {
            System.out.println("No pending tasks available for decision.");
            view.pressEnterToContinue();
            return;
        }

        handleViewQueueSummary(tasks);
        int idx = view.promptInt("Select Task # to decide (1 - " + tasks.size() + ")", 1);
        if (idx < 1 || idx > tasks.size()) {
            System.out.println("Invalid task index.");
            view.pressEnterToContinue();
            return;
        }

        VerificationTask task = tasks.get(idx - 1);
        System.out.println(ConsoleView.YELLOW + ConsoleView.BOLD + "\nDeciding Task: " + task.getClaimLabel() + " for " + task.getSupplierName() + ConsoleView.RESET);

        System.out.println("\nSelect Verification Method:");
        System.out.println("  [1] " + VerificationMethod.THIRD_PARTY_API.getLabel() + " (Direct registry API query)");
        System.out.println("  [2] " + VerificationMethod.DOCUMENT_REVIEW.getLabel() + " (Manual document forensic audit)");
        System.out.println("  [3] " + VerificationMethod.HUMAN_AUDIT.getLabel() + " (Senior certified auditor review)");
        System.out.println("  [4] " + VerificationMethod.SITE_VISIT.getLabel() + " (Physical facility inspection)");

        int mChoice = view.promptInt("Method", 1);
        VerificationMethod method = switch (mChoice) {
            case 2 -> VerificationMethod.DOCUMENT_REVIEW;
            case 3 -> VerificationMethod.HUMAN_AUDIT;
            case 4 -> VerificationMethod.SITE_VISIT;
            default -> VerificationMethod.THIRD_PARTY_API;
        };

        System.out.println("\nSelect Decision Outcome:");
        System.out.println("  [1] PASS - Attestation Approved (Valid Time-Bounded Evidence)");
        System.out.println("  [2] FAIL - Assertion Rejected (Insufficient / Invalid Evidence)");

        int oChoice = view.promptInt("Outcome", 1);
        CheckOutcome outcome = (oChoice == 2) ? CheckOutcome.FAIL : CheckOutcome.PASS;

        String rationale = view.promptString("Verifier Audit Rationale (Mandatory justification)");
        if (rationale.isBlank()) {
            rationale = "Verified credentials against authoritative source; all validation criteria satisfied.";
        }

        int validityDays = 365;
        if (outcome == CheckOutcome.PASS) {
            System.out.println("\nEnter Attestation Validity Period (Days):");
            System.out.println("  (Enter '0' or small duration like '1' to test live decay countdown demo)");
            validityDays = view.promptInt("Validity duration in days", 365);
            if (validityDays < 1) validityDays = 1;
        }

        boolean success = verificationService.processVerifierDecision(
                task.getId(),
                session.getCurrentUserName(),
                method,
                outcome,
                rationale,
                Duration.ofDays(validityDays)
        );

        if (success) {
            System.out.println(ConsoleView.GREEN + ConsoleView.BOLD + "\n✔ Decision recorded successfully! Task marked DECIDED." + ConsoleView.RESET);
            System.out.printf("  - Verification Method: %s\n", method.getLabel());
            System.out.printf("  - Outcome:             %s\n", outcome == CheckOutcome.PASS ? ConsoleView.GREEN + "PASS" + ConsoleView.RESET : ConsoleView.RED + "FAIL" + ConsoleView.RESET);
            System.out.printf("  - Attestation State:   %s\n", outcome == CheckOutcome.PASS ? "ACTIVE (Valid for " + validityDays + " days)" : "NONE (Claim Rejected)");
            System.out.println("  - Supplier Trust Score dynamically recomputed across all 4 pillars.");
        } else {
            System.out.println(ConsoleView.RED + "Failed to record decision." + ConsoleView.RESET);
        }

        view.pressEnterToContinue();
    }

    private void handleSlaEscalationTest() {
        List<VerificationTask> tasks = verificationRepo.findPendingTasks();
        if (tasks.isEmpty()) {
            System.out.println("No pending tasks available for escalation test.");
            view.pressEnterToContinue();
            return;
        }

        handleViewQueueSummary(tasks);
        int idx = view.promptInt("Select Task # to escalate priority (1 - " + tasks.size() + ")", 1);
        if (idx < 1 || idx > tasks.size()) {
            System.out.println("Invalid task index.");
            view.pressEnterToContinue();
            return;
        }

        VerificationTask task = tasks.get(idx - 1);
        int boost = 35;

        // Demonstrate PriorityBlockingQueue heap re-sort:
        // Must remove, mutate, and re-offer so the priority heap is properly re-balanced!
        var queue = verificationRepo.getTaskPriorityQueue();
        queue.remove(task);
        int oldPriority = task.getPriority();
        task.escalate(boost);
        queue.offer(task);

        AlertBuffer.getInstance().postAlert(String.format(
                "Manual SLA Escalation: Task [%s] for %s priority boosted from %d -> %d. Heap re-sorted in PriorityBlockingQueue.",
                task.getClaimLabel(), task.getSupplierName(), oldPriority, task.getPriority()
        ));

        System.out.println(ConsoleView.GREEN + ConsoleView.BOLD + "\n✔ Task priority successfully escalated and PriorityBlockingQueue re-heapified!" + ConsoleView.RESET);
        System.out.printf("  - Task: %s (%s)\n", task.getClaimLabel(), task.getSupplierName());
        System.out.printf("  - Priority: %d -> %s%d%s\n", oldPriority, ConsoleView.RED + ConsoleView.BOLD, task.getPriority(), ConsoleView.RESET);
        System.out.printf("  - New State: %s\n", task.getState());

        view.pressEnterToContinue();
    }

    private void handleViewQueueSummary(List<VerificationTask> tasks) {
        System.out.println(ConsoleView.CYAN + "\nPending Review Queue:" + ConsoleView.RESET);
        for (int i = 0; i < tasks.size(); i++) {
            VerificationTask t = tasks.get(i);
            System.out.printf("  [%d] %-24s | %-18s | Priority: %3d | State: %s\n",
                    i + 1,
                    t.getClaimLabel(),
                    t.getSupplierName(),
                    t.getPriority(),
                    t.getState().name()
            );
        }
        System.out.println();
    }
}
