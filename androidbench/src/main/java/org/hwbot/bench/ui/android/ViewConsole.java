package org.hwbot.bench.ui.android;

import org.hwbot.bench.ui.Output;

import android.widget.TextView;

public class ViewConsole implements Output {

	private final TextView console;

	public ViewConsole(TextView console) {
		this.console = console;
	}

	public void write(String string) {
		write(string, true);
	}

	public void write(String string, boolean newline) {
		if (newline) {
			this.console.append(string);
			this.console.append("\n");
		} else {
			this.console.append(string);
		}
	}

}
