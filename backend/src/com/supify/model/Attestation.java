package com.supify.model;

import java.time.Duration;
import java.time.LocalDateTime;

// Layer 4: active trust badge issued after PASS check with an expiry time
public class Attestation {
    private final String id;
    private final String claimId;
    private final String checkId;
    private final VerificationMethod method;
    private final LocalDateTime verifiedAt;
    private final LocalDateTime validUntil;
    private String state;

    public Attestation(String id, String claimId, String checkId,
                       VerificationMethod method, int validSeconds) {
        this.id = id;
        this.claimId = claimId;
        this.checkId = checkId;
        this.method = method;
        this.verifiedAt = LocalDateTime.now();
        this.validUntil = this.verifiedAt.plusSeconds(validSeconds);
        this.state = "ACTIVE";
    }

    public String getId() { return id; }
    public String getClaimId() { return claimId; }
    public String getCheckId() { return checkId; }
    public VerificationMethod getMethod() { return method; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public LocalDateTime getValidUntil() { return validUntil; }
    public synchronized String getState() { return state; }

    public boolean isExpired() {
        return validUntil != null && LocalDateTime.now().isAfter(validUntil);
    }

    public synchronized boolean expireIfNeeded() {
        if ("ACTIVE".equals(state) && isExpired()) {
            state = "EXPIRED";
            return true;
        }
        return false;
    }

    public synchronized void revoke() {
        this.state = "REVOKED";
    }

    public long secondsLeft() {
        if (!"ACTIVE".equals(state)) {
            return 0;
        }
        long sec = Duration.between(LocalDateTime.now(), validUntil).getSeconds();
        return Math.max(0, sec);
    }
}
