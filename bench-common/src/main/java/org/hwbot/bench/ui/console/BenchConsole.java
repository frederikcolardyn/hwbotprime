package org.hwbot.bench.ui.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.hwbot.bench.service.BenchService;
import org.hwbot.bench.ui.BenchUI;

public class BenchConsole implements BenchUI {

	private BufferedReader in;
	private final BenchService benchService;

	public BenchConsole(BenchService benchBot) {
		this.benchService = benchBot;
		InputStreamReader converter = new InputStreamReader(System.in);
		in = new BufferedReader(converter);
	}

	public void notifyBenchmarkFinished(Long score) {
		System.out.println("All done!");
		System.out.println("Score: " + benchService.formatScore(score) + ". Hit enter to compare online.");
		try {
			in.readLine();
			benchService.submit();
		} catch (IOException e) {
		}

	}

	public void waitForCommands() {
		System.out.println("Running benchmark using " + benchService.getAvailableProcessors() + " threads.");
		benchService.benchmark();
	}

}
