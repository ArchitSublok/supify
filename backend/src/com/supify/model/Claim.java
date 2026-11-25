package com.supify.model;

public class Claim {
    private final String id;
    private final String supplierId;
    private final String claimTypeKey;
    private final String label;
    private final String pillar;
    private final String value;
    private String status;

    public Claim(String id, String supplierId, String claimTypeKey,
                 String label, String pillar, String value) {
        this.id = id;
        this.supplierId = supplierId;
        this.claimTypeKey = claimTypeKey;
        this.label = label;
        this.pillar = pillar;
        this.value = value;
        this.status = "SUBMITTED";
    }

    public String getId() { return id; }
    public String getSupplierId() { return supplierId; }
    public String getClaimTypeKey() { return claimTypeKey; }
    public String getLabel() { return label; }
    public String getPillar() { return pillar; }
    public String getValue() { return value; }
    public synchronized String getStatus() { return status; }

    public synchronized void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return id + " [" + pillar + "] " + label + " = " + value + " (" + status + ")";
    }
}
