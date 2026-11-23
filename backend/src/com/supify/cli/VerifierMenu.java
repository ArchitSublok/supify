package com.supify.cli;

import com.supify.concurrency.AlertBuffer;
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
    private final VerificationService verificationService;

    public VerifierMenu(ConsoleView view, SessionContext session, VerificationRepository verificationRepo, VerificationService verificationService) {
        this.view = view;
        this.session = session;
        this.verificationRepo = verificationRepo;
        this.verificationService = verificationService;
    }

    public void display() {
        while (view.hasNext()) {
            view.printBanner(session);
            System.out.println(" VERIFIER CONSOLE:");
            System.out.println("  [1] View Pending Queue");
            System.out.println("  [2] Inspect Task & Evidence");
            System.out.println("  [3] Approve or Reject Task");
            System.out.println("  [4] Boost Task Priority (SLA Escalation Demo)");
            System.out.println("  [0] Back");
            System.out.println();

            int choice = view.promptInt("Choose an option", 0);
            if (choice == 0) break;

            switch (choice) {
                case 1 -> handleViewQueue();
                case 2 -> handleInspectTask();
                case 3 -> handleDecideTask();
                case 4 -> handleBoostPriority();
                default -> view.printWarn("Please enter a valid option (0-4).");
            }
        }
    }

    private void handleViewQueue() {
        List<VerificationTask> tasks = verificationRepo.findPendingTasks();
        view.printSection("PENDING VERIFICATION QUEUE (" + tasks.size() + " tasks)");

        if (tasks.isEmpty()) {
            view.printWarn("Queue is empty.");
            view.pressEnterToContinue();
            return;
        }

        List<String> headers = Arrays.asList("#", "TASK ID", "SUPPLIER", "CLAIM", "PRIORITY", "STATE", "DUAL?", "SLA LEFT");
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
        view.pressEnterToContinue();
    }

    private void handleInspectTask() {
        List<VerificationTask> tasks = verificationRepo.findPendingTasks();
        if (tasks.isEmpty()) {
            view.printWarn("No pending tasks to inspect.");
            view.pressEnterToContinue();
            return;
        }

        printQueueList(tasks);
        int idx = view.promptInt("Task # to inspect (1-" + tasks.size() + ")", 1);
        if (idx < 1 || idx > tasks.size()) {
            view.printWarn("Invalid task number.");
            view.pressEnterToContinue();
            return;
        }

        VerificationTask task = tasks.get(idx - 1);
        UUID claimId = task.getClaimId();
        Optional<Claim> claimOpt = verificationRepo.findClaimById(claimId);
        List<Evidence> evidenceList = verificationRepo.findEvidenceByClaim(claimId);
        List<VerificationCheck> checks = verificationRepo.findChecksByClaim(claimId);
        Optional<Attestation> attOpt = verificationRepo.findActiveAttestationByClaim(claimId);

        view.printSection("TASK DOSSIER :: " + task.getSupplierName());
        System.out.printf("  Task ID:       %s%n", task.getId());
        System.out.printf("  Priority/State:%d (%s)%n", task.getPriority(), task.getState());
        System.out.printf("  SLA Left:      %s%n", task.getSlaRemainingString());
        System.out.printf("  Dual Review:   %s%n%n", task.isRequiresDual() ? "Required" : "Single reviewer");

        if (claimOpt.isPresent()) {
            Claim c = claimOpt.get();
            System.out.println(ConsoleView.BOLD + "1. Claim Assertion" + ConsoleView.RESET);
            System.out.printf("   Title:   %s (%s)%n", c.getLabel(), c.getPillar().getLabel());
            System.out.printf("   Value:   %s%s%s%n", ConsoleView.GREEN, c.getAssertedValue(), ConsoleView.RESET);
            System.out.printf("   Status:  %s%n%n", c.getStatus().getLabel());
        }

        System.out.println(ConsoleView.BOLD + "2. Submitted Evidence (" + evidenceList.size() + ")" + ConsoleView.RESET);
        if (evidenceList.isEmpty()) {
            System.out.println("   (None attached)");
        } else {
            for (Evidence ev : evidenceList) {
                System.out.printf("   - [%s] %s (%s)%n", ev.getKind().name(), ev.getTitle(), ev.getStorageRef());
                System.out.printf("     Issued: %s | Expires: %s%n", ev.getIssuedAt(), ev.getExpiresAt());
            }
        }
        System.out.println();

        System.out.println(ConsoleView.BOLD + "3. Prior Checks (" + checks.size() + ")" + ConsoleView.RESET);
        if (checks.isEmpty()) {
            System.out.println("   (No checks recorded yet)");
        } else {
            for (VerificationCheck chk : checks) {
                System.out.printf("   - %s | %s | By: %s | Note: %s%n",
                        chk.getMethod().getLabel(), chk.getOutcome().name(), chk.getPerformedBy(), chk.getRationale());
            }
        }
        System.out.println();

        System.out.println(ConsoleView.BOLD + "4. Active Attestation" + ConsoleView.RESET);
        if (attOpt.isPresent()) {
            Attestation att = attOpt.get();
            System.out.printf("   State: %s | Valid Until: %s%n", att.getState().name(), att.getValidUntil());
        } else {
            System.out.println("   None (awaiting verifier decision)");
        }

        view.pressEnterToContinue();
    }

    private void handleDecideTask() {
        List<VerificationTask> tasks = verificationRepo.findPendingTasks();
        if (tasks.isEmpty()) {
            view.printWarn("No pending tasks to decide.");
            view.pressEnterToContinue();
            return;
        }

        printQueueList(tasks);
        int idx = view.promptInt("Task # to decide (1-" + tasks.size() + ")", 1);
        if (idx < 1 || idx > tasks.size()) {
            view.printWarn("Invalid task number.");
            view.pressEnterToContinue();
            return;
        }

        VerificationTask task = tasks.get(idx - 1);
        view.printSection("RECORD DECISION :: " + task.getClaimLabel() + " (" + task.getSupplierName() + ")");

        System.out.println("Verification Method:");
        System.out.println("  [1] " + VerificationMethod.THIRD_PARTY_API.getLabel());
        System.out.println("  [2] " + VerificationMethod.DOCUMENT_REVIEW.getLabel());
        System.out.println("  [3] " + VerificationMethod.HUMAN_AUDIT.getLabel());
        System.out.println("  [4] " + VerificationMethod.SITE_VISIT.getLabel());

        int mChoice = view.promptInt("Method", 1);
        VerificationMethod method = switch (mChoice) {
            case 2 -> VerificationMethod.DOCUMENT_REVIEW;
            case 3 -> VerificationMethod.HUMAN_AUDIT;
            case 4 -> VerificationMethod.SITE_VISIT;
            default -> VerificationMethod.THIRD_PARTY_API;
        };

        System.out.println("\nDecision Outcome:");
        System.out.println("  [1] PASS (Approve and issue attestation)");
        System.out.println("  [2] FAIL (Reject claim)");
        int oChoice = view.promptInt("Outcome", 1);
        CheckOutcome outcome = (oChoice == 2) ? CheckOutcome.FAIL : CheckOutcome.PASS;

        String rationale = view.promptString("Verifier note / rationale (ENTER for default)");
        if (rationale.isBlank()) {
            rationale = "Checked evidence against registry; criteria satisfied.";
        }

        int validityDays = 365;
        if (outcome == CheckOutcome.PASS) {
            validityDays = view.promptInt("Attestation valid for (days)", 365);
            if (validityDays < 1) validityDays = 1;
        }

        boolean ok = verificationService.processVerifierDecision(
                task.getId(),
                session.getCurrentUserName(),
                method,
                outcome,
                rationale,
                Duration.ofDays(validityDays)
        );

        if (ok) {
            view.printOk("Decision saved (" + outcome.name() + " via " + method.getLabel() + "). Supplier trust score updated.");
        } else {
            view.printError("Could not record decision.");
        }
        view.pressEnterToContinue();
    }

    private void handleBoostPriority() {
        List<VerificationTask> tasks = verificationRepo.findPendingTasks();
        if (tasks.isEmpty()) {
            view.printWarn("No pending tasks to escalate.");
            view.pressEnterToContinue();
            return;
        }

        printQueueList(tasks);
        int idx = view.promptInt("Task # to escalate (1-" + tasks.size() + ")", 1);
        if (idx < 1 || idx > tasks.size()) {
            view.printWarn("Invalid task number.");
            view.pressEnterToContinue();
            return;
        }

        VerificationTask task = tasks.get(idx - 1);
        var queue = verificationRepo.getTaskPriorityQueue();
        int oldPriority = task.getPriority();

        // Remove and re-offer so PriorityBlockingQueue re-balances its heap
        queue.remove(task);
        task.escalate(35);
        queue.offer(task);

        AlertBuffer.getInstance().postAlert(String.format(
                "SLA escalation: %s (%s) boosted from priority %d to %d.",
                task.getClaimLabel(), task.getSupplierName(), oldPriority, task.getPriority()
        ));

        view.printOk("Priority boosted (" + oldPriority + " -> " + task.getPriority() + ") and heap re-sorted.");
        view.pressEnterToContinue();
    }

    private void printQueueList(List<VerificationTask> tasks) {
        System.out.println(ConsoleView.CYAN + "\nPending Tasks:" + ConsoleView.RESET);
        for (int i = 0; i < tasks.size(); i++) {
            VerificationTask t = tasks.get(i);
            System.out.printf("  [%d] %-34s | %-22s | Priority: %3d | %s%n",
                    i + 1, t.getClaimLabel(), t.getSupplierName(), t.getPriority(), t.getState().name());
        }
        System.out.println();
    }
}
