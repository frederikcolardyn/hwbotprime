package org.hwbot.bench;

import java.util.concurrent.TimeUnit;

import org.hwbot.bench.api.Benchmark;
import org.hwbot.bench.api.BenchmarkConfiguration;
import org.hwbot.bench.service.BenchService;

public class PrimeBenchService extends BenchService {

    @Override
    public Benchmark instantiateBenchmark() {
        BenchmarkConfiguration configuration = new BenchmarkConfiguration();
        configuration.setValue(PrimeBenchmark.TIME_SPAN, TimeUnit.SECONDS.toMillis(10));
        configuration.setValue(PrimeBenchmark.SILENT, false);
        return new PrimeBenchmark(configuration, super.availableProcessors, super.progressBar);
    }

    @Override
    public String formatScore(Long score) {
        return String.valueOf(score);
    }

    @Override
    public String getSubtitle() {
        return "Multithreaded Prime Bench - ARM/x86 - Win/Mac/Linux";
    }

    @Override
    public String getTitle() {
        return "HWBOT Prime Benchmark";
    }

    @Override
    protected String getClientVersion() {
        return version;
    }

}
