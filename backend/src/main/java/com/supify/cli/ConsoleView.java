package com.supify.cli;

import com.supify.concurrency.AlertBuffer;
import java.util.List;
import java.util.Scanner;

public class ConsoleView {
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String CYAN = "\u001B[36m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RED = "\u001B[31m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";

    private final Scanner scanner;

    public ConsoleView() {
        this.scanner = new Scanner(System.in);
    }

    public boolean hasNext() {
        return scanner.hasNextLine();
    }

    public void checkAndPrintAlerts() {
        List<String> alerts = AlertBuffer.getInstance().drainAlerts();
        if (!alerts.isEmpty()) {
            System.out.println();
            for (String alert : alerts) {
                System.out.println(YELLOW + BOLD + "⚡ [BACKGROUND DAEMON ALERT] " + RESET + YELLOW + alert + RESET);
            }
            System.out.println();
        }
    }

    public void printBanner(SessionContext session) {
        checkAndPrintAlerts();
        System.out.println(CYAN + BOLD + "═".repeat(82) + RESET);
        System.out.println(CYAN + BOLD + "   SUPIFY :: Supplier Discovery & Evidence-Backed Verification Platform (CLI)" + RESET);
        System.out.printf("   Active Role: " + GREEN + BOLD + "[%s]" + RESET + " (%s) | Comparison Tray: " + YELLOW + "[%d/4 vendors]" + RESET + "\n",
                session.getCurrentRole().name(),
                session.getCurrentUserName(),
                session.getComparisonTray().size());
        System.out.println(CYAN + BOLD + "═".repeat(82) + RESET);
    }

    public String promptString(String label) {
        checkAndPrintAlerts();
        System.out.print(BOLD + label + ": " + RESET);
        if (!scanner.hasNextLine()) return "";
        return scanner.nextLine().trim();
    }

    public int promptInt(String label, int defaultValue) {
        checkAndPrintAlerts();
        System.out.print(BOLD + label + " [" + defaultValue + "]: " + RESET);
        if (!scanner.hasNextLine()) return defaultValue;
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void pressEnterToContinue() {
        System.out.print(CYAN + "\nPress ENTER to continue..." + RESET);
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }
}
