package org.hwbot.prime;

import org.hwbot.prime.api.SubmissionRankingAware;
import org.hwbot.prime.model.DeviceInfo;
import org.hwbot.prime.model.Result;
import org.hwbot.prime.model.SubmissionRanking;
import org.hwbot.prime.service.AndroidHardwareService;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.tasks.RankingLoaderTask;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ToggleButton;

public class TabFragmentCompare extends Fragment implements SubmissionRankingAware {

	protected ToggleButton compareProcessorButton;
	protected ToggleButton compareCoreButton;
	protected ToggleButton compareFamilyButton;
	protected LinearLayout compareView;
	protected View rootView;
	protected TabFragmentCompare tabFragment;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.i("CREATE", "compare tab");

		tabFragment = this;

		rootView = inflater.inflate(R.layout.fragment_main_compare, container, false);

		compareProcessorButton = (ToggleButton) rootView.findViewById(R.id.toggleButtonCompareProcessor);
		compareCoreButton = (ToggleButton) rootView.findViewById(R.id.toggleButtonCompareCore);
		compareFamilyButton = (ToggleButton) rootView.findViewById(R.id.toggleButtonCompareFamily);
		compareView = (LinearLayout) rootView.findViewById(R.id.compareView);

		compareProcessorButton.setOnClickListener(compareProcessorListener);
		compareCoreButton.setOnClickListener(compareCoreListener);
		compareFamilyButton.setOnClickListener(compareFamilyListener);

		loadActiveRanking();

		return rootView;
	}

	private View createHr(Context context) {
		View hr = new View(context);
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, 1);
		hr.setLayoutParams(params);
		hr.setBackgroundColor(0xffcccccc);
		hr.setPadding(20, 10, 20, 10);
		return hr;
	}

	public void loadProcessorRanking() {
		DeviceInfo deviceInfo = AndroidHardwareService.getInstance().getDeviceInfo();
		Context context = rootView.getContext();
		if (deviceInfo == null || deviceInfo.getProcessorId() == null) {
			TextView textView = new TextView(context);
			textView.setText("Sorry, unkown hardware can not be compared. The HWBOT crew has been notified to add your device.");
			compareView.addView(textView);
		} else {
			TextView rankingTitle = new TextView(context);
			rankingTitle.setText(deviceInfo.getProcessor());
			rankingTitle.setTextSize(16);

			TextView loading = new TextView(context);
			loading.setText("Loading...");
			loading.setTextSize(14);
			compareView.removeAllViews();
			compareView.addView(rankingTitle);
			compareView.addView(createHr(context));
			compareView.addView(loading);
			compareView.addView(createHr(context));

			Log.i(this.getClass().getName(), "Loading ranking...");
			RankingLoaderTask rankingLoaderTask = new RankingLoaderTask(tabFragment, BenchService.SERVER
					+ "/external/v3?type=submissionranking&limit=50&params=app=hwbot_prime&target=&cpuId=" + deviceInfo.getProcessorId());
			rankingLoaderTask.execute((Void) null);
		}
	}

	public void loadProcessorCoreRanking() {
		DeviceInfo deviceInfo = AndroidHardwareService.getInstance().getDeviceInfo();
		Context context = rootView.getContext();
		if (deviceInfo == null || deviceInfo.getProcessorCoreId() == null) {
			TextView textView = new TextView(rootView.getContext());
			textView.setText("Sorry, unkown hardware can not be compared. The HWBOT crew has been notified to add your device.");
			compareView.addView(textView);
		} else {
			TextView rankingTitle = new TextView(context);
			rankingTitle.setText(deviceInfo.getProcessorCore());
			rankingTitle.setTextSize(16);

			TextView loading = new TextView(context);
			loading.setText("Loading...");
			loading.setTextSize(14);
			compareView.removeAllViews();
			compareView.addView(rankingTitle);
			compareView.addView(createHr(context));
			compareView.addView(loading);
			compareView.addView(createHr(context));

			Log.i(this.getClass().getName(), "Loading ranking...");
			RankingLoaderTask rankingLoaderTask = new RankingLoaderTask(tabFragment, BenchService.SERVER
					+ "/external/v3?type=submissionranking&limit=50&params=app=hwbot_prime&target=&cpuCoreId=" + deviceInfo.getProcessorCoreId());
			rankingLoaderTask.execute((Void) null);
		}
	}

	public void loadProcessorFamilyRanking() {
		DeviceInfo deviceInfo = AndroidHardwareService.getInstance().getDeviceInfo();
		Context context = rootView.getContext();
		if (deviceInfo == null || deviceInfo.getProcessorCoreId() == null) {
			TextView textView = new TextView(rootView.getContext());
			textView.setText("Sorry, unkown hardware can not be compared. The HWBOT crew has been notified to add your device.");
			compareView.addView(textView);
		} else {
			TextView rankingTitle = new TextView(context);
			rankingTitle.setText(deviceInfo.getProcessorFamily());
			rankingTitle.setTextSize(16);

			TextView loading = new TextView(context);
			loading.setText("Loading...");
			loading.setTextSize(14);
			compareView.removeAllViews();
			compareView.addView(rankingTitle);
			compareView.addView(createHr(context));
			compareView.addView(loading);
			compareView.addView(createHr(context));

			Log.i(this.getClass().getName(), "Loading ranking...");
			RankingLoaderTask rankingLoaderTask = new RankingLoaderTask(tabFragment, BenchService.SERVER
					+ "/external/v3?type=submissionranking&limit=50&params=app=hwbot_prime&target=&cpuFamilyId=" + deviceInfo.getProcessorFamilyId());
			rankingLoaderTask.execute((Void) null);
		}
	}

	View.OnClickListener compareProcessorListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			try {
				Log.i(this.getClass().getName(), "Compare processor");
				if (!compareProcessorButton.isChecked()) {
					compareProcessorButton.toggle();
				}
				if (compareCoreButton.isChecked()) {
					compareCoreButton.toggle();
				}
				if (compareFamilyButton.isChecked()) {
					compareFamilyButton.toggle();
				}
				compareView.removeAllViews();
				loadProcessorRanking();
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(this.getClass().getName(), "error launching bench: " + e.getMessage());
			}
		}

	};

	View.OnClickListener compareCoreListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			try {
				Log.i(this.getClass().getName(), "Compare core");
				if (!compareCoreButton.isChecked()) {
					compareCoreButton.toggle();
				}
				if (compareProcessorButton.isChecked()) {
					compareProcessorButton.toggle();
				}
				if (compareFamilyButton.isChecked()) {
					compareFamilyButton.toggle();
				}
				compareView.removeAllViews();
				loadProcessorCoreRanking();
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(this.getClass().getName(), "error launching bench: " + e.getMessage());
			}
		}
	};

	View.OnClickListener compareFamilyListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			try {
				Log.i(this.getClass().getName(), "Compare family");
				if (!compareFamilyButton.isChecked()) {
					compareFamilyButton.toggle();
				}
				if (compareCoreButton.isChecked()) {
					compareCoreButton.toggle();
				}
				if (compareProcessorButton.isChecked()) {
					compareProcessorButton.toggle();
				}
				compareView.removeAllViews();
				loadProcessorFamilyRanking();
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(this.getClass().getName(), "error launching bench: " + e.getMessage());
			}
		}
	};

	@Override
	public void notifySubmissionRanking(final SubmissionRanking ranking) {
		Log.i(this.getClass().getSimpleName(), "Submission ranking: " + ranking);

		if (MainActivity.activity != null) {
			MainActivity.activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					TableLayout table = new TableLayout(rootView.getContext());

					table.setStretchAllColumns(true);
					table.setShrinkAllColumns(true);

					if (ranking.getSubmissions().size() == 0) {
						TableRow rowHeader = new TableRow(rootView.getContext());
						rowHeader.setGravity(Gravity.CENTER_HORIZONTAL);
						rowHeader.addView(createTextView("No submissions with this hardware yet.", 16, true));
						table.addView(rowHeader);
					} else {
						TableRow tableRowHeader = new TableRow(rootView.getContext());
						tableRowHeader.setGravity(Gravity.CENTER_HORIZONTAL);
						tableRowHeader.addView(createTextView("Score", 16, 10, 2, 3, 2, false));
						tableRowHeader.addView(createTextView("User", 2, 16, 10, 2, 3, 2));
						table.addView(tableRowHeader);
						for (Result result : ranking.getSubmissions()) {
							TableRow row = new TableRow(rootView.getContext());
							row.addView(createTextView(result.score, 14, 10, 2, 3, 2, false));
							row.addView(createTextView(result.user, 2, 14, 10, 2, 3, 2));
							table.addView(row);

							row = new TableRow(rootView.getContext());
							row.addView(createTextView("cpu:", 14, 10, 2, 3, 2, false));
							row.addView(createTextView(result.hardware, 2, 14, 10, 2, 3, 2));
							table.addView(row);
						}
					}
					Log.i(this.getClass().getSimpleName(), "child count: " + compareView.getChildCount());
					if (compareView.getChildCount() > 2) {
						compareView.removeViewAt(2);
					}
					compareView.addView(table);
				}
			});
		}

	}

	public final View createTextView(String applicationName, int size, int paddingLeft, int paddingTop, int paddingRight, int paddingBottom, boolean bold) {
		TextView textView = new TextView(rootView.getContext());
		textView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
		textView.setText(applicationName);
		textView.setTextSize(size);
		if (bold) {
			textView.setTypeface(Typeface.SERIF, Typeface.BOLD);
		}
		return textView;
	}

	public final View createTextView(String applicationName, int span, int size, int paddingLeft, int paddingTop, int paddingRight, int paddingBottom) {
		View header = createTextView(applicationName, size, paddingLeft, paddingTop, paddingRight, paddingBottom, false);
		TableRow.LayoutParams params = (TableRow.LayoutParams) header.getLayoutParams();
		if (params == null) {
			params = new TableRow.LayoutParams();
		}
		// params.span = span;
		header.setLayoutParams(params);
		return header;
	}

	public final View createTextView(String applicationName, int size, boolean bold) {
		return createTextView(applicationName, size, 3, 2, 3, 2, bold);
	}

	public void loadActiveRanking() {
		if (rootView != null) {
			if (compareProcessorButton.isChecked()) {
				loadProcessorRanking();
			}
			if (compareCoreButton.isChecked()) {
			}
			if (compareFamilyButton.isChecked()) {
			}
		}
	}

}
