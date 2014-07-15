package org.hwbot.opengl.tasks;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hwbot.opengl.MainActivity;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

/**
 * Represents an asynchronous login/registration task used to authenticate
 * the user.
 */
public class ImageLoaderTask extends AsyncTask<ImageView, Void, Void> {

	private Drawable defaultDrawable;
	private static Map<String, Drawable> cache = new LinkedHashMap<String, Drawable>(20) {
		private static final long serialVersionUID = 1L;

		// @Override
		protected boolean removeEldestEntry(Entry<String, Drawable> entry) {
			return size() > 20;
		}
	};

	public ImageLoaderTask(Drawable defaultDrawable) {
		this.defaultDrawable = defaultDrawable;
	}

	// @Override
	protected Void doInBackground(ImageView... imageViews) {
		for (int i = 0; i < imageViews.length; i++) {
			final ImageView imageView = imageViews[i];
			try {
				Drawable thumb_d;
				String url = (String) imageView.getTag();
				if (cache.get(url) != null) {
					thumb_d = cache.get(url);
					// Log.i(this.getClass().getSimpleName(), "Cached image: " + url);
				} else {
					URL thumb_u = new URL(url);
					// Log.i(this.getClass().getSimpleName(), "Loading image: " + url);
					HttpURLConnection.setFollowRedirects(true);
					HttpURLConnection conn = (HttpURLConnection) thumb_u.openConnection();
					conn.setInstanceFollowRedirects(true);
					thumb_d = Drawable.createFromStream(thumb_u.openStream(), "src");
					if (thumb_d == null && conn.getResponseCode() == 302) {
						// in case of facebook, it's a redirect
						String newUrl = conn.getHeaderField("Location");
						// Log.i(this.getClass().getSimpleName(), "Image redirect: " + newUrl);
						thumb_u = new URL((String) newUrl);
						thumb_d = Drawable.createFromStream(thumb_u.openStream(), "src");
					}
					cache.put(url, thumb_d);
				}

				final Drawable thumb = thumb_d;

				MainActivity.getActivity().runOnUiThread(new Runnable() {
					// @Override
					public void run() {
						if (thumb != null) {
							imageView.setImageDrawable(thumb);
						} else {
							imageView.setImageDrawable(defaultDrawable);
						}
					}
				});
			} catch (UnknownHostException e) {
				Log.w(this.getClass().getSimpleName(), "Failed to load image: " + e.getMessage());
				MainActivity.activity.showNetworkPopupOnce();
			} catch (Exception e) {
				Log.e(this.getClass().getSimpleName(), "Failed to load image: " + e.getMessage());
				e.printStackTrace();
			}
		}
		return null;
	}
}