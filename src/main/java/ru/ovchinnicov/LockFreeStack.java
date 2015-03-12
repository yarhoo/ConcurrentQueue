package ru.ovchinnicov;

import java.util.concurrent.atomic.AtomicReference;

public class LockFreeStack<T> {
    private final AtomicReference<Node<T>> top = new AtomicReference<>();
    private int size;

    public T pop() {
        Node<T> oldTop;
        do {
            oldTop = top.get();
            if (oldTop == null) {
                return null;
            }
        } while (!top.compareAndSet(oldTop, oldTop.prev));
        --size;
        return oldTop.value;
    }

    public void push(T e) {
        Node<T> oldTop;
        Node<T> newTop;
        do {
            oldTop = top.get();
            newTop = new Node<>(e, oldTop);
        } while (!top.compareAndSet(oldTop, newTop));
        ++size;
    }

    public int size() {
        return size;
    }

    private static class Node<T> {
        public final T value;
        public final Node<T> prev;

        public Node(T value, Node<T> prev) {
            this.value = value;
            this.prev = prev;
        }
    }
}
