package org.hwbot.prime.ui.android;

import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.ui.BenchUI;

public class BenchConsole implements BenchUI {

    private final BenchService benchService;

    public BenchConsole(BenchService benchService) {
        this.benchService = benchService;
    }

    public void notifyBenchmarkFinished(Number score) {
        this.benchService.getOutput().write("Done! Score: " + benchService.formatScore(score) + ".");
    }

    public void waitForCommands() {
        this.benchService.getOutput().write("Running benchmark using " + benchService.getAvailableProcessors() + " threads.");
        // benchService.benchmark();
    }

}
