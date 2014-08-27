package ru.ovchinnicov;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Non-Blocking queue implementation
 * @author Valerii Ovchinnikov
 */
class NonBlockingQueue<T> implements Queue<T> {
    // you can tune this according to contention
    private final static long BACK_OFF_TIME = 10;
    private AtomicReference<Node<T>> head = new AtomicReference<>(new Node(null));
    private AtomicReference<Node<T>> tail = new AtomicReference<>(head.get());

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(T elem) throws InterruptedException {
        if (elem == null) {
            throw new IllegalArgumentException("Element must not be null!");
        }
        while (true) {
            Node<T> newTail = new Node<>(elem);
            Node<T> t = tail.get();
            Node<T> n = t.next();
            if (n == null) {
                if (t.isEmpty()) {
                    // queue is empty
                    if (tail.compareAndSet(t, newTail)) {
                        // set head after tail! so nothing can be removed from queue
                        // before both tail and head set to new node
                        if (!head.compareAndSet(t, newTail)) {
                            assert false : "Add: couldn't change head from empty node";
                        }
                        return;
                    }
                } else if (t.casNext(null, newTail)) {
                    // no other thread can cas tail now
                    if (!tail.compareAndSet(t, newTail)) {
                        assert false : "Add: couldn't set tail after old tail's next set";
                    }
                    return;
                }
            }
            await();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T remove() throws InterruptedException {
        while (true) {
            Node<T> h = head.get();
            T value = h.value();
            if (value != null && h.casValue(value, null)) {
                // no other thread can cas head now
                if (h.next() != null) { // else removed last node from queue. tail/head reset handled by add method
                    if (!head.compareAndSet(h, h.next())) {
                        assert false : "Remove: couldn't set head after old head's value set to null";
                    }
                }
                return value;
            } else if (h.next() == null) {
                // empty queue
                return null;
            }
            // some other thread is removing head now
            await();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return head.get() == tail.get() && tail.get().value() == null;
    }

    private void await() throws InterruptedException {
        try {
            Thread.sleep(BACK_OFF_TIME);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    private static class Node<T> {
        final AtomicReference<Node<T>> next = new AtomicReference<>();
        final AtomicReference<T> value = new AtomicReference<>();

        Node(T value) {
            this.value.set(value);
        }

        boolean casNext(Node<T> exp, Node<T> upd) {
            return next.compareAndSet(exp, upd);
        }

        Node<T> next() {
            return next.get();
        }

        T value() {
            return value.get();
        }

        boolean casValue(T exp, T upd) {
            return value.compareAndSet(exp, upd);
        }

        boolean isEmpty() {
            return value.get() == null && next.get() == null;
        }
    }
}
