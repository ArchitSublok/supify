package com.supify.model;

public enum VerificationMethod {
    SELF_DECLARED(0.25),
    DOCUMENT_REVIEW(0.60),
    THIRD_PARTY_API(0.85),
    HUMAN_AUDIT(0.95),
    SITE_VISIT(1.00);

    private final double score;

    VerificationMethod(double score) {
        this.score = score;
    }

    public double getScore() {
        return score;
    }
}
