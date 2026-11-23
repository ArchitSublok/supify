package com.supify.concurrency;

import java.util.LinkedList;
import java.util.Queue;

/** Bounded blocking queue implemented with synchronized wait() / notifyAll() guards. */
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

    public synchronized void produce(T item) throws InterruptedException {
        while (queue.size() == capacity && active) {
            wait();
        }
        if (!active) {
            return;
        }
        queue.offer(item);
        notifyAll();
    }

    public synchronized T consume() throws InterruptedException {
        while (queue.isEmpty() && active) {
            wait();
        }
        if (queue.isEmpty() && !active) {
            return null;
        }
        T item = queue.poll();
        notifyAll();
        return item;
    }

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
