package com.supify.model.enums;

public enum UserRole {
    BUYER("Buyer"),
    SUPPLIER("Supplier"),
    VERIFIER("Verifier"),
    ADMIN("Platform Admin");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
