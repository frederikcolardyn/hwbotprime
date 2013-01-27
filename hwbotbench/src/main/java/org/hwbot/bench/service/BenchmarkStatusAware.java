package org.hwbot.bench.service;

public interface BenchmarkStatusAware {
	
	void notifyBenchmarkFinished(Long score);

}
