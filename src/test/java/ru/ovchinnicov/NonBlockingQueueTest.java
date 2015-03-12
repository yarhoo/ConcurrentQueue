package ru.ovchinnicov;

import org.junit.Test;

//import java.util.Queue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class NonBlockingQueueTest {

    @Test
    public void testAdd() throws Exception {
        Queue<Integer> q = createQueue();
        for (int i = 0; i < 10_000; i++) {
            q.add(i);
        }
    }

    private static Queue<Integer> createQueue() {
        return new LockFreeQueue<>();
    }

    @Test
    public void testRemove() throws Exception {
        Queue<Integer> q = createQueue();
        int x = 2;
        q.add(x);
        int y = q.poll();
        assertEquals(x, y);
    }

    @Test
    public void testIsEmpty() throws Exception {
        Queue<Integer> q = createQueue();
        assertTrue(q.isEmpty());
        q.add(2);
        assertFalse(q.isEmpty());
        q.poll();
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
        CountDownLatch stop = new CountDownLatch(0);
        Queue<Integer> queue = createQueue();
        prepareAdders(writers, executor, start, stop, queue);
        List<Future<List<Integer>>> res = prepareRemovers(readers, executor, start, queue);
        start.countDown();
        stop.await();
        List<Integer> results = new ArrayList<>();
        for (Future<List<Integer>> future : res) {
            results.addAll(future.get(10, TimeUnit.SECONDS));
        }
        Integer q;
        while ((q = queue.poll()) != null) {
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

    private static void prepareAdders(int addersNo, ExecutorService executor,
                                      CountDownLatch stop, CountDownLatch start,
                                      Queue<Integer> queue) {
        for (int i = 0; i < addersNo; i++) {
            executor.submit(new Adder(start, stop, queue));
        }
    }

    private static List<Future<List<Integer>>> prepareRemovers(int removersNo, ExecutorService executor,
                                                               CountDownLatch start, Queue<Integer> queue) {
        List<Future<List<Integer>>> ret = new ArrayList<>();
        for (int i = 0; i < removersNo; i++) {
            ret.add(executor.submit(new Remover(start, queue)));
        }
        return ret;
    }

    private static class Adder implements Callable<Void> {
        private final CountDownLatch start;
        private final CountDownLatch stop;
        private final Queue<Integer> queue;

        private Adder(CountDownLatch l, CountDownLatch stop, Queue<Integer> q) {
            this.start = l;
            this.stop = stop;
            this.queue = q;
        }

        @Override
        public Void call() throws Exception {
            start.await();
            for (int i = 0; i < 1000; i++) {
                queue.add(i);
            }
            stop.countDown();
            return null;
        }
    }

    private static class Remover implements Callable<List<Integer>> {
        private final CountDownLatch start;
        private final Queue<Integer> queue;
        private final List<Integer> removed = new ArrayList<>();

        private Remover(CountDownLatch l, Queue<Integer> q) {
            this.start = l;
            this.queue = q;
        }

        @Override
        public List<Integer> call() throws Exception {
            start.await();
            for (int i = 0; i < 1000; i++) {
                removed.add(queue.poll());
            }
            return removed;
        }
    }
}