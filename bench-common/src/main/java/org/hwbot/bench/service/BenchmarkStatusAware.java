package org.hwbot.bench.service;

import org.hwbot.bench.api.Benchmark;

public interface BenchmarkStatusAware {

    void notifyBenchmarkFinished(Benchmark benchmark);

}
