package com.supify.service;

import com.supify.dao.SupplierRepository;
import com.supify.dao.VerificationRepository;
import com.supify.model.Attestation;
import com.supify.model.Claim;
import com.supify.model.SupplierProfile;
import com.supify.model.TrustSnapshot;
import com.supify.model.enums.AttestationState;
import com.supify.model.enums.Pillar;
import com.supify.model.enums.TrustBand;
import com.supify.model.enums.VerificationMethod;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** Recomputes supplier pillar scores and applies trust band capping rules from active attestations. */
public class TrustEvaluationService {
    private final SupplierRepository supplierRepo;
    private final VerificationRepository verificationRepo;

    public TrustEvaluationService(SupplierRepository supplierRepo, VerificationRepository verificationRepo) {
        this.supplierRepo = supplierRepo;
        this.verificationRepo = verificationRepo;
    }

    public synchronized TrustSnapshot evaluateSupplier(UUID organisationId) {
        List<Claim> claims = verificationRepo.findClaimsByOrganisation(organisationId);
        Map<Pillar, Double> pillarScores = new EnumMap<>(Pillar.class);

        // Group active attestations by pillar
        for (Pillar p : Pillar.values()) {
            pillarScores.put(p, 0.0);
        }

        boolean hasIdentityVerified = false;
        boolean hasLegalVerified = false;
        boolean hasHighRigorAudit = false;

        for (Claim claim : claims) {
            Optional<Attestation> attestationOpt = verificationRepo.findActiveAttestationByClaim(claim.getId());
            if (attestationOpt.isPresent() && attestationOpt.get().getState() == AttestationState.ACTIVE) {
                Attestation att = attestationOpt.get();
                double scoreContribution = calculateMethodRigorScore(att.getMethodAchieved());

                pillarScores.merge(claim.getPillar(), scoreContribution, (oldVal, newVal) -> Math.min(1.0, oldVal + newVal));

                if (claim.getPillar() == Pillar.IDENTITY) hasIdentityVerified = true;
                if (claim.getPillar() == Pillar.LEGAL) hasLegalVerified = true;
                if (att.getMethodAchieved() == VerificationMethod.HUMAN_AUDIT || att.getMethodAchieved() == VerificationMethod.SITE_VISIT) {
                    hasHighRigorAudit = true;
                }
            }
        }

        // Weighted overall score calculation
        double totalWeightedScore = 0.0;
        double totalWeight = 0.0;
        for (Pillar p : Pillar.values()) {
            double score = pillarScores.get(p);
            double weight = p.getDefaultWeight();
            totalWeightedScore += (score * weight);
            totalWeight += weight;
        }
        double rawOverall = totalWeight > 0 ? (totalWeightedScore / totalWeight) : 0.0;

        // Apply Axiom Capping Rules
        TrustBand band;
        if (!hasIdentityVerified) {
            band = TrustBand.UNVERIFIED;
            rawOverall = Math.min(rawOverall, 0.30);
        } else if (!hasLegalVerified) {
            band = TrustBand.BASIC;
            rawOverall = Math.min(rawOverall, 0.65);
        } else if (hasHighRigorAudit && rawOverall >= 0.85) {
            band = TrustBand.AUDITED;
        } else {
            band = TrustBand.VERIFIED;
        }

        // Update profile
        Optional<SupplierProfile> profileOpt = supplierRepo.findProfileByOrganisationId(organisationId);
        if (profileOpt.isPresent()) {
            SupplierProfile profile = profileOpt.get();
            profile.setTrustBand(band);
            profile.setOverallTrustScore(rawOverall);
            supplierRepo.saveProfile(profile);
        }

        TrustSnapshot snapshot = new TrustSnapshot(organisationId, band, rawOverall, pillarScores);
        supplierRepo.saveSnapshot(snapshot);
        return snapshot;
    }

    private double calculateMethodRigorScore(VerificationMethod method) {
        return switch (method) {
            case SELF_DECLARED -> 0.25;
            case DOCUMENT_REVIEW -> 0.60;
            case THIRD_PARTY_API -> 0.85;
            case HUMAN_AUDIT -> 0.95;
            case SITE_VISIT -> 1.00;
        };
    }
}
