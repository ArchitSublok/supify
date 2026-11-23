package com.supify.service;

import com.supify.dao.AuditRepository;
import com.supify.dao.VerificationRepository;
import com.supify.model.Attestation;
import com.supify.model.AuditEvent;
import com.supify.model.Claim;
import com.supify.model.Evidence;
import com.supify.model.VerificationCheck;
import com.supify.model.VerificationTask;
import com.supify.model.enums.CheckOutcome;
import com.supify.model.enums.VerificationMethod;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class VerificationService {
    private final VerificationRepository verificationRepo;
    private final TrustEvaluationService trustEvaluationService;
    private final AuditRepository auditRepo;

    public VerificationService(VerificationRepository verificationRepo, TrustEvaluationService trustEvaluationService, AuditRepository auditRepo) {
        this.verificationRepo = Objects.requireNonNull(verificationRepo, "verificationRepo is required");
        this.trustEvaluationService = Objects.requireNonNull(trustEvaluationService, "trustEvaluationService is required");
        this.auditRepo = Objects.requireNonNull(auditRepo, "auditRepo is required");
    }

    public synchronized Claim submitClaimWithEvidence(UUID organisationId, String supplierName, String key, String label, com.supify.model.enums.Pillar pillar, String assertedValue, Evidence evidence) {
        Claim claim = new Claim(organisationId, key, label, pillar, assertedValue);
        claim.submit();
        verificationRepo.saveClaim(claim);

        if (evidence != null) {
            verificationRepo.saveEvidence(evidence);
        }

        // Generate and enqueue review task in PriorityBlockingQueue
        VerificationTask task = new VerificationTask(
                claim.getId(),
                organisationId,
                supplierName,
                label,
                (int) (pillar.getDefaultWeight() * 70),
                false,
                Instant.now().plus(Duration.ofHours(24))
        );
        verificationRepo.enqueueTask(task);

        auditRepo.recordEvent(new AuditEvent(
                "SUPPLIER:" + supplierName,
                "claim.submitted",
                "Claim",
                claim.getId().toString(),
                "Asserted " + label + " (" + assertedValue + ")"
        ));

        return claim;
    }

    public synchronized boolean processVerifierDecision(UUID taskId, String verifierName, VerificationMethod method, CheckOutcome outcome, String rationale, Duration validityDuration) {
        Optional<VerificationTask> taskOpt = verificationRepo.findTaskById(taskId);
        if (taskOpt.isEmpty()) return false;

        VerificationTask task = taskOpt.get();
        UUID claimId = task.getClaimId();

        // 1. Create immutable verification check
        VerificationCheck check = new VerificationCheck(
                claimId,
                method,
                outcome,
                outcome == CheckOutcome.PASS ? 0.95 : 0.20,
                verifierName,
                rationale
        );

        // 2. Create attestation if passed
        Attestation attestation = null;
        if (outcome == CheckOutcome.PASS) {
            Instant validUntil = validityDuration != null ? Instant.now().plus(validityDuration) : Instant.now().plus(Duration.ofDays(365));
            attestation = new Attestation(claimId, check.getId(), method, validUntil, verifierName);
        }

        // 3. Atomically execute decision in repository
        verificationRepo.executeVerificationDecision(taskId, check, attestation);

        // 4. Record audit event
        auditRepo.recordEvent(new AuditEvent(
                "VERIFIER:" + verifierName,
                "verification.decided",
                "VerificationTask",
                taskId.toString(),
                "Outcome: " + outcome + " via " + method.getLabel() + ". Rationale: " + rationale
        ));

        // 5. Recompute supplier trust score and band
        trustEvaluationService.evaluateSupplier(task.getOrganisationId());
        return true;
    }
}
