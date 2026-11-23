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

    private static final String LINE = "=".repeat(82);
    private final Scanner scanner;

    public ConsoleView() {
        this.scanner = new Scanner(System.in);
    }

    public boolean hasNext() {
        return scanner.hasNextLine();
    }

    public void checkAndPrintAlerts() {
        List<String> alerts = AlertBuffer.getInstance().drainAlerts();
        if (alerts.isEmpty()) return;

        System.out.println();
        for (String alert : alerts) {
            System.out.println(YELLOW + BOLD + "[ALERT] " + RESET + YELLOW + alert + RESET);
        }
        System.out.println();
    }

    public void printBanner(SessionContext session) {
        checkAndPrintAlerts();
        System.out.println(CYAN + BOLD + LINE + RESET);
        System.out.println(CYAN + BOLD + "  SUPIFY :: Supplier Discovery & Evidence-Backed Verification" + RESET);
        System.out.printf("  Role: %s[%s]%s (%s)  |  Compare Tray: %s[%d/4 vendors]%s%n",
                GREEN + BOLD, session.getCurrentRole().name(), RESET, session.getCurrentUserName(),
                YELLOW, session.getComparisonTray().size(), RESET);
        System.out.println(CYAN + BOLD + LINE + RESET);
    }

    public void printSection(String title) {
        System.out.println();
        System.out.println(CYAN + BOLD + LINE + RESET);
        System.out.println(CYAN + BOLD + "  " + title + RESET);
        System.out.println(CYAN + BOLD + LINE + RESET);
    }

    public void printOk(String msg) {
        System.out.println(GREEN + BOLD + "[OK] " + RESET + GREEN + msg + RESET);
    }

    public void printWarn(String msg) {
        System.out.println(YELLOW + "[!] " + msg + RESET);
    }

    public void printError(String msg) {
        System.out.println(RED + "[ERROR] " + msg + RESET);
    }

    public String promptString(String label) {
        checkAndPrintAlerts();
        System.out.print(BOLD + label + ": " + RESET);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "";
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
