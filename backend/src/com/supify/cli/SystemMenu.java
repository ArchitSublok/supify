package com.supify.cli;

import com.supify.concurrency.AlertBuffer;
import com.supify.concurrency.ThreadLifecycleMonitor;
import com.supify.config.DatabaseConfig;
import com.supify.dao.AuditRepository;
import com.supify.dao.SupplierRepository;
import com.supify.io.AuditByteStreamHandler;
import com.supify.io.CsvDataExporterImporter;
import com.supify.io.RuleStreamTokenizer;
import com.supify.model.AuditEvent;
import com.supify.model.Organisation;
import com.supify.model.SupplierProfile;
import com.supify.util.TableFormatter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SystemMenu {
    private final ConsoleView view;
    private final SessionContext session;
    private final SupplierRepository supplierRepo;
    private final AuditRepository auditRepo;

    public SystemMenu(ConsoleView view, SessionContext session, SupplierRepository supplierRepo, AuditRepository auditRepo) {
        this.view = view;
        this.session = session;
        this.supplierRepo = supplierRepo;
        this.auditRepo = auditRepo;
    }

    public void display() {
        while (true) {
            view.printBanner(session);
            System.out.println(" SYSTEM, CONCURRENCY & I/O DIAGNOSTICS:");
            System.out.println("  [1] Live Thread Lifecycle & Scheduling Monitor (Continuous Eval 2, Lec 11-12)");
            System.out.println("  [2] Background Daemon Alert Buffer (ConcurrentLinkedQueue Inspection)");
            System.out.println("  [3] Test Character Streams: CSV Supplier Export/Import (Lec 26-27)");
            System.out.println("  [4] Test Byte Streams: Binary Audit Stream & Magic Header (Lec 25)");
            System.out.println("  [5] Test StreamTokenizer: Verification Rule Engine (Lec 25)");
            System.out.println("  [6] Test JDBC Architecture & Database Connectivity (Lec 17-20)");
            System.out.println("  [7] View System Audit Ledger (Atomic Sequence Events)");
            System.out.println("  [0] Return to Main Menu");
            System.out.println();

            int choice = view.promptInt("Select an option", 0);
            if (choice == 0) break;

            switch (choice) {
                case 1 -> handleThreadMonitor();
                case 2 -> handleAlertBuffer();
                case 3 -> handleCsvExportImport();
                case 4 -> handleByteStreams();
                case 5 -> handleStreamTokenizer();
                case 6 -> handleJdbcStatus();
                case 7 -> handleAuditLedger();
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void handleThreadMonitor() {
        System.out.println(ConsoleView.CYAN + ConsoleView.BOLD + "\n=== JVM THREAD LIFECYCLE & SCHEDULING MONITOR ===" + ConsoleView.RESET);
        System.out.println(ConsoleView.YELLOW + "State coverage: NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED" + ConsoleView.RESET);
        System.out.println();
        System.out.println(ThreadLifecycleMonitor.generateThreadStateReport());
        System.out.println(ConsoleView.GREEN + "Notice background daemons 'DecayMonitorDaemon' & 'SlaEscalationDaemon' running with isDaemon=true." + ConsoleView.RESET);
        view.pressEnterToContinue();
    }

    private void handleAlertBuffer() {
        System.out.println(ConsoleView.CYAN + ConsoleView.BOLD + "\n=== CONCURRENT ALERT BUFFER (ConcurrentLinkedQueue) ===" + ConsoleView.RESET);
        List<String> alerts = AlertBuffer.getInstance().drainAlerts();
        if (alerts.isEmpty()) {
            System.out.println("Alert buffer is currently empty (no active daemon alerts in queue).");
        } else {
            System.out.printf("Drained %d alert(s) from thread-safe buffer:\n\n", alerts.size());
            for (int i = 0; i < alerts.size(); i++) {
                System.out.printf("  [%d] %s\n", i + 1, alerts.get(i));
            }
        }
        view.pressEnterToContinue();
    }

    private void handleCsvExportImport() {
        System.out.println(ConsoleView.CYAN + ConsoleView.BOLD + "\n=== CSV EXPORT / IMPORT VIA CHARACTER STREAMS ===" + ConsoleView.RESET);
        System.out.println("Demonstrating FileWriter, BufferedWriter, FileReader, BufferedReader, and Collection integration.");

        File exportDir = new File("data");
        if (!exportDir.exists()) exportDir.mkdirs();
        File csvFile = new File(exportDir, "suppliers_catalog_export.csv");

        List<SupplierProfile> profiles = supplierRepo.findAllProfiles();
        Map<UUID, Organisation> orgMap = new HashMap<>();
        for (SupplierProfile p : profiles) {
            supplierRepo.findOrganisationById(p.getOrganisationId()).ifPresent(o -> orgMap.put(p.getOrganisationId(), o));
        }

        try {
            // 1. Export using character stream
            System.out.printf("\n1. Exporting %d suppliers to '%s'...\n", profiles.size(), csvFile.getPath());
            CsvDataExporterImporter.exportSuppliersToCsv(csvFile, profiles, orgMap);
            System.out.printf(ConsoleView.GREEN + "✔ Export completed successfully! File size: %d bytes.\n" + ConsoleView.RESET, csvFile.length());

            // 2. Import using character stream
            System.out.printf("\n2. Re-importing and validating records via FileReader & BufferedReader...\n");
            List<CsvDataExporterImporter.ExportRecord> imported = CsvDataExporterImporter.importSuppliersFromCsv(csvFile);
            System.out.printf(ConsoleView.GREEN + "✔ Successfully parsed and mapped %d records into Java objects:\n" + ConsoleView.RESET, imported.size());

            List<String> headers = Arrays.asList("#", "Legal Name", "Category", "Location", "Capacity", "Trust Band", "Score");
            List<List<String>> rows = new ArrayList<>();
            for (int i = 0; i < imported.size(); i++) {
                var r = imported.get(i);
                rows.add(Arrays.asList(
                        String.valueOf(i + 1),
                        r.legalName,
                        r.category,
                        r.location,
                        r.capacity + " " + r.capacityUnit + "/mo",
                        r.trustBand,
                        String.format("%.2f", r.trustScore)
                ));
            }
            System.out.println(TableFormatter.renderTable(headers, rows));

        } catch (IOException e) {
            System.out.println(ConsoleView.RED + "CSV I/O Error: " + e.getMessage() + ConsoleView.RESET);
        }
        view.pressEnterToContinue();
    }

    private void handleByteStreams() {
        System.out.println(ConsoleView.CYAN + ConsoleView.BOLD + "\n=== BINARY AUDIT STREAM & MAGIC BYTE HEADER ===" + ConsoleView.RESET);
        System.out.println("Demonstrating FileOutputStream, FileInputStream, DataOutputStream, DataInputStream, and header verification.");

        File exportDir = new File("data");
        if (!exportDir.exists()) exportDir.mkdirs();
        File binaryFile = new File(exportDir, "audit_ledger.bin");

        try {
            // Append binary records
            System.out.println("\n1. Writing binary audit records with magic header '0x53504659' (SPFY)...");
            AuditByteStreamHandler.appendBinaryRecord(binaryFile, "system.diagnostics", "Triggered manual I/O byte stream benchmark");
            AuditByteStreamHandler.appendBinaryRecord(binaryFile, "security.checksum", "Validated SHA-256 integrity across active certificates");
            System.out.printf(ConsoleView.GREEN + "✔ Binary records appended. File size: %d bytes.\n" + ConsoleView.RESET, binaryFile.length());

            // Read back with magic check
            System.out.println("\n2. Reading binary records via FileInputStream and validating header...");
            List<AuditByteStreamHandler.BinaryAuditRecord> records = AuditByteStreamHandler.readBinaryRecords(binaryFile);
            System.out.printf(ConsoleView.GREEN + "✔ Verified magic header! Read %d binary records:\n" + ConsoleView.RESET, records.size());

            for (int i = 0; i < Math.min(5, records.size()); i++) {
                var rec = records.get(i);
                System.out.printf("  [%d] Epoch: %d | Action: %-22s | Payload: %s\n",
                        i + 1, rec.timestamp(), rec.action(), rec.payload());
            }

        } catch (IOException e) {
            System.out.println(ConsoleView.RED + "Binary Byte Stream Error: " + e.getMessage() + ConsoleView.RESET);
        }
        view.pressEnterToContinue();
    }

    private void handleStreamTokenizer() {
        System.out.println(ConsoleView.CYAN + ConsoleView.BOLD + "\n=== STREAMTOKENIZER RULE ENGINE PARSER ===" + ConsoleView.RESET);
        System.out.println("Demonstrating StreamTokenizer token classification: TT_WORD, TT_NUMBER, TT_EOL, and custom word/char syntax.");

        String sampleRule = "MIN_CAPACITY >= 50000 AND TRUST_SCORE >= 70.0 AND VERIFIED_GST == 1";
        System.out.println("\nDefault Rule Expression:\n  " + ConsoleView.YELLOW + sampleRule + ConsoleView.RESET);

        String input = view.promptString("Enter rule expression (or press ENTER to use default)");
        if (!input.isBlank()) sampleRule = input;

        try {
            List<RuleStreamTokenizer.Token> tokens = RuleStreamTokenizer.tokenizeCriteria(sampleRule);
            System.out.printf(ConsoleView.GREEN + "\n✔ Tokenized %d tokens via StreamTokenizer:\n" + ConsoleView.RESET, tokens.size());

            List<String> headers = Arrays.asList("#", "Token Class", "String Value (sval)", "Numeric Value (nval)", "Token Representation");
            List<List<String>> rows = new ArrayList<>();
            for (int i = 0; i < tokens.size(); i++) {
                var tok = tokens.get(i);
                String tokClass = switch (tok.type()) {
                    case java.io.StreamTokenizer.TT_WORD -> "TT_WORD";
                    case java.io.StreamTokenizer.TT_NUMBER -> "TT_NUMBER";
                    case java.io.StreamTokenizer.TT_EOL -> "TT_EOL";
                    case java.io.StreamTokenizer.TT_EOF -> "TT_EOF";
                    default -> "ORDINARY_CHAR ('" + (char) tok.type() + "')";
                };
                rows.add(Arrays.asList(
                        String.valueOf(i + 1),
                        tokClass,
                        tok.sval() != null ? tok.sval() : "-",
                        tok.type() == java.io.StreamTokenizer.TT_NUMBER ? String.valueOf(tok.nval()) : "-",
                        tok.toString()
                ));
            }
            System.out.println(TableFormatter.renderTable(headers, rows));

        } catch (IOException e) {
            System.out.println(ConsoleView.RED + "StreamTokenizer Error: " + e.getMessage() + ConsoleView.RESET);
        }
        view.pressEnterToContinue();
    }

    private void handleJdbcStatus() {
        System.out.println(ConsoleView.CYAN + ConsoleView.BOLD + "\n=== JDBC ARCHITECTURE & DATABASE CONNECTIVITY ===" + ConsoleView.RESET);
        System.out.println("Relational / JDBC concepts: DriverManager, Connection, PreparedStatement, ResultSet cursor processing.");
        System.out.println();

        System.out.println("Testing live MySQL connection via DatabaseConfig.getConnection()...");
        boolean connected = DatabaseConfig.testConnection();

        if (connected) {
            System.out.println(ConsoleView.GREEN + ConsoleView.BOLD + "✔ Active MySQL Server connection established successfully!" + ConsoleView.RESET);
            System.out.println("  - Driver:     com.mysql.cj.jdbc.Driver");
            System.out.println("  - URL:        jdbc:mysql://localhost:3306/supify");
            System.out.println("  - Connection: Active & Open");
        } else {
            System.out.println(ConsoleView.YELLOW + "ℹ MySQL server not detected on default port 3306 or credentials not provided." + ConsoleView.RESET);
            System.out.println("  - Running in pure high-performance In-Memory DAO mode.");
            System.out.println("  - All domain operations, concurrency queues, and 4-layer spine remain fully active.");
            System.out.println("  - Code implements full JdbcSupplierDao with PreparedStatement parameter binding and ResultSet iteration.");
        }
        view.pressEnterToContinue();
    }

    private void handleAuditLedger() {
        System.out.println(ConsoleView.CYAN + ConsoleView.BOLD + "\n=== IMMUTABLE SYSTEM AUDIT LEDGER (Atomic Sequence Events) ===" + ConsoleView.RESET);
        List<AuditEvent> events = auditRepo.findRecentEvents(15);

        if (events.isEmpty()) {
            System.out.println("No audit events recorded yet.");
            view.pressEnterToContinue();
            return;
        }

        List<String> headers = Arrays.asList("Seq #", "Actor", "Action", "Target", "Details", "Timestamp");
        List<List<String>> rows = new ArrayList<>();

        for (AuditEvent e : events) {
            rows.add(Arrays.asList(
                    String.valueOf(e.getSequenceNumber()),
                    e.getActor(),
                    e.getAction(),
                    e.getSubjectType() + " (" + (e.getSubjectId().length() > 8 ? e.getSubjectId().substring(0, 8) : e.getSubjectId()) + ")",
                    e.getDetails(),
                    e.getTimestamp().toString()
            ));
        }

        System.out.println(TableFormatter.renderTable(headers, rows));
        System.out.printf("Total audit records stored: %d\n", auditRepo.count());
        view.pressEnterToContinue();
    }
}
