package com.supify.model;

import com.supify.model.enums.ListingState;
import com.supify.model.enums.TrustBand;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class SupplierProfile extends AbstractAuditEntity {
    private final UUID organisationId;
    private String headline;
    private String category;
    private int capacityPerMonth;
    private String capacityUnit;
    private int minimumOrderQuantity;
    private String leadTime;
    private ListingState listingState;
    private TrustBand trustBand;
    private double overallTrustScore;

    public SupplierProfile(UUID organisationId, String headline, String category, int capacityPerMonth, String capacityUnit, int minimumOrderQuantity, String leadTime) {
        super();
        this.organisationId = Objects.requireNonNull(organisationId, "organisationId is required");
        this.headline = headline;
        this.category = category;
        this.capacityPerMonth = capacityPerMonth;
        this.capacityUnit = capacityUnit;
        this.minimumOrderQuantity = minimumOrderQuantity;
        this.leadTime = leadTime;
        this.listingState = ListingState.LISTED;
        this.trustBand = TrustBand.UNVERIFIED;
        this.overallTrustScore = 0.0;
    }

    public SupplierProfile(UUID id, Instant createdAt, Instant updatedAt, UUID organisationId, String headline, String category, int capacityPerMonth, String capacityUnit, int minimumOrderQuantity, String leadTime, ListingState listingState, TrustBand trustBand, double overallTrustScore) {
        super(id, createdAt, updatedAt);
        this.organisationId = organisationId;
        this.headline = headline;
        this.category = category;
        this.capacityPerMonth = capacityPerMonth;
        this.capacityUnit = capacityUnit;
        this.minimumOrderQuantity = minimumOrderQuantity;
        this.leadTime = leadTime;
        this.listingState = listingState;
        this.trustBand = trustBand;
        this.overallTrustScore = overallTrustScore;
    }

    public UUID getOrganisationId() { return organisationId; }
    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; touch(); }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; touch(); }

    public int getCapacityPerMonth() { return capacityPerMonth; }
    public void setCapacityPerMonth(int capacityPerMonth) { this.capacityPerMonth = capacityPerMonth; touch(); }

    public String getCapacityUnit() { return capacityUnit; }
    public void setCapacityUnit(String capacityUnit) { this.capacityUnit = capacityUnit; touch(); }

    public int getMinimumOrderQuantity() { return minimumOrderQuantity; }
    public void setMinimumOrderQuantity(int minimumOrderQuantity) { this.minimumOrderQuantity = minimumOrderQuantity; touch(); }

    public String getLeadTime() { return leadTime; }
    public void setLeadTime(String leadTime) { this.leadTime = leadTime; touch(); }

    public ListingState getListingState() { return listingState; }
    public void setListingState(ListingState listingState) { this.listingState = listingState; touch(); }

    public TrustBand getTrustBand() { return trustBand; }
    public synchronized void setTrustBand(TrustBand trustBand) { this.trustBand = trustBand; touch(); }

    public double getOverallTrustScore() { return overallTrustScore; }
    public synchronized void setOverallTrustScore(double overallTrustScore) { this.overallTrustScore = overallTrustScore; touch(); }

    public String getCapacityDisplay() {
        return String.format("%,d %s/mo", capacityPerMonth, capacityUnit);
    }
}
