package org.hwbot.prime.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.hwbot.prime.MainActivity;
import org.hwbot.prime.R;
import org.hwbot.prime.TabFragmentAccount;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.service.SecurityService;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

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
		JsonReader reader = null;
		try {
			URL url = new URL(BenchService.SERVER + "/api/notification?userId=" + SecurityService.getInstance().getCredentials().getUserId()
					+ (params.length > 0 && params[0] != null ? "&from=" + params[0] : ""));
			Log.i(this.getClass().getSimpleName(), "Loading notifications from: " + url);
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			reader = new JsonReader(in);
			List<NotificationDTO> notifications = NotificationLoaderTask.readNotifications(reader);
			Log.i(this.getClass().getSimpleName(), "Loaded " + notifications.size() + " notifications.");
			return notifications;
		} catch (UnknownHostException e) {
			MainActivity.activity.showNetworkPopupOnce();
		} catch (Exception e) {
			Log.e(this.getClass().getSimpleName(), "Failed to load notifications: " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private static List<NotificationDTO> readNotifications(JsonReader reader) {
		List<NotificationDTO> notifications = new ArrayList<NotificationDTO>();
		try {
			reader.beginArray();
			while (reader.hasNext()) {
				reader.beginObject();
				NotificationDTO dto = new NotificationDTO();
				while (reader.hasNext()) {
					String name = reader.nextName();
					if (name.equals("message")) {
						dto.setMessage(reader.nextString());
					} else if (name.equals("contestId")) {
						dto.setContestId(reader.nextInt());
					} else if (name.equals("date")) {
						dto.setDate(reader.nextLong());
					} else if (name.equals("id")) {
						dto.setId(reader.nextString());
					} else if (name.equals("link")) {
						dto.setLink(reader.nextString());
					} else if (name.equals("manufacturerId")) {
						dto.setManufacturerId(reader.nextInt());
					} else if (name.equals("resultId")) {
						dto.setResultId(reader.nextInt());
					} else if (name.equals("parentNotificationId")) {
						dto.setParentNotificationId(reader.nextString());
					} else if (name.equals("teamId")) {
						dto.setTeamId(reader.nextInt());
					} else if (name.equals("type")) {
						dto.setType(reader.nextInt());
					} else if (name.equals("userId")) {
						dto.setUserId(reader.nextInt());
					} else if (name.equals("votes")) {
						dto.setVotes(reader.nextInt());
					} else if (name.equals("image")) {
						dto.setImage(reader.nextString());
					} else if (name.equals("user")) {
						dto.setUser(reader.nextString());
					} else if (name.equals("team")) {
						dto.setTeam(reader.nextString());
					} else if (name.equals("contest")) {
						dto.setContest(reader.nextString());
					} else {
						reader.skipValue();
					}
				}
				reader.endObject();
				notifications.add(dto);
			}
			reader.endArray();
		} catch (IOException e) {
			Log.e(LoginTokenTask.class.getName(), "error loading rankings: " + e.getMessage());
			e.printStackTrace();
		}
		return notifications;
	}

	@Override
	protected void onPostExecute(final List<NotificationDTO> notifications) {
		if (notifications != null && TabFragmentAccount.rootView != null) {
			ViewGroup notificationContainer = (ViewGroup) TabFragmentAccount.rootView.findViewById(R.id.notifications);
			Context context = TabFragmentAccount.rootView.getContext();
			// int notificationTextColor = context.getResources().getColor(R.color.notification_text);

			for (final NotificationDTO notificationDTO : notifications) {
				RelativeLayout relativeLayout = new RelativeLayout(context);
				LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
				layoutParams.bottomMargin = 20;
				layoutParams.leftMargin = 1;
				relativeLayout.setLayoutParams(layoutParams);
				relativeLayout.setBackgroundResource(R.drawable.container_dropshadow);

				TextView user = new TextView(context);
				user.setText(notificationDTO.getUser());
				user.setPadding(170, 5, 5, 5);
				user.setTextAppearance(context, R.style.NotificationUser);

				TextView message = new TextView(context);
				message.setText(notificationDTO.getMessage());
				message.setPadding(170, 40, 5, 5);
				message.setTextAppearance(context, R.style.NotificationMessage);

				if (notificationDTO.getImage() != null) {
					try {
						// cache drawables?
						String url = BenchService.SERVER + notificationDTO.getImage();
						Log.i(this.getClass().getSimpleName(), "notificationDTO.getImage(): " + url);
						ImageView imageView = new ImageView(context);
						imageView.setMaxHeight(150);
						imageView.setMaxWidth(150);
						imageView.setMinimumHeight(150);
						imageView.setMinimumWidth(150);
						imageView.setScaleType(ScaleType.FIT_XY);
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
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(BenchService.SERVER_MOBILE + notificationDTO.getLink()));
						MainActivity.activity.startActivity(intent);
					}
				});
				relativeLayout.addView(user);
				relativeLayout.addView(message);
				notificationContainer.addView(relativeLayout);
			}
		} else {
			Log.e(this.getClass().getSimpleName(), "Can not show notifications: " + tabFragmentAccount.getView());
		}
	}

	@Override
	protected void onCancelled() {
	}
}