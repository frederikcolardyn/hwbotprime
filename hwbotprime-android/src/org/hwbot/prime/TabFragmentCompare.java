package org.hwbot.prime;

import static org.hwbot.prime.util.AndroidUtil.dpToPx;
import static org.hwbot.prime.util.AndroidUtil.relativeTo;
import static org.hwbot.prime.util.AndroidUtil.relativeToParent;

import java.util.Locale;

import org.hwbot.api.bench.dto.DeviceInfoDTO;
import org.hwbot.api.bench.dto.DeviceRecordDTO;
import org.hwbot.api.bench.dto.DeviceRecordDTO.RecordType;
import org.hwbot.api.bench.dto.DeviceRecordsDTO;
import org.hwbot.api.generic.dto.SubmissionDTO;
import org.hwbot.prime.api.CommentObserver;
import org.hwbot.prime.api.SubmissionRankingAware;
import org.hwbot.prime.api.VoteObserver;
import org.hwbot.prime.model.SubmissionRanking;
import org.hwbot.prime.service.AndroidHardwareService;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.SecurityService;
import org.hwbot.prime.tasks.ImageLoaderTask;
import org.hwbot.prime.tasks.RankingLoaderTask;
import org.hwbot.prime.tasks.SubmitVoteTask;
import org.hwbot.prime.util.AndroidUtil;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewSwitcher.ViewFactory;

public class TabFragmentCompare extends Fragment implements SubmissionRankingAware, VoteObserver, CommentObserver {

	protected ToggleButton compareProcessorButton;
	protected ToggleButton compareCoreButton;
	protected ToggleButton compareFamilyButton;
	protected LinearLayout compareView;
	protected View rootView;
	protected static TabFragmentCompare tabFragment;
	protected TextSwitcher rankLabel, hardwareLabel;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Log.i("CREATE", "compare tab");

		tabFragment = this;

		rootView = inflater.inflate(R.layout.fragment_main_compare, container, false);

		showLeaderboardIfDeviceInfoPresent();

		compareProcessorButton = (ToggleButton) rootView.findViewById(R.id.toggleButtonCompareProcessor);
		compareCoreButton = (ToggleButton) rootView.findViewById(R.id.toggleButtonCompareCore);
		compareFamilyButton = (ToggleButton) rootView.findViewById(R.id.toggleButtonCompareFamily);
		compareView = (LinearLayout) rootView.findViewById(R.id.compareView);
		rankLabel = (TextSwitcher) rootView.findViewById(R.id.leaderboardRank);
		hardwareLabel = (TextSwitcher) rootView.findViewById(R.id.leaderboardHardware);

		compareProcessorButton.setOnClickListener(compareProcessorListener);
		compareCoreButton.setOnClickListener(compareCoreListener);
		compareFamilyButton.setOnClickListener(compareFamilyListener);

		ViewFactory ViewFactory = new ViewFactory() {
			public View makeView() {
				TextView myText = new TextView(MainActivity.activity, null, R.style.leaderboardLargeLabel);
				myText.setEllipsize(TruncateAt.START);
				myText.setGravity(Gravity.CENTER_HORIZONTAL);
				myText.setTextAppearance(MainActivity.activity.getApplicationContext(), R.style.leaderboardLargeLabel);
				return myText;
			}
		};
		rankLabel.setFactory(ViewFactory);
		hardwareLabel.setFactory(ViewFactory);
		rankLabel.setText(getResources().getString(R.string.not_available));
		hardwareLabel.setText(getResources().getString(R.string.not_available));

		loadActiveRanking();

		boolean competeInfoSeen = MainActivity.getActivity().isSeen(MainActivity.COMPETE_INFO);
		if (competeInfoSeen || SecurityService.getInstance().isLoggedIn()) {
			rootView.findViewById(R.id.competeBox).setVisibility(View.GONE);
		}

		return rootView;
	}

	public void showLeaderboardIfDeviceInfoPresent() {
		// check hardware available or not
		if (AndroidHardwareService.getInstance().getDeviceInfo() != null) {
			// Log.i(this.getClass().getSimpleName(), "Device info present.");
			if (!SecurityService.getInstance().isLoggedIn()) {
				rootView.findViewById(R.id.leaderboardAvailableBox).setVisibility(View.VISIBLE);
			}
			rootView.findViewById(R.id.leaderboardNotAvailableBox).setVisibility(View.GONE);
		} else {
			// Log.i(this.getClass().getSimpleName(), "Device info present not present.");
			rootView.findViewById(R.id.leaderboardAvailableBox).setVisibility(View.GONE);
			rootView.findViewById(R.id.leaderboardNotAvailableBox).setVisibility(View.VISIBLE);
		}
	}

	public void loadProcessorRanking() {
		DeviceInfoDTO deviceInfo = AndroidHardwareService.getInstance().getDeviceInfo();
		Context context = rootView.getContext();
		if (deviceInfo == null || deviceInfo.getProcessorId() == null) {
			compareView.removeAllViews();
			TextView textView = new TextView(context);
			textView.setText(getResources().getString(R.string.not_available));
			compareView.addView(textView);
		} else {
			hardwareLabel.setText(deviceInfo.getProcessor());
			// android ui bug, call it twice or textswitcher does not resize
			hardwareLabel.setText(deviceInfo.getProcessor());

			DeviceRecordsDTO personalDeviceRecords = MainActivity.getActivity().loadPersonalRecords();
			if (personalDeviceRecords != null) {
				DeviceRecordDTO deviceRecordDTO = personalDeviceRecords.getRecords().get(RecordType.best_device);
				if (deviceRecordDTO == null || deviceRecordDTO.getScore() == null) {
					rankLabel.setText(getResources().getString(R.string.not_available));
				} else {
					String format = String.format(Locale.ENGLISH, "%.0f PPS", deviceRecordDTO.getScore());
					rankLabel.setText(format);
				}
			}

			compareView.removeAllViews();

			// Log.i(this.getClass().getName(), "Loading ranking...");
			RankingLoaderTask rankingLoaderTask = new RankingLoaderTask(MainActivity.activity, tabFragment, BenchService.SERVER
					+ "/external/v3?type=submissionranking&orderBy=rank&limit=100&params=app=hwbot_prime&hardwareType=processor&target=android&hardwareId="
					+ deviceInfo.getProcessorId());
			rankingLoading();

			rankingLoaderTask.execute((Void) null);
		}
	}

	public void rankingLoading() {
		compareView.removeAllViews();
		TextView noComments = new TextView(rootView.getContext());
		noComments.setText(R.string.loading);
		LinearLayout.LayoutParams authorLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		authorLayout.topMargin = AndroidUtil.dpToPx(20);
		authorLayout.bottomMargin = AndroidUtil.dpToPx(20);
		authorLayout.gravity = Gravity.CENTER;
		noComments.setLayoutParams(authorLayout);
		noComments.setGravity(Gravity.CENTER);
		compareView.addView(noComments);
	}

	public void loadProcessorCoreRanking() {
		DeviceInfoDTO deviceInfo = AndroidHardwareService.getInstance().getDeviceInfo();
		Context context = rootView.getContext();
		if (deviceInfo == null || deviceInfo.getProcessorCoreId() == null) {
			TextView textView = new TextView(context);
			textView.setText("Sorry, unkown hardware can not be compared. The HWBOT crew has been notified to add your device.");
			compareView.addView(textView);
		} else {
			hardwareLabel.setText(deviceInfo.getProcessorCore());
			// android ui bug, call it twice or textswitcher does not resize
			hardwareLabel.setText(deviceInfo.getProcessorCore());

			DeviceRecordsDTO personalDeviceRecords = MainActivity.getActivity().loadPersonalRecords();
			if (personalDeviceRecords != null) {
				DeviceRecordDTO deviceRecordDTO = personalDeviceRecords.getRecords().get(RecordType.best_cpu_core);
				if (deviceRecordDTO == null || deviceRecordDTO.getScore() == null) {
					rankLabel.setText(getResources().getString(R.string.not_available));
				} else {
					String format = String.format(Locale.ENGLISH, "%.0f PPS", deviceRecordDTO.getScore());
					rankLabel.setText(format);
				}
			}

			compareView.removeAllViews();

			// Log.i(this.getClass().getName(), "Loading ranking...");
			RankingLoaderTask rankingLoaderTask = new RankingLoaderTask(MainActivity.activity, tabFragment, BenchService.SERVER
					+ "/external/v3?type=submissionranking&orderBy=rank&limit=100&params=app=hwbot_prime&target=android&hardwareId="
					+ deviceInfo.getProcessorId() + "&coreId=" + deviceInfo.getProcessorCoreId());
			rankingLoading();
			rankingLoaderTask.execute((Void) null);
		}
	}

	public void loadProcessorFamilyRanking() {
		DeviceInfoDTO deviceInfo = AndroidHardwareService.getInstance().getDeviceInfo();
		Context context = rootView.getContext();
		if (deviceInfo == null || deviceInfo.getProcessorCoreId() == null) {
			TextView textView = new TextView(context);
			textView.setText("Sorry, unkown hardware can not be compared. The HWBOT crew has been notified to add your device.");
			compareView.addView(textView);
		} else {
			hardwareLabel.setText(deviceInfo.getProcessorFamily());
			// android ui bug, call it twice or textswitcher does not resize
			hardwareLabel.setText(deviceInfo.getProcessorFamily());

			DeviceRecordsDTO personalDeviceRecords = MainActivity.getActivity().loadPersonalRecords();
			if (personalDeviceRecords != null) {
				DeviceRecordDTO deviceRecordDTO = personalDeviceRecords.getRecords().get(RecordType.best_cpu_family);
				if (deviceRecordDTO == null || deviceRecordDTO.getScore() == null) {
					rankLabel.setText(getResources().getString(R.string.not_available));
				} else {
					String format = String.format(Locale.ENGLISH, "%.0f PPS", deviceRecordDTO.getScore());
					rankLabel.setText(format);
				}
			}

			compareView.removeAllViews();

			// Log.i(this.getClass().getName(), "Loading ranking...");
			RankingLoaderTask rankingLoaderTask = new RankingLoaderTask(MainActivity.activity, tabFragment, BenchService.SERVER
					+ "/external/v3?type=submissionranking&orderBy=rank&limit=100&params=app=hwbot_prime&target=android&hardwareId="
					+ deviceInfo.getProcessorId() + "&familyId=" + deviceInfo.getProcessorFamilyId());
			rankingLoading();
			rankingLoaderTask.execute((Void) null);
		}
	}

	View.OnClickListener compareProcessorListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			try {
				// Log.i(this.getClass().getName(), "Compare processor");
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
				// Log.i(this.getClass().getName(), "Compare core");
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
				// Log.i(this.getClass().getName(), "Compare family");
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

	@SuppressLint("NewApi")
	@Override
	public void notifySubmissionRanking(final SubmissionRanking ranking) {
		// // Log.i(this.getClass().getSimpleName(), "Submission ranking: " + ranking);

		if (MainActivity.activity != null) {
			MainActivity.activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					final Context context = rootView.getContext();
					// Log.i(this.getClass().getSimpleName(), "Ranking list: " + ranking.getList().size());

					compareView.removeAllViews();

					final ViewFactory textSwitcherViewFactory = new ViewFactory() {
						public View makeView() {
							TextView myText = new TextView(MainActivity.activity, null, R.style.leaderboardTextAction);
							myText.setTextAppearance(MainActivity.activity.getApplicationContext(), R.style.leaderboardTextAction);
							return myText;
						}
					};

					String userName = (SecurityService.getInstance().isLoggedIn() ? SecurityService.getInstance().getCredentials().getUserName() : null);
					int rank = 1;
					for (final SubmissionDTO result : ranking.getList()) {
						final boolean isMe = (userName != null && userName.equals(result.getUser()));
						final int style = isMe ? R.style.leaderboardTextMe : R.style.leaderboardText;
						final int backgroundResource = rank % 2 == 0 ? R.drawable.container_leaderboard_light : R.drawable.container_leaderboard_dark;
						final int row = rank;

						RelativeLayout recordSummary = new RelativeLayout(context, null, style);
						recordSummary.setBackgroundResource(backgroundResource);
						final LinearLayout.LayoutParams recordLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
								LinearLayout.LayoutParams.WRAP_CONTENT);
						recordLayout.bottomMargin = dpToPx(0);
						recordSummary.setLayoutParams(recordLayout);
						recordSummary.setClickable(true);

						TextView rankBox = new TextView(context, null, style);
						rankBox.setGravity(Gravity.CENTER);
						rankBox.setText(String.valueOf(rank));
						rankBox.setTextAppearance(context, style);
						rankBox.setLayoutParams(relativeTo(RelativeLayout.ALIGN_PARENT_LEFT, 8, 10, 8, 10));
						rankBox.setId(13 * rank);

						ImageView avatar = new ImageView(context);
						RelativeLayout.LayoutParams rankBoxLayout = new RelativeLayout.LayoutParams(dpToPx(24), dpToPx(24));
						rankBoxLayout.addRule(RelativeLayout.RIGHT_OF, rankBox.getId());
						rankBoxLayout.leftMargin = dpToPx(0);
						rankBoxLayout.topMargin = dpToPx(10);
						rankBoxLayout.rightMargin = dpToPx(5);
						rankBoxLayout.bottomMargin = dpToPx(10);
						avatar.setLayoutParams(rankBoxLayout);
						avatar.setId(17 * rank);

						if (result.getImage() != null) {
							try {
								String url;
								if (result.getImage().startsWith("http")) {
									url = result.getImage();
								} else {
									url = BenchService.SERVER + result.getImage();
								}
								Log.d(this.getClass().getSimpleName(), "Result.getImage(): " + url);
								avatar.setScaleType(ScaleType.FIT_XY);
								avatar.setTag(url);
								new ImageLoaderTask(MainActivity.getActivity().getAnonymousIcon()).execute(avatar);
							} catch (Exception e) {
								Log.w(this.getClass().getSimpleName(), "Failed to load image: " + e.getMessage());
								e.printStackTrace();
								avatar.setImageDrawable(MainActivity.getActivity().getAnonymousIcon());
							}
						} else {
							avatar.setImageDrawable(MainActivity.getActivity().getAnonymousIcon());
						}

						final TextView user = new TextView(context, null, style);
						user.setGravity(Gravity.CENTER);
						user.setText(result.getUser());
						user.setTextAppearance(context, style);
						user.setLayoutParams(relativeToParent(RelativeLayout.RIGHT_OF, rankBox.getId(), 30, 10, 10, 10));
						user.setId(19 * rank);

						final ImageView expandIcon = new ImageView(context);
						expandIcon.setImageDrawable(MainActivity.activity.getResources().getDrawable(R.drawable.ic_action_expand));
						// expandIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(40), dpToPx(80)));
						expandIcon.setLayoutParams(relativeTo(RelativeLayout.ALIGN_PARENT_RIGHT, 0, 6, 0, 0));
						expandIcon.setPadding(dpToPx(0), dpToPx(0), dpToPx(0), dpToPx(0));
						expandIcon.setDrawingCacheEnabled(true);
						expandIcon.setId(23 * rank);

						final TextView score = new TextView(context, null, style);
						score.setGravity(Gravity.CENTER);
						score.setText(result.getScore().toUpperCase(Locale.ENGLISH));
						score.setTextAppearance(context, style);
						score.setLayoutParams(relativeToParent(RelativeLayout.LEFT_OF, expandIcon.getId(), 0, 10, 0, 10));

						recordSummary.setOnClickListener(new View.OnClickListener() {

							@Override
							public void onClick(View v) {
								// Log.i(this.getClass().getSimpleName(), "Clicked on " + result);

								if (v.getTag() == null) {
									RelativeLayout recordDetails = new RelativeLayout(context, null, R.style.leaderboardText);
									recordDetails.setBackgroundResource(backgroundResource);
									recordDetails.setLayoutParams(recordLayout);

									TextView processor = new TextView(context);
									processor.setText(Html.fromHtml("<strong>CPU:</strong> " + result.getHardware()
											+ (result.getCpuFreq() != null && result.getCpuFreq() > 0 ? " @ " + result.getCpuFreq() + " MHz" : "")));
									//									processor.setPadding(dpToPx(2), dpToPx(0), dpToPx(2), dpToPx(2));
									processor.setTextAppearance(context, R.style.leaderboardTextSmall);
									processor.setEllipsize(TruncateAt.END);
									processor.setLayoutParams(relativeTo(RelativeLayout.ALIGN_PARENT_LEFT, 35, 0, 5, 0));
									processor.setId(23 * row);

									recordDetails.addView(processor);
									
									TextView device = null;
									if (result.getDevice() != null) {
										device = new TextView(context);
										device.setText(Html.fromHtml("<strong>Device:</strong> " + result.getDevice()));
										//										osBuild.setPadding(dpToPx(2), dpToPx(0), dpToPx(2), dpToPx(2));
										device.setTextAppearance(context, R.style.leaderboardTextSmall);
										device.setLayoutParams(relativeToParent(RelativeLayout.BELOW,
												recordDetails.getChildAt(recordDetails.getChildCount() - 1).getId(), 35, 0, 5, 0));
										device.setId(90000 + 29 * row);
										recordDetails.addView(device);
									}

									TextView osBuild = null;
									if (result.getOsBuild() != null) {
										osBuild = new TextView(context);
										osBuild.setText(Html.fromHtml("<strong>Android:</strong> " + result.getOsBuild()));
										//										osBuild.setPadding(dpToPx(2), dpToPx(0), dpToPx(2), dpToPx(2));
										osBuild.setTextAppearance(context, R.style.leaderboardTextSmall);
										osBuild.setLayoutParams(relativeToParent(RelativeLayout.BELOW,
												recordDetails.getChildAt(recordDetails.getChildCount() - 1).getId(), 35, 0, 5, 0));
										osBuild.setId(29 * row);
										recordDetails.addView(osBuild);
									}

									TextView kernel = null;
									if (result.getKernel() != null) {
										kernel = new TextView(context);
										kernel.setText(Html.fromHtml("<strong>Kernel:</strong> " + result.getKernel()));
										//										kernel.setPadding(dpToPx(2), dpToPx(0), dpToPx(2), dpToPx(2));
										kernel.setTextAppearance(context, R.style.leaderboardTextSmall);
										kernel.setMaxLines(1);
										kernel.setEllipsize(TruncateAt.END);
										kernel.setLayoutParams(relativeToParent(RelativeLayout.BELOW,
												recordDetails.getChildAt(recordDetails.getChildCount() - 1).getId(), 35, 0, 5, 0));
										kernel.setId(31 * row);
										recordDetails.addView(kernel);
									}

									TextView description = null;
									if (result.getDescription() != null) {
										description = new TextView(context);
										description.setText("\"" + result.getDescription() + "\"");
										// description.setPadding(dpToPx(2), dpToPx(0), dpToPx(2), dpToPx(2));
										description.setTextAppearance(context, R.style.leaderboardTextSmall);
										description.setMaxLines(1);
										description.setEllipsize(TruncateAt.END);
										description.setLayoutParams(relativeToParent(RelativeLayout.BELOW,
												recordDetails.getChildAt(recordDetails.getChildCount() - 1).getId(), 35, 0, 5, 0));
										description.setId(37 * row);
										recordDetails.addView(description);
									}

									LinearLayout actions = new LinearLayout(context);

									RelativeLayout.LayoutParams actionLayout = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
											RelativeLayout.LayoutParams.WRAP_CONTENT);
									actionLayout.addRule(RelativeLayout.BELOW, recordDetails.getChildAt(recordDetails.getChildCount() - 1).getId());
									actionLayout.leftMargin = dpToPx(10);
									actionLayout.rightMargin = dpToPx(5);
									actionLayout.bottomMargin = dpToPx(2);
									actions.setLayoutParams(actionLayout);
									actions.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);

									final TextSwitcher comments = new TextSwitcher(context);
									comments.setFactory(textSwitcherViewFactory);
									comments.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
											LinearLayout.LayoutParams.WRAP_CONTENT));
									comments.setPadding(dpToPx(5), dpToPx(0), dpToPx(5), dpToPx(0));
									comments.setText(String.valueOf(result.getComments()));
									actions.addView(comments);

									final ImageView chatIcon = new ImageView(context);
									chatIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_chat));
									chatIcon.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
											LinearLayout.LayoutParams.WRAP_CONTENT));
									if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
										// only for gingerbread and newer versions
										chatIcon.setBackground(getResources().getDrawable(R.drawable.container_icon));
									}
									chatIcon.setOnClickListener(new View.OnClickListener() {
										@Override
										public void onClick(View v) {
											// Log.i(this.getClass().getSimpleName(), "click me");
											v.setAlpha(0.4f);
											CommentDialog commentDialog = new CommentDialog();
											commentDialog.setTarget("submission");
											commentDialog.setTargetId(String.valueOf(result.getId()));
											commentDialog.setChatIcon(chatIcon);
											commentDialog.setTextSwitcher(comments);
											commentDialog.setCommentObserver(TabFragmentCompare.this);
											commentDialog.show(getFragmentManager(), "comments");
										}
									});
									actions.addView(chatIcon);

									final TextSwitcher likes = new TextSwitcher(context);
									likes.setFactory(textSwitcherViewFactory);
									likes.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
											LinearLayout.LayoutParams.WRAP_CONTENT));
									likes.setPadding(dpToPx(5), dpToPx(0), dpToPx(0), dpToPx(0));
									likes.setText(String.valueOf(result.getLikes()));
									actions.addView(likes);

									ImageView likeIcon = new ImageView(context);
									likeIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_favorite));
									likeIcon.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
											LinearLayout.LayoutParams.WRAP_CONTENT));
									if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
										// only for gingerbread and newer versions
										likeIcon.setBackground(getResources().getDrawable(R.drawable.container_icon));
									}
									likeIcon.setOnClickListener(new View.OnClickListener() {
										@Override
										public void onClick(View icon) {
											// Log.i(this.getClass().getSimpleName(), "like me");
											icon.setAlpha(0.4f);
											new SubmitVoteTask(String.valueOf(result.getId()), "submission", icon, likes, TabFragmentCompare.this)
													.execute((Void) null);
										}
									});
									actions.addView(likeIcon);

									ImageView infoIcon = new ImageView(context);
									infoIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_about));
									infoIcon.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
											LinearLayout.LayoutParams.WRAP_CONTENT));
									if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
										// only for gingerbread and newer versions
										infoIcon.setBackground(getResources().getDrawable(R.drawable.container_icon));
									}
									infoIcon.setOnClickListener(new View.OnClickListener() {
										@Override
										public void onClick(View icon) {
											// Log.i(this.getClass().getSimpleName(), "info me");
											Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(BenchService.SERVER_MOBILE + "/submission/"
													+ result.getId()));
											MainActivity.activity.startActivity(intent);
										}
									});
									actions.addView(infoIcon);

									recordDetails.addView(actions);

									v.setTag(true);

									RotateAnimation anim = new RotateAnimation(0f, 180f, 30f, 30f);
									anim.setInterpolator(new DecelerateInterpolator());
									anim.setRepeatCount(0);
									anim.setDuration(150);
									anim.setFillAfter(true);
									expandIcon.startAnimation(anim);

									compareView.addView(recordDetails, compareView.indexOfChild(v) + 1);
								} else {
									RotateAnimation anim = new RotateAnimation(180f, 0f, 30f, 30f);
									anim.setInterpolator(new AccelerateInterpolator());
									anim.setRepeatCount(0);
									anim.setDuration(150);
									anim.setFillAfter(true);
									expandIcon.startAnimation(anim);

									v.setTag(null);
									compareView.removeViewAt(compareView.indexOfChild(v) + 1);
								}

							}
						});

						recordSummary.addView(rankBox);
						recordSummary.addView(avatar);
						recordSummary.addView(user);
						recordSummary.addView(expandIcon);
						recordSummary.addView(score);

						compareView.addView(recordSummary);

						rank++;
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

	@Override
	public void notifyVoteFailed(final View view) {
		MainActivity.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				view.setAlpha(0.2f);
			}
		});
	}

	@Override
	public void notifyCommentFailed(final View view) {
		MainActivity.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				view.setAlpha(0.2f);
				view.setClickable(false);
			}
		});
	}

	@Override
	public void notifyCommentSucceeded(final View view, final TextSwitcher textSwitcher) {
		MainActivity.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				view.setAlpha(1.0f);
				TextView currentView = (TextView) textSwitcher.getCurrentView();
				int currentComments = Integer.parseInt(String.valueOf(currentView.getText()));
				textSwitcher.setText(String.valueOf(++currentComments));
			}
		});
	}

	@Override
	public void notifyVoteSucceeded(final View view, final TextSwitcher textSwitcher) {
		MainActivity.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				view.setAlpha(1.0f);
				view.setClickable(false);
				TextView currentView = (TextView) textSwitcher.getCurrentView();
				int currentVotes = Integer.parseInt(String.valueOf(currentView.getText()));
				textSwitcher.setText(String.valueOf(++currentVotes));
			}
		});
	}

	public static TabFragmentCompare getInstance() {
		return tabFragment;
	}

	public void prepareView() {
		showLeaderboardIfDeviceInfoPresent();
		loadActiveRanking();
	}

}
