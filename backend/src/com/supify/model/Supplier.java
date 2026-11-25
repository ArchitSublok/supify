package com.supify.model;

// Stores basic supplier details and computed trust standing
public class Supplier {
    private final String id;
    private String name;
    private String city;
    private String category;
    private int capacityPerMonth;
    private int moq;
    private String leadTime;
    private TrustBand trustBand;
    private double trustScore;

    public Supplier(String id, String name, String city, String category,
                    int capacityPerMonth, int moq, String leadTime) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.category = category;
        this.capacityPerMonth = capacityPerMonth;
        this.moq = moq;
        this.leadTime = leadTime;
        this.trustBand = TrustBand.UNVERIFIED;
        this.trustScore = 0.0;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public String getCategory() { return category; }
    public int getCapacityPerMonth() { return capacityPerMonth; }
    public int getMoq() { return moq; }
    public String getLeadTime() { return leadTime; }
    public TrustBand getTrustBand() { return trustBand; }
    public double getTrustScore() { return trustScore; }

    public void setName(String name) { this.name = name; }
    public void setCity(String city) { this.city = city; }
    public void setCategory(String category) { this.category = category; }
    public void setCapacityPerMonth(int capacityPerMonth) { this.capacityPerMonth = capacityPerMonth; }
    public void setMoq(int moq) { this.moq = moq; }
    public void setLeadTime(String leadTime) { this.leadTime = leadTime; }

    public synchronized void setTrustBand(TrustBand trustBand) { this.trustBand = trustBand; }
    public synchronized void setTrustScore(double trustScore) { this.trustScore = trustScore; }

    @Override
    public String toString() {
        double rounded = Math.round(trustScore * 100.0) / 100.0;
        return id + " | " + name + " | " + city + " | " + category
                + " | " + trustBand + " (" + rounded + ")";
    }
}
