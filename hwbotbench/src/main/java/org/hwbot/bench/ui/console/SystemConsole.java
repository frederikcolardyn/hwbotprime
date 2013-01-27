package org.hwbot.bench.ui.console;

import org.hwbot.bench.ui.Output;

public class SystemConsole implements Output {

	public void write(String string, boolean newline) {
		if (newline) {
			System.out.println(string);
		} else {
			System.out.print(string);
		}
	}

	public void write(String string) {
		write(string, true);
	}

}
