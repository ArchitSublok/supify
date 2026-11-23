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
        while (view.hasNext()) {
            view.printBanner(session);
            System.out.println(" BUYER CONSOLE:");
            System.out.println("  [1] List All Suppliers");
            System.out.println("  [2] Search & Filter Suppliers");
            System.out.println("  [3] View Supplier Profile & Claims");
            System.out.println("  [4] Add / Remove from Compare (" + session.getComparisonTray().size() + "/4)");
            System.out.println("  [5] Compare Selected Suppliers");
            System.out.println("  [0] Back");
            System.out.println();

            int choice = view.promptInt("Choose an option", 0);
            if (choice == 0) break;

            switch (choice) {
                case 1 -> handleListAll();
                case 2 -> handleSearch();
                case 3 -> handleViewProfile();
                case 4 -> handleToggleCompare();
                case 5 -> handleCompareMatrix();
                default -> view.printWarn("Please enter a valid option (0-5).");
            }
        }
    }

    private void handleListAll() {
        view.printSection("ALL REGISTERED SUPPLIERS (Ranked by Trust Score)");
        PaginatedResult<SupplierSearchResult> result = searchService.search("", "", null, null, "score", false, 1, 20);
        renderSupplierTable(result);
        view.pressEnterToContinue();
    }

    private void handleSearch() {
        view.printSection("SEARCH & FILTER SUPPLIERS");
        String keyword = view.promptString("Keyword (e.g. Fasteners, Sheet Metal, PCB, or ENTER for all)");
        String category = view.promptString("Category filter (or ENTER for all)");
        String sort = view.promptString("Sort by [name, score, capacity, moq] (default: score)");
        if (sort.isEmpty()) sort = "score";

        PaginatedResult<SupplierSearchResult> result = searchService.search(keyword, category, null, null, sort, false, 1, 20);
        renderSupplierTable(result);
        view.pressEnterToContinue();
    }

    private void renderSupplierTable(PaginatedResult<SupplierSearchResult> result) {
        if (result.getItems().isEmpty()) {
            view.printWarn("No suppliers matched your criteria.");
            return;
        }

        List<String> headers = Arrays.asList("#", "SUPPLIER", "CATEGORY", "LOCATION", "CAPACITY", "MOQ", "LEAD TIME", "TRUST BAND", "SCORE");
        List<List<String>> rows = new ArrayList<>();
        int idx = 1;

        for (SupplierSearchResult r : result.getItems()) {
            String prefix = session.isInComparison(r.organisationId()) ? "* " : "  ";
            rows.add(Arrays.asList(
                    String.valueOf(idx++),
                    prefix + r.legalName(),
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
        System.out.println(ConsoleView.CYAN + "Matches: " + result.getTotalCount() + "  (* = in compare tray)" + ConsoleView.RESET);
    }

    private void handleViewProfile() {
        List<SupplierProfile> profiles = supplierRepo.findAllProfiles();
        if (profiles.isEmpty()) {
            view.printWarn("No suppliers available.");
            view.pressEnterToContinue();
            return;
        }

        System.out.println(ConsoleView.BOLD + "\nSelect a supplier to inspect:" + ConsoleView.RESET);
        for (int i = 0; i < profiles.size(); i++) {
            SupplierProfile p = profiles.get(i);
            String name = supplierRepo.findOrganisationById(p.getOrganisationId())
                    .map(Organisation::getLegalName)
                    .orElse("Unknown");
            System.out.printf("  [%d] %s (%s) - %s%n", i + 1, name, p.getCategory(), p.getTrustBand().toFormattedString());
        }

        int choice = view.promptInt("Supplier number", 1);
        if (choice < 1 || choice > profiles.size()) {
            view.printWarn("Selection out of range.");
            view.pressEnterToContinue();
            return;
        }

        SupplierProfile profile = profiles.get(choice - 1);
        Organisation org = supplierRepo.findOrganisationById(profile.getOrganisationId()).orElse(null);
        Optional<TrustSnapshot> snapshotOpt = supplierRepo.findSnapshotByOrganisationId(profile.getOrganisationId());

        view.printSection("SUPPLIER PROFILE :: " + (org != null ? org.getLegalName() : "Unknown"));
        System.out.println("  Trade Name:    " + (org != null ? org.getTradeName() : "N/A"));
        System.out.println("  Entity Type:   " + (org != null ? org.getEntityType() : "N/A") + " (Est. " + (org != null ? org.getYearEstablished() : "N/A") + ")");
        System.out.println("  Location:      " + (org != null ? org.getLocation() : "N/A"));
        System.out.println("  Category:      " + profile.getCategory());
        System.out.println("  Capacity:      " + profile.getCapacityDisplay());
        System.out.println("  MOQ / Lead:    " + String.format("%,d", profile.getMinimumOrderQuantity()) + " units | " + profile.getLeadTime());
        System.out.println("  Trust Standing:" + " " + profile.getTrustBand().toFormattedString() + " (" + String.format("%.3f", profile.getOverallTrustScore()) + ")");

        if (snapshotOpt.isPresent()) {
            System.out.println("\n" + ConsoleView.BOLD + "  PILLAR COVERAGE:" + ConsoleView.RESET);
            TrustSnapshot snap = snapshotOpt.get();
            for (Pillar p : Pillar.values()) {
                double score = snap.getPillarScore(p);
                int filled = Math.max(0, Math.min(15, (int) (score * 15)));
                String bar = "#".repeat(filled) + ".".repeat(15 - filled);
                System.out.printf("    %-26s [%s] %3.0f%%%n", p.getLabel(), bar, score * 100);
            }
        }

        List<Claim> claims = verificationRepo.findClaimsByOrganisation(profile.getOrganisationId());
        System.out.println("\n" + ConsoleView.BOLD + "  CLAIMS & EVIDENCE LEDGER (" + claims.size() + " claims):" + ConsoleView.RESET);

        List<String> claimHeaders = Arrays.asList("PILLAR", "CLAIM", "VALUE", "METHOD", "STATUS / EXPIRY", "EVIDENCE");
        List<List<String>> claimRows = new ArrayList<>();
        for (Claim c : claims) {
            Optional<Attestation> attOpt = verificationRepo.findActiveAttestationByClaim(c.getId());
            List<Evidence> evList = verificationRepo.findEvidenceByClaim(c.getId());
            String method = attOpt.map(a -> a.getMethodAchieved().getLabel()).orElse("Pending Review");
            String statusOrExpiry = attOpt.map(Attestation::getDecayCountdownString).orElse(c.getStatus().getLabel());
            String evidence = evList.isEmpty() ? "None" : evList.get(0).getTitle();

            claimRows.add(Arrays.asList(
                    c.getPillar().name(),
                    c.getLabel(),
                    c.getAssertedValue(),
                    method,
                    statusOrExpiry,
                    evidence
            ));
        }

        System.out.println(TableFormatter.renderTable(claimHeaders, claimRows));
        view.pressEnterToContinue();
    }

    private void handleToggleCompare() {
        List<SupplierProfile> profiles = supplierRepo.findAllProfiles();
        System.out.println(ConsoleView.BOLD + "\nTOGGLE SUPPLIER IN COMPARE TRAY (Max 4):" + ConsoleView.RESET);
        for (int i = 0; i < profiles.size(); i++) {
            SupplierProfile p = profiles.get(i);
            String name = supplierRepo.findOrganisationById(p.getOrganisationId())
                    .map(Organisation::getLegalName)
                    .orElse("Unknown");
            boolean inTray = session.isInComparison(p.getOrganisationId());
            System.out.printf("  [%d] [%s] %s (%s)%n", i + 1, inTray ? "X" : " ", name, p.getCategory());
        }

        int choice = view.promptInt("Supplier number to toggle (0 to cancel)", 0);
        if (choice < 1 || choice > profiles.size()) return;

        SupplierProfile selected = profiles.get(choice - 1);
        if (session.isInComparison(selected.getOrganisationId())) {
            session.removeFromComparison(selected.getOrganisationId());
            view.printOk("Removed supplier from compare tray.");
        } else if (session.addToComparison(selected.getOrganisationId())) {
            view.printOk("Added supplier to compare tray (" + session.getComparisonTray().size() + "/4).");
        } else {
            view.printError("Compare tray is full (max 4 suppliers).");
        }
    }

    private void handleCompareMatrix() {
        if (session.getComparisonTray().isEmpty()) {
            view.printWarn("Compare tray is empty. Select 2 to 4 suppliers first via Option [4].");
            view.pressEnterToContinue();
            return;
        }

        List<SupplierProfile> profiles = new ArrayList<>();
        List<Organisation> orgs = new ArrayList<>();
        for (UUID orgId : session.getComparisonTray()) {
            supplierRepo.findProfileByOrganisationId(orgId).ifPresent(profiles::add);
            supplierRepo.findOrganisationById(orgId).ifPresent(orgs::add);
        }

        view.printSection("SIDE-BY-SIDE SUPPLIER COMPARISON (" + orgs.size() + " Selected)");

        List<String> headers = new ArrayList<>();
        headers.add("ATTRIBUTE");
        for (Organisation o : orgs) headers.add(o.getLegalName());

        List<List<String>> rows = new ArrayList<>();
        List<String> catRow = new ArrayList<>(List.of("Category"));
        List<String> locRow = new ArrayList<>(List.of("Location"));
        List<String> capRow = new ArrayList<>(List.of("Monthly Capacity"));
        List<String> moqRow = new ArrayList<>(List.of("Minimum Order Qty"));
        List<String> leadRow = new ArrayList<>(List.of("Lead Time"));
        List<String> bandRow = new ArrayList<>(List.of("Trust Standing"));
        List<String> scoreRow = new ArrayList<>(List.of("Trust Score"));

        for (int i = 0; i < profiles.size(); i++) {
            SupplierProfile p = profiles.get(i);
            Organisation o = orgs.get(i);
            catRow.add(p.getCategory());
            locRow.add(o.getLocation());
            capRow.add(p.getCapacityDisplay());
            moqRow.add(String.format("%,d", p.getMinimumOrderQuantity()));
            leadRow.add(p.getLeadTime());
            bandRow.add(p.getTrustBand().toFormattedString());
            scoreRow.add(String.format("%.3f", p.getOverallTrustScore()));
        }

        rows.add(catRow);
        rows.add(locRow);
        rows.add(capRow);
        rows.add(moqRow);
        rows.add(leadRow);
        rows.add(bandRow);
        rows.add(scoreRow);

        System.out.println(TableFormatter.renderTable(headers, rows));
        view.pressEnterToContinue();
    }
}
