package org.hwbot.bench;

import org.hwbot.bench.service.BenchService;

import java.awt.*;

public class BootStrap {

    public static void main(String[] args) throws Exception {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        BenchService bench = new BenchService();

        boolean console = false;
        String outputFile = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("console".equals(arg)) {
                console = true;
            } else {
                outputFile = arg;
                console = true;
            }
        }

        if (GraphicsEnvironment.isHeadless() || "true".equals(System.getProperty("java.awt.headless")) || console) {
            bench.initialize(false, outputFile);
        } else {
            try {
                bench.initialize(true, outputFile);
            } catch (HeadlessException e) {
                bench.initialize(false, outputFile);
            }
        }
    }

}
