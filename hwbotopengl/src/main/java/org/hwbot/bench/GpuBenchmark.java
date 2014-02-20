package org.hwbot.bench;

import org.hwbot.bench.api.Benchmark;
import org.hwbot.bench.api.BenchmarkConfiguration;
import org.hwbot.bench.service.BenchService;
import org.hwbot.bench.ui.BenchUI;
import org.hwbot.bench.ui.ProgressBar;

public class GpuBenchmark extends Benchmark {

    public GpuBenchmark(BenchService benchService, BenchUI benchUI, int threads, ProgressBar progressBar) {
        super(threads, progressBar);
    }

    @Override
    public Number benchmark(BenchmarkConfiguration configuration) {
        try {
            System.out.println("running benchmark");
            Long benchmark = new Gears().benchmark();
            System.out.println("end: " + benchmark);
            return benchmark;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getClient() {
        return "HWBOT OpenGL";
    }
}
