package com.supify.model;

import com.supify.model.enums.TaskState;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Operational task drives the verifier review queue.
 * Implements Comparable for priority-based ordering in heap/PriorityBlockingQueue.
 */
public class VerificationTask extends AbstractAuditEntity implements Comparable<VerificationTask> {
    private final UUID claimId;
    private final UUID organisationId;
    private final String supplierName;
    private final String claimLabel;
    private TaskState state;
    private String assignedTo;
    private int priority;
    private final boolean requiresDual;
    private final Instant slaDueAt;
    private Instant claimedAt;
    private Instant decidedAt;

    public VerificationTask(UUID claimId, UUID organisationId, String supplierName, String claimLabel, int initialPriority, boolean requiresDual, Instant slaDueAt) {
        super();
        this.claimId = Objects.requireNonNull(claimId, "claimId is required");
        this.organisationId = Objects.requireNonNull(organisationId, "organisationId is required");
        this.supplierName = Objects.requireNonNull(supplierName, "supplierName is required");
        this.claimLabel = Objects.requireNonNull(claimLabel, "claimLabel is required");
        this.priority = initialPriority;
        this.requiresDual = requiresDual;
        this.slaDueAt = slaDueAt != null ? slaDueAt : Instant.now().plus(Duration.ofHours(24));
        this.state = TaskState.QUEUED;
    }

    public VerificationTask(UUID id, Instant createdAt, Instant updatedAt, UUID claimId, UUID organisationId, String supplierName, String claimLabel, TaskState state, String assignedTo, int priority, boolean requiresDual, Instant slaDueAt, Instant claimedAt, Instant decidedAt) {
        super(id, createdAt, updatedAt);
        this.claimId = claimId;
        this.organisationId = organisationId;
        this.supplierName = supplierName;
        this.claimLabel = claimLabel;
        this.state = state;
        this.assignedTo = assignedTo;
        this.priority = priority;
        this.requiresDual = requiresDual;
        this.slaDueAt = slaDueAt;
        this.claimedAt = claimedAt;
        this.decidedAt = decidedAt;
    }

    public UUID getClaimId() { return claimId; }
    public UUID getOrganisationId() { return organisationId; }
    public String getSupplierName() { return supplierName; }
    public String getClaimLabel() { return claimLabel; }
    public TaskState getState() { return state; }
    public String getAssignedTo() { return assignedTo; }
    public int getPriority() { return priority; }
    public boolean isRequiresDual() { return requiresDual; }
    public Instant getSlaDueAt() { return slaDueAt; }
    public Instant getClaimedAt() { return claimedAt; }
    public Instant getDecidedAt() { return decidedAt; }

    public synchronized void claim(String verifierName) {
        this.state = TaskState.CLAIMED;
        this.assignedTo = verifierName;
        this.claimedAt = Instant.now();
        touch();
    }

    public synchronized void decide() {
        this.state = TaskState.DECIDED;
        this.decidedAt = Instant.now();
        touch();
    }

    public synchronized void escalate(int priorityBoost) {
        this.priority += priorityBoost;
        this.state = TaskState.ESCALATED;
        touch();
    }

    public String getSlaRemainingString() {
        if (state == TaskState.DECIDED) return "Resolved";
        Duration diff = Duration.between(Instant.now(), slaDueAt);
        if (diff.isNegative()) {
            return "BREACHED (" + Math.abs(diff.toHours()) + "h ago)";
        }
        long hours = diff.toHours();
        long minutes = diff.toMinutes() % 60;
        return hours + "h " + minutes + "m remaining";
    }

    @Override
    public int compareTo(VerificationTask other) {
        // High priority first
        int priorityComparison = Integer.compare(other.priority, this.priority);
        if (priorityComparison != 0) {
            return priorityComparison;
        }
        // Earlier SLA deadline first
        return this.slaDueAt.compareTo(other.slaDueAt);
    }
}
