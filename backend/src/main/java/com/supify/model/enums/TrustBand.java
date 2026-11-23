package com.supify.model.enums;

public enum TrustBand {
    UNVERIFIED("Unverified", 0, "\u001B[31m"), // Red
    BASIC("Basic Verified", 1, "\u001B[33m"),      // Yellow
    VERIFIED("Verified", 2, "\u001B[32m"),         // Green
    AUDITED("Fully Audited", 3, "\u001B[35m");     // Magenta

    private final String label;
    private final int tierLevel;
    private final String ansiColor;

    TrustBand(String label, int tierLevel, String ansiColor) {
        this.label = label;
        this.tierLevel = tierLevel;
        this.ansiColor = ansiColor;
    }

    public String getLabel() {
        return label;
    }

    public int getTierLevel() {
        return tierLevel;
    }

    public String getAnsiColor() {
        return ansiColor;
    }

    public String toFormattedString() {
        return ansiColor + "[" + label + "]\u001B[0m";
    }
}
