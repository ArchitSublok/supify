package com.supify.model;

import com.supify.model.enums.EvidenceKind;
import java.time.Instant;
import java.util.UUID;

public class Evidence extends AbstractAuditEntity {
    private final UUID claimId;
    private final String storageRef;
    private final EvidenceKind kind;
    private final String title;
    private final Instant issuedAt;
    private final Instant expiresAt;

    public Evidence(UUID claimId, String storageRef, EvidenceKind kind, String title, Instant issuedAt, Instant expiresAt) {
        super();
        this.claimId = claimId;
        this.storageRef = storageRef;
        this.kind = kind;
        this.title = title;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public UUID getClaimId() { return claimId; }
    public String getStorageRef() { return storageRef; }
    public EvidenceKind getKind() { return kind; }
    public String getTitle() { return title; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
