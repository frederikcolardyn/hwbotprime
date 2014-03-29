package org.hwbot.prime.tasks;

import java.util.concurrent.Callable;

import org.hwbot.bench.PrimeBenchmark;
import org.hwbot.prime.service.BenchmarkStatusAware;

import android.util.Log;

public class BenchmarkTask implements Callable<Number> {

    private BenchmarkStatusAware observer;
    private PrimeBenchmark benchmark;

    public BenchmarkTask(BenchmarkStatusAware observer, PrimeBenchmark benchmark) {
        this.observer = observer;
        this.benchmark = benchmark;
    }

    @Override
    public Number call() {
        Number score;
        try {
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
