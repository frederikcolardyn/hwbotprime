package org.hwbot.opengl.tasks;

import static org.hwbot.opengl.util.AndroidUtil.dpToPx;
import static org.hwbot.opengl.util.AndroidUtil.relativeTo;
import static org.hwbot.opengl.util.AndroidUtil.relativeToParent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import org.hwbot.api.bench.dto.NotificationDTO;
import org.hwbot.api.bench.dto.NotificationsDTO;
import org.hwbot.api.bench.dto.PersistentLoginDTO;
import org.hwbot.opengl.CommentDialog;
import org.hwbot.opengl.MainActivity;
import org.hwbot.opengl.R;
import org.hwbot.opengl.TabFragmentAccount;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.SecurityService;
import org.hwbot.prime.tasks.SubmitVoteTask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

import com.google.gson.Gson;

/**
 * Represents an asynchronous login/registration task used to authenticate
 * the user.
 */
public class NotificationLoaderTask extends AsyncTask<String, Void, List<NotificationDTO>> {
	/**
	 * 
	 */
	private final TabFragmentAccount tabFragmentAccount;

	/**
	 * @param tabFragmentAccount
	 */
	public NotificationLoaderTask(TabFragmentAccount tabFragmentAccount) {
		this.tabFragmentAccount = tabFragmentAccount;
	}

	// @Override
	protected List<NotificationDTO> doInBackground(String... params) {
		BufferedReader reader = null;
		try {
			PersistentLoginDTO credentials = SecurityService.getInstance().getCredentials();
			if (credentials != null && credentials.getUserId() != null) {
				URL url = new URL(BenchService.SERVER + "/api/notification?userId=" + credentials.getUserId()
						+ (params.length > 0 && params[0] != null ? "&from=" + params[0] : ""));
				// Log.i(this.getClass().getSimpleName(), "Loading notifications from: " + url);
				reader = new BufferedReader(new InputStreamReader(url.openStream()));
				NotificationsDTO notificationsDto = new Gson().fromJson(reader, NotificationsDTO.class);
				// Log.i(this.getClass().getSimpleName(), "Loaded " + notificationsDto.getList().size() + " notifications.");
				return notificationsDto.getList();
			} else {
				return Collections.emptyList();
			}
		} catch (UnknownHostException e) {
			MainActivity.activity.showNetworkPopupOnce();
		} catch (Exception e) {
			Log.e(this.getClass().getSimpleName(), "Failed to load notifications: " + e.getMessage());
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@SuppressLint("NewApi")
	// @Override
	protected void onPostExecute(final List<NotificationDTO> notifications) {
		if (notifications != null && TabFragmentAccount.rootView != null) {
			final ViewFactory textSwitcherViewFactory = new ViewFactory() {
				public View makeView() {
					TextView myText = new TextView(MainActivity.activity, null, R.style.leaderboardTextAction);
					myText.setTextAppearance(MainActivity.activity.getApplicationContext(), R.style.leaderboardTextAction);
					return myText;
				}
			};

			final ViewGroup notificationContainer = (ViewGroup) TabFragmentAccount.rootView.findViewById(R.id.notifications);
			final Context context = TabFragmentAccount.rootView.getContext();
			final Resources resources = TabFragmentAccount.rootView.getResources();
			// int notificationTextColor = context.getResources().getColor(R.color.notification_text);
			notificationContainer.removeAllViews();

			int row = 1;

			for (final NotificationDTO notificationDTO : notifications) {
				final int backgroundResource = row % 2 == 0 ? R.drawable.container_leaderboard_light : R.drawable.container_leaderboard_dark;

				RelativeLayout contentLayout = new RelativeLayout(context, null, R.style.leaderboardText);
				LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT);
				layoutParams.bottomMargin = dpToPx(0);
				contentLayout.setBackgroundResource(backgroundResource);
				contentLayout.setLayoutParams(layoutParams);

				//				TextView user = new TextView(context);
				//				user.setText(notificationDTO.getUser());
				//				user.setPadding(dpToPx(49), dpToPx(5), dpToPx(5), dpToPx(5));
				//				user.setTextAppearance(context, R.style.leaderboardText);

				//				LinearLayout contentLayout = new LinearLayout(context, null, R.style.leaderboardText);
				//				LinearLayout.LayoutParams contentLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
				//						LinearLayout.LayoutParams.WRAP_CONTENT);
				//				contentLayout.setOrientation(LinearLayout.VERTICAL);
				//				contentLayout.setPadding(dpToPx(49), dpToPx(30), dpToPx(5), dpToPx(5));
				//				contentLayout.setLayoutParams(contentLayoutParams);
				//				contentLayout.setId(1000000 + (59 * row));

				TextView message = new TextView(context);
				message.setText(notificationDTO.getMessage());
				message.setLines(1);
				// message.setWidth(RelativeLayout.LayoutParams.MATCH_PARENT);
				message.setSingleLine();
				message.setEllipsize(TruncateAt.END);
				message.setLayoutParams(relativeTo(RelativeLayout.ALIGN_TOP, 40, 10, 140, 5));
				message.setTextAppearance(context, R.style.leaderboardText);
				message.setId(1000000 + (53 * row));

				ImageView avatar = new ImageView(context);
				RelativeLayout.LayoutParams avatarLayout = new RelativeLayout.LayoutParams(dpToPx(24), dpToPx(24));
				avatarLayout.addRule(RelativeLayout.BELOW);
				avatarLayout.leftMargin = dpToPx(10);
				avatarLayout.topMargin = dpToPx(10);
				avatarLayout.rightMargin = dpToPx(5);
				avatarLayout.bottomMargin = dpToPx(10);
				avatar.setLayoutParams(avatarLayout);
				avatar.setId(1000000 + (17 * row));

				if (notificationDTO.getImage() != null) {
					try {
						String url;
						if (notificationDTO.getImage().startsWith("http")) {
							url = notificationDTO.getImage();
						} else {
							url = BenchService.SERVER + notificationDTO.getImage();
						}
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
				contentLayout.addView(message);
				contentLayout.addView(avatar);

				final ImageView expandIcon = new ImageView(context);
				expandIcon.setImageDrawable(MainActivity.activity.getResources().getDrawable(R.drawable.ic_action_expand));
				// expandIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(40), dpToPx(80)));
				expandIcon.setLayoutParams(relativeTo(RelativeLayout.ALIGN_PARENT_RIGHT, 0, 6, 0, 0));
				expandIcon.setPadding(dpToPx(0), dpToPx(0), dpToPx(0), dpToPx(0));
				expandIcon.setDrawingCacheEnabled(true);
				expandIcon.setId(2000000 + (23 * row));
				contentLayout.addView(expandIcon);

				// actions
				LinearLayout actions = new LinearLayout(context);
				//				actionLayout.leftMargin = dpToPx(10);
				//				actionLayout.rightMargin = dpToPx(5);
				//				actionLayout.bottomMargin = dpToPx(2);
				LayoutParams actionLayout = relativeTo(RelativeLayout.ALIGN_PARENT_RIGHT, 0, 6, 35, 0);
				actions.setLayoutParams(actionLayout);

				final TextSwitcher comments = new TextSwitcher(context);
				comments.setFactory(textSwitcherViewFactory);
				comments.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
				comments.setPadding(dpToPx(5), dpToPx(5), dpToPx(5), dpToPx(0));
				comments.setText(String.valueOf(notificationDTO.getComments().size()));
				actions.addView(comments);

				final ImageView chatIcon = new ImageView(context);
				chatIcon.setImageDrawable(resources.getDrawable(R.drawable.ic_action_chat));
				chatIcon.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
					// only for gingerbread and newer versions
					chatIcon.setBackground(resources.getDrawable(R.drawable.container_icon));
				}
				chatIcon.setOnClickListener(new View.OnClickListener() {
					// @Override
					public void onClick(View v) {
						v.setAlpha(0.4f);
						CommentDialog commentDialog = new CommentDialog();
						commentDialog.setTargetId(notificationDTO.getId());
						commentDialog.setTarget("notification");
						commentDialog.setChatIcon(chatIcon);
						commentDialog.setTextSwitcher(comments);
						commentDialog.setCommentObserver(tabFragmentAccount);
						commentDialog.setNotificationDTOs(notificationDTO.getComments());
						if (tabFragmentAccount.getFragmentManager() != null) {
							commentDialog.show(tabFragmentAccount.getFragmentManager(), "comments");
						}
					}
				});
				actions.addView(chatIcon);

				final TextSwitcher likes = new TextSwitcher(context);
				likes.setFactory(textSwitcherViewFactory);
				likes.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
				likes.setPadding(dpToPx(5), dpToPx(5), dpToPx(0), dpToPx(0));
				likes.setText(String.valueOf(notificationDTO.getVotes()));
				actions.addView(likes);

				ImageView likeIcon = new ImageView(context);
				likeIcon.setImageDrawable(resources.getDrawable(R.drawable.ic_action_favorite));
				likeIcon.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
					// only for gingerbread and newer versions
					likeIcon.setBackground(resources.getDrawable(R.drawable.container_icon));
				}
				likeIcon.setOnClickListener(new View.OnClickListener() {
					// @Override
					public void onClick(View icon) {
						icon.setAlpha(0.4f);
						new SubmitVoteTask(notificationDTO.getId(), "notification", icon, likes, tabFragmentAccount).execute((Void) null);
					}
				});
				actions.addView(likeIcon);

				contentLayout.addView(actions);

				final int subrow = row;

				contentLayout.setOnClickListener(new View.OnClickListener() {

					// @Override
					public void onClick(View v) {
						// Log.i(this.getClass().getSimpleName(), "Clicked on " + notificationDTO);

						if (v.getTag() == null) {

							RelativeLayout contentLayout = new RelativeLayout(context, null, R.style.leaderboardText);
							LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
									LinearLayout.LayoutParams.WRAP_CONTENT);
							layoutParams.bottomMargin = dpToPx(0);
							contentLayout.setBackgroundResource(backgroundResource);
							contentLayout.setLayoutParams(layoutParams);

							TextView message = new TextView(context);
							message.setText(notificationDTO.getMessage());
							// message.setLines(1);
							// message.setWidth(RelativeLayout.LayoutParams.MATCH_PARENT);
							LayoutParams messageLayoutParams = relativeTo(RelativeLayout.ALIGN_TOP, 20, 5, 5, 10);
							messageLayoutParams.rightMargin = dpToPx(35);
							message.setLayoutParams(messageLayoutParams);
							message.setTextAppearance(context, R.style.leaderboardTextSmall);
							message.setId(6000000 + (53 * subrow));

							contentLayout.addView(message);

							LinearLayout childContentLayout = new LinearLayout(context, null, R.style.leaderboardTextSmall);
							RelativeLayout.LayoutParams childLayoutParams = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
									LinearLayout.LayoutParams.WRAP_CONTENT);
							childLayoutParams.topMargin = dpToPx(10);
							childLayoutParams.bottomMargin = dpToPx(5);
							childLayoutParams.rightMargin = dpToPx(5);
							childLayoutParams.addRule(RelativeLayout.BELOW, message.getId());
							childContentLayout.setOrientation(LinearLayout.VERTICAL);
							childContentLayout.setLayoutParams(childLayoutParams);
							childContentLayout.setId(6000000 + (61 * subrow));

							// points change
							for (NotificationDTO pointChange : notificationDTO.getPointChanges()) {
								TextView view = new TextView(context);
								view.setText(Html.fromHtml("<i>" + pointChange.getMessage() + "</i>"));
								// view.setCompoundDrawablesWithIntrinsicBounds(resources.getDrawable(R.drawable.ic_action_about), null, null, null);
								view.setPadding(dpToPx(20), dpToPx(0), dpToPx(5), dpToPx(5));
								view.setTextAppearance(context, R.style.leaderboardTextTiny);
								childContentLayout.addView(view);
							}

							// rank change
							for (NotificationDTO pointChange : notificationDTO.getRankChanges()) {
								TextView view = new TextView(context);
								view.setText(Html.fromHtml("<i>" + pointChange.getMessage() + "</i>"));
								// view.setCompoundDrawablesWithIntrinsicBounds(resources.getDrawable(R.drawable.ic_action_about), null, null, null);
								view.setPadding(dpToPx(20), dpToPx(0), dpToPx(5), dpToPx(5));
								view.setTextAppearance(context, R.style.leaderboardTextTiny);
								childContentLayout.addView(view);
							}

							// comments
							for (NotificationDTO comment : notificationDTO.getComments()) {
								TextView view = new TextView(context);
								// Log.i(this.getClass().getSimpleName(), comment.toString());
								view.setText(Html.fromHtml("<b>" + comment.getUser() + "</b>: " + comment.getMessage()));
								// view.setCompoundDrawablesWithIntrinsicBounds(resources.getDrawable(R.drawable.ic_action_person), null, null, null);
								view.setPadding(dpToPx(20), dpToPx(0), dpToPx(5), dpToPx(5));
								view.setTextAppearance(context, R.style.leaderboardTextTiny);
								childContentLayout.addView(view);
							}

							contentLayout.addView(childContentLayout);

							if (notificationDTO.getLink() != null) {
								ImageView infoIcon = new ImageView(context);
								infoIcon.setImageDrawable(resources.getDrawable(R.drawable.ic_action_about));
								infoIcon.setLayoutParams(relativeToParent(RelativeLayout.ALIGN_PARENT_RIGHT, childContentLayout.getId(), 5, 5, 5, 5));

								if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
									// only for gingerbread and newer versions
									infoIcon.setBackground(resources.getDrawable(R.drawable.container_icon));
								}
								infoIcon.setOnClickListener(new View.OnClickListener() {
									// @Override
									public void onClick(View icon) {
										// Log.i(this.getClass().getSimpleName(), "info me");
										Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(BenchService.SERVER_MOBILE + "/" + notificationDTO.getLink()));
										MainActivity.activity.startActivity(intent);
									}
								});
								contentLayout.addView(infoIcon);
							}

							RotateAnimation anim = new RotateAnimation(0f, 180f, 30f, 30f);
							anim.setInterpolator(new DecelerateInterpolator());
							anim.setRepeatCount(0);
							anim.setDuration(150);
							anim.setFillAfter(true);
							expandIcon.startAnimation(anim);

							v.setTag(true);
							notificationContainer.addView(contentLayout, notificationContainer.indexOfChild(v) + 1);
							// compareView.addView(recordDetails, compareView.indexOfChild(v) + 1);
						} else {
							RotateAnimation anim = new RotateAnimation(180f, 0f, 30f, 30f);
							anim.setInterpolator(new AccelerateInterpolator());
							anim.setRepeatCount(0);
							anim.setDuration(150);
							anim.setFillAfter(true);
							expandIcon.startAnimation(anim);

							v.setTag(null);
							notificationContainer.removeViewAt(notificationContainer.indexOfChild(v) + 1);
							// compareView.removeViewAt(compareView.indexOfChild(v) + 1);
						}

					}
				});

				//				relativeLayout.setClickable(true);
				//				relativeLayout.setOnClickListener(new OnClickListener() {
				//					// @Override
				//					public void onClick(View v) {
				//						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(BenchService.SERVER_MOBILE + notificationDTO.getLink()));
				//						MainActivity.activity.startActivity(intent);
				//					}
				//				});

				//				relativeLayout.addView(user);
				// relativeLayout.addView(contentLayout);
				notificationContainer.addView(contentLayout);

				row++;
			}
		} else {
			Log.e(this.getClass().getSimpleName(), "Can not show notifications: " + tabFragmentAccount.getView());
		}
	}
}