package bank;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ConcurrencyTest {

    // private static final String IP = "192.168.1.136"; // raspberry
    //private static final String IP = "178.128.198.205"; // do
    private static final String IP = "127.0.0.1"; // local
    private static final int PORT = 5001;

    private static final String ACCOUNT_NUMBER = "CH5610000000000000000";

    private static final int POOL_SIZE = 10;
    private static final int REQUESTS_NUM = 1_000_000;

    private final ExecutorService pool;

    public static void main(String[] args) throws IOException, InterruptedException {
        new ConcurrencyTest();
    }

    ConcurrencyTest() throws IOException, InterruptedException {
        pool = Executors.newFixedThreadPool(POOL_SIZE);
        long start = System.currentTimeMillis();
        testConcurrentDepositWithdraw();
        long stop = System.currentTimeMillis();
        System.out.println("duration: " + (stop-start)/1000);
    }

    private void runRequestsConcurrently(String[] requests) throws IOException, InterruptedException {
        int step = requests.length/POOL_SIZE;

        for (int i = 0; i < requests.length; i += step) {
            if (i + step >= requests.length) {
                pool.execute(new RequestDispatcher(requests, i, requests.length));
            } else {
                pool.execute(new RequestDispatcher(requests, i, i + step));
            }
            System.out.println("added task to pool");
        }
        pool.shutdown();
        pool.awaitTermination(180, TimeUnit.SECONDS);
    }

    public static class RequestDispatcher implements Runnable {

        private final String[] requests;
        private final int begin;
        private final int end;

        public RequestDispatcher(String[] requests, int begin, int end) {
            this.requests = requests;
            this.begin = begin;
            this.end = end;
        }

        @Override
        public void run() {
            try {
                Socket socket = new Socket(IP, PORT);
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();
                for (int i = begin; i < end; i++) {
                    writeString(out, requests[i]);
                    discardResponse(in);
                }
                socket.close();
            }
            catch (IOException e) { e.printStackTrace(); }
        }

        private static void discardResponse(InputStream in) throws IOException {
            int buf;
            boolean lineBreak = false;
            while ((buf = in.read()) != -1) {
                if ((char) buf == '\n') {
                    if (lineBreak) break;
                    lineBreak = true;
                } else {
                    lineBreak = false;
                }
            }
        }

        private static void writeString(OutputStream out, String s) throws IOException {
            for (int i = 0; i < s.length(); i++) {
                out.write(s.charAt(i));
            }
        }
    }

    private void testConcurrentDepositWithdraw() throws IOException, InterruptedException {
        double amount = 1;
        String[] requests = new String[REQUESTS_NUM];
        int i = 0;
        for (; i < REQUESTS_NUM/2; i++) {
            requests[i] = actionDeposit(ACCOUNT_NUMBER, amount);
        }
        for (; i < REQUESTS_NUM; i++) {
            //requests[i] = actionWithdraw(ACCOUNT_NUMBER, amount);
            requests[i] = actionDeposit(ACCOUNT_NUMBER, amount);
        }
        shuffleArray(requests);
        shuffleArray(requests);

        runRequestsConcurrently(requests);
    }

    private static String actionDeposit(String accountNumber, double amount) {
        return "6\n" + accountNumber + "\n" + amount + "\n\n";
    }

    private static String actionWithdraw(String accountNumber, double amount) {
        return "7\n" + accountNumber + "\n" + amount + "\n\n";
    }

    private void shuffleArray(String[] requests) {
        Random rnd = ThreadLocalRandom.current();
        String r;
        for (int i = requests.length - 1; i > 0; i--)
        {
            int index = rnd.nextInt(i + 1);
            r = requests[index];
            requests[index] = requests[i];
            requests[i] = r;
        }
    }

}
