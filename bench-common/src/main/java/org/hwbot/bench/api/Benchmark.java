package org.hwbot.bench.api;

import java.util.concurrent.Callable;

import org.hwbot.bench.service.BenchService;
import org.hwbot.bench.ui.BenchUI;
import org.hwbot.bench.ui.ProgressBar;

public abstract class Benchmark implements Callable<Long> {

	protected final int threads;
	protected ProgressBar progressBar;
	protected final BenchUI benchUI;
	protected final BenchService benchService;

	public Benchmark(BenchService benchService, BenchUI benchUI, int threads, ProgressBar progressBar) {
		this.benchService = benchService;
		this.benchUI = benchUI;
		this.threads = threads;
		this.progressBar = progressBar;
	}

	public abstract Long benchmark();

	public Long call() throws Exception {
		System.out.println("Starting benchmark...");
		Long score = benchmark();
		benchService.score = score;
		benchUI.notifyBenchmarkFinished(score);
		return score;
	}

}
