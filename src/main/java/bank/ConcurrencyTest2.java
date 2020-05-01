package bank;

import java.lang.Thread;
import java.lang.Runnable;
import java.io.IOException;
import java.lang.InterruptedException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConcurrencyTest2 {

    public static final int ITERATIONS = 10_000;
    public static final int THREADS = 10;
    private static final Object LOCK = new Object();
    public static int counter = 0;

    public static void main(String[] args) throws IOException,InterruptedException {
        test2();
    }

    public static void test2() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        for (int i = 0; i < THREADS; i++) {
            pool.execute(new Task());
        }
        pool.shutdown();
        pool.awaitTermination(60, TimeUnit.SECONDS);
        System.out.println(counter);
    }

    public static void test1() throws InterruptedException {
        Thread[] threads = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            threads[i] = new Thread(new Task());
            threads[i].start();
        }
        for (int i = 0; i < THREADS; i++) threads[i].join();
        System.out.println(counter);
    }

    public static class Task implements Runnable {

        public void run() {
            for(int i = 0; i < ConcurrencyTest2.ITERATIONS; i++) {
                // synchronized (ConcurrencyTest2.LOCK) {
                ConcurrencyTest2.counter++;
                // }
            }
        }
    }
}
