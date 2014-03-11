package org.hwbot.prime.util;

import org.hwbot.prime.MainActivity;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AndroidUtil {

	public static void setTextInView(View rootView, int id, String text) {
		View findViewById = rootView.findViewById(id);
		if (findViewById instanceof ViewGroup) {
			ViewGroup viewGroup = (ViewGroup) findViewById;
			TextView view = new TextView(MainActivity.activity);
			viewGroup.removeAllViews();
			viewGroup.addView(view);
			view.setText(text);
		} else if (findViewById instanceof TextView) {
			TextView textView = (TextView) findViewById;
			textView.setText(text);
		}
	}
	
}
