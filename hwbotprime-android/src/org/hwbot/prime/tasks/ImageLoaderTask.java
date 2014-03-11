package org.hwbot.prime.tasks;

import java.net.URL;
import java.net.UnknownHostException;

import org.hwbot.prime.MainActivity;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

/**
 * Represents an asynchronous login/registration task used to authenticate
 * the user.
 */
public class ImageLoaderTask extends AsyncTask<ImageView, Void, Void> {

	@Override
	protected Void doInBackground(ImageView... imageViews) {
		for (int i = 0; i < imageViews.length; i++) {
			final ImageView imageView = imageViews[i];
			try {
				URL thumb_u = new URL((String) imageView.getTag());
				final Drawable thumb_d = Drawable.createFromStream(thumb_u.openStream(), "src");
				MainActivity.activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						imageView.setImageDrawable(thumb_d);
					}
				});
			} catch (UnknownHostException e) {
				MainActivity.activity.showNetworkPopupOnce();
			} catch (Exception e) {
				Log.e(this.getClass().getSimpleName(), "Failed to load image: " + e.getMessage());
				e.printStackTrace();
			}
		}
		return null;
	}

}