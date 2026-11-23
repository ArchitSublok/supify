package com.supify.model;

import com.supify.model.enums.AttestationState;
import com.supify.model.enums.VerificationMethod;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Attestation extends AbstractAuditEntity {
    private final UUID claimId;
    private final UUID sourceCheckId;
    private final VerificationMethod methodAchieved;
    private final Instant verifiedAt;
    private Instant validUntil;
    private AttestationState state;
    private String revocationReason;
    private final String issuedBy;

    public Attestation(UUID claimId, UUID sourceCheckId, VerificationMethod methodAchieved, Instant validUntil, String issuedBy) {
        super();
        this.claimId = Objects.requireNonNull(claimId, "claimId is required");
        this.sourceCheckId = Objects.requireNonNull(sourceCheckId, "sourceCheckId is required");
        this.methodAchieved = Objects.requireNonNull(methodAchieved, "methodAchieved is required");
        this.verifiedAt = Instant.now();
        this.validUntil = validUntil;
        this.state = AttestationState.ACTIVE;
        this.issuedBy = issuedBy;
    }

    public Attestation(UUID id, Instant createdAt, Instant updatedAt, UUID claimId, UUID sourceCheckId, VerificationMethod methodAchieved, Instant verifiedAt, Instant validUntil, AttestationState state, String revocationReason, String issuedBy) {
        super(id, createdAt, updatedAt);
        this.claimId = claimId;
        this.sourceCheckId = sourceCheckId;
        this.methodAchieved = methodAchieved;
        this.verifiedAt = verifiedAt;
        this.validUntil = validUntil;
        this.state = state;
        this.revocationReason = revocationReason;
        this.issuedBy = issuedBy;
    }

    public UUID getClaimId() { return claimId; }
    public UUID getSourceCheckId() { return sourceCheckId; }
    public VerificationMethod getMethodAchieved() { return methodAchieved; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getValidUntil() { return validUntil; }
    public AttestationState getState() { return state; }
    public String getRevocationReason() { return revocationReason; }
    public String getIssuedBy() { return issuedBy; }

    public boolean isExpired() {
        return validUntil != null && Instant.now().isAfter(validUntil);
    }

    public synchronized boolean checkAndApplyDecay() {
        if (state == AttestationState.ACTIVE && isExpired()) {
            this.state = AttestationState.EXPIRED;
            touch();
            return true;
        }
        return false;
    }

    public synchronized void revoke(String reason) {
        this.state = AttestationState.REVOKED;
        this.revocationReason = reason;
        touch();
    }

    public String getDecayCountdownString() {
        if (state == AttestationState.EXPIRED) return "EXPIRED";
        if (state == AttestationState.REVOKED) return "REVOKED";
        if (validUntil == null) return "Indefinite";

        Duration remaining = Duration.between(Instant.now(), validUntil);
        if (remaining.isNegative()) return "EXPIRED";

        long days = remaining.toDays();
        if (days > 0) return days + "d remaining";
        long hours = remaining.toHours();
        if (hours > 0) return hours + "h remaining";
        long minutes = remaining.toMinutes();
        if (minutes > 0) return minutes + "m remaining";
        return remaining.getSeconds() + "s remaining";
    }
}
