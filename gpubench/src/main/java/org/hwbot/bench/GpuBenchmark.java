package org.hwbot.bench;

import org.hwbot.bench.api.Benchmark;
import org.hwbot.bench.service.BenchService;
import org.hwbot.bench.ui.BenchUI;
import org.hwbot.bench.ui.ProgressBar;

public class GpuBenchmark extends Benchmark {

	public GpuBenchmark(BenchService benchService, BenchUI benchUI, int threads, ProgressBar progressBar) {
		super(benchService, benchUI, threads, progressBar);
	}

	@Override
	public Long benchmark() {
		try {
			return new Gears().benchmark();
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}

	}
}
