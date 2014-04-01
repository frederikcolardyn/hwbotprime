package org.hwbot.prime;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.hwbot.api.bench.dto.DeviceInfoDTO;
import org.hwbot.api.bench.dto.DeviceRecordDTO;
import org.hwbot.api.bench.dto.DeviceRecordDTO.RecordType;
import org.hwbot.api.bench.dto.DeviceRecordsDTO;
import org.hwbot.prime.api.HardwareRecordsStatusAware;
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
import org.hwbot.prime.tasks.HardwareRecordsTask;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher.ViewFactory;

public class TabFragmentBench extends Fragment implements BenchmarkStatusAware, HardwareStatusAware, SubmissionStatusAware, HardwareRecordsStatusAware {

	// the benchmark
	protected View rootView;
	protected BenchmarkStatusAware benchUI;
	protected SubmissionStatusAware submissionStatusAware;
	protected TextSwitcher temperatureStatus;
	protected TextSwitcher bestPhoneMe, bestPhone, bestCoreMe, bestCore, bestFamilyMe, bestFamily, bestOverallMe, bestOverall;
	private static Handler monitorThreadHandler;
	private static Thread monitorThread;
	private static Runnable monitorTask;
	private static TabFragmentBench fragment;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.i(this.getClass().getSimpleName(), "creating tab fragment bench.");
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

			temperatureStatus = (TextSwitcher) rootView.findViewById(R.id.temperatureLabel);

			bestCore = (TextSwitcher) rootView.findViewById(R.id.bestCore);
			bestCoreMe = (TextSwitcher) rootView.findViewById(R.id.bestCoreMe);
			bestFamily = (TextSwitcher) rootView.findViewById(R.id.bestFamily);
			bestFamilyMe = (TextSwitcher) rootView.findViewById(R.id.bestFamilyMe);
			bestOverall = (TextSwitcher) rootView.findViewById(R.id.bestOverall);
			bestOverallMe = (TextSwitcher) rootView.findViewById(R.id.bestOverallMe);
			bestPhone = (TextSwitcher) rootView.findViewById(R.id.bestPhone);
			bestPhoneMe = (TextSwitcher) rootView.findViewById(R.id.bestPhoneMe);

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

			temperatureStatus.setFactory(viewFactory);
			bestCore.setFactory(scoreViewFactory);
			bestCoreMe.setFactory(scoreViewFactory);
			bestPhone.setFactory(scoreViewFactory);
			bestPhoneMe.setFactory(scoreViewFactory);
			bestFamily.setFactory(scoreViewFactory);
			bestFamilyMe.setFactory(scoreViewFactory);
			bestOverall.setFactory(scoreViewFactory);
			bestOverallMe.setFactory(scoreViewFactory);

			// Declare the in and out animations and initialize them 
			Animation in = AnimationUtils.loadAnimation(MainActivity.getActivity(), android.R.anim.fade_in);
			Animation out = AnimationUtils.loadAnimation(MainActivity.getActivity(), android.R.anim.fade_out);
			bestPhoneMe.setInAnimation(in);
			bestPhoneMe.setOutAnimation(out);
			bestCoreMe.setInAnimation(in);
			bestCoreMe.setOutAnimation(out);
			bestFamilyMe.setInAnimation(in);
			bestFamilyMe.setOutAnimation(out);
			bestOverallMe.setInAnimation(in);
			bestOverallMe.setOutAnimation(out);

			setScore(bestPhone, (Float) null);
			setScore(bestCore, (Float) null);
			setScore(bestFamily, (Float) null);
			setScore(bestOverall, (Float) null);
			setScore(bestPhoneMe, (Float) null);
			setScore(bestCoreMe, (Float) null);
			setScore(bestFamilyMe, (Float) null);
			setScore(bestOverallMe, (Float) null);

			// updateOfflineMode();
			updateDeviceInfo();
			updateShowPersonalRecords();

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

	public void updateShowPersonalRecords() {
		if (SecurityService.getInstance().isLoggedIn()) {
			Log.i(this.getClass().getSimpleName(), "Show my records.");
			rootView.findViewById(R.id.myRecords).setVisibility(View.VISIBLE);
		} else {
			Log.i(this.getClass().getSimpleName(), "Do not show personal records.");
			rootView.findViewById(R.id.myRecords).setVisibility(View.GONE);
		}
	}

	private void updateDeviceInfo() {
		DeviceInfoDTO deviceInfo = MainActivity.activity.loadDeviceInfo();
		if (deviceInfo == null || deviceInfo.getId() == null) {
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
		Log.i(this.getClass().getSimpleName(), "New score: " + score);
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
								toast("Personal record! Disable offline mode to compare.");
							} else {
								if (AndroidHardwareService.getInstance().getDeviceInfo() != null
										&& AndroidHardwareService.getInstance().getDeviceInfo().getProcessorId() != null) {
									if (SecurityService.getInstance().isLoggedIn()) {
										toast("Personal record! Adding to leaderboard.");
									} else {
										toast("Personal record! Log in to compete on leaderboard.");
									}
									try {
										new SubmitResultTask(MainActivity.activity, submissionStatusAware, BenchService.getInstance().getDataFile())
												.execute((Void) null);
									} catch (Exception e) {
										Log.e(this.getClass().getSimpleName(), "Failed to submit " + e.getMessage());
										e.printStackTrace();
									}
								} else {
									toast("Personal record!");
								}
							}
							updateScoreIfBetter(score.floatValue());
						}
					} catch (UnsignedAppException e1) {
						toast("Done! Use production version to compete in leaderboard.");
					}
				}

			});
		} else {
			Log.e(this.getClass().getSimpleName(), "No main activity!");
		}
	}

	public void updateScoreIfBetter(Float score) {
		Log.i(this.getClass().getSimpleName(), "Updating score if better: " + score);
		setScoreIfBetter(bestPhoneMe, score);
		setScoreIfBetter(bestCoreMe, score);
		setScoreIfBetter(bestFamilyMe, score);
		setScoreIfBetter(bestOverallMe, score);
	}

	public void updateScores(Map<RecordType, DeviceRecordDTO> hwbotPrimeRecords) {
		if (hwbotPrimeRecords != null) {
			setScore(bestPhone, hwbotPrimeRecords.get(RecordType.best_device));
			setScore(bestCore, hwbotPrimeRecords.get(RecordType.best_cpu_core));
			setScore(bestFamily, hwbotPrimeRecords.get(RecordType.best_cpu_family));
			setScore(bestOverall, hwbotPrimeRecords.get(RecordType.best_overall_soc));
		}
	}

	public void updatePersonalScores(Map<RecordType, DeviceRecordDTO> hwbotPrimeRecordsPersonal) {
		BenchmarkResult bestScore = MainActivity.getActivity().getBestScore();
		Float score = (bestScore != null && bestScore.getScore() > 0) ? bestScore.getScore() : null;

		if (hwbotPrimeRecordsPersonal != null && !hwbotPrimeRecordsPersonal.isEmpty()) {
			Log.i(this.getClass().getSimpleName(), "My records: " + hwbotPrimeRecordsPersonal + " keys: " + hwbotPrimeRecordsPersonal.keySet());
			if (hwbotPrimeRecordsPersonal.get(RecordType.best_device) != null) {
				if (score == null || score < hwbotPrimeRecordsPersonal.get(RecordType.best_device).getScore()) {
					setScore(bestPhoneMe, hwbotPrimeRecordsPersonal.get(RecordType.best_device));
				}
			}
			setScore(bestCoreMe, hwbotPrimeRecordsPersonal.get(RecordType.best_cpu_core));
			setScore(bestFamilyMe, hwbotPrimeRecordsPersonal.get(RecordType.best_cpu_family));
			setScore(bestOverallMe, hwbotPrimeRecordsPersonal.get(RecordType.best_overall_soc));
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
		} else {
			Log.w(this.getClass().getSimpleName(), "Score null.");
		}
	}

	private void setScoreIfBetter(TextSwitcher switcher, Float score) {
		if (score != null) {
			TextView current = (TextView) switcher.getCurrentView();
			String value = StringUtils.remove("" + current.getText(), " PPS");
			if (getResources().getString(R.string.not_available).equals(value)) {
				setScore(switcher, score);
			} else if (NumberUtils.isNumber(value)) {
				if (Float.parseFloat(value) < score) {
					setScore(switcher, score);
				}
			} else {
				setScore(switcher, score);
			}

		}
	}

	private void setScore(TextSwitcher switcher, Float score) {
		if (score == null) {
			switcher.setText(getResources().getString(R.string.not_available));
		} else {
			String format = String.format(Locale.ENGLISH, "%.0f PPS", score);
			switcher.setText(format);
		}
	}

	@Override
	public void notifyDeviceInfo(final DeviceInfoDTO deviceInfo) {
		Log.i(this.getClass().getSimpleName(), "Device: " + deviceInfo);
		AndroidHardwareService.getInstance().setDeviceInfo(deviceInfo);

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
		Log.i(this.getClass().getSimpleName(), "Failed to load device info: " + status);
		if (MainActivity.activity != null) {
			MainActivity.activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					DeviceInfoDTO storedDeviceInfo = MainActivity.activity.loadDeviceInfo();
					if (storedDeviceInfo != null) {
						toast("Can't contact HWBOT, using cached info.");
						notifyDeviceInfo(storedDeviceInfo);
						rootView.findViewById(R.id.myRecords).setVisibility(View.VISIBLE);
						rootView.findViewById(R.id.worldRecords).setVisibility(View.VISIBLE);
					} else {
						switch (status) {
						case service_down:
							toast("Sorry, HWBOT is unavailable now.");
							break;
						case no_network:
							toast("No network access...");
							break;
						case unknown_device:
							toast("We don't know this phone yet, functionality will be limited.");
							HardwareDetectionTask hardwareDetectionTask = new HardwareDetectionTask(MainActivity.getActivity(), TabFragmentBench.this);
							hardwareDetectionTask.execute(getDeviceIdentification());

							rootView.findViewById(R.id.myRecords).setVisibility(View.GONE);
							rootView.findViewById(R.id.worldRecords).setVisibility(View.GONE);

							AndroidUtil.setTextInView(rootView, R.id.tableRowDevice, "unknown");
							AndroidUtil.setTextInView(rootView, R.id.tableRowProcessor, "unknown");
							AndroidUtil.setTextInView(rootView, R.id.tableRowVideocard, "unknown");
							AndroidUtil.setTextInView(rootView, R.id.tableRowMemory, "unknown");
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
				Log.i(this.getClass().getSimpleName(), response.toString());
			} else {
				if (org.apache.commons.lang.StringUtils.isNotEmpty(response.getMessage())) {
					toast("Error: " + response.getMessage());
				} else {
					toast("Sorry, can not contact HWBOT. :(");
				}
				Log.e(this.getClass().getSimpleName(), "Communication error: " + response.getTechnicalMessage());
			}
		} else {
			toast("Communication with HWBOT failed. :(");
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

	public void presentDeviceInfo(final DeviceInfoDTO deviceInfo) {
		if (deviceInfo != null && deviceInfo.getId() != null) {

			rootView.findViewById(R.id.myRecords).setVisibility(View.VISIBLE);
			rootView.findViewById(R.id.worldRecords).setVisibility(View.VISIBLE);

			if (TabFragmentCompare.getInstance() != null) {
				TabFragmentCompare.getInstance().showLeaderboardIfDeviceInfoPresent();
			}

			// toast("Hardware detected. Benchmark to analyze.");
			if (deviceInfo.getId() != null) {
				new HardwareRecordsTask(MainActivity.getActivity(), TabFragmentBench.getInstance(), deviceInfo.getId(), null).execute((Void) null);
				if (SecurityService.getInstance().isLoggedIn()) {
					new HardwareRecordsTask(MainActivity.getActivity(), TabFragmentBench.getInstance(), deviceInfo.getId(), SecurityService.getInstance()
							.getCredentials().getUserId()).execute((Void) null);
				}
			}

			AndroidUtil.setTextInView(rootView, R.id.tableRowDevice,
					(deviceInfo.getName() != null ? deviceInfo.getName() : getResources().getString(R.string.unknown)));
			AndroidUtil.setTextInView(rootView, R.id.tableRowProcessor, (deviceInfo.getProcessor() != null ? deviceInfo.getProcessor() : getResources()
					.getString(R.string.unknown)));
			AndroidUtil.setTextInView(rootView, R.id.tableRowVideocard, (deviceInfo.getVideocard() != null ? deviceInfo.getVideocard() : getResources()
					.getString(R.string.unknown)));
			AndroidUtil.setTextInView(rootView, R.id.tableRowMemory,
					(deviceInfo.getRam() != null ? deviceInfo.getRam() + " MB" : getResources().getString(R.string.unknown)));
		} else {
			toast("Unkown phone. Leaderboards not available.");
			Log.w(this.getClass().getSimpleName(), "No device info: " + deviceInfo);
		}
	}

	// is there a better way?
	public static TabFragmentBench getInstance() {
		return fragment;
	}

	public void toast(String message) {
		Toast toast = Toast.makeText(MainActivity.activity, message, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 30);
		toast.show();
	}

	@Override
	public void notifyDevicePersonalRecords(final DeviceRecordsDTO records) {
		if (MainActivity.activity != null) {
			MainActivity.activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Map<RecordType, DeviceRecordDTO> hwbotPrimeRecordsPersonal = records.getRecords();
					if (hwbotPrimeRecordsPersonal != null && !hwbotPrimeRecordsPersonal.isEmpty()) {
						Log.i(this.getClass().getSimpleName(), "Updating personal: " + hwbotPrimeRecordsPersonal);
						setScore(bestPhoneMe, hwbotPrimeRecordsPersonal.get(RecordType.best_device));
						setScore(bestCoreMe, hwbotPrimeRecordsPersonal.get(RecordType.best_cpu_core));
						setScore(bestFamilyMe, hwbotPrimeRecordsPersonal.get(RecordType.best_cpu_family));
						setScore(bestOverallMe, hwbotPrimeRecordsPersonal.get(RecordType.best_overall_soc));
					} else {
						rootView.findViewById(R.id.myRecords).setVisibility(View.GONE);
					}
				}
			});
		}
	}

	@Override
	public void notifyDeviceRecords(final DeviceRecordsDTO records) {
		if (MainActivity.activity != null) {
			MainActivity.activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Map<RecordType, DeviceRecordDTO> hwbotPrimeRecords = records.getRecords();
					if (hwbotPrimeRecords != null) {
						Log.i(this.getClass().getSimpleName(), "Updating WR: " + hwbotPrimeRecords);
						setScore(bestPhone, hwbotPrimeRecords.get(RecordType.best_device));
						setScore(bestCore, hwbotPrimeRecords.get(RecordType.best_cpu_core));
						setScore(bestFamily, hwbotPrimeRecords.get(RecordType.best_cpu_family));
						setScore(bestOverall, hwbotPrimeRecords.get(RecordType.best_overall_soc));
					}
				}
			});
		}
	}

	@Override
	public void notifyRecordsFailed(Status status) {
		if (MainActivity.activity != null) {
			MainActivity.activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					toast("Failed to update records.");
				}
			});
		}
	}

	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		Log.i(this.getClass().getSimpleName(), "View state restored.");
		super.onViewStateRestored(savedInstanceState);
	}

	public String getDeviceIdentification() {
		return Build.MANUFACTURER + " - " + Build.MODEL + " - " + Build.PRODUCT + " -- " + AndroidHardwareService.getInstance().getHardwareFromCpuInfo();
	}
}
