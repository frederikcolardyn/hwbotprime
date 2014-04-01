package org.hwbot.prime.tasks;

import static org.hwbot.prime.util.AndroidUtil.dpToPx;

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
import org.hwbot.prime.CommentDialog;
import org.hwbot.prime.MainActivity;
import org.hwbot.prime.R;
import org.hwbot.prime.TabFragmentAccount;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.SecurityService;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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

	@Override
	protected List<NotificationDTO> doInBackground(String... params) {
		BufferedReader reader = null;
		try {
			PersistentLoginDTO credentials = SecurityService.getInstance().getCredentials();
			if (credentials != null && credentials.getUserId() != null) {
				URL url = new URL(BenchService.SERVER + "/api/notification?userId=" + credentials.getUserId()
						+ (params.length > 0 && params[0] != null ? "&from=" + params[0] : ""));
				Log.i(this.getClass().getSimpleName(), "Loading notifications from: " + url);
				reader = new BufferedReader(new InputStreamReader(url.openStream()));
				NotificationsDTO notificationsDto = new Gson().fromJson(reader, NotificationsDTO.class);
				Log.i(this.getClass().getSimpleName(), "Loaded " + notificationsDto.getList().size() + " notifications.");
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
	@Override
	protected void onPostExecute(final List<NotificationDTO> notifications) {
		if (notifications != null && TabFragmentAccount.rootView != null) {

			final ViewFactory textSwitcherViewFactory = new ViewFactory() {
				public View makeView() {
					TextView myText = new TextView(MainActivity.activity, null, R.style.leaderboardTextAction);
					myText.setTextAppearance(MainActivity.activity.getApplicationContext(), R.style.leaderboardTextAction);
					return myText;
				}
			};

			ViewGroup notificationContainer = (ViewGroup) TabFragmentAccount.rootView.findViewById(R.id.notifications);
			Context context = TabFragmentAccount.rootView.getContext();
			Resources resources = TabFragmentAccount.rootView.getResources();
			// int notificationTextColor = context.getResources().getColor(R.color.notification_text);

			int row = 1;

			for (final NotificationDTO notificationDTO : notifications) {
				final int backgroundResource = row % 2 == 0 ? R.drawable.container_leaderboard_light : R.drawable.container_leaderboard_dark;

				RelativeLayout relativeLayout = new RelativeLayout(context, null, R.style.leaderboardText);
				LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT);
				// layoutParams.bottomMargin = dpToPx(6);
				relativeLayout.setBackgroundResource(backgroundResource);
				relativeLayout.setLayoutParams(layoutParams);

				TextView user = new TextView(context);
				user.setText(notificationDTO.getUser());
				user.setPadding(dpToPx(49), dpToPx(5), dpToPx(5), dpToPx(5));
				user.setTextAppearance(context, R.style.leaderboardText);

				LinearLayout contentLayout = new LinearLayout(context, null, R.style.leaderboardText);
				LinearLayout.LayoutParams contentLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT);
				contentLayout.setOrientation(LinearLayout.VERTICAL);
				contentLayout.setPadding(dpToPx(49), dpToPx(30), dpToPx(5), dpToPx(5));
				contentLayout.setLayoutParams(contentLayoutParams);
				contentLayout.setId(1000000 + (59 * row));

				TextView message = new TextView(context);
				message.setText(notificationDTO.getMessage());
				// message.setPadding(dpToPx(49), dpToPx(30), dpToPx(5), dpToPx(5));
				message.setTextAppearance(context, R.style.leaderboardTextSmall);

				if (notificationDTO.getImage() != null) {
					try {
						// cache drawables?
						String url = BenchService.SERVER + notificationDTO.getImage();
						ImageView imageView = new ImageView(context);
						imageView.setMaxHeight(dpToPx(50));
						imageView.setMaxWidth(dpToPx(50));
						imageView.setAdjustViewBounds(true);
						imageView.setMinimumHeight(dpToPx(50));
						imageView.setMinimumWidth(dpToPx(50));
						imageView.setScaleType(ScaleType.FIT_XY);
						imageView.setTag(url);
						imageView.setPadding(dpToPx(5), dpToPx(5), dpToPx(5), dpToPx(5));
						imageView.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(50), dpToPx(50)));
						relativeLayout.addView(imageView);
						new ImageLoaderTask(MainActivity.getActivity().getAnonymousIcon()).execute(imageView);
					} catch (Exception e) {
						Log.w(this.getClass().getSimpleName(), "Failed to load image: " + e.getMessage());
						e.printStackTrace();
					}
				} else {
					// unkown icon?
				}

				contentLayout.addView(message);

				// points change
				//				LinearLayout pointChangesLayout = new LinearLayout(context, null, R.style.leaderboardText);
				//				pointChangesLayout
				//						.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
				for (NotificationDTO pointChange : notificationDTO.getPointChanges()) {
					TextView view = new TextView(context);
					view.setText(pointChange.getMessage());
					view.setCompoundDrawablesWithIntrinsicBounds(resources.getDrawable(R.drawable.ic_action_about), null, null, null);
					// view.setPadding(dpToPx(55), dpToPx(5), dpToPx(5), dpToPx(5));
					view.setTextAppearance(context, R.style.leaderboardTextTiny);
					contentLayout.addView(view);
				}

				// rank change
				//				LinearLayout rankChangesLayout = new LinearLayout(context, null, R.style.leaderboardText);
				//				rankChangesLayout
				//						.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
				for (NotificationDTO pointChange : notificationDTO.getRankChanges()) {
					TextView view = new TextView(context);
					view.setText(pointChange.getMessage());
					view.setCompoundDrawablesWithIntrinsicBounds(resources.getDrawable(R.drawable.ic_action_about), null, null, null);
					// view.setPadding(dpToPx(55), dpToPx(5), dpToPx(5), dpToPx(5));
					view.setTextAppearance(context, R.style.leaderboardTextTiny);
					contentLayout.addView(view);
				}

				// actions
				LinearLayout actions = new LinearLayout(context);
				//				actionLayout.leftMargin = dpToPx(10);
				//				actionLayout.rightMargin = dpToPx(5);
				//				actionLayout.bottomMargin = dpToPx(2);
				actions.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
				actions.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);

				final TextSwitcher comments = new TextSwitcher(context);
				comments.setFactory(textSwitcherViewFactory);
				comments.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
				comments.setPadding(dpToPx(5), dpToPx(0), dpToPx(5), dpToPx(0));
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
					@Override
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
				likes.setPadding(dpToPx(5), dpToPx(0), dpToPx(0), dpToPx(0));
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
					@Override
					public void onClick(View icon) {
						icon.setAlpha(0.4f);
						new SubmitVoteTask(notificationDTO.getId(), "notification", icon, likes, tabFragmentAccount).execute((Void) null);
					}
				});
				actions.addView(likeIcon);

				//				relativeLayout.setClickable(true);
				//				relativeLayout.setOnClickListener(new OnClickListener() {
				//					@Override
				//					public void onClick(View v) {
				//						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(BenchService.SERVER_MOBILE + notificationDTO.getLink()));
				//						MainActivity.activity.startActivity(intent);
				//					}
				//				});

				relativeLayout.addView(user);
				relativeLayout.addView(contentLayout);
				relativeLayout.addView(actions);
				notificationContainer.addView(relativeLayout);

				row++;
			}
		} else {
			Log.e(this.getClass().getSimpleName(), "Can not show notifications: " + tabFragmentAccount.getView());
		}
	}
}