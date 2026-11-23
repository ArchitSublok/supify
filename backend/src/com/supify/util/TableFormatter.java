package com.supify.util;

import java.util.List;

/** Renders ASCII tables with ANSI-aware column padding. */
public class TableFormatter {

    public static String renderTable(List<String> headers, List<List<String>> rows) {
        if (headers == null || headers.isEmpty()) return "";

        int cols = headers.size();
        int[] widths = new int[cols];

        for (int i = 0; i < cols; i++) {
            widths[i] = stripAnsi(headers.get(i)).length();
        }
        for (List<String> row : rows) {
            for (int i = 0; i < Math.min(cols, row.size()); i++) {
                widths[i] = Math.max(widths[i], stripAnsi(row.get(i)).length());
            }
        }

        String border = buildBorder(widths);
        StringBuilder sb = new StringBuilder();
        sb.append(border);
        appendRow(sb, headers, widths, cols);
        sb.append(border);

        for (List<String> row : rows) {
            appendRow(sb, row, widths, cols);
        }

        sb.append(border);
        return sb.toString();
    }

    private static String buildBorder(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            sb.append("-".repeat(w + 2)).append("+");
        }
        sb.append("\n");
        return sb.toString();
    }

    private static void appendRow(StringBuilder sb, List<String> row, int[] widths, int cols) {
        sb.append("|");
        for (int i = 0; i < cols; i++) {
            String val = (i < row.size() && row.get(i) != null) ? row.get(i) : "";
            sb.append(" ").append(padRight(val, widths[i])).append(" |");
        }
        sb.append("\n");
    }

    private static String padRight(String s, int n) {
        int visibleLength = stripAnsi(s).length();
        int padding = Math.max(0, n - visibleLength);
        return s + " ".repeat(padding);
    }

    public static String stripAnsi(String s) {
        if (s == null) return "";
        return s.replaceAll("\u001B\\[[;\\d]*m", "");
    }
}
