package com.supify.concurrency;

import com.supify.dao.InMemoryStore;
import com.supify.model.Attestation;
import com.supify.model.Claim;
import com.supify.service.SupplierService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Background daemon thread that expires aged attestations and buffers user-safe alerts. */
public class DecayMonitorThread extends Thread {
    private final InMemoryStore store;
    private final SupplierService supplierService;
    private final long pollMillis;
    private volatile boolean running = true;
    private final List<String> alerts = new ArrayList<>();

    public DecayMonitorThread(InMemoryStore store, SupplierService supplierService, long pollMillis) {
        super("Supify-DecayMonitor");
        this.store = store;
        this.supplierService = supplierService;
        this.pollMillis = pollMillis;
        setDaemon(true);
        setPriority(Thread.NORM_PRIORITY - 1);
    }

    @Override
    public void run() {
        while (running && !isInterrupted()) {
            try {
                checkExpiredAttestations();
                Thread.sleep(pollMillis);
            } catch (InterruptedException e) {
                interrupt();
                break;
            }
        }
    }

    public void checkExpiredAttestations() {
        for (Attestation att : store.getActiveAttestations()) {
            if (att.expireIfNeeded()) {
                Optional<Claim> claimOpt = store.findClaimById(att.getClaimId());
                if (claimOpt.isPresent()) {
                    Claim claim = claimOpt.get();
                    claim.setStatus("EXPIRED");
                    supplierService.recalculateTrust(claim.getSupplierId());
                    addAlert("Attestation " + att.getId() + " expired for claim '" + claim.getLabel()
                            + "' (Supplier " + claim.getSupplierId() + ")");
                }
            }
        }
    }

    private synchronized void addAlert(String message) {
        alerts.add(message);
    }

    public synchronized List<String> drainAlerts() {
        List<String> copy = new ArrayList<>(alerts);
        alerts.clear();
        return copy;
    }

    public void stopMonitor() {
        running = false;
        interrupt();
        try {
            join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void printThreadStates() {
        Thread[] threads = new Thread[Thread.activeCount() * 2];
        int count = Thread.enumerate(threads);
        System.out.println("\n--- Active JVM Threads ---");
        System.out.printf("%-28s | %-14s | %-8s | %-6s%n", "THREAD NAME", "STATE", "PRIORITY", "DAEMON");
        System.out.println("-".repeat(66));
        for (int i = 0; i < count; i++) {
            Thread t = threads[i];
            if (t != null) {
                System.out.printf("%-28s | %-14s | %-8d | %-6s%n",
                        t.getName(), t.getState(), t.getPriority(), t.isDaemon());
            }
        }
    }
}
