package org.hwbot.bench;

import java.util.concurrent.TimeUnit;

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
        configuration.setValue(PrimeBenchmark.TIME_SPAN, TimeUnit.SECONDS.toMillis(5));
        configuration.setValue(PrimeBenchmark.SILENT, false);

        PrimeBenchmark primeBenchmark = new PrimeBenchmark(16, new SystemProgressBar(100));

        int iterations = 3;
        float min = Integer.MAX_VALUE;
        float max = Integer.MIN_VALUE;

        primeBenchmark.warmup();

        while (iterations-- > 0) {
            float benchmark = primeBenchmark.benchmark(configuration).floatValue();
            System.out.println("pps: " + benchmark);
            if (benchmark > max) {
                max = benchmark;
            } else if (benchmark < min) {
                min = benchmark;
            }
        }

        float deviation = max - min;
        float perc = (deviation * min * 1f) / 1000f;
        System.out.println("deviation: " + min + " to " + max + ": deviation: " + deviation + " => " + perc + "%");

        Assert.assertTrue("deviation is too big! output should be more reliable.", perc <= 3f);
    }

}
