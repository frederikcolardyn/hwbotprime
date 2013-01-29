package org.hwbot.bench.ui.swing;

import javax.swing.JTextArea;

import org.hwbot.bench.ui.Output;

public class JTextAreaConsole implements Output {

	private final JTextArea jTextArea;

	public JTextAreaConsole(JTextArea jTextArea) {
		this.jTextArea = jTextArea;
	}

	public void write(String string, boolean newline) {
		if (newline) {
			jTextArea.append(string + "\n");
		} else {
			jTextArea.append(string);
		}
	}

	public void write(String string) {
		write(string, true);
	}

}
