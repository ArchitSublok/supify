package com.supify.model;

import com.supify.model.enums.CheckOutcome;
import com.supify.model.enums.VerificationMethod;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable verification event. Platform-authored.
 * Once written, this record cannot be modified.
 */
public final class VerificationCheck implements Identifiable<UUID> {
    private final UUID id;
    private final UUID claimId;
    private final VerificationMethod method;
    private final CheckOutcome outcome;
    private final double confidence;
    private final String performedBy;
    private final Instant performedAt;
    private final String rationale;

    public VerificationCheck(UUID claimId, VerificationMethod method, CheckOutcome outcome, double confidence, String performedBy, String rationale) {
        this.id = UUID.randomUUID();
        this.claimId = Objects.requireNonNull(claimId, "claimId is required");
        this.method = Objects.requireNonNull(method, "method is required");
        this.outcome = Objects.requireNonNull(outcome, "outcome is required");
        this.confidence = confidence;
        this.performedBy = Objects.requireNonNull(performedBy, "performedBy is required");
        this.performedAt = Instant.now();
        this.rationale = Objects.requireNonNull(rationale, "rationale is mandatory");
    }

    public VerificationCheck(UUID id, UUID claimId, VerificationMethod method, CheckOutcome outcome, double confidence, String performedBy, Instant performedAt, String rationale) {
        this.id = id;
        this.claimId = claimId;
        this.method = method;
        this.outcome = outcome;
        this.confidence = confidence;
        this.performedBy = performedBy;
        this.performedAt = performedAt;
        this.rationale = rationale;
    }

    @Override
    public UUID getId() { return id; }
    public UUID getClaimId() { return claimId; }
    public VerificationMethod getMethod() { return method; }
    public CheckOutcome getOutcome() { return outcome; }
    public double getConfidence() { return confidence; }
    public String getPerformedBy() { return performedBy; }
    public Instant getPerformedAt() { return performedAt; }
    public String getRationale() { return rationale; }
}
