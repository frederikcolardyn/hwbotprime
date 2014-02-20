package org.hwbot.bench.api;

import java.util.concurrent.Callable;

import org.hwbot.bench.prime.ProgressBar;
import org.hwbot.bench.service.BenchService;
import org.hwbot.bench.ui.BenchUI;

import android.util.Log;
import android.widget.Button;

public abstract class Benchmark implements Callable<Number> {

    protected final int threads;
    protected ProgressBar progressBar;
    protected final BenchUI benchUI;
    protected final BenchService benchService;
    protected final Button comparebutton;

    public Benchmark(BenchService benchService, BenchUI benchUI, int threads, ProgressBar progressBar, Button comparebutton) {
        this.benchService = benchService;
        this.benchUI = benchUI;
        this.threads = threads;
        this.progressBar = progressBar;
        this.comparebutton = comparebutton;
    }

    public abstract Number benchmark();

    public Number call() throws Exception {
        Log.i(this.getClass().getName(), "Starting benchmark...");
        Number score = benchmark();
        try {
            // comparebutton.setEnabled(true);
            benchService.score = score;
            benchUI.notifyBenchmarkFinished(score);
        } catch (Throwable e) {
            Log.e(this.getClass().getName(), e.getMessage());
            e.printStackTrace();
        }
        return score;
    }

}
