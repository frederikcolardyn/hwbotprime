package org.hwbot.prime.util;

import org.hwbot.prime.MainActivity;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class UIUtil {

	public static void setTextInView(View rootView, int id, String text) {
		View findViewById = rootView.findViewById(id);
		ViewGroup cpuRow = (ViewGroup) findViewById;
		TextView cpuView = new TextView(MainActivity.activity);
		cpuRow.removeAllViews();
		cpuRow.addView(cpuView);
		cpuView.setText(text);
	}

}
