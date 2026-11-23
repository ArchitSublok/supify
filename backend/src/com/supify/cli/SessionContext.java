package com.supify.cli;

import com.supify.model.enums.UserRole;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Encapsulates the active interactive CLI session state.
 * Stores the current role, username, and comparison tray (up to 4 suppliers).
 */
public class SessionContext {
    private UserRole currentRole;
    private String currentUserName;
    private final Set<UUID> comparisonTray;

    public SessionContext() {
        this.currentRole = UserRole.BUYER;
        this.currentUserName = "Procurement Specialist";
        this.comparisonTray = new LinkedHashSet<>();
    }

    public UserRole getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(UserRole currentRole) {
        this.currentRole = currentRole;
        switch (currentRole) {
            case BUYER -> this.currentUserName = "Procurement Specialist";
            case SUPPLIER -> this.currentUserName = "Aravind Fasteners Admin";
            case VERIFIER -> this.currentUserName = "Trained Verifier #04";
            case ADMIN -> this.currentUserName = "Platform System Admin";
        }
    }

    public String getCurrentUserName() {
        return currentUserName;
    }

    public void setCurrentUserName(String currentUserName) {
        this.currentUserName = currentUserName;
    }

    public Set<UUID> getComparisonTray() {
        return Collections.unmodifiableSet(comparisonTray);
    }

    public boolean addToComparison(UUID supplierId) {
        if (comparisonTray.size() >= 4) {
            return false; // Tray capacity maximum of 4 suppliers
        }
        return comparisonTray.add(supplierId);
    }

    public boolean removeFromComparison(UUID supplierId) {
        return comparisonTray.remove(supplierId);
    }

    public void clearComparisonTray() {
        comparisonTray.clear();
    }

    public boolean isInComparison(UUID supplierId) {
        return comparisonTray.contains(supplierId);
    }
}
