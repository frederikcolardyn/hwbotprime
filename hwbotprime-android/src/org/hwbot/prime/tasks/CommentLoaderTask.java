package org.hwbot.prime.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import org.hwbot.api.generic.dto.JsonpApiResponse;
import org.hwbot.prime.MainActivity;
import org.hwbot.prime.R;
import org.hwbot.prime.service.BenchService;
import org.hwbot.prime.util.AndroidUtil;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;

public class CommentLoaderTask extends AsyncTask<Void, Void, Void> {

	private LinearLayout commentBox;
	private String tag;

	public CommentLoaderTask(LinearLayout commentBox, String tag) {
		this.commentBox = commentBox;
		this.tag = tag;
	}

	@Override
	protected Void doInBackground(Void... params) {
		BufferedReader in = null;
		try {
			URL commentsApiUrl = new URL(BenchService.SERVER + "/external/v3?type=comments&target=android&params=" + tag);
			in = new BufferedReader(new InputStreamReader(commentsApiUrl.openStream()));
			Log.i(this.getClass().getSimpleName(), "Loading comments from url: " + commentsApiUrl);
			final JsonpApiResponse response = new Gson().fromJson(in, JsonpApiResponse.class);

			if (response != null) {
				MainActivity.getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Context context = commentBox.getContext();
						commentBox.removeAllViews();
						List<Object> list = response.getList();
						Log.i(this.getClass().getSimpleName(), "Loaded " + list.size() + " comments.");
						int row = 1;
						if (list.size() == 0) {
							TextView noComments = new TextView(context);
							noComments.setText(R.string.no_comments);
							LinearLayout.LayoutParams authorLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
									LinearLayout.LayoutParams.WRAP_CONTENT);
							authorLayout.topMargin = AndroidUtil.dpToPx(20);
							authorLayout.bottomMargin = AndroidUtil.dpToPx(20);
							authorLayout.gravity = Gravity.CENTER;
							noComments.setLayoutParams(authorLayout);
							noComments.setGravity(Gravity.CENTER);
							commentBox.addView(noComments);
						}

						for (Object object : list) {
							@SuppressWarnings("unchecked")
							Map<String, String> comment = (Map<String, String>) object;
							Log.i(this.getClass().getSimpleName(), comment.toString());
							RelativeLayout linearLayout = new RelativeLayout(context);
							RelativeLayout.LayoutParams authorLayout = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
									LinearLayout.LayoutParams.WRAP_CONTENT);
							RelativeLayout.LayoutParams contentLayout = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
									LinearLayout.LayoutParams.WRAP_CONTENT);
							RelativeLayout.LayoutParams timeAgoLayout = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
									LinearLayout.LayoutParams.WRAP_CONTENT);
							RelativeLayout.LayoutParams recordLayout = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
									LinearLayout.LayoutParams.MATCH_PARENT);

							linearLayout.setBackgroundResource(R.drawable.container_comment);
							linearLayout.setLayoutParams(recordLayout);

							TextView author = new TextView(context);
							author.setText(Html.fromHtml("<b>" + comment.get("author") + ":</b>"));
							authorLayout.topMargin = AndroidUtil.dpToPx(5);
							authorLayout.leftMargin = AndroidUtil.dpToPx(10);
							authorLayout.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
							author.setLayoutParams(authorLayout);
							author.setId(71 * row);

							TextView content = new TextView(context);
							content.setText(comment.get("content"));
							contentLayout.leftMargin = AndroidUtil.dpToPx(30);
							contentLayout.topMargin = AndroidUtil.dpToPx(5);
							contentLayout.bottomMargin = AndroidUtil.dpToPx(10);
							contentLayout.addRule(RelativeLayout.BELOW, author.getId());
							content.setLayoutParams(contentLayout);

							TextView timeAgo = new TextView(context);
							timeAgo.setText(Html.fromHtml("<i>" + comment.get("timeAgo") + "</i>"));
							timeAgoLayout.topMargin = AndroidUtil.dpToPx(8);
							timeAgoLayout.rightMargin = AndroidUtil.dpToPx(5);
							timeAgoLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
							timeAgo.setTextSize(10f);
							timeAgo.setLayoutParams(timeAgoLayout);

							linearLayout.addView(author);
							linearLayout.addView(content);
							linearLayout.addView(timeAgo);

							commentBox.addView(linearLayout);
							row++;
						}
					}
				});
			}

		} catch (UnknownHostException e) {
			Log.w(this.getClass().getSimpleName(), "No network access: " + e.getMessage());
		} catch (Exception e) {
			Log.e(this.getClass().getSimpleName(), "Error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

}
