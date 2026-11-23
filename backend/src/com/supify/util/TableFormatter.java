package com.supify.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility for rendering formatted ASCII/Unicode terminal tables.
 */
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

        StringBuilder sb = new StringBuilder();

        // Top border
        sb.append("┌");
        for (int i = 0; i < cols; i++) {
            sb.append("─".repeat(widths[i] + 2));
            if (i < cols - 1) sb.append("┬");
        }
        sb.append("┐\n");

        // Headers
        sb.append("│");
        for (int i = 0; i < cols; i++) {
            sb.append(" ").append(padRight(headers.get(i), widths[i])).append(" │");
        }
        sb.append("\n");

        // Header separator
        sb.append("├");
        for (int i = 0; i < cols; i++) {
            sb.append("─".repeat(widths[i] + 2));
            if (i < cols - 1) sb.append("┼");
        }
        sb.append("┤\n");

        // Data rows
        for (List<String> row : rows) {
            sb.append("│");
            for (int i = 0; i < cols; i++) {
                String val = i < row.size() ? row.get(i) : "";
                sb.append(" ").append(padRight(val, widths[i])).append(" │");
            }
            sb.append("\n");
        }

        // Bottom border
        sb.append("└");
        for (int i = 0; i < cols; i++) {
            sb.append("─".repeat(widths[i] + 2));
            if (i < cols - 1) sb.append("┴");
        }
        sb.append("┘\n");

        return sb.toString();
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
