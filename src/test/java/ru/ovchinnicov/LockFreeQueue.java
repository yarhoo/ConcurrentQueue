package ru.ovchinnikov;

import java.util.concurrent.atomic.AtomicReference;

public class LockFreeQueue<T> implements Queue<T> {
    private AtomicReference<Node<T>> head = new AtomicReference<>();
    private AtomicReference<Node<T>> tail = new AtomicReference<>();
    private int size;

    public LockFreeQueue() {
        Node<T> dummy = new Node<>(null, null);
        head.set(dummy);
        tail.set(dummy);
    }

    @Override
    public void add(T e) {
        Node<T> newNode = new Node<>(e, null);
        Node<T> oldTail;
        while (true) {
            oldTail = tail.get();
            Node<T> afterTail = oldTail.next.get();
            if (afterTail != null) {
                tail.compareAndSet(oldTail, afterTail);
                continue;
            }
            if (oldTail.next.compareAndSet(null, newNode)) {
                break;
            }
        }
        ++size;
        tail.compareAndSet(oldTail, newNode);
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public T poll() {
        Node<T> oldHead;
        Node<T> newHead;
        T result;
        while (true) {
            oldHead = head.get();
            newHead = oldHead.next.get();
            if (oldHead != head.get()) {
                continue;
            }
            if (newHead == null) {
                return null;
            }
            if (oldHead == tail.get()) {
                tail.compareAndSet(oldHead, newHead);
                continue;
            }
            result = newHead.value;
            if (head.compareAndSet(oldHead, newHead)) {
                break;
            }
        }
        --size;
        return result;
    }

    public int size() {
        return size;
    }

    private static class Node<T> {
        public final T value;
        public final AtomicReference<Node<T>> next;

        private Node(T value, Node<T> next) {
            this.value = value;
            this.next = new AtomicReference<>(next);
        }
    }
}
