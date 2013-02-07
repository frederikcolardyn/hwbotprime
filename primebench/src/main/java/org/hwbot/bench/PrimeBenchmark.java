package org.hwbot.bench;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.hwbot.bench.api.Benchmark;
import org.hwbot.bench.service.BenchService;
import org.hwbot.bench.ui.BenchUI;
import org.hwbot.bench.ui.ProgressBar;

public class PrimeBenchmark extends Benchmark {

	protected static final int workCount = 1000000;
	protected static final int iterations = 100;

	public PrimeBenchmark(BenchService benchService, BenchUI benchUI, int threads, ProgressBar progressBar) {
		super(benchService, benchUI, threads, progressBar);
	}

	@Override
	public Long benchmark() {
		// Make thread-safe list for adding results to
		int workleft = workCount;
		long before = System.currentTimeMillis();
		int primeStart = 5;
		int iteration = 0;
		int blocksize = threads * 8;
		List<Number> list = Collections.synchronizedList(new ArrayList<Number>());
		ExecutorService exec = Executors.newFixedThreadPool(threads);
		List<Future<Void>> workers = new ArrayList<Future<Void>>(100);
		for (int i = 0; i < workCount; i++) {
			// submit work to the svc for execution across the thread pool
			PrimeRunnable worker = new PrimeRunnable(primeStart + i, list);
			Future<Void> submit = exec.submit(worker);
			workers.add(submit);

			if (workers.size() == blocksize * 2) {
				ArrayList<Future<Void>> runningWorkers = new ArrayList<Future<Void>>(workers.subList(0, blocksize));
				workers = new ArrayList<Future<Void>>(workers.subList(blocksize, blocksize * 2));
				for (Future<Void> future : runningWorkers) {
					try {
						future.get();
						workleft--;
						if (workleft % (workCount / iterations) == 0) {
							iteration++;
							this.progressBar.setValue(iteration);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
						throw new RuntimeException();
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
				workleft--;
				if (workleft % (workCount / iterations) == 0) {
					iteration++;
					this.progressBar.setValue(iteration);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException();
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException();
			}
		}
		return System.currentTimeMillis() - before;
	}

	public static int getWorkcount() {
		return workCount;
	}

	public static int getIterations() {
		return iterations;
	}

}
