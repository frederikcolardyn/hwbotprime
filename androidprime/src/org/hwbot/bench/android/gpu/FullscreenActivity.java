package org.hwbot.bench.android.gpu;

import org.hwbot.bench.android.gpu.util.SystemUiHider;
import org.hwbot.bench.service.BenchService;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e. status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class FullscreenActivity extends Activity {

	protected BenchService bench = new PrimeBenchService();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_fullscreen);

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.comparebutton).setOnClickListener(compareListener);
		findViewById(R.id.startbutton).setOnClickListener(launchBenchmarkListener);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the system UI. This is to prevent the jarring behavior of controls going away while
	 * interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			return false;
		}
	};

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the system UI. This is to prevent the jarring behavior of controls going away while
	 * interacting with activity UI.
	 */
	View.OnClickListener launchBenchmarkListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			try {
				Log.i(this.getClass().getName(), "Starting benchmark");
				TextView text = (TextView) findViewById(R.id.console);
				text.setText("");
				ProgressBar progressbar = (ProgressBar) findViewById(R.id.progressbar);
				Button comparebutton = (Button) findViewById(R.id.comparebutton);
				bench.initialize(text, progressbar, comparebutton);
				comparebutton.setEnabled(true);
				// bench.benchmark();
			} catch (Exception e) {
				Log.e(this.getClass().getName(), "error launching bench: " + e.getMessage());
			}
		}
	};

	View.OnClickListener compareListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			try {
				Log.i(this.getClass().getName(), "Comparing...");
				Intent browser = bench.submit();
				if (browser != null) {
					startActivity(browser);
				}
			} catch (Exception e) {
				Log.e(this.getClass().getName(), "error comparing: " + e.getMessage());
			}
		}
	};

}
