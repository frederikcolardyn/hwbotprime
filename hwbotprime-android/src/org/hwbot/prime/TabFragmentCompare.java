package org.hwbot.prime;

import org.hwbot.prime.api.SubmissionRankingAware;
import org.hwbot.prime.model.DeviceInfo;
import org.hwbot.prime.model.Result;
import org.hwbot.prime.model.SubmissionRanking;
import org.hwbot.prime.service.AndroidHardwareService;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.tasks.ImageLoaderTask;
import org.hwbot.prime.tasks.RankingLoaderTask;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
			rankingTitle.setTextSize(18);

			compareView.removeAllViews();
			compareView.addView(rankingTitle);
			compareView.addView(createHr(context));

			Log.i(this.getClass().getName(), "Loading ranking...");
			RankingLoaderTask rankingLoaderTask = new RankingLoaderTask(MainActivity.activity, tabFragment, BenchService.SERVER
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
			rankingTitle.setTextSize(18);

			compareView.removeAllViews();
			compareView.addView(rankingTitle);
			compareView.addView(createHr(context));

			Log.i(this.getClass().getName(), "Loading ranking...");
			RankingLoaderTask rankingLoaderTask = new RankingLoaderTask(MainActivity.activity, tabFragment, BenchService.SERVER
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
			rankingTitle.setTextSize(18);

			compareView.removeAllViews();
			compareView.addView(rankingTitle);
			compareView.addView(createHr(context));

			Log.i(this.getClass().getName(), "Loading ranking...");
			RankingLoaderTask rankingLoaderTask = new RankingLoaderTask(MainActivity.activity, tabFragment, BenchService.SERVER
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
		// Log.i(this.getClass().getSimpleName(), "Submission ranking: " + ranking);

		if (MainActivity.activity != null) {
			MainActivity.activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Context context = rootView.getContext();
					Log.i(this.getClass().getSimpleName(), "List: " + ranking.getSubmissions().size());
					for (final Result result : ranking.getSubmissions()) {
						RelativeLayout relativeLayout = new RelativeLayout(context);
						RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
								RelativeLayout.LayoutParams.WRAP_CONTENT);
						layoutParams.bottomMargin = 20;
						layoutParams.leftMargin = 1;
						relativeLayout.setLayoutParams(layoutParams);
						relativeLayout.setBackgroundResource(R.drawable.container_dropshadow);

						int paddingTop = 0;

						TextView user = new TextView(context);
						user.setText(result.getScore() + " - " + result.getUser());
						user.setPadding(155, paddingTop += 10, 5, 5);
						user.setTextAppearance(context, R.style.NotificationUser);

						TextView message = new TextView(context);
						message.setText(result.getHardware()
								+ (result.getCpuFreq() != null && result.getCpuFreq() > 0 ? " @ " + result.getCpuFreq() + " MHz" : ""));
						message.setPadding(155, paddingTop += 30, 5, 5);
						message.setTextAppearance(context, R.style.NotificationHardware);
						message.setHorizontallyScrolling(true);
						message.setSingleLine();
						message.setEllipsize(TruncateAt.END);
						message.setMaxLines(1);

						TextView osBuild = null;
						if (result.getOsBuild() != null) {
							osBuild = new TextView(context);
							osBuild.setText("Android " + result.getOsBuild());
							osBuild.setPadding(155, paddingTop += 30, 5, 5);
							osBuild.setTextAppearance(context, R.style.NotificationHardware);
						}

						TextView kernel = null;
						if (result.getKernel() != null) {
							kernel = new TextView(context);
							kernel.setText("Kernel" + result.getKernel());
							kernel.setPadding(155, paddingTop += 30, 5, 5);
							kernel.setTextAppearance(context, R.style.NotificationHardware);
							kernel.setMaxLines(2);
							kernel.setEllipsize(TruncateAt.END);
						}

						TextView description = null;
						if (result.getDescription() != null) {
							description = new TextView(context);
							description.setText("\"" + result.getDescription() + "\"");
							description.setPadding(155, paddingTop += 40, 5, 5);
							description.setTextAppearance(context, R.style.NotificationDescription);
							description.setMaxLines(2);
							description.setEllipsize(TruncateAt.END);
						}

						if (result.getImage() != null) {
							try {
								// cache drawables?
								String url;
								if (result.getImage().startsWith("http")) {
									url = result.getImage();
								} else {
									url = BenchService.SERVER + result.getImage();
								}
								ImageView imageView = new ImageView(context);
								imageView.setMaxHeight(150);
								imageView.setMaxWidth(150);
								imageView.setMinimumHeight(150);
								imageView.setMinimumWidth(150);
								imageView.setScaleType(ScaleType.FIT_XY);
								// imageView.setAdjustViewBounds(true);
								imageView.setTag(url);
								imageView.setPadding(10, 5, 5, 5);
								relativeLayout.addView(imageView);
								new ImageLoaderTask().execute(imageView);
							} catch (Exception e) {
								Log.w(this.getClass().getSimpleName(), "Failed to load image: " + e.getMessage());
								e.printStackTrace();
							}
						}
						relativeLayout.setHapticFeedbackEnabled(true);
						relativeLayout.setClickable(true);
						relativeLayout.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(BenchService.SERVER_MOBILE + "/submission/" + result.getId()));
								MainActivity.activity.startActivity(intent);
							}
						});
						relativeLayout.addView(user);
						relativeLayout.addView(message);
						if (kernel != null) {
							relativeLayout.addView(kernel);
						}
						if (osBuild != null) {
							relativeLayout.addView(osBuild);
						}
						if (description != null) {
							relativeLayout.addView(description);
						}
						compareView.addView(relativeLayout);
					}
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
