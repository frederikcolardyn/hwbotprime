package org.hwbot.bench;

import org.hwbot.bench.prime.ProgressBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PrimeBenchmark extends Benchmark {

    public static final String TIME_SPAN = "timespan";
    public static final String SILENT = "silent";
    public static final Long QUICK_TEST_MS = TimeUnit.SECONDS.toMillis(10);
    public static final Long STABILITY_TEST_MS = TimeUnit.MINUTES.toMillis(30);
    protected static final int iterations = 100;
    protected int batchsize = Integer.valueOf(System.getProperty("batchsize", "16"));
    private boolean silent;

    public PrimeBenchmark(int threads, ProgressBar progressBar) {
        super(threads, progressBar);
    }

    public PrimeBenchmark(BenchmarkConfiguration config, int threads, ProgressBar progressBar) {
        super(config, Integer.valueOf(System.getProperty("threads", String.valueOf(threads))), progressBar);
    }

    public static int getIterations() {
        return iterations;
    }

    @Override
    public String getClient() {
        if (QUICK_TEST_MS.equals(getConfig().getValue(TIME_SPAN))) {
            return "HWBOT Prime";
        } else if (STABILITY_TEST_MS.equals(getConfig().getValue(TIME_SPAN))) {
            return "HWBOT Prime 30min";
        } else {
            return "HWBOT Prime Custom Run";
        }
    }

    @Override
    public void warmup() {
        silent = false;

        if (!silent) {
            System.out.print("Warm up phase:   ");
        }
        benchrun(2000l);
        if (!silent) {
            System.out.println(" done!");
        }
    }

    @Override
    public Number benchmark(BenchmarkConfiguration configuration) {
        super.config = configuration;
        Long timespan = (Long) super.config.getValue(TIME_SPAN);
        if (Boolean.TRUE.equals(super.config.getValue(SILENT))) {
            this.silent = true;
        }
        if (!silent) {
            System.out.print("Benchmark phase: ");
        }
        Number benchrun = benchrun(timespan);
        if (!silent) {
            System.out.println(" done!");
        }
        return benchrun;
    }

    public Number benchrun(long timespanInMillis) {
        long before = System.currentTimeMillis();
        int primeStart = 5;
        int iteration = 0;
        int brokenWorkers = 0;
        int blocksize = threads * batchsize;
        int seconds = (int) (timespanInMillis / 1000);
        int progressFactor = 100 / seconds;

        List<Number> list = Collections.synchronizedList(new ArrayList<Number>());
        ThreadFactory tf = new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setPriority(Thread.MAX_PRIORITY);
                return thread;
            }
        };
        ExecutorService exec = Executors.newFixedThreadPool(threads, tf);
        List<Future<Void>> workers = new ArrayList<Future<Void>>(blocksize * 2);
        long time = getTime();
        long currTime = time;
        long endTime = time + timespanInMillis;
        int i = 0;
        while (getTime() <= endTime) {
            // submit work to the svc for execution across the thread pool
            long timelapse = getTime() - currTime;
            if (timelapse < 0) {
                throw new RuntimeException("clock adjustment detected! " + timelapse + "ms");
            }
            i++;
            PrimeRunnable worker = new PrimeRunnable(primeStart + i, list);
            Future<Void> submit = exec.submit(worker);
            workers.add(submit);

            if (workers.size() == blocksize * 2) {
                ArrayList<Future<Void>> runningWorkers = new ArrayList<Future<Void>>(workers.subList(0, blocksize));
                workers = new ArrayList<Future<Void>>(workers.subList(blocksize, blocksize * 2));
                for (Future<Void> future : runningWorkers) {
                    try {
                        future.get(1, TimeUnit.SECONDS);
                        long tl = (getTime() - time) / (1000 / Math.max(progressFactor, 0));
                        if (tl > iteration) {
                            iteration++;
                            if (!silent) {
                                this.progressBar.setValue(iteration);
                            }
                        }
                    } catch (TimeoutException e) {
                        System.err.print("x");
                        brokenWorkers++;
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException();
                    }
                }
            }
        }

        for (Future<Void> future : workers) {
            try {
                future.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        this.progressBar.setValue(100);

        if (brokenWorkers > 0) {
            System.err.println("[UNSTABLE] There were " + brokenWorkers + " broken workers out of " + i);
        }

        float timeneeded = (getTime() - before) / 1000f;

        if (QUICK_TEST_MS.equals(getConfig().getValue(TIME_SPAN))) {
            int primescalculated = list.size();
            return (primescalculated / timeneeded);
        } else if (STABILITY_TEST_MS.equals(getConfig().getValue(TIME_SPAN))) {
            return list.get(list.size()-1);
        } else {
            throw new RuntimeException("no score for custom run.");
        }
    }

    private long getTime() {
        return System.currentTimeMillis();
    }

    @Override
    protected void finalize() throws Throwable {
        System.out.println("exiting prime benchmark");
        super.finalize();
    }

}
