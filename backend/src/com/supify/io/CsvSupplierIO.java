package com.supify.io;

import com.supify.model.Supplier;
import com.supify.model.TrustBand;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Character stream (FileWriter/FileReader + Buffered) CSV export and import. */
public class CsvSupplierIO {

    public static void exportToCsv(File file, List<Supplier> suppliers) throws IOException {
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        try (FileWriter fw = new FileWriter(file);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("id,name,city,category,capacity,moq,leadTime,trustBand,trustScore");
            bw.newLine();
            for (Supplier s : suppliers) {
                String row = String.format("%s,%s,%s,%s,%d,%d,%s,%s,%.2f",
                        clean(s.getId()),
                        clean(s.getName()),
                        clean(s.getCity()),
                        clean(s.getCategory()),
                        s.getCapacityPerMonth(),
                        s.getMoq(),
                        clean(s.getLeadTime()),
                        s.getTrustBand().name(),
                        s.getTrustScore());
                bw.write(row);
                bw.newLine();
            }
        }
    }

    public static List<Supplier> importFromCsv(File file) throws IOException {
        List<Supplier> loaded = new ArrayList<>();
        try (FileReader fr = new FileReader(file);
             BufferedReader br = new BufferedReader(fr)) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",");
                if (parts.length < 9) continue;
                try {
                    Supplier s = new Supplier(
                            parts[0].trim(),
                            parts[1].trim(),
                            parts[2].trim(),
                            parts[3].trim(),
                            Integer.parseInt(parts[4].trim()),
                            Integer.parseInt(parts[5].trim()),
                            parts[6].trim()
                    );
                    s.setTrustBand(TrustBand.valueOf(parts[7].trim()));
                    s.setTrustScore(Double.parseDouble(parts[8].trim()));
                    loaded.add(s);
                } catch (IllegalArgumentException e) {
                    System.err.println("Skipping invalid CSV row: " + line);
                }
            }
        }
        return loaded;
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace(",", " ").trim();
    }
}
