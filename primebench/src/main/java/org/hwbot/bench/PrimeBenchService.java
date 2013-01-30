package org.hwbot.bench;

import org.hwbot.bench.api.Benchmark;
import org.hwbot.bench.service.BenchService;

public class PrimeBenchService extends BenchService {

	@Override
	public Benchmark instantiateBenchmark() {
		return new PrimeBenchmark(this, super.benchUI, super.availableProcessors, super.progressBar);
	}

	@Override
	public String formatScore(Long score) {
		return (score / 1000) + "s " + (score % 1000) + "ms.";
	}

	@Override
	public String getSubtitle() {
		return "Multithreaded Prime Bench - 32/64Bit - Win/Mac/Linux";
	}

	@Override
	public String getTitle() {
		return "HWBOT Prime Benchmark";
	}

	@Override
	protected String getClient() {
		return "hwbotprime";
	}

	@Override
	protected String getClientVersion() {
		return "0.1.0";
	}

}
