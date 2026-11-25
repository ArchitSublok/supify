package com.supify.model;

import java.time.LocalDateTime;

// Layer 3: verifier decision record (final so fields cannot be changed later)
public final class VerificationCheck {
    private final String id;
    private final String claimId;
    private final VerificationMethod method;
    private final String outcome;
    private final String verifierName;
    private final LocalDateTime checkedAt;
    private final String note;

    public VerificationCheck(String id, String claimId, VerificationMethod method,
                             String outcome, String verifierName, String note) {
        this.id = id;
        this.claimId = claimId;
        this.method = method;
        this.outcome = outcome;
        this.verifierName = verifierName;
        this.checkedAt = LocalDateTime.now();
        this.note = note;
    }

    public String getId() { return id; }
    public String getClaimId() { return claimId; }
    public VerificationMethod getMethod() { return method; }
    public String getOutcome() { return outcome; }
    public String getVerifierName() { return verifierName; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
    public String getNote() { return note; }

    @Override
    public String toString() {
        return id + " | " + outcome + " via " + method + " by " + verifierName + " (" + note + ")";
    }
}
