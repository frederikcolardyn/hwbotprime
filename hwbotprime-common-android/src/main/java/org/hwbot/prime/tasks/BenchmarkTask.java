package org.hwbot.prime.tasks;

import org.hwbot.bench.PrimeBenchmark;
import org.hwbot.prime.service.BenchmarkStatusAware;

import android.os.AsyncTask;
import android.util.Log;

public class BenchmarkTask extends AsyncTask<Void, Void, Number> {

    private BenchmarkStatusAware observer;
    private PrimeBenchmark benchmark;

    public BenchmarkTask(BenchmarkStatusAware observer, PrimeBenchmark benchmark) {
        this.observer = observer;
        this.benchmark = benchmark;
    }

    @Override
    public Number doInBackground(Void... params) {
        Number score;
        try {
            score = benchmark.call();
            observer.notifyBenchmarkFinished(score);
            return score;
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Error during benchmark: " + e.getMessage());
            return null;
        }
    }

}
