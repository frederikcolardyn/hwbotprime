package org.hwbot.bench;

import java.util.concurrent.TimeUnit;

import org.hwbot.bench.prime.ProgressBar;
import org.junit.Assert;
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

        PrimeBenchmark primeBenchmark = new PrimeBenchmark(16, new ProgressBar() {
            private int progress;
            @SuppressWarnings("unused")
            private int max;

            @Override
            public void setValue(int progress) {
                this.progress = progress;
            }

            @Override
            public void setMaxValue(int max) {
                this.max = max;
            }

            @Override
            public int getProgress() {
                return progress;
            }
        });

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
