package com.supify.concurrency;

import com.supify.dao.AuditRepository;
import com.supify.dao.SupplierRepository;
import com.supify.dao.VerificationRepository;
import com.supify.model.Attestation;
import com.supify.model.AuditEvent;
import com.supify.model.Claim;
import com.supify.model.SupplierProfile;
import com.supify.model.enums.AttestationState;
import com.supify.model.enums.TrustBand;
import java.util.List;
import java.util.Optional;

/**
 * Background daemon thread demonstrating:
 * - Thread subclassing, start(), run(), sleep(), join(), and interrupt()
 * - Daemon thread configuration (setDaemon(true)) and custom priority
 * - Periodic background attestation validity decay monitoring
 * - Non-intrusive alert posting via AlertBuffer
 */
public class DecayMonitorDaemon extends Thread {
    private final VerificationRepository verificationRepo;
    private final SupplierRepository supplierRepo;
    private final AuditRepository auditRepo;
    private final long pollIntervalMillis;
    private volatile boolean running = true;

    public DecayMonitorDaemon(VerificationRepository verificationRepo, SupplierRepository supplierRepo, AuditRepository auditRepo, long pollIntervalMillis) {
        super("Supify-DecayMonitorDaemon");
        this.verificationRepo = verificationRepo;
        this.supplierRepo = supplierRepo;
        this.auditRepo = auditRepo;
        this.pollIntervalMillis = pollIntervalMillis;
        // Mark as background daemon thread so it doesn't prevent JVM exit
        setDaemon(true);
        setPriority(Thread.NORM_PRIORITY - 1);
    }

    @Override
    public void run() {
        while (running && !isInterrupted()) {
            try {
                scanAndApplyDecay();
                Thread.sleep(pollIntervalMillis);
            } catch (InterruptedException e) {
                // Thread interrupted during sleep: restore interrupt flag and exit cleanly
                interrupt();
                break;
            } catch (Exception e) {
                AlertBuffer.getInstance().post("[DECAY ERROR] " + e.getMessage());
            }
        }
    }

    private void scanAndApplyDecay() {
        List<Attestation> activeAttestations = verificationRepo.findAttestationsByState(AttestationState.ACTIVE);
        for (Attestation attestation : activeAttestations) {
            if (attestation.checkAndApplyDecay()) {
                // Decay applied!
                Optional<Claim> claimOpt = verificationRepo.findClaimById(attestation.getClaimId());
                if (claimOpt.isPresent()) {
                    Claim claim = claimOpt.get();
                    // Recalculate supplier trust band and post alert
                    Optional<SupplierProfile> profileOpt = supplierRepo.findProfileByOrganisationId(claim.getOrganisationId());
                    String supplierName = profileOpt.map(SupplierProfile::getHeadline).orElse(claim.getOrganisationId().toString());

                    // Audit event
                    auditRepo.recordEvent(new AuditEvent(
                            "SYSTEM_DECAY_DAEMON",
                            "attestation.expired",
                            "Attestation",
                            attestation.getId().toString(),
                            "Validity expired for claim [" + claim.getLabel() + "]. Trust band decayed."
                    ));

                    // Alert buffer decouples output from console prompt
                    AlertBuffer.getInstance().post(String.format(
                            "[ALERT - DECAY TRIGGERED] Attestation for %s on claim '%s' expired. Status -> EXPIRED.",
                            supplierName, claim.getLabel()
                    ));
                }
            }
        }
    }

    public void stopMonitoring() {
        this.running = false;
        interrupt();
        try {
            // Join with 1000ms timeout for graceful shutdown
            join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
