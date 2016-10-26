package org.hwbot.bench;

import java.util.concurrent.Callable;

import org.hwbot.bench.prime.ProgressBar;

public abstract class Benchmark implements Callable<Number> {

    protected final int threads;
    protected final ProgressBar progressBar;
    protected BenchmarkConfiguration config;
    protected Number score;
    protected Integer applicationId;

    public Benchmark(int threads, ProgressBar progressBar) {
        this.threads = threads;
        this.progressBar = progressBar;
    }

    public Benchmark(BenchmarkConfiguration config, int threads, ProgressBar progressBar) {
        this.config = config;
        this.threads = threads;
        this.progressBar = progressBar;
    }

    public BenchmarkConfiguration getConfig() {
        return config;
    }

    public void setConfig(BenchmarkConfiguration config) {
        this.config = config;
    }

    public void warmup() {
    };

    public abstract Number benchmark(BenchmarkConfiguration configuration);

    public abstract String getClient();

    public Number call() throws Exception {
        // System.out.println("Starting benchmark with config " + config);
        // if (config == null) {
        // return null;
        // } else {
        warmup();
        this.score = benchmark(config);
        return score;
        // }
    }

    public Number getScore() {
        return score;
    }

    public void setScore(Long score) {
        this.score = score;
    }

    public Integer getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Integer applicationId) {
        this.applicationId = applicationId;
    }
}
