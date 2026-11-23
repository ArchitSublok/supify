package com.supify.model.enums;

public enum Pillar {
    IDENTITY("Identity", 1.0),
    LEGAL("Legal & Corporate", 1.0),
    FINANCIAL("Financial Health", 0.9),
    QUALITY("Quality & Standards", 1.2),
    CAPABILITY("Operational Capacity", 1.1),
    COMPLIANCE("Regulatory & Environmental", 1.0),
    REPUTATION("Market Reputation", 0.8);

    private final String label;
    private final double defaultWeight;

    Pillar(String label, double defaultWeight) {
        this.label = label;
        this.defaultWeight = defaultWeight;
    }

    public String getLabel() {
        return label;
    }

    public double getDefaultWeight() {
        return defaultWeight;
    }
}
