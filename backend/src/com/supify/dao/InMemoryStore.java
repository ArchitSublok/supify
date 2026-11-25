package com.supify.dao;

import com.supify.model.Attestation;
import com.supify.model.Claim;
import com.supify.model.Evidence;
import com.supify.model.Supplier;
import com.supify.model.VerificationCheck;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Thread-safe in-memory data store shared across CLI menus and background monitor threads. */
public class InMemoryStore implements Repository<Supplier, String> {
    private final Map<String, Supplier> suppliers = new HashMap<>();
    private final Map<String, Claim> claims = new HashMap<>();
    private final List<Evidence> evidences = new ArrayList<>();
    private final List<VerificationCheck> checks = new ArrayList<>();
    private final Map<String, Attestation> attestations = new HashMap<>();
    private int idCounter = 100;

    public synchronized String nextId(String prefix) {
        idCounter++;
        return prefix + "-" + idCounter;
    }

    @Override
    public synchronized Supplier save(Supplier supplier) {
        suppliers.put(supplier.getId(), supplier);
        return supplier;
    }

    @Override
    public synchronized Optional<Supplier> findById(String id) {
        return Optional.ofNullable(suppliers.get(id));
    }

    @Override
    public synchronized List<Supplier> findAll() {
        return new ArrayList<>(suppliers.values());
    }

    @Override
    public synchronized List<Supplier> findMatching(Predicate<? super Supplier> filter) {
        return suppliers.values().stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized boolean deleteById(String id) {
        return suppliers.remove(id) != null;
    }

    @Override
    public synchronized long count() {
        return suppliers.size();
    }

    public synchronized void saveClaim(Claim claim) {
        claims.put(claim.getId(), claim);
    }

    public synchronized Optional<Claim> findClaimById(String claimId) {
        return Optional.ofNullable(claims.get(claimId));
    }

    public synchronized List<Claim> getClaimsForSupplier(String supplierId) {
        return claims.values().stream()
                .filter(c -> c.getSupplierId().equals(supplierId))
                .collect(Collectors.toList());
    }

    public synchronized List<Claim> getPendingClaims() {
        return claims.values().stream()
                .filter(c -> "SUBMITTED".equals(c.getStatus()))
                .collect(Collectors.toList());
    }

    public synchronized void saveEvidence(Evidence evidence) {
        evidences.add(evidence);
    }

    public synchronized List<Evidence> getEvidenceForClaim(String claimId) {
        return evidences.stream()
                .filter(e -> e.getClaimId().equals(claimId))
                .collect(Collectors.toList());
    }

    public synchronized void saveCheck(VerificationCheck check) {
        checks.add(check);
    }

    public synchronized List<VerificationCheck> getChecksForClaim(String claimId) {
        return checks.stream()
                .filter(c -> c.getClaimId().equals(claimId))
                .collect(Collectors.toList());
    }

    public synchronized void saveAttestation(Attestation attestation) {
        attestations.put(attestation.getId(), attestation);
    }

    public synchronized Optional<Attestation> getActiveAttestationForClaim(String claimId) {
        return attestations.values().stream()
                .filter(a -> a.getClaimId().equals(claimId) && "ACTIVE".equals(a.getState()))
                .findFirst();
    }

    public synchronized List<Attestation> getActiveAttestations() {
        return attestations.values().stream()
                .filter(a -> "ACTIVE".equals(a.getState()))
                .collect(Collectors.toList());
    }

    /** Executes check recording, optional attestation creation, and claim status update atomically. */
    public synchronized void recordDecision(Claim claim, VerificationCheck check, Attestation attestation) {
        checks.add(check);
        if (attestation != null) {
            attestations.put(attestation.getId(), attestation);
        }
        claim.setStatus("PASS".equals(check.getOutcome()) ? "VERIFIED" : "REJECTED");
        claims.put(claim.getId(), claim);
    }
}
