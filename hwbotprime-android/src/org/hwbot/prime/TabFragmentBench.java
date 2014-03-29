package org.hwbot.prime;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.hwbot.api.bench.dto.DeviceInfoDTO;
import org.hwbot.api.bench.dto.DeviceInfoWithRecordsDTO;
import org.hwbot.api.bench.dto.DeviceRecordDTO;
import org.hwbot.api.bench.dto.DeviceRecordDTO.RecordType;
import org.hwbot.prime.api.HardwareStatusAware;
import org.hwbot.prime.api.SubmissionStatusAware;
import org.hwbot.prime.exception.UnsignedAppException;
import org.hwbot.prime.model.BenchmarkResult;
import org.hwbot.prime.service.AndroidHardwareService;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.BenchmarkStatusAware;
import org.hwbot.prime.service.SecurityService;
import org.hwbot.prime.service.SubmitResponse;
import org.hwbot.prime.tasks.BenchmarkTask;
import org.hwbot.prime.tasks.HardwareDetectionTask;
import org.hwbot.prime.tasks.SubmitResultTask;
import org.hwbot.prime.util.AndroidUtil;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.TextUtils.TruncateAt;
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
	protected TextSwitcher statusLabel, temperatureStatus;
	protected TextSwitcher bestPhoneMe, bestPhone, bestCoreMe, bestCore, bestFamilyMe, bestFamily, bestOverallMe, bestOverall;
	private static Handler monitorThreadHandler;
	private static Thread monitorThread;
	private static Runnable monitorTask;
	private static TabFragmentBench fragment;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		Log.i("CREATE", "bench tab");
		rootView = inflater.inflate(R.layout.fragment_main_bench, container, false);
		View benchbutton = rootView.findViewById(R.id.benchbutton);
		benchbutton.requestFocus();
		benchbutton.setOnClickListener(launchBenchmarkListener);

		fragment = this;
		benchUI = this;
		submissionStatusAware = this;

		if (MainActivity.activity != null) {

			AndroidUtil.setTextInView(rootView, R.id.tableRowSoC, Build.BOARD);
			AndroidUtil.setTextInView(rootView, R.id.tableRowModel, Build.MODEL);
			AndroidUtil.setTextInView(rootView, R.id.tableRowDevice, getResources().getString(R.string.resolving));
			AndroidUtil.setTextInView(rootView, R.id.tableRowProcessor, getResources().getString(R.string.resolving));
			AndroidUtil.setTextInView(rootView, R.id.tableRowVideocard, getResources().getString(R.string.resolving));
			AndroidUtil.setTextInView(rootView, R.id.tableRowMemory, getResources().getString(R.string.resolving));
			AndroidUtil.setTextInView(rootView, R.id.tableRowBuild, "Android " + Build.VERSION.RELEASE);
			AndroidUtil.setTextInView(rootView, R.id.tableRowKernel, AndroidHardwareService.getInstance().getKernel());

			statusLabel = (TextSwitcher) rootView.findViewById(R.id.textSwitcher);
			temperatureStatus = (TextSwitcher) rootView.findViewById(R.id.temperatureLabel);

			bestCore = (TextSwitcher) rootView.findViewById(R.id.bestCore);
			bestCoreMe = (TextSwitcher) rootView.findViewById(R.id.bestCoreMe);
			bestFamily = (TextSwitcher) rootView.findViewById(R.id.bestFamily);
			bestFamilyMe = (TextSwitcher) rootView.findViewById(R.id.bestFamilyMe);
			bestOverall = (TextSwitcher) rootView.findViewById(R.id.bestOverall);
			bestOverallMe = (TextSwitcher) rootView.findViewById(R.id.bestOverallMe);
			bestPhone = (TextSwitcher) rootView.findViewById(R.id.bestPhone);
			bestPhoneMe = (TextSwitcher) rootView.findViewById(R.id.bestPhoneMe);

			//			View findViewById = rootView.findViewById(R.id.lastScore);
			//			lastScore = (TextSwitcher) findViewById;
			// bestScore = (TextSwitcher) rootView.findViewById(R.id.bestScore);
			// Set the ViewFactory of the TextSwitcher that will create TextView object when asked
			ViewFactory viewFactory = new ViewFactory() {
				public View makeView() {
					TextView myText = new TextView(MainActivity.activity, null, R.style.ValueChanging);
					myText.setEllipsize(TruncateAt.START);
					myText.setGravity(Gravity.LEFT);
					myText.setTextAppearance(MainActivity.activity.getApplicationContext(), R.style.ValueChanging);
					return myText;
				}
			};

			ViewFactory scoreViewFactory = new ViewFactory() {
				public View makeView() {
					TextView myText = new TextView(MainActivity.activity, null, R.style.ValueScore);
					myText.setEllipsize(TruncateAt.START);
					myText.setGravity(Gravity.CENTER_HORIZONTAL);
					myText.setTextAppearance(MainActivity.activity.getApplicationContext(), R.style.ValueScore);
					return myText;
				}
			};

			statusLabel.setFactory(viewFactory);
			temperatureStatus.setFactory(viewFactory);
			bestCore.setFactory(scoreViewFactory);
			bestCoreMe.setFactory(scoreViewFactory);
			bestPhone.setFactory(scoreViewFactory);
			bestPhoneMe.setFactory(scoreViewFactory);
			bestFamily.setFactory(scoreViewFactory);
			bestFamilyMe.setFactory(scoreViewFactory);
			bestOverall.setFactory(scoreViewFactory);
			bestOverallMe.setFactory(scoreViewFactory);

			statusLabel.setText("Running hardware detection...");

			updateScores(null);
			// updateOfflineMode();
			updateDeviceInfo();

			// cpu load bars
			AndroidHardwareService hardwareService = AndroidHardwareService.getInstance();
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
				layoutParams.setMargins(AndroidUtil.dpToPx(32), 0, 0, 0);
				tableLayout.addView(tableRow, (3 + core), layoutParams);

				hardwareService.monitorCpuFrequency(core, frequencyLabel);
			}
			hardwareService.monitorTemperature(temperatureStatus);
			restartMonitorCpuFrequency();
		}
		return rootView;
	}

	private void updateDeviceInfo() {
		DeviceInfoWithRecordsDTO deviceInfo = MainActivity.activity.loadDeviceInfo();
		if (deviceInfo == null) {
			Log.i(this.getClass().getName(), "Submitting hardware detection task...");
			HardwareDetectionTask hardwareDetectionTask = new HardwareDetectionTask(MainActivity.activity, this);
			hardwareDetectionTask.execute(Build.MODEL);
		} else {
			Log.i(this.getClass().getName(), "Loaded device info from cache.");
			presentDeviceInfo(deviceInfo);
		}
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
				statusLabel.setText("Analyzing phone...");
				Button text = (Button) rootView.findViewById(R.id.benchbutton);
				text.setText("Running benchmark...");
				// text.refreshDrawableState();
				ProgressBar progressbar = (ProgressBar) rootView.findViewById(R.id.progressBar1);
				progressbar.setProgress(0);
				Log.i(this.getClass().getSimpleName(), "Using benchmark: " + BenchService.getInstance() + " with progress bar: " + progressbar);
				BenchService.getInstance().initialize(text, progressbar, benchUI);

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
					text.setText(String.format(Locale.ENGLISH, "%.0f Primes Per Second", score));
					BenchService.getInstance().setScore(score.intValue());
					// store
					boolean best;
					try {
						best = MainActivity.activity.updateBestScore();
						if (best) {
							if (MainActivity.activity.isOfflineMode()) {
								statusLabel.setText("Personal record! Disable offline mode to compare.");
							} else {
								if (SecurityService.getInstance().getCredentials() != null) {
									statusLabel.setText("Personal record! Syncing...");
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
							}
							updateScores(null);
						} else {
							statusLabel.setText("Done! Not your best score.");
						}
					} catch (UnsignedAppException e1) {
						statusLabel.setText("Done! Use production version to compare.");
					}
				}

			});
		} else {
			Log.e(this.getClass().getSimpleName(), "No main activity!");
		}
	}

	public void updateScores(DeviceInfoWithRecordsDTO deviceInfoWithRecordsDTO) {
		BenchmarkResult bestScore = MainActivity.getActivity().getBestScore();
		Float score = (bestScore != null && bestScore.getScore() > 0) ? bestScore.getScore() : null;

		Log.i(this.getClass().getSimpleName(), "Best score: " + bestScore);
		setScore(bestPhoneMe, score);

		if (deviceInfoWithRecordsDTO != null) {
			setScore(bestCoreMe, (Float) null);
			setScore(bestFamilyMe, (Float) null);
			setScore(bestOverallMe, (Float) null);
			
			setScore(bestPhone, (Float) null);
			setScore(bestCore, (Float) null);
			setScore(bestFamily, (Float) null);
			setScore(bestOverall, (Float) null);
			Map<RecordType, DeviceRecordDTO> hwbotPrimeRecords = deviceInfoWithRecordsDTO.getHwbotPrimeRecords();
			Map<RecordType, DeviceRecordDTO> hwbotPrimeRecordsPersonal = deviceInfoWithRecordsDTO.getHwbotPrimeRecordsPersonal();

			if (hwbotPrimeRecords != null) {
				setScore(bestPhone, hwbotPrimeRecords.get(RecordType.best_device));
				setScore(bestCore, hwbotPrimeRecords.get(RecordType.best_cpu_core));
				setScore(bestFamily, hwbotPrimeRecords.get(RecordType.best_cpu_family));
				setScore(bestOverall, hwbotPrimeRecords.get(RecordType.best_overall_soc));
			}

			if (hwbotPrimeRecordsPersonal != null) {
				if (hwbotPrimeRecordsPersonal.get(RecordType.best_device) != null) {
					if (score == null || score < hwbotPrimeRecordsPersonal.get(RecordType.best_device).getScore()) {
						setScore(bestPhoneMe, hwbotPrimeRecordsPersonal.get(RecordType.best_device));
					}
				}
				setScore(bestCoreMe, hwbotPrimeRecordsPersonal.get(RecordType.best_cpu_core));
				setScore(bestFamilyMe, hwbotPrimeRecordsPersonal.get(RecordType.best_cpu_family));
				setScore(bestOverallMe, hwbotPrimeRecordsPersonal.get(RecordType.best_overall_soc));
			} else {
				setScore(bestCoreMe, (Float) null);
				setScore(bestFamilyMe, (Float) null);
				setScore(bestOverallMe, (Float) null);
			}

		}
	}

	private void setScore(final TextSwitcher switcher, final DeviceRecordDTO score) {
		if (score != null) {
			if (score.getSubmissionId() != null) {
				switcher.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(BenchService.SERVER_MOBILE + "/submission/" + score.getSubmissionId()));
						MainActivity.activity.startActivity(intent);
					}
				});
			}
			setScore(switcher, score.getScore());
		}
	}

	private void setScore(TextSwitcher switcher, Float score) {
		if (score == null) {
			switcher.setText(getResources().getString(R.string.not_available));
		} else {
			switcher.setText(String.format(Locale.ENGLISH, "%.0f PPS", score));
		}
	}

	@Override
	public void notifyDeviceInfo(final DeviceInfoWithRecordsDTO deviceInfo) {
		Log.i(this.getClass().getSimpleName(), "Device: " + deviceInfo);
		AndroidHardwareService.getInstance().setDeviceInfo(deviceInfo.getDevice());

		MainActivity.activity.storeDeviceInfo(deviceInfo);

		if (MainActivity.activity != null) {
			MainActivity.activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					presentDeviceInfo(deviceInfo);
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
					DeviceInfoWithRecordsDTO storedDeviceInfo = MainActivity.activity.loadDeviceInfo();
					if (storedDeviceInfo != null) {
						statusLabel.setText("Can't contact HWBOT, using cached info.");
						notifyDeviceInfo(storedDeviceInfo);
					} else {
						switch (status) {
						case service_down:
							statusLabel.setText("Sorry, HWBOT is down...");
							break;
						case no_network:
							statusLabel.setText("No network access...");
							break;
						case unknown_device:
							statusLabel.setText("Could not resolve hardware by model.");
							HardwareDetectionTask hardwareDetectionTask = new HardwareDetectionTask(MainActivity.getActivity(), TabFragmentBench.this);
							hardwareDetectionTask.execute(Build.MANUFACTURER + " | " + Build.MODEL + " | " + Build.PRODUCT + " | "
									+ AndroidHardwareService.getInstance().getHardwareFromCpuInfo());
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
				statusLabel.setText("Score synced with HWBOT!");
				Log.i(this.getClass().getSimpleName(), response.toString());
			} else {
				if (org.apache.commons.lang.StringUtils.isNotEmpty(response.getMessage())) {
					statusLabel.setText(response.getMessage());
				} else {
					statusLabel.setText("Sorry, can not contact HWBOT. :(");
				}
				Log.e(this.getClass().getSimpleName(), "Communication error: " + response.getTechnicalMessage());
			}
		} else {
			statusLabel.setText("Communication with HWBOT failed. :(");
		}
	}

	// monitor
	public void startMonitor() {
		Log.i(this.getClass().getSimpleName(), "Restarting hardware monitor.");
		monitorThreadHandler = new Handler();
		monitorTask = new Runnable() {
			public void run() {
				while (!Thread.currentThread().isInterrupted()) {
					//Do time consuming stuff
					//The handler schedules the new runnable on the UI thread
					monitorThreadHandler.post(AndroidHardwareService.getInstance());
					//Add some downtime
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						Log.i(this.getClass().getSimpleName(), "Stopping monitoring task...");
						Thread.currentThread().interrupt();
					}
				}
				Log.i(this.getClass().getSimpleName(), "Stopped monitoring task.");
			}
		};
		monitorThread = new Thread(monitorTask);
		monitorThread.start();
	}

	public void stopMonitorCpuFrequency() {
		try {
			if (monitorTask != null) {
				Log.i(this.getClass().getSimpleName(), "Stop task: " + monitorTask);
				monitorThreadHandler.removeCallbacks(AndroidHardwareService.getInstance());
				monitorThread.interrupt();
				monitorTask = null;
			}
		} catch (Exception e) {
			Log.e(this.getClass().getSimpleName(), "Failed to stop monitoring task.");
		}
	}

	public void restartMonitorCpuFrequency() {
		try {
			if (monitorTask != null) {
				stopMonitorCpuFrequency();
			}
			startMonitor();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void presentDeviceInfo(final DeviceInfoWithRecordsDTO deviceInfoWithRecordsDTO) {
		statusLabel.setText("Hardware detected. Benchmark to analyze.");
		DeviceInfoDTO deviceInfo = deviceInfoWithRecordsDTO.getDevice();
		if (deviceInfo != null) {
			updateScores(deviceInfoWithRecordsDTO);

			AndroidUtil.setTextInView(rootView, R.id.tableRowDevice,
					(deviceInfo.getName() != null ? deviceInfo.getName() : getResources().getString(R.string.unknown)));
			AndroidUtil.setTextInView(rootView, R.id.tableRowProcessor, (deviceInfo.getProcessor() != null ? deviceInfo.getProcessor() : getResources()
					.getString(R.string.unknown)));
			AndroidUtil.setTextInView(rootView, R.id.tableRowVideocard, (deviceInfo.getVideocard() != null ? deviceInfo.getVideocard() : getResources()
					.getString(R.string.unknown)));
			AndroidUtil.setTextInView(rootView, R.id.tableRowMemory,
					(deviceInfo.getRam() != null ? deviceInfo.getRam() + " MB" : getResources().getString(R.string.unknown)));
		} else {
			Log.w(this.getClass().getSimpleName(), "No device info: " + deviceInfoWithRecordsDTO);
		}
	}

	// is there a better way?
	public static TabFragmentBench getInstance() {
		return fragment;
	}

}
