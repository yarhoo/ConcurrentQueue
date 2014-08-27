package ru.ovchinnicov;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class NonBlockingQueueTest {

    @Test
    public void testAdd() throws Exception {
        NonBlockingQueue<Integer> q = new NonBlockingQueue<Integer>();
        for (int i = 0; i < 10_000; i++) {
            q.add(i);
        }
    }

    @Test
    public void testRemove() throws Exception {
        NonBlockingQueue<Integer> q = new NonBlockingQueue<Integer>();
        int x = 2;
        q.add(x);
        int y = q.remove();
        assertEquals(x, y);
    }

    @Test
    public void testIsEmpty() throws Exception {
        NonBlockingQueue<Integer> q = new NonBlockingQueue<Integer>();
        assertTrue(q.isEmpty());
        q.add(2);
        assertFalse(q.isEmpty());
        q.remove();
        assertTrue(q.isEmpty());
    }

    @Test
    public void concurrentUniformLoadTest() throws ExecutionException, InterruptedException, TimeoutException {
        for (int i = 1; i < 10; i++) {
            runConcurrent(i * 10, i * 10);
        }
    }

    @Test
    public void concurrentHighReadLoadTest() throws ExecutionException, InterruptedException, TimeoutException {
        for (int i = 1; i < 10; i++) {
            runConcurrent(i, i * 10);
        }
    }

    @Test
    public void concurrentHighWrightLoadTest() throws ExecutionException, InterruptedException, TimeoutException {
        for (int i = 1; i < 10; i++) {
            runConcurrent(i * 10, i);
        }
    }

    private static void runConcurrent(int writers, int readers) throws ExecutionException, InterruptedException, TimeoutException {
        ExecutorService executor = Executors.newFixedThreadPool(readers + writers);
        CountDownLatch start = new CountDownLatch(1);
        Queue<Integer> queue = new NonBlockingQueue<>();
        List<Future<List<Integer>>> res = prepareAdders(writers, executor, start, queue);
        prepareRemovers(readers, executor, start, queue);
        start.countDown();
        List<Integer> results = new ArrayList<>();
        for (Future<List<Integer>> future : res) {
            results.addAll(future.get(10, TimeUnit.SECONDS));
        }
        Integer q;
        while((q = queue.remove()) != null) {
            results.add(q);
        }
//        assertEquals("writers: " + writers + " readers: " + readers, 1000 * writers, results.size());
        for (int i = 0; i < 1000; i++) {
            final int j = i;
            assertEquals("writers: " + writers + " readers: " + readers,
                    writers, results.stream().filter((e) -> e != null && e.equals(j)).count());
        }
        executor.shutdownNow();
    }

    private static List<Future<List<Integer>>> prepareAdders(int addersNo, ExecutorService executor,
                                      CountDownLatch start, Queue<Integer> queue) {
        List<Future<List<Integer>>> ret = new ArrayList<>();
        for (int i = 0; i < addersNo; i++) {
            ret.add(executor.submit(new Adder(start, queue)));
        }
        return ret;
    }

    private static void prepareRemovers(int removersNo, ExecutorService executor,
                                        CountDownLatch start, Queue<Integer> queue) {
        for (int i = 0; i < removersNo; i++) {
            executor.submit(new Remover(start, queue));
        }
    }

    private static class Adder implements Callable<List<Integer>> {
        private final CountDownLatch start;
        private final Queue<Integer> queue;
        private final List<Integer> removed = new ArrayList<>();

        private Adder(CountDownLatch l, Queue<Integer> q) {
            this.start = l;
            this.queue = q;
        }

        @Override
        public List<Integer> call() throws Exception {
            start.await();
            for (int i = 0; i < 1000; i++) {
                removed.add(queue.remove());
            }
            return removed;
        }
    }

    private static class Remover implements Callable<List<Integer>> {
        private final CountDownLatch start;
        private final Queue<Integer> queue;

        private Remover(CountDownLatch l, Queue<Integer> q) {
            this.start = l;
            this.queue = q;
        }

        @Override
        public List<Integer> call() throws Exception {
            start.await();
            for (int i = 0; i < 1000; i++) {
                queue.add(i);
            }
            return null;
        }
    }
}