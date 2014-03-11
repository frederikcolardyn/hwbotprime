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
import org.hwbot.prime.service.SecurityService;
import org.hwbot.prime.service.SubmitResponse;
import org.hwbot.prime.tasks.BenchmarkTask;
import org.hwbot.prime.tasks.HardwareDetectionTask;
import org.hwbot.prime.tasks.SubmitResultTask;
import org.hwbot.prime.util.AndroidUtil;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

public class TabFragmentBench extends Fragment implements BenchmarkStatusAware, HardwareStatusAware, SubmissionStatusAware {

	// the benchmark
	protected View rootView;
	protected BenchmarkStatusAware benchUI;
	protected SubmissionStatusAware submissionStatusAware;
	protected TextSwitcher statusLabel, temperatureStatus, lastScore, bestScore;
	private Handler monitorThreadHandler;
	private Thread monitorThread;
	private Runnable monitorTask;

	protected final static String UNKOWN = "unkown";
	protected final static String RESOLVING = "resolving...";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		Log.i("CREATE", "bench tab");
		rootView = inflater.inflate(R.layout.fragment_main_bench, container, false);
		rootView.findViewById(R.id.benchbutton).setOnClickListener(launchBenchmarkListener);
		benchUI = this;
		submissionStatusAware = this;

		if (MainActivity.activity != null) {

			AndroidUtil.setTextInView(rootView, R.id.tableRowSoC, Build.BOARD);
			AndroidUtil.setTextInView(rootView, R.id.tableRowModel, Build.MODEL);
			AndroidUtil.setTextInView(rootView, R.id.tableRowDevice, RESOLVING);
			AndroidUtil.setTextInView(rootView, R.id.tableRowProcessor, RESOLVING);
			AndroidUtil.setTextInView(rootView, R.id.tableRowVideocard, RESOLVING);
			AndroidUtil.setTextInView(rootView, R.id.tableRowMemory, RESOLVING);
			AndroidUtil.setTextInView(rootView, R.id.tableRowBuild, "Android " + Build.VERSION.RELEASE);
			AndroidUtil.setTextInView(rootView, R.id.tableRowKernel, AndroidHardwareService.getInstance().getKernel());

			statusLabel = (TextSwitcher) rootView.findViewById(R.id.textSwitcher);
			temperatureStatus = (TextSwitcher) rootView.findViewById(R.id.temperatureLabel);
			lastScore = (TextSwitcher) rootView.findViewById(R.id.lastScore);
			bestScore = (TextSwitcher) rootView.findViewById(R.id.bestScore);
			// Set the ViewFactory of the TextSwitcher that will create TextView object when asked
			ViewFactory viewFactory = new ViewFactory() {
				public View makeView() {
					// create new textView and set the properties like clolr, size etc
					TextView myText = new TextView(MainActivity.activity);
					myText.setGravity(Gravity.LEFT);
					myText.setTextSize(14);
					myText.setTextColor(Color.DKGRAY);
					return myText;
				}
			};
			statusLabel.setFactory(viewFactory);
			temperatureStatus.setFactory(viewFactory);
			lastScore.setFactory(viewFactory);
			bestScore.setFactory(viewFactory);

			// Declare the in and out animations and initialize them  
			//			Animation in = AnimationUtils.loadAnimation(MainActivity.activity, android.R.anim.slide_out_right);
			//			Animation out = AnimationUtils.loadAnimation(MainActivity.activity, android.R.anim.slide_in_left);

			// set the animation type of textSwitcher
			//			statusLabel.setInAnimation(in);
			//			statusLabel.setOutAnimation(out);

			statusLabel.setText("Running hardware detection...");

			updateLastScore(null);
			updateBestScore();

			Log.i(this.getClass().getName(), "Submitting hardware worker...");
			HardwareDetectionTask hardwareDetectionTask = new HardwareDetectionTask(MainActivity.activity, this, Build.MODEL);
			hardwareDetectionTask.execute((Void) null);

			// cpu load bars
			AndroidHardwareService hardwareService = AndroidHardwareService.getInstance();
			stopMonitorCpuFrequency();
			int cores = hardwareService.getProcessorCores();

			TableLayout tableLayout = (TableLayout) rootView.findViewById(R.id.tableHardware);
			for (int core = 0; core < cores; core++) {
				TextSwitcher frequencyLabel = new TextSwitcher(MainActivity.activity);
				frequencyLabel.setFactory(viewFactory);

				TableRow tableRow = new TableRow(MainActivity.activity);
				TextView textView = new TextView(MainActivity.activity);
				textView.setText("  Core #" + (core + 1));

				tableRow.addView(textView);
				tableRow.addView(frequencyLabel);

				android.widget.TableLayout.LayoutParams layoutParams = new TableLayout.LayoutParams();
				layoutParams.setMargins(32, 0, 0, 0);
				tableLayout.addView(tableRow, (3 + core), layoutParams);

				hardwareService.monitorCpuFrequency(core, frequencyLabel);
			}
			hardwareService.monitorTemperature(temperatureStatus);
			startMonitorCpuFrequency();
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
				statusLabel.setText("Benchmark running...");
				Button text = (Button) rootView.findViewById(R.id.benchbutton);
				text.setText("Calculating...");
				// text.refreshDrawableState();
				ProgressBar progressbar = (ProgressBar) rootView.findViewById(R.id.progressBar1);
				progressbar.setProgress(0);
				// Button comparebutton = (Button) findViewById(R.id.comparebutton);
				Log.i(this.getClass().getSimpleName(), "Using benchmark: " + BenchService.getInstance() + " with progress bar: " + progressbar);
				BenchService.getInstance().initialize(text, progressbar, benchUI);
				// comparebutton.setEnabled(true);
				// bench.benchmark();

				Log.i(this.getClass().getName(), "Submitting worker...");
				BenchmarkTask benchmarkTask = new BenchmarkTask(benchUI, BenchService.getInstance().instantiateBenchmark());

				ExecutorService exec = Executors.newFixedThreadPool(2, new ThreadFactory() {
					public Thread newThread(Runnable runnable) {
						Thread thread = new Thread(runnable);
						thread.setPriority(Thread.MAX_PRIORITY);
						thread.setName("benchmark");
						thread.setDaemon(false);
						return thread;
					}
				});

				exec.submit(benchmarkTask);
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
					updateLastScore(score);
					// store
					boolean best = MainActivity.activity.updateBestScore(score);
					if (best) {
						if (SecurityService.getInstance().getCredentials() != null) {
							statusLabel.setText("Personal record! Slide to compare.");
						} else {
							statusLabel.setText("Personal record! Log in to compare.");
						}
						if (AndroidHardwareService.getInstance().getDeviceInfo() != null
								&& AndroidHardwareService.getInstance().getDeviceInfo().getProcessorId() != null) {
							try {
								new SubmitResultTask(MainActivity.activity, submissionStatusAware, BenchService.getInstance().getDataFile())
										.execute((Void) null);
							} catch (Exception e) {
								Log.e(this.getClass().getSimpleName(), "Failed to submit " + e.getMessage());
								e.printStackTrace();
							}
						}
					} else {
						statusLabel.setText("Done! Not your best score.");
					}
					updateBestScore();
				}

			});
		} else {
			Log.e(this.getClass().getSimpleName(), "No main activity!");
		}
	}

	protected void updateLastScore(final Number score) {
		lastScore = (TextSwitcher) rootView.findViewById(R.id.lastScore);
		if (score != null) {
			lastScore.setText(String.format(Locale.ENGLISH, "%.0f PPS", score));
		}
	}

	protected void updateBestScore() {
		TextSwitcher bestScore = (TextSwitcher) rootView.findViewById(R.id.bestScore);
		float bestScore2 = MainActivity.activity.getBestScore();
		if (bestScore2 > 0) {
			bestScore.setText(String.format(Locale.ENGLISH, "%.0f PPS", bestScore2));
		}
	}

	@Override
	public void notifyDeviceInfo(final DeviceInfo deviceInfo) {
		Log.i(this.getClass().getSimpleName(), "Device: " + deviceInfo);
		AndroidHardwareService.getInstance().setDeviceInfo(deviceInfo);

		// use gson
		MainActivity.activity.storeDeviceInfo(deviceInfo);

		if (MainActivity.activity != null) {
			MainActivity.activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					statusLabel.setText("ready");
					AndroidUtil.setTextInView(rootView, R.id.tableRowDevice, (deviceInfo.getName() != null ? deviceInfo.getName() : UNKOWN));
					AndroidUtil.setTextInView(rootView, R.id.tableRowProcessor, (deviceInfo.getProcessor() != null ? deviceInfo.getProcessor() : UNKOWN));
					AndroidUtil.setTextInView(rootView, R.id.tableRowVideocard, (deviceInfo.getVideocard() != null ? deviceInfo.getVideocard() : UNKOWN));
					AndroidUtil.setTextInView(rootView, R.id.tableRowMemory, (deviceInfo.getRam() != null ? deviceInfo.getRam() + " MB" : UNKOWN));
				}
			});
		}
	}

	@Override
	public void notifyDeviceInfoFailed(final Status status) {
		Log.w(this.getClass().getSimpleName(), "Failed to load device info: " + status);
		if (MainActivity.activity != null) {
			MainActivity.activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (AndroidHardwareService.getInstance().getDeviceInfo() != null) {
						statusLabel.setText("HWBOT service unavailable, using cached info.");
						notifyDeviceInfo(AndroidHardwareService.getInstance().getDeviceInfo());
					} else {
						switch (status) {
						case service_down:
							statusLabel.setText("Sorry, HWBOT service down...");
							break;
						case no_network:
							statusLabel.setText("No network access...");
							break;
						default:
							break;
						}
					}
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

	// monitor
	public void restartMonitor() {
		monitorThreadHandler = new Handler();
		monitorTask = new Runnable() {
			public void run() {
				while (true) {
					//Do time consuming stuff

					//The handler schedules the new runnable on the UI thread
					monitorThreadHandler.post(AndroidHardwareService.getInstance());
					//Add some downtime
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
	}

	public void stopMonitorCpuFrequency() {
		try {
			if (monitorTask != null) {
				System.out.println("should stop task: " + monitorTask);
				// monitorThreadHandler = null;
				monitorTask = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void startMonitorCpuFrequency() {
		try {
			if (monitorThreadHandler == null) {
				restartMonitor();
			}
			monitorThread = new Thread(monitorTask);
			monitorThread.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
