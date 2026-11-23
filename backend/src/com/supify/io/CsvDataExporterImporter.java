package com.supify.io;

import com.supify.model.Organisation;
import com.supify.model.SupplierProfile;
import com.supify.model.enums.ListingState;
import com.supify.model.enums.TrustBand;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Demonstrates:
 * - Character streams: FileReader, FileWriter, BufferedReader, BufferedWriter
 * - File Handling + Collections Integration: Collection import/export
 * - Input validation & exception-safe try-with-resources
 */
public class CsvDataExporterImporter {

    public static class ExportRecord {
        public final String orgId;
        public final String legalName;
        public final String category;
        public final String location;
        public final int capacity;
        public final String capacityUnit;
        public final int moq;
        public final String leadTime;
        public final String trustBand;
        public final double trustScore;

        public ExportRecord(String orgId, String legalName, String category, String location, int capacity, String capacityUnit, int moq, String leadTime, String trustBand, double trustScore) {
            this.orgId = orgId;
            this.legalName = legalName;
            this.category = category;
            this.location = location;
            this.capacity = capacity;
            this.capacityUnit = capacityUnit;
            this.moq = moq;
            this.leadTime = leadTime;
            this.trustBand = trustBand;
            this.trustScore = trustScore;
        }
    }

    /**
     * Exports a collection of suppliers to a CSV file using character streams (FileWriter + BufferedWriter).
     */
    public static void exportSuppliersToCsv(File file, Collection<SupplierProfile> profiles, Map<UUID, Organisation> orgMap) throws IOException {
        try (FileWriter fw = new FileWriter(file);
             BufferedWriter bw = new BufferedWriter(fw)) {

            // CSV Header
            bw.write("org_id,legal_name,category,location,capacity,capacity_unit,moq,lead_time,trust_band,trust_score");
            bw.newLine();

            for (SupplierProfile p : profiles) {
                Organisation org = orgMap.get(p.getOrganisationId());
                String legalName = org != null ? escapeCsv(org.getLegalName()) : "Unknown";
                String location = org != null ? escapeCsv(org.getLocation()) : "India";

                String row = String.format("%s,%s,%s,%s,%d,%s,%d,%s,%s,%.2f",
                        p.getOrganisationId(),
                        legalName,
                        escapeCsv(p.getCategory()),
                        location,
                        p.getCapacityPerMonth(),
                        escapeCsv(p.getCapacityUnit()),
                        p.getMinimumOrderQuantity(),
                        escapeCsv(p.getLeadTime()),
                        p.getTrustBand().name(),
                        p.getOverallTrustScore()
                );
                bw.write(row);
                bw.newLine();
            }
            bw.flush();
        }
    }

    /**
     * Imports records from a CSV file using character streams (FileReader + BufferedReader).
     * Demonstrates parsing, data validation, and building collection objects.
     */
    public static List<ExportRecord> importSuppliersFromCsv(File file) throws IOException {
        List<ExportRecord> records = new ArrayList<>();
        if (!file.exists()) {
            throw new IOException("File does not exist: " + file.getAbsolutePath());
        }

        try (FileReader fr = new FileReader(file);
             BufferedReader br = new BufferedReader(fr)) {

            String line = br.readLine(); // skip header
            int lineNum = 1;

            while ((line = br.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] cols = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"); // regex for CSV with quotes
                if (cols.length < 10) {
                    System.err.println("Warning: skipping invalid CSV row at line " + lineNum + ": " + line);
                    continue;
                }

                try {
                    String orgId = unescapeCsv(cols[0]);
                    String legalName = unescapeCsv(cols[1]);
                    String category = unescapeCsv(cols[2]);
                    String location = unescapeCsv(cols[3]);
                    int capacity = Integer.parseInt(cols[4].trim());
                    String capacityUnit = unescapeCsv(cols[5]);
                    int moq = Integer.parseInt(cols[6].trim());
                    String leadTime = unescapeCsv(cols[7]);
                    String trustBand = unescapeCsv(cols[8]);
                    double trustScore = Double.parseDouble(cols[9].trim());

                    records.add(new ExportRecord(orgId, legalName, category, location, capacity, capacityUnit, moq, leadTime, trustBand, trustScore));
                } catch (NumberFormatException e) {
                    System.err.println("Number format error parsing line " + lineNum + ": " + e.getMessage());
                }
            }
        }
        return records;
    }

    private static String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private static String unescapeCsv(String val) {
        if (val == null) return "";
        val = val.trim();
        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
            val = val.substring(1, val.length() - 1).replace("\"\"", "\"");
        }
        return val;
    }
}
