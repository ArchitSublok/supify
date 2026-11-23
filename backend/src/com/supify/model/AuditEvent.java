package com.supify.model;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** Immutable audit record stamped with a strictly increasing AtomicLong sequence number. */
public final class AuditEvent implements Identifiable<Long> {
    private static final AtomicLong SEQUENCE = new AtomicLong(1000);

    private final long sequenceNumber;
    private final Instant timestamp;
    private final String actor;
    private final String action;
    private final String subjectType;
    private final String subjectId;
    private final String details;

    public AuditEvent(String actor, String action, String subjectType, String subjectId, String details) {
        this.sequenceNumber = SEQUENCE.getAndIncrement();
        this.timestamp = Instant.now();
        this.actor = Objects.requireNonNull(actor, "actor is required");
        this.action = Objects.requireNonNull(action, "action is required");
        this.subjectType = Objects.requireNonNull(subjectType, "subjectType is required");
        this.subjectId = Objects.requireNonNull(subjectId, "subjectId is required");
        this.details = details != null ? details : "";
    }

    @Override
    public Long getId() {
        return sequenceNumber;
    }

    public long getSequenceNumber() { return sequenceNumber; }
    public Instant getTimestamp() { return timestamp; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getSubjectType() { return subjectType; }
    public String getSubjectId() { return subjectId; }
    public String getDetails() { return details; }

    @Override
    public String toString() {
        return String.format("#%d [%s] %s by %s on %s:%s -> %s",
                sequenceNumber, timestamp, action, actor, subjectType, subjectId, details);
    }
}
