package com.supify.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread-safe alert buffer decoupling background daemon threads from the CLI console.
 * Uses ConcurrentLinkedQueue to prevent background threads from printing directly
 * to System.out mid-prompt and corrupting user interactive input.
 */
public final class AlertBuffer {
    private static final AlertBuffer INSTANCE = new AlertBuffer();
    private final ConcurrentLinkedQueue<String> buffer = new ConcurrentLinkedQueue<>();

    private AlertBuffer() {}

    public static AlertBuffer getInstance() {
        return INSTANCE;
    }

    public void post(String alertMessage) {
        if (alertMessage != null && !alertMessage.trim().isEmpty()) {
            buffer.offer(alertMessage);
        }
    }

    public List<String> drainAlerts() {
        List<String> drained = new ArrayList<>();
        String alert;
        while ((alert = buffer.poll()) != null) {
            drained.add(alert);
        }
        return drained;
    }

    public boolean hasAlerts() {
        return !buffer.isEmpty();
    }

    public int getPendingCount() {
        return buffer.size();
    }
}
