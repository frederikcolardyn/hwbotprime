package org.hwbot.bench;

import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.io.IOException;

import org.apache.commons.codec.DecoderException;
import org.hwbot.bench.service.BenchService;

public class BootStrap {

	public static void main(String[] args) throws IOException, DecoderException {
		BenchService bench = new GpuBenchService();

		if (GraphicsEnvironment.isHeadless() || "true".equals(System.getProperty("java.awt.headless")) || (args.length >= 1 && args[0].equals("console"))) {
			bench.initialize(false, null);
		} else {
			try {
				bench.initialize(true, null);
			} catch (HeadlessException e) {
				bench.initialize(false, null);
			}
		}
	}

}
