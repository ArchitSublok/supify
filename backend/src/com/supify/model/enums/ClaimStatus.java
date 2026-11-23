package com.supify.model.enums;

public enum ClaimStatus {
    DRAFT("Draft"),
    SUBMITTED("Submitted for Review"),
    IN_REVIEW("Under Review"),
    VERIFIED("Verified & Active"),
    REJECTED("Rejected"),
    EXPIRED("Expired"),
    REVOKED("Revoked");

    private final String label;

    ClaimStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
