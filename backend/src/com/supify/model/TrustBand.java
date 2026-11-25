package com.supify.model;

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
