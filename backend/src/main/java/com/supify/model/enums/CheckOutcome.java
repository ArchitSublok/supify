package com.supify.model.enums;

public enum CheckOutcome {
    PASS("Pass"),
    FAIL("Fail"),
    INCONCLUSIVE("Inconclusive"),
    NEEDS_MORE_INFO("Needs More Information");

    private final String label;

    CheckOutcome(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
