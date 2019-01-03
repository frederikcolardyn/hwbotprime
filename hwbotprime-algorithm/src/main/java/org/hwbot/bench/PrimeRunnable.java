package org.hwbot.bench;

import java.util.List;
import java.util.concurrent.Callable;

public class PrimeRunnable implements Callable<Void> {
    private int from;
    private List<Number> results; // shared but thread-safe

    public PrimeRunnable(int from, List<Number> results) {
        this.from = from;
        this.results = results;
    }

    public void isPrime(int number) {
        for (int i = 2; i < from; i++) {
            if ((number % i) == 0) {
                return;
            }
        }
        // found prime, add to shared results
        this.results.add(number);
    }

    public Void call() throws Exception {
        try {
            isPrime(from); // don't increment, just check one number
        } catch (Throwable e) {
            System.err.println("Failed to calc prime number!");
            e.printStackTrace();
        }
        return null;
    }

}
