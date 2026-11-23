package com.supify.concurrency;

import com.supify.dao.VerificationRepository;
import com.supify.model.VerificationTask;
import com.supify.model.enums.TaskState;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

/** Background daemon that boosts near-SLA task priority and re-balances the heap. */
public class SlaEscalationDaemon extends Thread {
    private final VerificationRepository verificationRepo;
    private final long pollIntervalMillis;
    private volatile boolean running = true;

    public SlaEscalationDaemon(VerificationRepository verificationRepo, long pollIntervalMillis) {
        super("Supify-SlaEscalationDaemon");
        this.verificationRepo = verificationRepo;
        this.pollIntervalMillis = pollIntervalMillis;
        setDaemon(true);
        setPriority(Thread.NORM_PRIORITY - 1);
    }

    @Override
    public void run() {
        while (running && !isInterrupted()) {
            try {
                scanAndEscalateTasks();
                Thread.sleep(pollIntervalMillis);
            } catch (InterruptedException e) {
                interrupt();
                break;
            } catch (Exception e) {
                AlertBuffer.getInstance().post("[SLA DAEMON ERROR] " + e.getMessage());
            }
        }
    }

    private void scanAndEscalateTasks() {
        PriorityBlockingQueue<VerificationTask> queue = verificationRepo.getTaskPriorityQueue();
        if (queue == null || queue.isEmpty()) return;

        // Drain to snapshot array for safe inspection
        List<VerificationTask> currentTasks = new ArrayList<>(queue);
        Instant now = Instant.now();

        for (VerificationTask task : currentTasks) {
            if (task.getState() == TaskState.QUEUED || task.getState() == TaskState.CLAIMED) {
                Duration remaining = Duration.between(now, task.getSlaDueAt());
                // If SLA is within 2 hours or breached, and not already escalated
                if (remaining.toHours() < 2 && task.getState() != TaskState.ESCALATED) {
                    // Critical Heap Re-sort: remove() then offer() ensures heap invariant is restored
                    boolean removed = queue.remove(task);
                    if (removed) {
                        task.escalate(25); // boost priority score
                        queue.offer(task); // re-heapify with new priority!

                        AlertBuffer.getInstance().post(String.format(
                                "[ALERT - SLA ESCALATION] Task %s for supplier '%s' (%s) escalated! Priority boosted to %d. %s",
                                task.getId().toString().substring(0, 8),
                                task.getSupplierName(),
                                task.getClaimLabel(),
                                task.getPriority(),
                                task.getSlaRemainingString()
                        ));
                    }
                }
            }
        }
    }

    public void stopDaemon() {
        this.running = false;
        interrupt();
        try {
            join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
