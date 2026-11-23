package com.supify.model;

import com.supify.model.enums.Pillar;
import com.supify.model.enums.TrustBand;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class TrustSnapshot implements Identifiable<UUID> {
    private final UUID organisationId;
    private final TrustBand band;
    private final double overallScore;
    private final Map<Pillar, Double> pillarScores;
    private final Instant computedAt;

    public TrustSnapshot(UUID organisationId, TrustBand band, double overallScore, Map<Pillar, Double> pillarScores) {
        this.organisationId = Objects.requireNonNull(organisationId, "organisationId is required");
        this.band = Objects.requireNonNull(band, "band is required");
        this.overallScore = overallScore;
        this.pillarScores = pillarScores != null ? Collections.unmodifiableMap(new EnumMap<>(pillarScores)) : Collections.emptyMap();
        this.computedAt = Instant.now();
    }

    @Override
    public UUID getId() {
        return organisationId;
    }

    public UUID getOrganisationId() {
        return organisationId;
    }

    public TrustBand getBand() {
        return band;
    }

    public double getOverallScore() {
        return overallScore;
    }

    public Map<Pillar, Double> getPillarScores() {
        return pillarScores;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    public double getPillarScore(Pillar pillar) {
        return pillarScores.getOrDefault(pillar, 0.0);
    }
}
