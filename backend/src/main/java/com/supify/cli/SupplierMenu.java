package com.supify.cli;

import com.supify.dao.SupplierRepository;
import com.supify.dao.VerificationRepository;
import com.supify.model.Attestation;
import com.supify.model.Claim;
import com.supify.model.Evidence;
import com.supify.model.Organisation;
import com.supify.model.SupplierProfile;
import com.supify.model.enums.EvidenceKind;
import com.supify.model.enums.Pillar;
import com.supify.service.VerificationService;
import com.supify.util.TableFormatter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SupplierMenu {
    private final ConsoleView view;
    private final SessionContext session;
    private final SupplierRepository supplierRepo;
    private final VerificationRepository verificationRepo;
    private final VerificationService verificationService;

    public SupplierMenu(ConsoleView view, SessionContext session, SupplierRepository supplierRepo, VerificationRepository verificationRepo, VerificationService verificationService) {
        this.view = view;
        this.session = session;
        this.supplierRepo = supplierRepo;
        this.verificationRepo = verificationRepo;
        this.verificationService = verificationService;
    }

    public void display() {
        // Pick primary supplier for demonstration (Aravind Fasteners)
        List<SupplierProfile> profiles = supplierRepo.findAllProfiles();
        if (profiles.isEmpty()) return;
        SupplierProfile profile = profiles.get(0);
        Organisation org = supplierRepo.findOrganisationById(profile.getOrganisationId()).orElse(null);

        while (true) {
            view.printBanner(session);
            System.out.println(" SUPPLIER WORKSPACE COMMANDS (" + (org != null ? org.getLegalName() : "Supplier Portal") + "):");
            System.out.println("  [1] View Onboarding Profile & Standings");
            System.out.println("  [2] Submit New Claim & Evidence (4-Step Intake Wizard)");
            System.out.println("  [3] View Active Attestation Decay Countdowns");
            System.out.println("  [0] Return to Main Menu");
            System.out.println();

            int choice = view.promptInt("Select an option", 0);
            if (choice == 0) break;

            switch (choice) {
                case 1 -> handleViewProfile(profile, org);
                case 2 -> handleWizardClaimSubmit(profile, org);
                case 3 -> handleViewDecayCountdowns(profile);
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void handleViewProfile(SupplierProfile profile, Organisation org) {
        System.out.println(ConsoleView.BOLD + "\n--- SUPPLIER PROFILE SUMMARY ---" + ConsoleView.RESET);
        System.out.println("  Legal Name:    " + (org != null ? org.getLegalName() : "Unknown"));
        System.out.println("  Trade Name:    " + (org != null ? org.getTradeName() : "N/A"));
        System.out.println("  Category:      " + profile.getCategory());
        System.out.println("  Capacity:      " + profile.getCapacityDisplay());
        System.out.println("  MOQ:           " + String.format("%,d", profile.getMinimumOrderQuantity()));
        System.out.println("  Lead Time:     " + profile.getLeadTime());
        System.out.println("  Trust Band:    " + profile.getTrustBand().toFormattedString());
        System.out.println("  Trust Score:   " + String.format("%.3f", profile.getOverallTrustScore()));
        view.pressEnterToContinue();
    }

    private void handleWizardClaimSubmit(SupplierProfile profile, Organisation org) {
        System.out.println("\n" + ConsoleView.BOLD + ConsoleView.CYAN + "═".repeat(82) + ConsoleView.RESET);
        System.out.println(ConsoleView.BOLD + "   SUPPLIER INTAKE WIZARD :: Step-by-Step Claim Assertion" + ConsoleView.RESET);
        System.out.println(ConsoleView.BOLD + ConsoleView.CYAN + "═".repeat(82) + ConsoleView.RESET);

        // Step 1: Select Pillar Standard
        System.out.println("\n" + ConsoleView.BOLD + "[STEP 1/4] Select Audit Pillar Dimension:" + ConsoleView.RESET);
        Pillar[] pillars = Pillar.values();
        for (int i = 0; i < pillars.length; i++) {
            System.out.printf("  [%d] %s (Weight: %.1fx)\n", i + 1, pillars[i].getLabel(), pillars[i].getDefaultWeight());
        }
        int pChoice = view.promptInt("Choose pillar number", 4);
        Pillar chosenPillar = (pChoice >= 1 && pChoice <= pillars.length) ? pillars[pChoice - 1] : Pillar.QUALITY;

        // Step 2: Assert Claim
        System.out.println("\n" + ConsoleView.BOLD + "[STEP 2/4] Assert Credential / Capability Details:" + ConsoleView.RESET);
        String label = view.promptString("Claim Title / Certification Name (e.g. IATF 16949 Automotive Quality)");
        if (label.isEmpty()) label = "ISO 45001 Occupational Health & Safety";
        String value = view.promptString("Asserted Value / Certificate Identifier (e.g. Reg #OHS-2026-8812)");
        if (value.isEmpty()) value = "Standard Certified by DNV";

        // Step 3: Evidence Document Attachment
        System.out.println("\n" + ConsoleView.BOLD + "[STEP 3/4] Attach Supporting Evidentiary Artifact:" + ConsoleView.RESET);
        String docTitle = view.promptString("Document Title (e.g. Audit Certificate PDF)");
        if (docTitle.isEmpty()) docTitle = label + " Certificate";
        String refUri = "storage://proofs/" + System.currentTimeMillis() + ".pdf";

        Evidence evidence = new Evidence(
                UUID.randomUUID(),
                refUri,
                EvidenceKind.PDF,
                docTitle,
                Instant.now().minus(Duration.ofDays(30)),
                Instant.now().plus(Duration.ofDays(365))
        );

        // Step 4: Confirmation & Submission
        System.out.println("\n" + ConsoleView.BOLD + "[STEP 4/4] Review & Enqueue into Verifier Task Queue:" + ConsoleView.RESET);
        System.out.println("  Pillar:       " + chosenPillar.getLabel());
        System.out.println("  Claim:        " + label);
        System.out.println("  Value:        " + value);
        System.out.println("  Evidence:     " + docTitle + " (" + refUri + ")");

        String confirm = view.promptString("Submit claim to Verifier Queue? (Y/N)");
        if ("y".equalsIgnoreCase(confirm) || "yes".equalsIgnoreCase(confirm)) {
            Claim submittedClaim = verificationService.submitClaimWithEvidence(
                    profile.getOrganisationId(),
                    org != null ? org.getLegalName() : "Aravind Fasteners",
                    label.toLowerCase().replace(" ", "_"),
                    label,
                    chosenPillar,
                    value,
                    evidence
            );
            System.out.println(ConsoleView.GREEN + ConsoleView.BOLD + "\n✓ Claim submitted successfully!" + ConsoleView.RESET);
            System.out.println("Task has been prioritized and enqueued into PriorityBlockingQueue for verifier review.");
        } else {
            System.out.println(ConsoleView.YELLOW + "Claim submission cancelled." + ConsoleView.RESET);
        }
        view.pressEnterToContinue();
    }

    private void handleViewDecayCountdowns(SupplierProfile profile) {
        List<Claim> claims = verificationRepo.findClaimsByOrganisation(profile.getOrganisationId());
        System.out.println(ConsoleView.BOLD + "\n--- ACTIVE ATTESTATION DECAY STATUS ---" + ConsoleView.RESET);

        List<String> headers = Arrays.asList("CLAIM", "PILLAR", "METHOD", "VERIFIED AT", "VALID UNTIL", "DECAY TIMEOUT / REMAINING");
        List<List<String>> rows = new ArrayList<>();

        for (Claim c : claims) {
            Optional<Attestation> attOpt = verificationRepo.findActiveAttestationByClaim(c.getId());
            if (attOpt.isPresent()) {
                Attestation a = attOpt.get();
                rows.add(Arrays.asList(
                        c.getLabel(),
                        c.getPillar().name(),
                        a.getMethodAchieved().getLabel(),
                        a.getVerifiedAt().toString().substring(0, 10),
                        a.getValidUntil() != null ? a.getValidUntil().toString().substring(0, 19).replace('T', ' ') : "Indefinite",
                        a.getDecayCountdownString()
                ));
            }
        }

        if (rows.isEmpty()) {
            System.out.println(ConsoleView.YELLOW + "No active attestations found for this profile." + ConsoleView.RESET);
        } else {
            System.out.println(TableFormatter.renderTable(headers, rows));
        }
        view.pressEnterToContinue();
    }
}
