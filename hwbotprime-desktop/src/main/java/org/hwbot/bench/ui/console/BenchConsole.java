package org.hwbot.bench.ui.console;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.lang.StringUtils;
import org.hwbot.bench.Benchmark;
import org.hwbot.bench.service.BenchService;
import org.hwbot.bench.service.HardwareServiceFactory;
import org.hwbot.bench.ui.BenchUI;

public class BenchConsole implements BenchUI {

    private BufferedReader in;
    private final BenchService benchService;
    private String outputFile;

    public BenchConsole(BenchService benchService, String outputFile) {
        this.benchService = benchService;
        this.outputFile = outputFile;
        InputStreamReader converter = new InputStreamReader(System.in);
        in = new BufferedReader(converter);
    }

    public void notifyBenchmarkFinished(Benchmark benchmark) {

        Float processorTemperature = HardwareServiceFactory.getInstance().getProcessorTemperature();

        System.out.println("All done!" + (processorTemperature != null ? " Current CPU temperature: " + processorTemperature + " C" : ""));
        if (StringUtils.isNotEmpty(outputFile)) {
            if (!outputFile.endsWith(".hwbot")) {
                outputFile += ".hwbot";
            }
            File file = new File(outputFile);
            this.benchService.saveToFile(file);
            System.out.println("Score: " + benchService.formatScore(benchmark.getScore()) + " saved to file " + file.getName() + ".");
            System.exit(0);
        } else {
            System.out.println("Score: " + benchService.formatScore(benchmark.getScore()) + ".");
            while (true) {
                System.out.println("Hit enter to compare online, enter a filename to save to file, or type q to quit.");
                try {
                    String line = in.readLine();
                    if ("q".equals(line)) {
                        System.out.println("Bye!");
                        System.exit(0);
                    } else if (StringUtils.isNotEmpty(line)) {
                        if (!line.endsWith(".hwbot")) {
                            line += ".hwbot";
                        }
                        File file = new File(line);
                        this.benchService.saveToFile(file);
                        System.out.println("Saved file: " + file);
                    } else {
                        System.out.println("Submitting to HWBOT...");
                        benchService.submit();
                    }
                } catch (IOException e) {
                }
            }
        }

    }

    public void waitForCommands() {
        System.out.println("Running benchmark using " + HardwareServiceFactory.getInstance().getAvailableProcessors() + " threads.");
        benchService.benchmark();
    }

    @Override
    public void notifyError(String message) {
        System.out.println("[ERROR] " + message);
    }

}
