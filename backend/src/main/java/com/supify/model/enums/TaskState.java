package com.supify.model.enums;

public enum TaskState {
    QUEUED("Queued"),
    CLAIMED("Claimed"),
    IN_REVIEW("In Review"),
    AWAITING_SUPPLIER("Awaiting Supplier"),
    DECIDED("Decided"),
    ESCALATED("Escalated");

    private final String label;

    TaskState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
