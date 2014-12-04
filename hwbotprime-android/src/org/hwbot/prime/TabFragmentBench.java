package org.hwbot.prime;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import android.annotation.TargetApi;
import android.widget.*;
import android.widget.LinearLayout.LayoutParams;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.hwbot.api.bench.dto.DeviceInfoDTO;
import org.hwbot.api.bench.dto.DeviceRecordDTO;
import org.hwbot.api.bench.dto.DeviceRecordDTO.RecordType;
import org.hwbot.api.bench.dto.DeviceRecordsDTO;
import org.hwbot.api.esports.CompetitionStageDTO;
import org.hwbot.prime.api.CompetitionsStatusAware;
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
import org.hwbot.prime.tasks.*;
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
import android.widget.ViewSwitcher.ViewFactory;

public class TabFragmentBench extends Fragment implements BenchmarkStatusAware, HardwareStatusAware, SubmissionStatusAware, HardwareRecordsStatusAware, CompetitionsStatusAware {

	public static final int POSITION = 0;
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
	private AvailableCompetitionsTask availableCompetitionsTask;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Log.i(TabFragmentBench.class.getSimpleName(), "creating tab fragment bench.");
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
//			AndroidUtil.setTextInView(rootView, R.id.tableRowDevice, getResources().getString(R.string.resolving));
//			AndroidUtil.setTextInView(rootView, R.id.tableRowProcessor, getResources().getString(R.string.resolving));
//			AndroidUtil.setTextInView(rootView, R.id.tableRowVideocard, getResources().getString(R.string.resolving));
//			AndroidUtil.setTextInView(rootView, R.id.tableRowMemory, getResources().getString(R.string.resolving));
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
			loadRecordsFromStore();

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

				LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
				layoutParams.setMargins(AndroidUtil.dpToPx(32), 0, 0, 0);
				tableLayout.addView(tableRow, (3 + core), layoutParams);

				hardwareService.monitorCpuFrequency(core, frequencyLabel);
			}
			hardwareService.monitorTemperature(temperatureStatus);
			restartMonitorCpuFrequency();

			rootView.findViewById(R.id.hardwareUnknownButton).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					rootView.findViewById(R.id.hardwareUnknownButton).setVisibility(View.GONE);
					rootView.findViewById(R.id.hardwareUnknownAddDescription).setVisibility(View.VISIBLE);
					rootView.findViewById(R.id.hardwareUnknownAddButton).setVisibility(View.VISIBLE);
				}
			});

			final TabFragmentBench parent = this;

			rootView.findViewById(R.id.hardwareUnknownAddButton).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					rootView.findViewById(R.id.hardwareUnknown).setVisibility(View.GONE);
					rootView.findViewById(R.id.hardwareUnknownButton).setVisibility(View.GONE);
					rootView.findViewById(R.id.hardwareUnknownAddDescription).setVisibility(View.GONE);
					rootView.findViewById(R.id.hardwareUnknownAddButton).setVisibility(View.GONE);
					rootView.findViewById(R.id.hardwareUnknownThanks).setVisibility(View.VISIBLE);

					HardwareDetectionTask hardwareDetectionTask = new HardwareDetectionTask(MainActivity.activity, parent);
					hardwareDetectionTask.execute(Build.MODEL, ((EditText)rootView.findViewById(R.id.hardwareUnknownAddDescription)).getText().toString());

					MainActivity.getActivity().setDeviceSubmitted();
				}
			});
		}
		return rootView;
	}

	public void loadRecordsFromStore() {
		DeviceRecordsDTO personalDeviceRecords = MainActivity.getActivity().loadPersonalRecords();
		if (personalDeviceRecords != null) {
			updatePersonalScores(personalDeviceRecords.getRecords());
		}

		DeviceRecordsDTO deviceRecords = MainActivity.getActivity().loadRecords();
		if (deviceRecords != null) {
			updateScores(deviceRecords.getRecords());
		}
	}

	public void updateShowPersonalRecords() {
		if (rootView != null) {
			if (SecurityService.getInstance().isLoggedIn()) {
				// Log.i(TabFragmentBench.class.getSimpleName(), "Show my records.");
				rootView.findViewById(R.id.myRecords).setVisibility(View.VISIBLE);
			} else {
				// Log.i(TabFragmentBench.class.getSimpleName(), "Do not show personal records.");
				rootView.findViewById(R.id.myRecords).setVisibility(View.GONE);
			}
		}
	}

	private void updateDeviceInfo() {
		DeviceInfoDTO deviceInfo = MainActivity.activity.loadDeviceInfo();
		if (deviceInfo == null || deviceInfo.getId() == null) {
			// Log.i(TabFragmentBench.class.getName(), "Submitting hardware detection task...");
			HardwareDetectionTask hardwareDetectionTask = new HardwareDetectionTask(MainActivity.activity, this);
			hardwareDetectionTask.execute(Build.MODEL);
		} else {
			// Log.i(TabFragmentBench.class.getName(), "Loaded device info from cache.");
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
				// Log.i(TabFragmentBench.class.getName(), "Starting benchmark");
				Button text = (Button) rootView.findViewById(R.id.benchbutton);
				text.setText("Running benchmark...");
				// text.refreshDrawableState();
				ProgressBar progressbar = (ProgressBar) rootView.findViewById(R.id.progressBar1);
				progressbar.setProgress(0);
				// Log.i(TabFragmentBench.class.getSimpleName(), "Using benchmark: " + PrimeBenchService.getInstance() + " with progress bar: " + progressbar);
				PrimeBenchService.getInstance().initialize(text, progressbar, benchUI);

				// Log.i(TabFragmentBench.class.getName(), "Submitting worker...");
				BenchmarkTask benchmarkTask = new BenchmarkTask(benchUI, PrimeBenchService.getInstance().instantiateBenchmark());

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
				Log.e(TabFragmentBench.class.getName(), "error launching bench: " + e.getMessage());
			}
		}
	};

	@Override
	public void notifyBenchmarkFinished(final Number score) {
		// Log.i(TabFragmentBench.class.getSimpleName(), "New score: " + score);
		if (MainActivity.activity != null) {
			MainActivity.activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// Log.i(TabFragmentBench.class.getSimpleName(), "Displaying score: " + score);
					Button text = (Button) rootView.findViewById(R.id.benchbutton);
					text.setText(String.format(Locale.ENGLISH, "%.0f Primes Per Second", score));
					PrimeBenchService.getInstance().setScore(score.intValue());
					// store
					boolean best;
					try {
						best = MainActivity.activity.updateBestScore();
						// if (best) {
							if (MainActivity.activity.isOfflineMode()) {
								MainActivity.toast("Disable offline mode to compare score.");
							} else {
//								if (AndroidHardwareService.getInstance().getDeviceInfo() != null
//										&& AndroidHardwareService.getInstance().getDeviceInfo().getProcessorId() != null) {
									if (best) {
										MainActivity.toast("Personal record! Hooray!");
									} else {
										BenchmarkResult bestScore = MainActivity.activity.getBestScore();
										MainActivity.toast("Not your best." + (bestScore != null ? " Try to beat " + ((int)bestScore.getScore()) : ""));
									}
									try {
										new SubmitResultTask(MainActivity.activity, submissionStatusAware, PrimeBenchService.getInstance().getDataFile(
												MainActivity.getActivity().getApplicationContext())).execute((Void) null);
									} catch (Exception e) {
										Log.e(TabFragmentBench.class.getSimpleName(), "Failed to submit " + e.getMessage());
										e.printStackTrace();
									}
//								} else {
//									MainActivity.toast("Personal record!");
//								}
							}
							updateScoreIfBetter(score.floatValue());
						// }
					} catch (UnsignedAppException e1) {
						e1.printStackTrace();
						MainActivity.toast("Done! Use production version to compete in leaderboard.");
					}
				}

			});
		} else {
			Log.e(TabFragmentBench.class.getSimpleName(), "No main activity!");
		}
	}

	public void updateScoreIfBetter(Float score) {
		// Log.i(TabFragmentBench.class.getSimpleName(), "Updating score if better: " + score);
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
			// Log.i(TabFragmentBench.class.getSimpleName(), "My records: " + hwbotPrimeRecordsPersonal + " keys: " + hwbotPrimeRecordsPersonal.keySet());
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
			Log.w(TabFragmentBench.class.getSimpleName(), "Score null.");
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
		// Log.i(TabFragmentBench.class.getSimpleName(), "Device: " + deviceInfo);
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
		// Log.i(TabFragmentBench.class.getSimpleName(), "Failed to load device info: " + status);
		if (MainActivity.activity != null) {
			MainActivity.activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					DeviceInfoDTO storedDeviceInfo = MainActivity.activity.loadDeviceInfo();
					if (storedDeviceInfo != null) {
						MainActivity.toast("Can't contact HWBOT, using cached info.");
						notifyDeviceInfo(storedDeviceInfo);
						rootView.findViewById(R.id.myRecords).setVisibility(View.VISIBLE);
						rootView.findViewById(R.id.worldRecords).setVisibility(View.VISIBLE);
					} else {
						switch (status) {
						case service_down:
							MainActivity.toast("Sorry, HWBOT is unavailable now.");
							break;
						case no_network:
							// MainActivity.toast("No network access...");
							break;
						case unknown_device:

							if (rootView.findViewById(R.id.hardwareUnknownThanks).getVisibility() != View.VISIBLE){
								HardwareDetectionTask hardwareDetectionTask = new HardwareDetectionTask(MainActivity.getActivity(), TabFragmentBench.this);
								hardwareDetectionTask.execute(getDeviceIdentification());

								rootView.findViewById(R.id.myRecords).setVisibility(View.GONE);
								rootView.findViewById(R.id.worldRecords).setVisibility(View.GONE);

								((TableRow)rootView.findViewById(R.id.tableRowDevice).getParent()).setVisibility(View.GONE);
								((TableRow)rootView.findViewById(R.id.tableRowProcessor).getParent()).setVisibility(View.GONE);
								((TableRow)rootView.findViewById(R.id.tableRowVideocard).getParent()).setVisibility(View.GONE);
								((TableRow)rootView.findViewById(R.id.tableRowMemory).getParent()).setVisibility(View.GONE);

								if (!MainActivity.getActivity().isDeviceSubmitted()){
									MainActivity.toast("We don't know this phone yet, functionality will be limited. Please let us know which model you have so we can add support?");
									rootView.findViewById(R.id.hardwareUnknownButton).setVisibility(View.VISIBLE);
									rootView.findViewById(R.id.hardwareUnknown).setVisibility(View.VISIBLE);
								}
							}
//							AndroidUtil.setTextInView(rootView, R.id.tableRowDevice, "unknown");
//							AndroidUtil.setTextInView(rootView, R.id.tableRowProcessor, "unknown");
//							AndroidUtil.setTextInView(rootView, R.id.tableRowVideocard, "unknown");
//							AndroidUtil.setTextInView(rootView, R.id.tableRowMemory, "unknown");
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
				// Log.i(TabFragmentBench.class.getSimpleName(), response.toString());
				MainActivity.getActivity().markBestScoreSubmitted();
			} else {
				if (org.apache.commons.lang.StringUtils.isNotEmpty(response.getMessage())) {
					MainActivity.toast(response.getMessage());
				} else {
					MainActivity.toast("Sorry, can not contact HWBOT. :(");
				}
				Log.e(TabFragmentBench.class.getSimpleName(), "Communication error: " + response.getTechnicalMessage());
			}
		} else {
			MainActivity.toast("Communication with HWBOT failed. :(");
		}
	}

	// monitor
	public void startMonitor() {
		// Log.i(TabFragmentBench.class.getSimpleName(), "Restarting hardware monitor.");
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
						// Log.i(TabFragmentBench.class.getSimpleName(), "Stopping monitoring task...");
						Thread.currentThread().interrupt();
					}
				}
				// Log.i(TabFragmentBench.class.getSimpleName(), "Stopped monitoring task.");
			}
		};
		monitorThread = new Thread(monitorTask);
		monitorThread.start();
	}

	public void stopMonitorCpuFrequency() {
		try {
			if (monitorTask != null) {
				// Log.i(TabFragmentBench.class.getSimpleName(), "Stop monitor task.");
				monitorThreadHandler.removeCallbacks(AndroidHardwareService.getInstance());
				monitorThread.interrupt();
				monitorTask = null;
			}
		} catch (Exception e) {
			Log.e(TabFragmentBench.class.getSimpleName(), "Failed to stop monitoring task.");
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

			if (SecurityService.getInstance().isLoggedIn()) {
				rootView.findViewById(R.id.myRecords).setVisibility(View.VISIBLE);
			}
			rootView.findViewById(R.id.worldRecords).setVisibility(View.VISIBLE);

			if (TabFragmentCompare.getInstance() != null) {
				TabFragmentCompare.getInstance().showLeaderboardIfDeviceInfoPresent();
			}

			// MainActivity.toast("Hardware detected. Benchmark to analyze.");
			if (deviceInfo.getId() != null) {
				if (MainActivity.getActivity().loadRecords() == null) {
					new HardwareRecordsTask(MainActivity.getActivity(), TabFragmentBench.getInstance(), deviceInfo.getId(), null).execute((Void) null);
				}
				if (SecurityService.getInstance().isLoggedIn() && MainActivity.getActivity().loadPersonalRecords() == null) {
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

			// load competitions for this device!
			if (availableCompetitionsTask == null){
				availableCompetitionsTask = new AvailableCompetitionsTask(MainActivity.getActivity(), TabFragmentBench.getInstance());
				availableCompetitionsTask.execute(deviceInfo.getId());
			}
		} else {
			MainActivity.toast("Unkown phone. Leaderboards not available.");
			Log.w(TabFragmentBench.class.getSimpleName(), "No device info: " + deviceInfo);
		}
	}

	// is there a better way?
	public static TabFragmentBench getInstance() {
		if (fragment == null) {
			fragment = (TabFragmentBench) MainActivity.getActivity().mSectionsPagerAdapter.getItem(TabFragmentBench.POSITION);
		}
		return fragment;
	}

	@Override
	public void notifyDevicePersonalRecords(final DeviceRecordsDTO records) {
		if (MainActivity.getActivity() != null) {
			MainActivity.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Map<RecordType, DeviceRecordDTO> hwbotPrimeRecordsPersonal = records.getRecords();
					if (hwbotPrimeRecordsPersonal != null && !hwbotPrimeRecordsPersonal.isEmpty()) {
						// Log.i(TabFragmentBench.class.getSimpleName(), "Updating personal: " + hwbotPrimeRecordsPersonal);
						setScore(bestPhoneMe, hwbotPrimeRecordsPersonal.get(RecordType.best_device));
						setScore(bestCoreMe, hwbotPrimeRecordsPersonal.get(RecordType.best_cpu_core));
						setScore(bestFamilyMe, hwbotPrimeRecordsPersonal.get(RecordType.best_cpu_family));
						setScore(bestOverallMe, hwbotPrimeRecordsPersonal.get(RecordType.best_overall_soc));
						MainActivity.getActivity().storePersonalRecords(records);
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
						// Log.i(TabFragmentBench.class.getSimpleName(), "Updating WR: " + hwbotPrimeRecords);
						setScore(bestPhone, hwbotPrimeRecords.get(RecordType.best_device));
						setScore(bestCore, hwbotPrimeRecords.get(RecordType.best_cpu_core));
						setScore(bestFamily, hwbotPrimeRecords.get(RecordType.best_cpu_family));
						setScore(bestOverall, hwbotPrimeRecords.get(RecordType.best_overall_soc));
					}
					MainActivity.getActivity().storeRecords(records);
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
					MainActivity.toast("Failed to update records.");
				}
			});
		}
	}

	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		// Log.i(TabFragmentBench.class.getSimpleName(), "View state restored.");
		super.onViewStateRestored(savedInstanceState);
	}

	public String getDeviceIdentification() {
		return Build.MANUFACTURER + " - " + Build.MODEL + " - " + Build.PRODUCT + " -- " + AndroidHardwareService.getInstance().getHardwareFromCpuInfo();
	}

	public void prepareView() {
		updateDeviceInfo();
	}

	@Override
	public void notifyAvailableCompetitions(final List<CompetitionStageDTO> stages) {
		MainActivity.getActivity().runOnUiThread(
				new Runnable() {
					@Override
					public void run() {
						LinearLayout competitionBox = (LinearLayout) rootView.findViewById(R.id.competitionBox);
						if (stages != null && stages.size() > 0){
							competitionBox.setVisibility(View.VISIBLE);
							for (final CompetitionStageDTO stage : stages) {
								String label;
								if (SecurityService.getInstance().isLoggedIn()){
									label = rootView.getResources().getString(R.string.participate_checkbox, stage.getRound().getName());
								} else {
									label = rootView.getResources().getString(R.string.participate_login_checkbox, stage.getRound().getName());
								}

								final CheckBox button = new CheckBox(competitionBox.getContext());
								LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
								button.setLayoutParams(params);
								button.setText(label);
								button.setOnClickListener(new View.OnClickListener() {
									@Override
									public void onClick(View v) {
										boolean checked = ((CheckBox) v).isChecked();

										if (SecurityService.getInstance().isLoggedIn()){
											if (checked) {
												MainActivity.bench.setStageId(stage.getId());
											} else {
												MainActivity.bench.setStageId(null);
											}
										} else {
											// reset and go to account tab
											((CheckBox) v).setChecked(!checked);
											MainActivity.getActivity().mViewPager.setCurrentItem(TabFragmentAccount.POSITION);
										}
									}
								});
								if (StringUtils.isNotEmpty(stage.getMobileBanner())){
									ImageButton banner = new ImageButton(competitionBox.getContext());
									banner.setScaleType(ImageView.ScaleType.FIT_XY);
									banner.setTag(stage.getMobileBanner());
									new ImageLoaderTask(null).execute(banner);

									banner.setOnClickListener(new View.OnClickListener() {
										@Override
										@TargetApi(value = 15)
										public void onClick(View v) {
											// button.setChecked(!button.isChecked());
											Intent i = new Intent(Intent.ACTION_VIEW);
											i.setData(Uri.parse(BenchService.SERVER +"/competition/"+stage.getRound().getSafeName()));
											startActivity(i);
										}
									});

									competitionBox.addView(banner);
								}
								competitionBox.addView(button);
							}
						} else {
							Log.i(TabFragmentBench.class.getName(), "hiding competition box");
							competitionBox.setVisibility(View.GONE);
						}
						availableCompetitionsTask = null;
					}
				}
		);
	}
}
