package com.supify.service;

import com.supify.concurrency.TaskBuffer;
import com.supify.dao.InMemoryStore;
import com.supify.exception.SupifyException;
import com.supify.model.Attestation;
import com.supify.model.Claim;
import com.supify.model.Evidence;
import com.supify.model.VerificationCheck;
import com.supify.model.VerificationMethod;

/** Enforces the 4-layer verification spine: Claim -> Evidence -> VerificationCheck -> Attestation. */
public class VerificationService {
    private final InMemoryStore store;
    private final SupplierService supplierService;
    private final TaskBuffer<Claim> reviewQueue;

    public VerificationService(InMemoryStore store, SupplierService supplierService, TaskBuffer<Claim> reviewQueue) {
        this.store = store;
        this.supplierService = supplierService;
        this.reviewQueue = reviewQueue;
    }

    public Claim submitClaim(String supplierId, String claimTypeKey, String label, String pillar,
                             String value, String docTitle, String docType, String filePath) throws SupifyException {
        if (store.findById(supplierId).isEmpty()) {
            throw new SupifyException("Supplier not found: " + supplierId);
        }
        if (label == null || label.isBlank()) {
            throw new SupifyException("Claim label cannot be empty.");
        }

        Claim claim = new Claim(store.nextId("CLM"), supplierId, claimTypeKey, label, pillar, value);
        store.saveClaim(claim);

        if (docTitle != null && !docTitle.isBlank()) {
            Evidence evidence = new Evidence(store.nextId("EVD"), claim.getId(), docTitle, docType, filePath);
            store.saveEvidence(evidence);
        }

        if (reviewQueue != null) {
            try {
                reviewQueue.produce(claim);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return claim;
    }

    public synchronized VerificationCheck verifyClaim(String claimId, String verifierName,
                                                      VerificationMethod method, boolean approved,
                                                      String note, int validSeconds) throws SupifyException {
        Claim claim = store.findClaimById(claimId)
                .orElseThrow(() -> new SupifyException("Claim not found: " + claimId));

        if (note == null || note.isBlank()) {
            throw new SupifyException("Verifier note is required.");
        }

        String outcome = approved ? "PASS" : "FAIL";
        VerificationCheck check = new VerificationCheck(
                store.nextId("CHK"), claimId, method, outcome, verifierName, note);

        Attestation attestation = null;
        if (approved) {
            attestation = new Attestation(store.nextId("ATT"), claimId, check.getId(), method, validSeconds);
        }

        store.recordDecision(claim, check, attestation);
        supplierService.recalculateTrust(claim.getSupplierId());
        return check;
    }
}
