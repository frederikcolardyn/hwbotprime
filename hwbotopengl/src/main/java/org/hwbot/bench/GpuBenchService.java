package org.hwbot.bench;

import org.hwbot.bench.api.Benchmark;
import org.hwbot.bench.service.BenchService;

public class GpuBenchService extends BenchService {

    @Override
    public Benchmark instantiateBenchmark() {
        return new GpuBenchmark(this, super.benchUI, super.availableProcessors, super.progressBar);
    }

    @Override
    public String formatScore(Number score) {
        if (score == null) {
            return "no score";
        } else {
            return (score.intValue()) + "fps";

        }
    }

    @Override
    public String getSubtitle() {
        return "OpenGL GPU Bench - 32/64Bit - Win/Mac/Linux";
    }

    @Override
    public String getTitle() {
        return "HWBOT OpenGL Benchmark";
    }

    @Override
    protected String getClientVersion() {
        return "0.1.0";
    }

}
