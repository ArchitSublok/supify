package com.supify.model;

// Trust levels from 0 (unverified) to 3 (audited)
public enum TrustBand {
    UNVERIFIED(0),
    BASIC(1),
    VERIFIED(2),
    AUDITED(3);

    private final int level;

    TrustBand(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }
}
