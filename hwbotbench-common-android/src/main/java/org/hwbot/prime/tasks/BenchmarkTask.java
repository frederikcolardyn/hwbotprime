package org.hwbot.prime.tasks;

import java.util.concurrent.Callable;

import org.hwbot.bench.Benchmark;
import org.hwbot.prime.service.BenchmarkStatusAware;

import android.util.Log;

public class BenchmarkTask implements Callable<Number> {

    private BenchmarkStatusAware observer;
    private Benchmark benchmark;

    public BenchmarkTask(BenchmarkStatusAware observer, Benchmark benchmark) {
        this.observer = observer;
        this.benchmark = benchmark;
    }

    @Override
    public Number call() {
        Number score;
        try {
            Log.i(this.getClass().getSimpleName(), "Running benchmark " + benchmark.getClass().getName());
            score = benchmark.call();
            // round to int for android
            score = (float) score.intValue();
            observer.notifyBenchmarkFinished(score);
            return score;
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Error during benchmark: " + e.getMessage());
            return null;
        }
    }

}
