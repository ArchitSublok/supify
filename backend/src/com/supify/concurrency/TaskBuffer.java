package com.supify.concurrency;

import java.util.LinkedList;
import java.util.Queue;

/** Bounded producer-consumer buffer using synchronized guarded blocks (wait / notifyAll). */
public class TaskBuffer<T> {
    private final Queue<T> queue = new LinkedList<>();
    private final int capacity;
    private boolean open = true;

    public TaskBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be > 0");
        }
        this.capacity = capacity;
    }

    public synchronized void produce(T item) throws InterruptedException {
        while (queue.size() == capacity && open) {
            wait();
        }
        if (!open) return;
        queue.offer(item);
        notifyAll();
    }

    public synchronized T consume() throws InterruptedException {
        while (queue.isEmpty() && open) {
            wait();
        }
        if (queue.isEmpty() && !open) {
            return null;
        }
        T item = queue.poll();
        notifyAll();
        return item;
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized void shutdown() {
        open = false;
        notifyAll();
    }
}
