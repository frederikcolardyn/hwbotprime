package org.hwbot.prime.util;

import static org.hwbot.prime.util.AndroidUtil.dpToPx;

import org.hwbot.prime.MainActivity;

import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AndroidUtil {

	public static int dpToPx(int padding_in_dp) {
		if (MainActivity.activity != null) {
			final float scale = MainActivity.activity.getResources().getDisplayMetrics().density;
			int padding_in_px = (int) (padding_in_dp * scale + 0.5f);
			return padding_in_px;
		} else {
			// Log.w(AndroidUtil.class.getSimpleName(), "can not get display metrics");
			return padding_in_dp;
		}
	}

	public static void setTextInView(View rootView, int id, Object text) {
		View findViewById = rootView.findViewById(id);
		if (findViewById instanceof ViewGroup) {
			ViewGroup viewGroup = (ViewGroup) findViewById;
			TextView view = new TextView(MainActivity.activity);
			viewGroup.removeAllViews();
			viewGroup.addView(view);
			if (text instanceof Spanned) {
				view.setText((Spanned) text);
			} else {
				view.setText(String.valueOf(text));
			}
		} else if (findViewById instanceof TextView) {
			TextView view = (TextView) findViewById;
			if (text instanceof Spanned) {
				view.setText((Spanned) text);
			} else {
				view.setText(String.valueOf(text));
			}
		}
	}
	
	public static RelativeLayout.LayoutParams relativeTo(int align, Integer... margins) {
		RelativeLayout.LayoutParams rankBoxLayout = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		rankBoxLayout.addRule(align);
		if (margins != null && margins.length == 4) {
			rankBoxLayout.leftMargin = dpToPx(margins[0]);
			rankBoxLayout.topMargin = dpToPx(margins[1]);
			rankBoxLayout.rightMargin = dpToPx(margins[2]);
			rankBoxLayout.bottomMargin = dpToPx(margins[3]);
		}
		return rankBoxLayout;
	}

	public static RelativeLayout.LayoutParams relativeToParent(int align, int parentId, Integer... margins) {
		RelativeLayout.LayoutParams rankBoxLayout = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		rankBoxLayout.addRule(align, parentId);
		if (margins != null && margins.length == 4) {
			rankBoxLayout.leftMargin = dpToPx(margins[0]);
			rankBoxLayout.topMargin = dpToPx(margins[1]);
			rankBoxLayout.rightMargin = dpToPx(margins[2]);
			rankBoxLayout.bottomMargin = dpToPx(margins[3]);
		}
		return rankBoxLayout;
	}

}
