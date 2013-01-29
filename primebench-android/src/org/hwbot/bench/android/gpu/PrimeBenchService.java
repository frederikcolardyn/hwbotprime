package org.hwbot.bench.android.gpu;

import org.hwbot.bench.api.Benchmark;
import org.hwbot.bench.service.BenchService;

public class PrimeBenchService extends BenchService {

	@Override
	public Benchmark instantiateBenchmark() {
		return new PrimeBenchmark(this, super.benchUI, super.availableProcessors, super.progressBar, super.compareButton);
	}

	@Override
	public String formatScore(Long score) {
		return (score / 1000) + "s " + (score % 1000) + "ms";
	}

}
