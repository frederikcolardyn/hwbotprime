package org.hwbot.bench.ui.android;

import org.hwbot.bench.service.BenchService;
import org.hwbot.bench.ui.BenchUI;

public class BenchConsole implements BenchUI {

	private final BenchService benchService;

	public BenchConsole(BenchService benchService) {
		this.benchService = benchService;
	}

	public void notifyBenchmarkFinished(Long score) {
		this.benchService.getOutput().write("Done! Score: " + benchService.formatScore(score) + ".");
	}

	public void waitForCommands() {
		this.benchService.getOutput().write("Running benchmark using " + benchService.getAvailableProcessors() + " threads.");
		benchService.benchmark();
	}

}
