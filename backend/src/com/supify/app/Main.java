package com.supify.app;

import com.supify.cli.BuyerMenu;
import com.supify.cli.ConsoleView;
import com.supify.cli.DemoRunner;
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

/** Entry point for the Supify CLI backend and background daemons. */
public class Main {

    public static void main(String[] args) {
        InMemorySupplierRepository supplierRepo = new InMemorySupplierRepository();
        InMemoryVerificationRepository verificationRepo = new InMemoryVerificationRepository();
        InMemoryAuditRepository auditRepo = new InMemoryAuditRepository();

        SeedDataFactory.populate(supplierRepo, verificationRepo);

        TrustEvaluationService trustService = new TrustEvaluationService(supplierRepo, verificationRepo);
        SupplierSearchService searchService = new SupplierSearchService(supplierRepo);
        VerificationService verificationService = new VerificationService(verificationRepo, trustService, auditRepo);

        SessionContext session = new SessionContext();
        ConsoleView view = new ConsoleView();

        BuyerMenu buyerMenu = new BuyerMenu(view, session, supplierRepo, verificationRepo, searchService);
        SupplierMenu supplierMenu = new SupplierMenu(view, session, supplierRepo, verificationRepo, verificationService);
        VerifierMenu verifierMenu = new VerifierMenu(view, session, verificationRepo, verificationService);
        SystemMenu systemMenu = new SystemMenu(view, session, supplierRepo, auditRepo);
        DemoRunner demoRunner = new DemoRunner(view);

        DecayMonitorDaemon decayDaemon = new DecayMonitorDaemon(verificationRepo, supplierRepo, auditRepo, 4000L);
        decayDaemon.start();

        SlaEscalationDaemon slaDaemon = new SlaEscalationDaemon(verificationRepo, 6000L);
        slaDaemon.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            decayDaemon.stopMonitoring();
            slaDaemon.stopDaemon();
        }, "Supify-ShutdownWorker"));

        boolean running = true;
        while (running && view.hasNext()) {
            view.printBanner(session);
            System.out.println(" MAIN MENU:");
            System.out.println("  [1] Buyer Console     - List, search, view profiles, compare");
            System.out.println("  [2] Supplier Console  - View profile, submit claim, expiry timers");
            System.out.println("  [3] Verifier Console  - Review queue, inspect evidence, approve/reject");
            System.out.println("  [4] System & I/O      - Threads, alerts, CSV/binary files, rules, audit");
            System.out.println("  [5] Quick Guided Demo - Run automated 5-step walkthrough");
            System.out.println("  [0] Exit");
            System.out.println();

            int choice = view.promptInt("Choose an option", 1);

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
                case 5 -> demoRunner.runGuidedDemo();
                case 0 -> {
                    running = false;
                    System.out.println(ConsoleView.CYAN + "\nStopping background daemons..." + ConsoleView.RESET);
                    decayDaemon.stopMonitoring();
                    slaDaemon.stopDaemon();
                    view.printOk("Session closed cleanly.");
                }
                default -> view.printWarn("Please enter a valid option (0-5).");
            }
        }
    }
}
