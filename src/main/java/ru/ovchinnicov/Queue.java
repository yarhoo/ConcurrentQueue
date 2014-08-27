package ru.ovchinnicov;

/**
 * FIFO (First-in-First-out) interface
 * @author Valerii Ovchinnikov
 */
interface Queue<T> {

    /**
     * Adds new element to the end of queue
     *
     * @param elem to enqueue
     * @throws InterruptedException when adding thread interrupted
     */
    void add(T elem) throws InterruptedException;

    /**
     * Removes and returns element from the beginning of queue
     *
     * @return element from the beginning of queue
     * @throws InterruptedException when removing thread interrupted
     */
    T remove() throws InterruptedException;

    /**
     * Checks if queue is empty
     *
     * @return true if queue is empty, false otherwise
     */
    boolean isEmpty();
}
