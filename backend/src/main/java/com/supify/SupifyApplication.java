package com.supify;

import com.supify.cli.BuyerMenu;
import com.supify.cli.ConsoleView;
import com.supify.cli.SessionContext;
import com.supify.cli.SupplierMenu;
import com.supify.cli.SystemMenu;
import com.supify.cli.VerifierMenu;
import com.supify.concurrency.DecayMonitorDaemon;
import com.supify.concurrency.SlaEscalationDaemon;
import com.supify.dao.memory.InMemoryAuditRepository;
import com.supify.dao.memory.InMemorySupplierRepository;
import com.supify.dao.memory.InMemoryVerificationRepository;
import com.supify.model.enums.UserRole;
import com.supify.service.SupplierSearchService;
import com.supify.service.TrustEvaluationService;
import com.supify.service.VerificationService;
import com.supify.util.SeedDataFactory;

/**
 * Supify Platform Backend & CLI Application Entry Point.
 * 
 * Demonstrates:
 * - Multithreading, Concurrency, and Thread Scheduling (Daemons, Priority Queue)
 * - 4-Layer Verification Spine (Claim -> Evidence -> VerificationCheck -> Attestation)
 * - Object-Oriented Principles, Generics, and Lambda Streams API
 * - File I/O (CSV Character Streams, Binary Byte Streams, StreamTokenizer)
 * - JDBC Architecture with fallback to atomic In-Memory repositories
 */
public class SupifyApplication {

    public static void main(String[] args) {
        // 1. Initialize Thread-Safe In-Memory Repositories
        InMemorySupplierRepository supplierRepo = new InMemorySupplierRepository();
        InMemoryVerificationRepository verificationRepo = new InMemoryVerificationRepository();
        InMemoryAuditRepository auditRepo = new InMemoryAuditRepository();

        // 2. Populate Realistic & Adversarial Seed Data (including 35s live expiring attestation)
        SeedDataFactory.populate(supplierRepo, verificationRepo);

        // 3. Initialize Domain Services (Lambdas, Stream API Pipelines, Trust Evaluation)
        TrustEvaluationService trustEvaluationService = new TrustEvaluationService(supplierRepo, verificationRepo);
        SupplierSearchService searchService = new SupplierSearchService(supplierRepo);
        VerificationService verificationService = new VerificationService(verificationRepo, trustEvaluationService, auditRepo);

        // 4. Initialize CLI Session & UI Views
        SessionContext session = new SessionContext();
        ConsoleView view = new ConsoleView();

        BuyerMenu buyerMenu = new BuyerMenu(view, session, supplierRepo, verificationRepo, searchService);
        SupplierMenu supplierMenu = new SupplierMenu(view, session, supplierRepo, verificationRepo, verificationService);
        VerifierMenu verifierMenu = new VerifierMenu(view, session, verificationRepo, supplierRepo, verificationService);
        SystemMenu systemMenu = new SystemMenu(view, session, supplierRepo, auditRepo);

        // 5. Start Background Concurrency Daemons
        DecayMonitorDaemon decayDaemon = new DecayMonitorDaemon(verificationRepo, supplierRepo, auditRepo, 4000L);
        decayDaemon.start();

        SlaEscalationDaemon slaDaemon = new SlaEscalationDaemon(verificationRepo, 6000L);
        slaDaemon.start();

        // Register Graceful JVM Shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            decayDaemon.stopMonitoring();
            slaDaemon.stopDaemon();
        }, "Supify-ShutdownWorker"));

        // 6. Interactive CLI Main Loop
        boolean running = true;
        while (running) {
            view.printBanner(session);
            System.out.println(" SUPIFY PLATFORM MAIN NAVIGATION:");
            System.out.println("  [1] Buyer Discovery Console (Faceted Search, Comparison Tray, Dossiers)");
            System.out.println("  [2] Supplier Workspace (Standings, Claims Ledger, 4-Step Intake Wizard)");
            System.out.println("  [3] Verifier Station (Priority Queue, Evidence Dossiers, Pass/Fail Decisions)");
            System.out.println("  [4] System, Concurrency & I/O Monitor (Threads, CSV, Byte Streams, Tokenizer)");
            System.out.println("  [5] Switch Active Persona / User Role");
            System.out.println("  [0] Exit Supify Application");
            System.out.println();

            if (!view.hasNext()) {
                break;
            }
            int choice = view.promptInt("Select an option", 1);

            switch (choice) {
                case 1 -> {
                    session.setCurrentRole(UserRole.BUYER);
                    buyerMenu.display();
                }
                case 2 -> {
                    session.setCurrentRole(UserRole.SUPPLIER);
                    supplierMenu.display();
                }
                case 3 -> {
                    session.setCurrentRole(UserRole.VERIFIER);
                    verifierMenu.display();
                }
                case 4 -> {
                    session.setCurrentRole(UserRole.ADMIN);
                    systemMenu.display();
                }
                case 5 -> handleSwitchRole(view, session);
                case 0 -> {
                    running = false;
                    System.out.println(ConsoleView.CYAN + "\nShutting down Supify background daemons gracefully..." + ConsoleView.RESET);
                    decayDaemon.stopMonitoring();
                    slaDaemon.stopDaemon();
                    System.out.println(ConsoleView.GREEN + "Thank you for using Supify. Session terminated cleanly." + ConsoleView.RESET);
                }
                default -> System.out.println("Invalid selection. Please choose an option from the menu.");
            }
        }
    }

    private static void handleSwitchRole(ConsoleView view, SessionContext session) {
        System.out.println(ConsoleView.CYAN + "\nSelect Persona / Role to switch to:" + ConsoleView.RESET);
        System.out.println("  [1] BUYER (Procurement Specialist - Discovery, Search, Comparison)");
        System.out.println("  [2] SUPPLIER (Aravind Fasteners Admin - Intake Wizard, Claims)");
        System.out.println("  [3] VERIFIER (Trained Verifier #04 - Evidence Review, Priority Queue)");
        System.out.println("  [4] ADMIN (Platform System Admin - Concurrency, Diagnostics, Audit)");

        int roleChoice = view.promptInt("Select Role", 1);
        switch (roleChoice) {
            case 2 -> session.setCurrentRole(UserRole.SUPPLIER);
            case 3 -> session.setCurrentRole(UserRole.VERIFIER);
            case 4 -> session.setCurrentRole(UserRole.ADMIN);
            default -> session.setCurrentRole(UserRole.BUYER);
        }

        System.out.println(ConsoleView.GREEN + "✔ Switched active role to: " + session.getCurrentRole() + " (" + session.getCurrentUserName() + ")" + ConsoleView.RESET);
        view.pressEnterToContinue();
    }
}
