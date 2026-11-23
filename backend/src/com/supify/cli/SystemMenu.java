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
import java.io.StreamTokenizer;
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
        while (view.hasNext()) {
            view.printBanner(session);
            System.out.println(" SYSTEM & I/O DIAGNOSTICS:");
            System.out.println("  [1] Active Threads");
            System.out.println("  [2] Background Alerts");
            System.out.println("  [3] Export & Import CSV");
            System.out.println("  [4] Write & Read Binary Audit File");
            System.out.println("  [5] Parse Rule Expression (StreamTokenizer)");
            System.out.println("  [6] Database (JDBC) Status");
            System.out.println("  [7] Audit Log");
            System.out.println("  [0] Back");
            System.out.println();

            int choice = view.promptInt("Choose an option", 0);
            if (choice == 0) break;

            switch (choice) {
                case 1 -> handleThreadMonitor();
                case 2 -> handleAlertBuffer();
                case 3 -> handleCsvExportImport();
                case 4 -> handleByteStreams();
                case 5 -> handleStreamTokenizer();
                case 6 -> handleJdbcStatus();
                case 7 -> handleAuditLog();
                default -> view.printWarn("Please enter a valid option (0-7).");
            }
        }
    }

    private void handleThreadMonitor() {
        view.printSection("ACTIVE JVM THREADS & DAEMON STATES");
        System.out.println(ThreadLifecycleMonitor.generateThreadStateReport());
        view.printOk("DecayMonitorDaemon & SlaEscalationDaemon are running in background (isDaemon=true).");
        view.pressEnterToContinue();
    }

    private void handleAlertBuffer() {
        view.printSection("BACKGROUND ALERT QUEUE");
        List<String> alerts = AlertBuffer.getInstance().drainAlerts();
        if (alerts.isEmpty()) {
            System.out.println("No pending alerts in queue.");
        } else {
            System.out.printf("Drained %d alert(s):%n%n", alerts.size());
            for (int i = 0; i < alerts.size(); i++) {
                System.out.printf("  [%d] %s%n", i + 1, alerts.get(i));
            }
        }
        view.pressEnterToContinue();
    }

    private void handleCsvExportImport() {
        view.printSection("CSV CATALOG EXPORT & IMPORT");

        File exportDir = new File("data");
        if (!exportDir.exists()) exportDir.mkdirs();
        File csvFile = new File(exportDir, "suppliers_catalog_export.csv");

        List<SupplierProfile> profiles = supplierRepo.findAllProfiles();
        Map<UUID, Organisation> orgMap = new HashMap<>();
        for (SupplierProfile p : profiles) {
            supplierRepo.findOrganisationById(p.getOrganisationId()).ifPresent(o -> orgMap.put(p.getOrganisationId(), o));
        }

        try {
            CsvDataExporterImporter.exportSuppliersToCsv(csvFile, profiles, orgMap);
            view.printOk("Exported " + profiles.size() + " suppliers to '" + csvFile.getPath() + "' (" + csvFile.length() + " bytes).");

            List<CsvDataExporterImporter.ExportRecord> imported = CsvDataExporterImporter.importSuppliersFromCsv(csvFile);
            view.printOk("Read back and parsed " + imported.size() + " CSV rows:\n");

            List<String> headers = Arrays.asList("#", "SUPPLIER", "CATEGORY", "LOCATION", "CAPACITY", "TRUST BAND", "SCORE");
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
            view.printError("CSV I/O failed: " + e.getMessage());
        }
        view.pressEnterToContinue();
    }

    private void handleByteStreams() {
        view.printSection("BINARY AUDIT LOG (SPFY Header + Raw Bytes)");

        File exportDir = new File("data");
        if (!exportDir.exists()) exportDir.mkdirs();
        File binaryFile = new File(exportDir, "audit_ledger.bin");

        try {
            AuditByteStreamHandler.appendBinaryRecord(binaryFile, "system.diagnostics", "Manual binary stream check");
            AuditByteStreamHandler.appendBinaryRecord(binaryFile, "security.checksum", "Verified active certificate checksums");
            view.printOk("Appended 2 binary records to '" + binaryFile.getPath() + "' (" + binaryFile.length() + " bytes).");

            List<AuditByteStreamHandler.BinaryAuditRecord> records = AuditByteStreamHandler.readBinaryRecords(binaryFile);
            view.printOk("Verified 0x53504659 (SPFY) header. Total records in file: " + records.size() + "\n");

            int start = Math.max(0, records.size() - 5);
            for (int i = start; i < records.size(); i++) {
                var rec = records.get(i);
                System.out.printf("  [%d] Epoch: %d | Action: %-20s | Payload: %s%n",
                        i + 1, rec.timestamp(), rec.action(), rec.payload());
            }
        } catch (IOException e) {
            view.printError("Binary I/O failed: " + e.getMessage());
        }
        view.pressEnterToContinue();
    }

    private void handleStreamTokenizer() {
        view.printSection("RULE EXPRESSION PARSER (StreamTokenizer)");
        String rule = "MIN_CAPACITY >= 50000 AND TRUST_SCORE >= 70.0 AND VERIFIED_GST == 1";
        System.out.println("Default rule: " + ConsoleView.YELLOW + rule + ConsoleView.RESET);

        String input = view.promptString("Enter rule expression (or ENTER for default)");
        if (!input.isBlank()) rule = input;

        try {
            List<RuleStreamTokenizer.Token> tokens = RuleStreamTokenizer.tokenizeCriteria(rule);
            view.printOk("Parsed " + tokens.size() + " tokens:\n");

            List<String> headers = Arrays.asList("#", "TOKEN TYPE", "WORD (sval)", "NUMBER (nval)", "OUTPUT");
            List<List<String>> rows = new ArrayList<>();
            for (int i = 0; i < tokens.size(); i++) {
                var tok = tokens.get(i);
                String tokClass = switch (tok.type()) {
                    case StreamTokenizer.TT_WORD -> "WORD";
                    case StreamTokenizer.TT_NUMBER -> "NUMBER";
                    case StreamTokenizer.TT_EOL -> "EOL";
                    case StreamTokenizer.TT_EOF -> "EOF";
                    default -> "CHAR ('" + (char) tok.type() + "')";
                };
                rows.add(Arrays.asList(
                        String.valueOf(i + 1),
                        tokClass,
                        tok.sval() != null ? tok.sval() : "-",
                        tok.type() == StreamTokenizer.TT_NUMBER ? String.valueOf(tok.nval()) : "-",
                        tok.toString()
                ));
            }
            System.out.println(TableFormatter.renderTable(headers, rows));
        } catch (IOException e) {
            view.printError("Tokenizer failed: " + e.getMessage());
        }
        view.pressEnterToContinue();
    }

    private void handleJdbcStatus() {
        view.printSection("MYSQL / JDBC CONNECTION STATUS");
        boolean connected = DatabaseConfig.testConnection();
        if (connected) {
            view.printOk("Connected to MySQL via DatabaseConfig (jdbc:mysql://localhost:3306/supify).");
        } else {
            view.printWarn("MySQL driver or server not connected; CLI is running in In-Memory mode.");
            System.out.println("  JdbcSupplierDao is ready for MySQL integration in the next phase.");
        }
        view.pressEnterToContinue();
    }

    private void handleAuditLog() {
        view.printSection("SYSTEM AUDIT LOG (Latest 15 Events)");
        List<AuditEvent> events = auditRepo.findRecentEvents(15);

        if (events.isEmpty()) {
            view.printWarn("No audit events recorded yet.");
            view.pressEnterToContinue();
            return;
        }

        List<String> headers = Arrays.asList("SEQ #", "ACTOR", "ACTION", "SUBJECT", "DETAILS");
        List<List<String>> rows = new ArrayList<>();
        for (AuditEvent e : events) {
            String subjectId = e.getSubjectId().length() > 8 ? e.getSubjectId().substring(0, 8) : e.getSubjectId();
            rows.add(Arrays.asList(
                    String.valueOf(e.getSequenceNumber()),
                    e.getActor(),
                    e.getAction(),
                    e.getSubjectType() + ":" + subjectId,
                    e.getDetails()
            ));
        }

        System.out.println(TableFormatter.renderTable(headers, rows));
        System.out.printf("Total recorded events: %d%n", auditRepo.count());
        view.pressEnterToContinue();
    }
}
