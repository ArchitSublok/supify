package com.supify.concurrency;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Custom bounded thread-safe task queue demonstrating:
 * - Shared data and synchronization
 * - Intrinsic locks and critical sections (synchronized)
 * - Inter-thread communication via guarded blocks: wait() and notifyAll()
 * - Producer-Consumer coordination pattern
 */
public class CustomTaskQueue<T> {
    private final Queue<T> queue = new LinkedList<>();
    private final int capacity;
    private boolean active = true;

    public CustomTaskQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Queue capacity must be > 0");
        }
        this.capacity = capacity;
    }

    /**
     * Producer method: puts an item into the queue.
     * If the queue is full, the calling thread enters WAITING state via wait().
     */
    public synchronized void produce(T item) throws InterruptedException {
        while (queue.size() == capacity && active) {
            // Guarded block: release lock and wait until consumer removes an item
            wait();
        }
        if (!active) {
            return;
        }
        queue.offer(item);
        // Wake up waiting consumers
        notifyAll();
    }

    /**
     * Consumer method: takes an item from the queue.
     * If the queue is empty, the calling thread enters WAITING state via wait().
     */
    public synchronized T consume() throws InterruptedException {
        while (queue.isEmpty() && active) {
            // Guarded block: release lock and wait until producer inserts an item
            wait();
        }
        if (queue.isEmpty() && !active) {
            return null;
        }
        T item = queue.poll();
        // Wake up waiting producers
        notifyAll();
        return item;
    }

    /**
     * Non-blocking poll attempt with timeout simulation.
     */
    public synchronized T pollWithTimeout(long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (queue.isEmpty() && active) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                return null;
            }
            wait(remaining);
        }
        if (queue.isEmpty()) {
            return null;
        }
        T item = queue.poll();
        notifyAll();
        return item;
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    public synchronized void shutdown() {
        this.active = false;
        notifyAll();
    }
}
