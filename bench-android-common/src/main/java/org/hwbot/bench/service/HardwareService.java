package org.hwbot.bench.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.jar.JarEntry;

import android.os.Build;
import android.util.Log;

public class HardwareService {

	public static String OS = System.getProperty("os.name").toLowerCase();

	public String getProcessorInfo() {
		String processor;

		String meminfo = execRuntime(new String[] { "cat", "/proc/meminfo" });
		Log.i(HardwareService.class.getName(), "meminfo: " + meminfo);

		String version = execRuntime(new String[] { "cat", "/proc/version" });
		Log.i(HardwareService.class.getName(), "version: " + version);

		String execRuntime = execRuntime(new String[] { "cat", "/proc/cpuinfo" });
		Log.i(HardwareService.class.getName(), execRuntime);

		// 12-25 01:13:57.669: I/org.hwbot.bench.service.HardwareService(1600): Processor : ARMv7 Processor rev 2 (v7l)
		int start = execRuntime.indexOf("Processor");
		if (start >= 0) {
			processor = execRuntime.substring(start);
			int end = processor.indexOf("\n");
			if (end >= 0) {
				processor = execRuntime.substring(0, end);
			}
			if (processor.indexOf(":") >= 0) {
				processor = processor.substring(processor.indexOf(":") + 2);
			}
		} else {
			processor = "unkown";
		}

		Log.i(this.getClass().getName(), "BOARD: " + Build.BOARD);
		Log.i(this.getClass().getName(), "BRAND: " + Build.BRAND);
		Log.i(this.getClass().getName(), "CPU_ABI: " + Build.CPU_ABI);
		Log.i(this.getClass().getName(), "CPU_ABI2: " + Build.CPU_ABI2);
		Log.i(this.getClass().getName(), "DEVICE: " + Build.DEVICE);
		Log.i(this.getClass().getName(), "DISPLAY: " + Build.DISPLAY);
		Log.i(this.getClass().getName(), "FINGERPRINT: " + Build.FINGERPRINT);
		Log.i(this.getClass().getName(), "HARDWARE: " + Build.HARDWARE);
		Log.i(this.getClass().getName(), "TYPE: " + Build.SERIAL);
		Log.i(this.getClass().getName(), "TYPE: " + Build.TYPE);
		Log.i(this.getClass().getName(), "TAGS: " + Build.TAGS);

		return Build.MODEL + " - cpu: " + Build.HARDWARE + " - arch: " + processor;
	}

	public static void extractFile(String fileToExtract, File targetFile, boolean permissions) throws IOException {

		if (targetFile.exists()) {
			// ok!
			// System.out.println("Using CPU executable: " + getCpuIdExecutable().getAbsolutePath());
		} else {
			// System.out.println("preparing cpu info for " + OS);
			String path = HardwareService.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			String decodedPath;
			decodedPath = URLDecoder.decode(path, "UTF-8");
			// System.out.println("jar " + decodedPath);
			java.util.jar.JarFile jar = new java.util.jar.JarFile(decodedPath);
			Enumeration<JarEntry> entries = jar.entries();
			boolean installed = false;
			while (entries.hasMoreElements()) {
				java.util.jar.JarEntry file = (java.util.jar.JarEntry) entries.nextElement();
				java.io.File f = targetFile;
				if (file.getName().equals(fileToExtract)) {
					if (file.isDirectory()) { // if its a directory, create it
						f.mkdir();
						continue;
					}
					java.io.InputStream is = jar.getInputStream(file); // get the input stream
					java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
					while (is.available() > 0) { // write contents of 'is' to 'fos'
						fos.write(is.read());
					}
					fos.close();
					is.close();
					// System.out.println("cpuid executable written to " + f);
					System.out.println("Prepared: " + targetFile.getAbsolutePath());
					if (permissions) {
						Runtime.getRuntime().exec("chmod +x " + f.getAbsolutePath());
					}
					installed = true;
					break;
				} else {
					System.out.println("skipping " + file + " <> " + fileToExtract);
				}
			}
			if (!installed) {
				System.err.println("Sorry, we can not run the bechmark on this platform. Please inform HWBOT crew this does not work on " + OS);
				throw new RuntimeException("OS not supported!");
			}
		}
	}

	public Float getProcessorSpeed() {
		return cpufreq(4, 15);
	}

	private static Float cpufreq(float dpt, float dpc) {
		try {
			/* run cycles for Integer.MAX_VALUE times */
			long maxValue = 100000000 / 2;
			long l1 = System.currentTimeMillis();
			for (long i = 0; i < maxValue; i++)
				;
			long l2 = System.currentTimeMillis();
			// System.out.println("l1: "+l1);
			// System.out.println("l2: "+l2);

			/* compute the cycles per second */
			long cps = (maxValue / (l2 - l1)) / 1000 * 100;

			/* output the computation result */
			// System.out.println("Directives per tick: " + dpt);
			// System.out.println("Directives per cycle: " + dpc);
			Log.i(HardwareService.class.getName(), "Cycles per second: " + cps);
			Log.i(HardwareService.class.getName(), "Freq: " + (cps * dpc / dpt));
			float mhz = (((cps * dpc / dpt) / 10) / 1000);
			return mhz;
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}

	public File getCpuIdExecutable() {
		return new java.io.File(System.getProperty("java.io.tmpdir") + java.io.File.separator + "cpuid");
	}

	public static boolean isWindows() {
		return (OS.indexOf("win") >= 0);
	}

	public static boolean isMac() {
		return (OS.indexOf("mac") >= 0);
	}

	public static boolean isUnix() {
		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);
	}

	public String execRuntime(String[] strings) {

		Process proc = null;
		int inBuffer, errBuffer;
		int result = 0;
		StringBuffer outputReport = new StringBuffer();
		StringBuffer errorBuffer = new StringBuffer();

		try {
			proc = Runtime.getRuntime().exec(strings);
		} catch (IOException e) {
			return "";
		}
		try {
			result = proc.waitFor();
		} catch (InterruptedException e) {
			return "";
		}
		if (proc != null && null != proc.getInputStream()) {
			InputStream is = proc.getInputStream();
			InputStream es = proc.getErrorStream();
			OutputStream os = proc.getOutputStream();

			try {
				while ((inBuffer = is.read()) != -1) {
					outputReport.append((char) inBuffer);
				}

				while ((errBuffer = es.read()) != -1) {
					errorBuffer.append((char) errBuffer);
				}

			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("error using cpuid");
				return "";
			}
			try {
				is.close();
				is = null;
				es.close();
				es = null;
				os.close();
				os = null;
			} catch (IOException e) {
				e.printStackTrace();
				return "";
			}

			// try {
			// proc.destroy();
			// } catch (Exception e) {
			//
			// }
			proc = null;
		}

		if (errorBuffer.length() > 0) {
			System.err.println("could not finish execution because of error(s).");
			System.err.println("*** Error : " + errorBuffer.toString());
			return "";
		}

		return outputReport.toString();
	}

}
