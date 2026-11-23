package com.supify.model.enums;

public enum EvidenceKind {
    PDF("PDF Document"),
    IMAGE("Scanned Image"),
    JSON("API / Registry JSON Payload"),
    URL("External Verification URL");

    private final String description;

    EvidenceKind(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
