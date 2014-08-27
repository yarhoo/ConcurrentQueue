package ru.ovchinnicov;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class BlockingQueue<T> implements Queue<T> {
    private List<T> queue = new LinkedList<>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Condition elementAdded = new ReentrantLock().newCondition();

    // todo: add blocking on size preset
    public void add(T object) {
        lock.writeLock().lock();
        try {
            queue.add(object);
            elementAdded.signalAll();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public T remove() throws InterruptedException {
        lock.readLock().lock();
        try {
            while(queue.isEmpty()) {
                try {
                    elementAdded.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
            return queue.remove(0);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return queue.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }
}
