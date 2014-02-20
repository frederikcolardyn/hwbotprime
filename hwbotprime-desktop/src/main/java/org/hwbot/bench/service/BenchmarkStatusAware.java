package org.hwbot.bench.service;

import org.hwbot.bench.Benchmark;

public interface BenchmarkStatusAware {

    void notifyBenchmarkFinished(Benchmark benchmark);

}
