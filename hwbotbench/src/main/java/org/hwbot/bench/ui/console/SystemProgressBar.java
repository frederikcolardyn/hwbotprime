package org.hwbot.bench.ui.console;

import org.hwbot.bench.ui.ProgressBar;

public class SystemProgressBar implements ProgressBar {

	private int iterations;

	public SystemProgressBar(int iterations) {
		this.iterations = iterations;
	}

	public void setValue(int i) {
		System.out.print((iterations--) + "...");
	}

}
