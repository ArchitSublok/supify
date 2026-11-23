package com.supify.model.enums;

public enum VerificationMethod {
    SELF_DECLARED("Self Declared", 1),
    DOCUMENT_REVIEW("Document Review", 2),
    THIRD_PARTY_API("Third-Party Registry API", 3),
    HUMAN_AUDIT("Human Auditor Verification", 4),
    SITE_VISIT("Physical Site Inspection", 5);

    private final String label;
    private final int rigorRank;

    VerificationMethod(String label, int rigorRank) {
        this.label = label;
        this.rigorRank = rigorRank;
    }

    public String getLabel() {
        return label;
    }

    public int getRigorRank() {
        return rigorRank;
    }
}
