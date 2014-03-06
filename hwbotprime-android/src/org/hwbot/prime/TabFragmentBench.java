package org.hwbot.prime;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.hwbot.prime.api.HardwareStatusAware;
import org.hwbot.prime.api.SubmissionStatusAware;
import org.hwbot.prime.model.DeviceInfo;
import org.hwbot.prime.service.AndroidHardwareService;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.BenchmarkStatusAware;
import org.hwbot.prime.service.SubmitResponse;
import org.hwbot.prime.tasks.BenchmarkTask;
import org.hwbot.prime.tasks.HardwareDetectionTask;
import org.hwbot.prime.tasks.SubmitResultTask;
import org.hwbot.prime.util.UIUtil;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

public class TabFragmentBench extends Fragment implements BenchmarkStatusAware, HardwareStatusAware, SubmissionStatusAware {

	// the benchmark
	protected View rootView;
	protected BenchmarkStatusAware benchUI;
	protected SubmissionStatusAware submissionStatusAware;
	private ExecutorService exec;
	public static TextSwitcher statusLabel;

	protected final static String UNKOWN = "unkown";
	protected final static String RESOLVING = "resolving...";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		Log.i("CREATE", "bench tab");

		exec = Executors.newFixedThreadPool(1, new ThreadFactory() {
			public Thread newThread(Runnable runnable) {
				Thread thread = new Thread(runnable);
				thread.setPriority(Thread.MAX_PRIORITY);
				thread.setName("benchmark");
				thread.setDaemon(false);
				return thread;
			}
		});

		rootView = inflater.inflate(R.layout.fragment_main_bench, container, false);
		rootView.findViewById(R.id.benchbutton).setOnClickListener(launchBenchmarkListener);
		benchUI = this;
		submissionStatusAware = this;

		if (MainActivity.activity != null) {
			UIUtil.setTextInView(rootView, R.id.tableRowSoC, "SoC:" + Build.MODEL + " - " + Build.BOARD);
			UIUtil.setTextInView(rootView, R.id.tableRowModel, "Device: " + RESOLVING);
			UIUtil.setTextInView(rootView, R.id.tableRowProcessor, "Processor: " + RESOLVING);
			UIUtil.setTextInView(rootView, R.id.tableRowVideocard, "Videocard: " + RESOLVING);
			UIUtil.setTextInView(rootView, R.id.tableRowMemory, "Memory: " + RESOLVING);
			UIUtil.setTextInView(rootView, R.id.tableRowBuild,
					"Android: " + Build.VERSION.RELEASE + " - " + (AndroidHardwareService.getFileContents("/proc/version")));

			statusLabel = (TextSwitcher) rootView.findViewById(R.id.textSwitcher);
			// Set the ViewFactory of the TextSwitcher that will create TextView object when asked
			statusLabel.setFactory(new ViewFactory() {
				public View makeView() {
					// create new textView and set the properties like clolr, size etc
					TextView myText = new TextView(MainActivity.activity);
					myText.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
					myText.setTextSize(20);
					myText.setTextColor(Color.DKGRAY);
					return myText;
				}
			});

			// Declare the in and out animations and initialize them  
			//			Animation in = AnimationUtils.loadAnimation(MainActivity.activity, android.R.anim.slide_out_right);
			//			Animation out = AnimationUtils.loadAnimation(MainActivity.activity, android.R.anim.slide_in_left);

			// set the animation type of textSwitcher
			//			statusLabel.setInAnimation(in);
			//			statusLabel.setOutAnimation(out);

			statusLabel.setText("Checking hardware...");

			Log.i(this.getClass().getName(), "Submitting hardware worker...");
			HardwareDetectionTask hardwareDetectionTask = new HardwareDetectionTask(this, Build.MODEL);
			hardwareDetectionTask.execute((Void) null);
		}
		return rootView;
	}

	/**
	* Touch listener to use for in-layout UI controls to delay hiding the system UI. This is to prevent the jarring behavior of controls going away while
	* interacting with activity UI.
	*/
	View.OnClickListener launchBenchmarkListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			try {
				Log.i(this.getClass().getName(), "Starting benchmark");
				TabFragmentBench.statusLabel.setText("Benchmark running...");
				Button text = (Button) rootView.findViewById(R.id.benchbutton);
				text.setText("Calculating...");
				text.refreshDrawableState();
				ProgressBar progressbar = (ProgressBar) rootView.findViewById(R.id.progressBar1);
				progressbar.setProgress(0);
				// Button comparebutton = (Button) findViewById(R.id.comparebutton);
				Log.i(this.getClass().getSimpleName(), "Using benchmark: " + BenchService.getInstance() + " with progress bar: " + progressbar);
				BenchService.getInstance().initialize(text, progressbar, benchUI);
				// comparebutton.setEnabled(true);
				// bench.benchmark();

				Log.i(this.getClass().getName(), "Submitting worker...");
				BenchmarkTask benchmarkTask = new BenchmarkTask(benchUI, BenchService.getInstance().instantiateBenchmark());

				ExecutorService exec = Executors.newFixedThreadPool(1, new ThreadFactory() {
					public Thread newThread(Runnable runnable) {
						Thread thread = new Thread(runnable);
						thread.setName("benchmark");
						thread.setDaemon(false);
						thread.setPriority(Thread.MAX_PRIORITY);
						return thread;
					}
				});

				benchmarkTask.executeOnExecutor(exec, (Void) null);
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(this.getClass().getName(), "error launching bench: " + e.getMessage());
			}
		}
	};

	@Override
	public void notifyBenchmarkFinished(final Number score) {
		Log.i(this.getClass().getSimpleName(), "Score " + score + " on parent " + MainActivity.activity);
		if (MainActivity.activity != null) {
			MainActivity.activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Log.i(this.getClass().getSimpleName(), "Displaying score: " + score);
					Button text = (Button) rootView.findViewById(R.id.benchbutton);
					text.setText(String.format(Locale.ENGLISH, "%.0f Primes per second", score));
					BenchService.getInstance().setScore(score.intValue());

					// store
					boolean best = MainActivity.activity.updateBestScore(score);
					if (best) {
						statusLabel.setText("Personal record! Slide to compare.");
						if (AndroidHardwareService.getInstance().getDeviceInfo() != null
								&& AndroidHardwareService.getInstance().getDeviceInfo().getProcessorId() != null) {
							try {
								new SubmitResultTask(submissionStatusAware, BenchService.getInstance().getDataFile()).execute((Void) null);
							} catch (Exception e) {
								Log.e(this.getClass().getSimpleName(), "Failed to submit " + e.getMessage());
								e.printStackTrace();
							}
						}
					} else {
						statusLabel.setText("Personal best: " + String.format(Locale.ENGLISH, "%.0f PPS", MainActivity.activity.getBestScore()));
					}

				}
			});
		} else {
			Log.e(this.getClass().getSimpleName(), "No main activity!");
		}
	}

	@Override
	public void notifyDeviceInfo(final DeviceInfo deviceInfo) {
		Log.i(this.getClass().getSimpleName(), "Device: " + deviceInfo);
		AndroidHardwareService.getInstance().setDeviceInfo(deviceInfo);
		if (MainActivity.activity != null) {
			MainActivity.activity.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					TabFragmentBench.statusLabel.setText("Tap to benchmark.");
					UIUtil.setTextInView(rootView, R.id.tableRowModel, "Device: " + (deviceInfo.getName() != null ? deviceInfo.getName() : UNKOWN));
					UIUtil.setTextInView(rootView, R.id.tableRowProcessor, "Processor: "
							+ (deviceInfo.getProcessor() != null ? deviceInfo.getProcessor() : UNKOWN));
					UIUtil.setTextInView(rootView, R.id.tableRowVideocard, "Videocard: "
							+ (deviceInfo.getVideocard() != null ? deviceInfo.getVideocard() : UNKOWN));
					UIUtil.setTextInView(rootView, R.id.tableRowMemory, "Memory: " + (deviceInfo.getRam() != null ? deviceInfo.getRam() + " MB" : UNKOWN));
				}
			});
		}
	}

	@Override
	public void notifySubmissionDone(SubmitResponse response) {
		if (response != null) {
			if (response.isSuccess()) {
				statusLabel.setText("Score synced!");
				Log.i(this.getClass().getSimpleName(), response.toString());
				//				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(response.getUrl()));
				//				MainActivity.activity.startActivity(intent);
			} else {
				statusLabel.setText("Communication with HWBOT failed. :(");
				Log.e(this.getClass().getSimpleName(), "Communication error: " + response.getTechnicalMessage());
			}
		} else {
			statusLabel.setText("Communication with HWBOT failed. :(");
		}
	}
}
