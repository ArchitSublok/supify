package com.supify.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Base entity supplying UUID identity and creation/update timestamps. */
public abstract class AbstractAuditEntity implements Identifiable<UUID> {
    private final UUID id;
    private final Instant createdAt;
    private Instant updatedAt;

    protected AbstractAuditEntity() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    protected AbstractAuditEntity(UUID id, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    @Override
    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
