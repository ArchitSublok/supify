package com.supify.service;

import com.supify.dao.InMemoryStore;
import com.supify.model.Attestation;
import com.supify.model.Claim;
import com.supify.model.Supplier;
import com.supify.model.TrustBand;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Stream-based search and trust calculation from active attestations. */
public class SupplierService {
    private final InMemoryStore store;

    public SupplierService(InMemoryStore store) {
        this.store = store;
    }

    public List<Supplier> search(String keyword, String category, TrustBand minBand,
                                 Integer minCapacity, String sortBy, int page, int pageSize) {
        Predicate<Supplier> filter = s -> true;

        if (keyword != null && !keyword.isBlank()) {
            String q = keyword.trim().toLowerCase();
            filter = filter.and(s -> s.getName().toLowerCase().contains(q)
                    || s.getCity().toLowerCase().contains(q)
                    || s.getCategory().toLowerCase().contains(q));
        }
        if (category != null && !category.isBlank()) {
            String cat = category.trim().toLowerCase();
            filter = filter.and(s -> s.getCategory().toLowerCase().contains(cat));
        }
        if (minBand != null) {
            filter = filter.and(s -> s.getTrustBand().getLevel() >= minBand.getLevel());
        }
        if (minCapacity != null && minCapacity > 0) {
            filter = filter.and(s -> s.getCapacityPerMonth() >= minCapacity);
        }

        Comparator<Supplier> comparator = getComparator(sortBy);
        int offset = Math.max(0, (page - 1) * pageSize);

        return store.findAll().stream()
                .filter(filter)
                .distinct()
                .sorted(comparator)
                .skip(offset)
                .limit(pageSize)
                .collect(Collectors.toList());
    }

    public List<String> getAllCategories() {
        return store.findAll().stream()
                .map(Supplier::getCategory)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private Comparator<Supplier> getComparator(String sortBy) {
        if ("capacity".equalsIgnoreCase(sortBy)) {
            return Comparator.comparingInt(Supplier::getCapacityPerMonth).reversed();
        }
        if ("name".equalsIgnoreCase(sortBy)) {
            return Comparator.comparing(Supplier::getName, String.CASE_INSENSITIVE_ORDER);
        }
        return Comparator.comparingDouble(Supplier::getTrustScore).reversed();
    }

    public synchronized void recalculateTrust(String supplierId) {
        Optional<Supplier> supOpt = store.findById(supplierId);
        if (supOpt.isEmpty()) return;

        Supplier supplier = supOpt.get();
        List<Claim> claims = store.getClaimsForSupplier(supplierId);

        double total = 0.0;
        int activeCount = 0;
        boolean hasIdentity = false;
        boolean hasLegal = false;
        boolean hasAudit = false;

        for (Claim claim : claims) {
            Optional<Attestation> attOpt = store.getActiveAttestationForClaim(claim.getId());
            if (attOpt.isPresent()) {
                Attestation att = attOpt.get();
                total += att.getMethod().getScore();
                activeCount++;

                if ("IDENTITY".equalsIgnoreCase(claim.getPillar())) hasIdentity = true;
                if ("LEGAL".equalsIgnoreCase(claim.getPillar())) hasLegal = true;
                String methodName = att.getMethod().name();
                if ("HUMAN_AUDIT".equals(methodName) || "SITE_VISIT".equals(methodName)) {
                    hasAudit = true;
                }
            }
        }

        double avgScore = activeCount == 0 ? 0.0 : Math.min(1.0, total / Math.max(3, activeCount));
        TrustBand band;
        if (!hasIdentity) {
            band = TrustBand.UNVERIFIED;
            avgScore = Math.min(avgScore, 0.30);
        } else if (!hasLegal) {
            band = TrustBand.BASIC;
            avgScore = Math.min(avgScore, 0.65);
        } else if (hasAudit && avgScore >= 0.85) {
            band = TrustBand.AUDITED;
        } else {
            band = TrustBand.VERIFIED;
        }

        supplier.setTrustBand(band);
        supplier.setTrustScore(avgScore);
    }
}
