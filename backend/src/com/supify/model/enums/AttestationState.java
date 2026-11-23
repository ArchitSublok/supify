package com.supify.model.enums;

public enum AttestationState {
    ACTIVE("Active"),
    EXPIRED("Expired"),
    REVOKED("Revoked"),
    SUPERSEDED("Superseded");

    private final String label;

    AttestationState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
