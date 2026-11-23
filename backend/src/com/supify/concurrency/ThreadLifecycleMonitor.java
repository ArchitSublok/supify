package com.supify.concurrency;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostic utility demonstrating:
 * - Thread lifecycle and scheduling awareness
 * - NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED states
 * - Daemon threads, priorities, and execution tracing
 */
public final class ThreadLifecycleMonitor {

    public record ThreadSnapshot(
            long id,
            String name,
            Thread.State state,
            int priority,
            boolean isDaemon,
            boolean isAlive
    ) {}

    public static List<ThreadSnapshot> captureSupifyThreads() {
        List<ThreadSnapshot> snapshots = new ArrayList<>();
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }

        Thread[] threads = new Thread[rootGroup.activeCount() * 2];
        int count = rootGroup.enumerate(threads, true);

        for (int i = 0; i < count; i++) {
            Thread t = threads[i];
            if (t != null) {
                // Focus on application threads and active workers
                snapshots.add(new ThreadSnapshot(
                        t.threadId(),
                        t.getName(),
                        t.getState(),
                        t.getPriority(),
                        t.isDaemon(),
                        t.isAlive()
                ));
            }
        }
        return snapshots;
    }

    public static String generateThreadStateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-6s | %-32s | %-14s | %-8s | %-8s | %-7s\n",
                "ID", "THREAD NAME", "STATE", "PRIORITY", "DAEMON", "ALIVE"));
        sb.append("-".repeat(85)).append("\n");

        for (ThreadSnapshot s : captureSupifyThreads()) {
            sb.append(String.format("%-6d | %-32s | %-14s | %-8d | %-8b | %-7b\n",
                    s.id(),
                    truncate(s.name(), 32),
                    s.state(),
                    s.priority(),
                    s.isDaemon(),
                    s.isAlive()));
        }
        return sb.toString();
    }

    private static String truncate(String val, int maxLen) {
        if (val.length() <= maxLen) return val;
        return val.substring(0, maxLen - 3) + "...";
    }
}
