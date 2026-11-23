package com.supify.model;

import com.supify.model.enums.ClaimStatus;
import com.supify.model.enums.Pillar;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Claim extends AbstractAuditEntity {
    private final UUID organisationId;
    private final String claimTypeKey;
    private final String label;
    private final Pillar pillar;
    private final String assertedValue;
    private ClaimStatus status;
    private Instant submittedAt;
    private UUID supersededBy;

    public Claim(UUID organisationId, String claimTypeKey, String label, Pillar pillar, String assertedValue) {
        super();
        this.organisationId = Objects.requireNonNull(organisationId, "organisationId is required");
        this.claimTypeKey = Objects.requireNonNull(claimTypeKey, "claimTypeKey is required");
        this.label = Objects.requireNonNull(label, "label is required");
        this.pillar = Objects.requireNonNull(pillar, "pillar is required");
        this.assertedValue = assertedValue;
        this.status = ClaimStatus.DRAFT;
    }

    public Claim(UUID id, Instant createdAt, Instant updatedAt, UUID organisationId, String claimTypeKey, String label, Pillar pillar, String assertedValue, ClaimStatus status, Instant submittedAt, UUID supersededBy) {
        super(id, createdAt, updatedAt);
        this.organisationId = organisationId;
        this.claimTypeKey = claimTypeKey;
        this.label = label;
        this.pillar = pillar;
        this.assertedValue = assertedValue;
        this.status = status;
        this.submittedAt = submittedAt;
        this.supersededBy = supersededBy;
    }

    public UUID getOrganisationId() { return organisationId; }
    public String getClaimTypeKey() { return claimTypeKey; }
    public String getLabel() { return label; }
    public Pillar getPillar() { return pillar; }
    public String getAssertedValue() { return assertedValue; }
    public ClaimStatus getStatus() { return status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public UUID getSupersededBy() { return supersededBy; }

    public synchronized void submit() {
        this.status = ClaimStatus.SUBMITTED;
        this.submittedAt = Instant.now();
        touch();
    }

    public synchronized void setStatus(ClaimStatus status) {
        this.status = status;
        touch();
    }

    public synchronized void markSuperseded(UUID newClaimId) {
        this.supersededBy = newClaimId;
        this.status = ClaimStatus.REVOKED;
        touch();
    }
}
