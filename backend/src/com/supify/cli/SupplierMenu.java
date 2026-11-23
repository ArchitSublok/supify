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
        List<SupplierProfile> profiles = supplierRepo.findAllProfiles();
        if (profiles.isEmpty()) {
            view.printWarn("No supplier profile found.");
            return;
        }
        SupplierProfile profile = profiles.get(0);
        Organisation org = supplierRepo.findOrganisationById(profile.getOrganisationId()).orElse(null);
        String orgName = org != null ? org.getLegalName() : "Supplier Portal";

        while (view.hasNext()) {
            view.printBanner(session);
            System.out.println(" SUPPLIER CONSOLE (" + orgName + "):");
            System.out.println("  [1] View My Profile & Score");
            System.out.println("  [2] Submit New Claim (with Evidence)");
            System.out.println("  [3] Check Attestation Expiry Timers");
            System.out.println("  [0] Back");
            System.out.println();

            int choice = view.promptInt("Choose an option", 0);
            if (choice == 0) break;

            switch (choice) {
                case 1 -> handleViewProfile(profile, org);
                case 2 -> handleSubmitClaimWizard(profile, org);
                case 3 -> handleViewExpiryTimers(profile);
                default -> view.printWarn("Please enter a valid option (0-3).");
            }
        }
    }

    private void handleViewProfile(SupplierProfile profile, Organisation org) {
        view.printSection("MY SUPPLIER PROFILE");
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

    private void handleSubmitClaimWizard(SupplierProfile profile, Organisation org) {
        view.printSection("SUBMIT NEW CLAIM (4-Step Intake)");

        System.out.println(ConsoleView.BOLD + "[Step 1/4] Choose Category Pillar:" + ConsoleView.RESET);
        Pillar[] pillars = Pillar.values();
        for (int i = 0; i < pillars.length; i++) {
            System.out.printf("  [%d] %s (Weight: %.1fx)%n", i + 1, pillars[i].getLabel(), pillars[i].getDefaultWeight());
        }
        int pChoice = view.promptInt("Pillar number", 4);
        Pillar chosenPillar = (pChoice >= 1 && pChoice <= pillars.length) ? pillars[pChoice - 1] : Pillar.QUALITY;

        System.out.println("\n" + ConsoleView.BOLD + "[Step 2/4] Claim Details:" + ConsoleView.RESET);
        String label = view.promptString("Claim title (ENTER for 'ISO 45001 Safety Standard')");
        if (label.isEmpty()) label = "ISO 45001 Safety Standard";
        String value = view.promptString("Certificate ID or value (ENTER for 'Cert #OHS-2026-8812')");
        if (value.isEmpty()) value = "Cert #OHS-2026-8812";

        System.out.println("\n" + ConsoleView.BOLD + "[Step 3/4] Attach Supporting Document:" + ConsoleView.RESET);
        String docTitle = view.promptString("Document title (ENTER for default)");
        if (docTitle.isEmpty()) docTitle = label + " Certificate.pdf";
        String refUri = "storage://proofs/" + System.currentTimeMillis() + ".pdf";

        Evidence evidence = new Evidence(
                UUID.randomUUID(),
                refUri,
                EvidenceKind.PDF,
                docTitle,
                Instant.now().minus(Duration.ofDays(30)),
                Instant.now().plus(Duration.ofDays(365))
        );

        System.out.println("\n" + ConsoleView.BOLD + "[Step 4/4] Confirm Submission:" + ConsoleView.RESET);
        System.out.println("  Pillar:   " + chosenPillar.getLabel());
        System.out.println("  Claim:    " + label);
        System.out.println("  Value:    " + value);
        System.out.println("  Evidence: " + docTitle + " (" + refUri + ")");

        String confirm = view.promptString("Send to verifier queue? (Y/N)");
        if ("y".equalsIgnoreCase(confirm) || "yes".equalsIgnoreCase(confirm)) {
            verificationService.submitClaimWithEvidence(
                    profile.getOrganisationId(),
                    org != null ? org.getLegalName() : "Aravind Fasteners",
                    label.toLowerCase().replace(" ", "_"),
                    label,
                    chosenPillar,
                    value,
                    evidence
            );
            view.printOk("Claim submitted and queued for verifier review.");
        } else {
            view.printWarn("Claim submission cancelled.");
        }
        view.pressEnterToContinue();
    }

    private void handleViewExpiryTimers(SupplierProfile profile) {
        List<Claim> claims = verificationRepo.findClaimsByOrganisation(profile.getOrganisationId());
        view.printSection("ACTIVE ATTESTATION EXPIRY TIMERS");

        List<String> headers = Arrays.asList("CLAIM", "PILLAR", "METHOD", "VERIFIED ON", "EXPIRES AT", "TIME LEFT");
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
                        a.getValidUntil() != null ? a.getValidUntil().toString().substring(0, 19).replace('T', ' ') : "No expiry",
                        a.getDecayCountdownString()
                ));
            }
        }

        if (rows.isEmpty()) {
            view.printWarn("No active attestations found.");
        } else {
            System.out.println(TableFormatter.renderTable(headers, rows));
        }
        view.pressEnterToContinue();
    }
}
