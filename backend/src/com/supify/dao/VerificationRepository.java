package com.supify.dao;

import com.supify.model.Attestation;
import com.supify.model.Claim;
import com.supify.model.Evidence;
import com.supify.model.VerificationCheck;
import com.supify.model.VerificationTask;
import com.supify.model.enums.AttestationState;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;

/** Repository contract for the 4-layer verification spine and task queue. */
public interface VerificationRepository {
    // Claims
    Claim saveClaim(Claim claim);
    Optional<Claim> findClaimById(UUID id);
    List<Claim> findClaimsByOrganisation(UUID organisationId);

    // Evidence
    Evidence saveEvidence(Evidence evidence);
    List<Evidence> findEvidenceByClaim(UUID claimId);

    // Checks (Immutable events)
    VerificationCheck recordCheck(VerificationCheck check);
    List<VerificationCheck> findChecksByClaim(UUID claimId);

    // Attestations
    Attestation saveAttestation(Attestation attestation);
    Optional<Attestation> findActiveAttestationByClaim(UUID claimId);
    List<Attestation> findAttestationsByState(AttestationState state);
    List<Attestation> findAllAttestations();

    // Verification Tasks & Priority Queue
    VerificationTask enqueueTask(VerificationTask task);
    Optional<VerificationTask> findTaskById(UUID taskId);
    List<VerificationTask> findPendingTasks();
    PriorityBlockingQueue<VerificationTask> getTaskPriorityQueue();

    // Atomic transaction for audit decision
    void executeVerificationDecision(UUID taskId, VerificationCheck check, Attestation attestation);
}
