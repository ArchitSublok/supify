package com.supify.cli;

import com.supify.dao.SupplierRepository;
import com.supify.dao.VerificationRepository;
import com.supify.model.Attestation;
import com.supify.model.Claim;
import com.supify.model.Evidence;
import com.supify.model.Organisation;
import com.supify.model.PaginatedResult;
import com.supify.model.SupplierProfile;
import com.supify.model.TrustSnapshot;
import com.supify.model.enums.Pillar;
import com.supify.model.enums.TrustBand;
import com.supify.service.SupplierSearchService;
import com.supify.service.SupplierSearchService.SupplierSearchResult;
import com.supify.util.TableFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BuyerMenu {
    private final ConsoleView view;
    private final SessionContext session;
    private final SupplierRepository supplierRepo;
    private final VerificationRepository verificationRepo;
    private final SupplierSearchService searchService;

    public BuyerMenu(ConsoleView view, SessionContext session, SupplierRepository supplierRepo, VerificationRepository verificationRepo, SupplierSearchService searchService) {
        this.view = view;
        this.session = session;
        this.supplierRepo = supplierRepo;
        this.verificationRepo = verificationRepo;
        this.searchService = searchService;
    }

    public void display() {
        while (true) {
            view.printBanner(session);
            System.out.println(" BUYER CONSOLE COMMANDS:");
            System.out.println("  [1] Search Supplier Catalog (Multi-faceted filter & sorting)");
            System.out.println("  [2] View Supplier Dossier & Verifiable Claims Ledger");
            System.out.println("  [3] Add / Remove Supplier from Comparison Tray");
            System.out.println("  [4] Render Side-by-Side Comparison Matrix (" + session.getComparisonTray().size() + "/4 selected)");
            System.out.println("  [0] Return to Main Menu");
            System.out.println();

            int choice = view.promptInt("Select an option", 0);
            if (choice == 0) break;

            switch (choice) {
                case 1 -> handleSearch();
                case 2 -> handleViewDossier();
                case 3 -> handleToggleCompare();
                case 4 -> handleRenderCompareMatrix();
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void handleSearch() {
        System.out.println(ConsoleView.BOLD + "\n--- MULTI-FACETED SUPPLIER DISCOVERY ---" + ConsoleView.RESET);
        String keyword = view.promptString("Keyword (e.g. Fasteners, Sheet Metal, Aero, PCB, or press ENTER for all)");
        String category = view.promptString("Category filter (or press ENTER for all)");
        String sort = view.promptString("Sort by [name, score, capacity, moq] (default: score)");
        if (sort.isEmpty()) sort = "score";

        PaginatedResult<SupplierSearchResult> result = searchService.search(
                keyword,
                category,
                null,
                null,
                sort,
                false,
                1,
                20
        );

        if (result.getItems().isEmpty()) {
            System.out.println(ConsoleView.YELLOW + "No suppliers matched your criteria." + ConsoleView.RESET);
            view.pressEnterToContinue();
            return;
        }

        List<String> headers = Arrays.asList("#", "LEGAL NAME", "CATEGORY", "LOCATION", "CAPACITY", "MOQ", "LEAD TIME", "TRUST BAND", "SCORE");
        List<List<String>> rows = new ArrayList<>();
        int idx = 1;

        for (SupplierSearchResult r : result.getItems()) {
            boolean inTray = session.isInComparison(r.organisationId());
            String nameCol = (inTray ? "★ " : "  ") + r.legalName();
            rows.add(Arrays.asList(
                    String.valueOf(idx++),
                    nameCol,
                    r.category(),
                    r.location(),
                    r.getCapacityString(),
                    String.format("%,d", r.moq()),
                    r.leadTime(),
                    r.trustBand().toFormattedString(),
                    String.format("%.3f", r.overallTrustScore())
            ));
        }

        System.out.println(TableFormatter.renderTable(headers, rows));
        System.out.println(ConsoleView.CYAN + "Total Matches: " + result.getTotalCount() + " | (★ indicates supplier in Comparison Tray)" + ConsoleView.RESET);
        view.pressEnterToContinue();
    }

    private void handleViewDossier() {
        List<SupplierProfile> profiles = supplierRepo.findAllProfiles();
        if (profiles.isEmpty()) return;

        System.out.println(ConsoleView.BOLD + "\nSELECT SUPPLIER FOR AUDIT DOSSIER:" + ConsoleView.RESET);
        for (int i = 0; i < profiles.size(); i++) {
            SupplierProfile p = profiles.get(i);
            Optional<Organisation> org = supplierRepo.findOrganisationById(p.getOrganisationId());
            System.out.printf("  [%d] %s (%s) — %s\n", i + 1, org.map(Organisation::getLegalName).orElse("Unknown"), p.getCategory(), p.getTrustBand().toFormattedString());
        }

        int choice = view.promptInt("Select supplier number", 1);
        if (choice < 1 || choice > profiles.size()) return;

        SupplierProfile profile = profiles.get(choice - 1);
        Organisation org = supplierRepo.findOrganisationById(profile.getOrganisationId()).orElse(null);
        Optional<TrustSnapshot> snapshotOpt = supplierRepo.findSnapshotByOrganisationId(profile.getOrganisationId());

        System.out.println("\n" + ConsoleView.BOLD + ConsoleView.CYAN + "═".repeat(82) + ConsoleView.RESET);
        System.out.println(ConsoleView.BOLD + "   SUPPLIER AUDIT DOSSIER :: " + (org != null ? org.getLegalName() : "Unknown") + ConsoleView.RESET);
        System.out.println(ConsoleView.BOLD + ConsoleView.CYAN + "═".repeat(82) + ConsoleView.RESET);
        System.out.println("  Trade Name:       " + (org != null ? org.getTradeName() : "N/A"));
        System.out.println("  Entity Type:      " + (org != null ? org.getEntityType() : "N/A") + " (Est. " + (org != null ? org.getYearEstablished() : "N/A") + ")");
        System.out.println("  Registered City:  " + (org != null ? org.getLocation() : "N/A"));
        System.out.println("  Category:         " + profile.getCategory());
        System.out.println("  Operational Cap:  " + profile.getCapacityDisplay());
        System.out.println("  Min Order Qty:    " + String.format("%,d", profile.getMinimumOrderQuantity()) + " | Lead Time: " + profile.getLeadTime());
        System.out.println("  Trust Band:       " + profile.getTrustBand().toFormattedString() + " | Trust Score: " + String.format("%.3f", profile.getOverallTrustScore()));

        // Render Multi-pillar breakdown
        if (snapshotOpt.isPresent()) {
            System.out.println("\n" + ConsoleView.BOLD + "  TRUST PILLAR RADAR SCORES:" + ConsoleView.RESET);
            TrustSnapshot s = snapshotOpt.get();
            for (Pillar p : Pillar.values()) {
                double score = s.getPillarScore(p);
                String bar = "█".repeat((int) (score * 15)) + "░".repeat(15 - (int) (score * 15));
                System.out.printf("    %-24s [%s] %3.0f%%\n", p.getLabel(), bar, score * 100);
            }
        }

        // Render 4-Layer Claims Ledger (Claim -> Method -> Attestation -> Decay)
        List<Claim> claims = verificationRepo.findClaimsByOrganisation(profile.getOrganisationId());
        System.out.println("\n" + ConsoleView.BOLD + "  VERIFIABLE EVIDENCE LEDGER (Axiom A1-A5 Compliant):" + ConsoleView.RESET);

        List<String> claimHeaders = Arrays.asList("PILLAR", "ASSERTED CLAIM", "VALUE", "METHOD ACHIEVED", "STATUS / DECAY TIMER", "EVIDENCE");
        List<List<String>> claimRows = new ArrayList<>();

        for (Claim c : claims) {
            Optional<Attestation> attOpt = verificationRepo.findActiveAttestationByClaim(c.getId());
            List<Evidence> evidenceList = verificationRepo.findEvidenceByClaim(c.getId());
            String methodStr = attOpt.map(a -> a.getMethodAchieved().getLabel()).orElse("Pending Review");
            String decayStr = attOpt.map(Attestation::getDecayCountdownString).orElse(c.getStatus().getLabel());
            String evidenceStr = evidenceList.isEmpty() ? "None" : evidenceList.get(0).getTitle();

            claimRows.add(Arrays.asList(
                    c.getPillar().name(),
                    c.getLabel(),
                    c.getAssertedValue(),
                    methodStr,
                    decayStr,
                    evidenceStr
            ));
        }

        System.out.println(TableFormatter.renderTable(claimHeaders, claimRows));
        view.pressEnterToContinue();
    }

    private void handleToggleCompare() {
        List<SupplierProfile> profiles = supplierRepo.findAllProfiles();
        System.out.println(ConsoleView.BOLD + "\nADD / REMOVE VENDOR FROM COMPARISON TRAY:" + ConsoleView.RESET);
        for (int i = 0; i < profiles.size(); i++) {
            SupplierProfile p = profiles.get(i);
            Organisation org = supplierRepo.findOrganisationById(p.getOrganisationId()).orElse(null);
            boolean inTray = session.isInComparison(p.getOrganisationId());
            System.out.printf("  [%d] [%s] %s (%s)\n",
                    i + 1,
                    inTray ? "SELECTED" : "        ",
                    org != null ? org.getLegalName() : "Unknown",
                    p.getCategory());
        }

        int choice = view.promptInt("Select supplier number to toggle (0 to cancel)", 0);
        if (choice < 1 || choice > profiles.size()) return;

        SupplierProfile selected = profiles.get(choice - 1);
        if (session.isInComparison(selected.getOrganisationId())) {
            session.removeFromComparison(selected.getOrganisationId());
            System.out.println(ConsoleView.GREEN + "Removed from comparison tray." + ConsoleView.RESET);
        } else {
            boolean added = session.addToComparison(selected.getOrganisationId());
            if (!added) {
                System.out.println(ConsoleView.RED + "Comparison tray is full (maximum 4 suppliers allowed)." + ConsoleView.RESET);
            } else {
                System.out.println(ConsoleView.GREEN + "Added to comparison tray!" + ConsoleView.RESET);
            }
        }
    }

    private void handleRenderCompareMatrix() {
        if (session.getComparisonTray().isEmpty()) {
            System.out.println(ConsoleView.YELLOW + "\nComparison tray is empty. Add 2 to 4 suppliers first via Option [3]!" + ConsoleView.RESET);
            view.pressEnterToContinue();
            return;
        }

        List<SupplierProfile> comparedProfiles = new ArrayList<>();
        List<Organisation> comparedOrgs = new ArrayList<>();

        for (UUID orgId : session.getComparisonTray()) {
            supplierRepo.findProfileByOrganisationId(orgId).ifPresent(comparedProfiles::add);
            supplierRepo.findOrganisationById(orgId).ifPresent(comparedOrgs::add);
        }

        System.out.println("\n" + ConsoleView.BOLD + ConsoleView.CYAN + "═".repeat(82) + ConsoleView.RESET);
        System.out.println(ConsoleView.BOLD + "   SIDE-BY-SIDE SUPPLIER COMPARISON MATRIX" + ConsoleView.RESET);
        System.out.println(ConsoleView.BOLD + ConsoleView.CYAN + "═".repeat(82) + ConsoleView.RESET);

        List<String> headers = new ArrayList<>();
        headers.add("METRIC / ATTRIBUTE");
        for (Organisation o : comparedOrgs) {
            headers.add(o.getLegalName());
        }

        List<List<String>> rows = new ArrayList<>();

        // Category
        List<String> catRow = new ArrayList<>(List.of("Primary Category"));
        for (SupplierProfile p : comparedProfiles) catRow.add(p.getCategory());
        rows.add(catRow);

        // Location
        List<String> locRow = new ArrayList<>(List.of("Manufacturing Hub"));
        for (Organisation o : comparedOrgs) locRow.add(o.getLocation());
        rows.add(locRow);

        // Capacity
        List<String> capRow = new ArrayList<>(List.of("Monthly Capacity"));
        for (SupplierProfile p : comparedProfiles) capRow.add(p.getCapacityDisplay());
        rows.add(capRow);

        // MOQ
        List<String> moqRow = new ArrayList<>(List.of("Minimum Order Qty"));
        for (SupplierProfile p : comparedProfiles) moqRow.add(String.format("%,d", p.getMinimumOrderQuantity()));
        rows.add(moqRow);

        // Lead Time
        List<String> leadRow = new ArrayList<>(List.of("Standard Lead Time"));
        for (SupplierProfile p : comparedProfiles) leadRow.add(p.getLeadTime());
        rows.add(leadRow);

        // Trust Band
        List<String> bandRow = new ArrayList<>(List.of("Trust Standing"));
        for (SupplierProfile p : comparedProfiles) bandRow.add(p.getTrustBand().toFormattedString());
        rows.add(bandRow);

        // Trust Score
        List<String> scoreRow = new ArrayList<>(List.of("Audit Score (0-1)"));
        for (SupplierProfile p : comparedProfiles) scoreRow.add(String.format("%.3f", p.getOverallTrustScore()));
        rows.add(scoreRow);

        System.out.println(TableFormatter.renderTable(headers, rows));
        view.pressEnterToContinue();
    }
}
