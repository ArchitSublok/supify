package com.supify.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Organisation extends AbstractAuditEntity {
    private String legalName;
    private String tradeName;
    private String entityType;
    private String countryCode;
    private String city;
    private String state;
    private int yearEstablished;

    public Organisation(String legalName, String tradeName, String entityType, String countryCode, String city, String state, int yearEstablished) {
        super();
        this.legalName = Objects.requireNonNull(legalName, "legalName is required");
        this.tradeName = tradeName;
        this.entityType = entityType;
        this.countryCode = countryCode;
        this.city = city;
        this.state = state;
        this.yearEstablished = yearEstablished;
    }

    public Organisation(UUID id, Instant createdAt, Instant updatedAt, String legalName, String tradeName, String entityType, String countryCode, String city, String state, int yearEstablished) {
        super(id, createdAt, updatedAt);
        this.legalName = legalName;
        this.tradeName = tradeName;
        this.entityType = entityType;
        this.countryCode = countryCode;
        this.city = city;
        this.state = state;
        this.yearEstablished = yearEstablished;
    }

    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; touch(); }

    public String getTradeName() { return tradeName; }
    public void setTradeName(String tradeName) { this.tradeName = tradeName; touch(); }

    public String getEntityType() { return entityType; }
    public String getCountryCode() { return countryCode; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public int getYearEstablished() { return yearEstablished; }

    public String getLocation() {
        if (city != null && state != null) return city + ", " + state;
        if (city != null) return city + ", " + countryCode;
        return countryCode;
    }

    @Override
    public String toString() {
        return legalName + (tradeName != null ? " (" + tradeName + ")" : "") + " [" + getLocation() + "]";
    }
}
