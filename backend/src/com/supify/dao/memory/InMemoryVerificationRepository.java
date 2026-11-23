package com.supify.dao.memory;

import com.supify.dao.VerificationRepository;
import com.supify.model.Attestation;
import com.supify.model.Claim;
import com.supify.model.Evidence;
import com.supify.model.VerificationCheck;
import com.supify.model.VerificationTask;
import com.supify.model.enums.AttestationState;
import com.supify.model.enums.ClaimStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/** Thread-safe in-memory store keeping 4-layer spine mutations atomic under ReentrantReadWriteLock. */
public class InMemoryVerificationRepository implements VerificationRepository {
    private final Map<UUID, Claim> claims = new ConcurrentHashMap<>();
    private final Map<UUID, Evidence> evidenceMap = new ConcurrentHashMap<>();
    private final Map<UUID, VerificationCheck> checks = new ConcurrentHashMap<>();
    private final Map<UUID, Attestation> attestations = new ConcurrentHashMap<>();
    private final Map<UUID, VerificationTask> tasks = new ConcurrentHashMap<>();

    // Priority heap ordered by priority descending and SLA deadline ascending
    private final PriorityBlockingQueue<VerificationTask> taskPriorityQueue = new PriorityBlockingQueue<>(50);

    // ReentrantReadWriteLock guarantees atomic spine updates without global blocking on reads
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    @Override
    public Claim saveClaim(Claim claim) {
        writeLock.lock();
        try {
            claims.put(claim.getId(), claim);
            return claim;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Optional<Claim> findClaimById(UUID id) {
        readLock.lock();
        try {
            return Optional.ofNullable(claims.get(id));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<Claim> findClaimsByOrganisation(UUID organisationId) {
        readLock.lock();
        try {
            return claims.values().stream()
                    .filter(c -> c.getOrganisationId().equals(organisationId))
                    .collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Evidence saveEvidence(Evidence evidence) {
        writeLock.lock();
        try {
            evidenceMap.put(evidence.getId(), evidence);
            return evidence;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public List<Evidence> findEvidenceByClaim(UUID claimId) {
        readLock.lock();
        try {
            return evidenceMap.values().stream()
                    .filter(e -> e.getClaimId().equals(claimId))
                    .collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public VerificationCheck recordCheck(VerificationCheck check) {
        writeLock.lock();
        try {
            checks.put(check.getId(), check);
            return check;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public List<VerificationCheck> findChecksByClaim(UUID claimId) {
        readLock.lock();
        try {
            return checks.values().stream()
                    .filter(c -> c.getClaimId().equals(claimId))
                    .collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Attestation saveAttestation(Attestation attestation) {
        writeLock.lock();
        try {
            attestations.put(attestation.getId(), attestation);
            return attestation;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Optional<Attestation> findActiveAttestationByClaim(UUID claimId) {
        readLock.lock();
        try {
            return attestations.values().stream()
                    .filter(a -> a.getClaimId().equals(claimId) && a.getState() == AttestationState.ACTIVE)
                    .findFirst();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<Attestation> findAttestationsByState(AttestationState state) {
        readLock.lock();
        try {
            return attestations.values().stream()
                    .filter(a -> a.getState() == state)
                    .collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<Attestation> findAllAttestations() {
        readLock.lock();
        try {
            return new ArrayList<>(attestations.values());
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public VerificationTask enqueueTask(VerificationTask task) {
        writeLock.lock();
        try {
            tasks.put(task.getId(), task);
            taskPriorityQueue.offer(task);
            return task;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Optional<VerificationTask> findTaskById(UUID taskId) {
        readLock.lock();
        try {
            return Optional.ofNullable(tasks.get(taskId));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<VerificationTask> findPendingTasks() {
        readLock.lock();
        try {
            return new ArrayList<>(taskPriorityQueue);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public PriorityBlockingQueue<VerificationTask> getTaskPriorityQueue() {
        return taskPriorityQueue;
    }

    @Override
    public void executeVerificationDecision(UUID taskId, VerificationCheck check, Attestation attestation) {
        writeLock.lock();
        try {
            // 1. Record immutable check
            checks.put(check.getId(), check);

            // 2. Issue or update attestation
            if (attestation != null) {
                attestations.put(attestation.getId(), attestation);
            }

            // 3. Update claim status
            Claim claim = claims.get(check.getClaimId());
            if (claim != null) {
                switch (check.getOutcome()) {
                    case PASS -> claim.setStatus(ClaimStatus.VERIFIED);
                    case FAIL -> claim.setStatus(ClaimStatus.REJECTED);
                    case INCONCLUSIVE, NEEDS_MORE_INFO -> claim.setStatus(ClaimStatus.IN_REVIEW);
                }
            }

            // 4. Update and remove task from priority queue
            VerificationTask task = tasks.get(taskId);
            if (task != null) {
                task.decide();
                taskPriorityQueue.remove(task);
            }
        } finally {
            writeLock.unlock();
        }
    }
}
