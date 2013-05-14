package org.hwbot.bench;

import junit.framework.Assert;

import org.hwbot.bench.api.BenchmarkConfiguration;
import org.hwbot.bench.ui.console.SystemProgressBar;
import org.junit.Ignore;
import org.junit.Test;

public class PrimeBenchmarkTest {

    @Test
    @Ignore
    public void testSpeed() {

        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        BenchmarkConfiguration configuration = new BenchmarkConfiguration();
        configuration.setValue(PrimeBenchmark.WORK_COUNT, 100000l);
        configuration.setValue(PrimeBenchmark.SILENT, true);

        PrimeBenchmark primeBenchmark = new PrimeBenchmark(16, new SystemProgressBar(100));

        int iterations = 10;
        long min = Integer.MAX_VALUE;
        long max = Integer.MIN_VALUE;

        System.out.println("warmup");
        primeBenchmark.warmup();
        System.out.println("done");

        while (iterations-- > 0) {
            Long benchmark = primeBenchmark.benchmark(configuration);
            System.out.println(benchmark);
            if (benchmark > max) {
                max = benchmark;
            } else if (benchmark < min) {
                min = benchmark;
            }
        }

        long deviation = max - min;
        float perc = (deviation * min * 1f) / 1000f;
        System.out.println("deviation: " + min + " to " + max + ": deviation: " + deviation + " => " + perc + "%");

        Assert.assertTrue("deviation is too big! output should be more reliable.", perc <= 3f);
    }

}
