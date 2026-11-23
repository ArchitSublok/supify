package com.supify.model.enums;

public enum ListingState {
    DRAFT("Draft"),
    PENDING_REVIEW("Pending Review"),
    LISTED("Publicly Listed"),
    SUSPENDED("Suspended"),
    DELISTED("Delisted");

    private final String label;

    ListingState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
