package org.hwbot.bench.service;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.Key;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.hwbot.bench.api.Benchmark;
import org.hwbot.bench.remote.BasicResponseStatusHandler;
import org.hwbot.bench.security.EncryptionModule;
import org.hwbot.bench.ui.BenchUI;
import org.hwbot.bench.ui.Output;
import org.hwbot.bench.ui.ProgressBar;
import org.hwbot.bench.ui.console.BenchConsole;
import org.hwbot.bench.ui.console.SystemConsole;
import org.hwbot.bench.ui.console.SystemProgressBar;
import org.hwbot.bench.ui.swing.BenchSwingUI;
import org.hwbot.bench.ui.swing.JProgressBarProgressBar;
import org.hwbot.bench.ui.swing.JTextAreaConsole;

public abstract class BenchService {

	public String processor;
	public long score;
	private String version = this.getClass().getPackage().getImplementationVersion();
	protected int availableProcessors;

	protected BenchUI benchUI;
	protected ProgressBar progressBar;
	protected Output output;

	protected byte[] key;
	protected byte[] iv;
        // processor speed in Mhz
	protected Float processorSpeed;

	public BenchService() {
		try {
			ServiceLoader<EncryptionModule> encryptionLoader = ServiceLoader.load(EncryptionModule.class);
			for (EncryptionModule encryptionModule : encryptionLoader) {
				try {
					key = Hex.decodeHex(encryptionModule.getKey());
					iv = Hex.decodeHex(encryptionModule.getIv());
				} catch (DecoderException e) {
					throw new RuntimeException(e);
				}
			}
			if (key == null) {
				System.out.println("No encryption modules loaded.");
			}
		} catch (Exception e) {
			// no encryption
			System.err.println("No encryption module found.");
			e.printStackTrace();
		}
	}

	public void initialize(boolean ui) throws IOException {
		HardwareService hardwareService = new HardwareService();
		processor = hardwareService.getProcessorInfo();
		availableProcessors = Runtime.getRuntime().availableProcessors();

		if (ui) {
			System.out.println("Using UI mode.");
			try {
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			} catch (Exception e) {
				try {
					UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
				} catch (Exception e1) {
				}
			}

			JFrame frame = new JFrame("HWBOT Bench");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			BenchSwingUI benchUI = new BenchSwingUI(this, getTitle(), getSubtitle());
			// Get the size of the screen
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			// Determine the new location of the window
			int w = frame.getSize().width;
			int h = frame.getSize().height;
			int x = (dim.width - w) / 2;
			int y = (dim.height - h) / 2;
			// Move the window
			frame.setLocation(x, y);
			frame.setContentPane(benchUI);
			frame.pack();
			frame.setVisible(true);

			JProgressBar progressbar = benchUI.getjProgressBar1();
			progressbar.setMaximum(100);

			output = new JTextAreaConsole(benchUI.getConsole());
			progressBar = new JProgressBarProgressBar(progressbar);
			this.benchUI = benchUI;
		} else {
			BenchConsole benchUI = new BenchConsole(this);

			output = new SystemConsole();
			progressBar = new SystemProgressBar(100);
			this.benchUI = benchUI;
		}

		output.write("--------- HWBOT BENCH " + version + " ----------\n");
		output.write("Processor detected:\n" + processor);
		output.write("Estimating speed... ", false);
		processorSpeed = hardwareService.getProcessorSpeed();
		output.write(getProcessorFrequency() + " Mhz");

		benchUI.waitForCommands();
	}

	public abstract String getSubtitle();

	public abstract String getTitle();

	public String getProcessorFrequency() {
		String freq;
		if (processorSpeed == null) {
			freq = "n/a";
		} else {
			NumberFormat instance = NumberFormat.getInstance(Locale.ENGLISH);
			instance.setMaximumFractionDigits(2);
			freq = instance.format(processorSpeed);
		}
		return freq;
	}

	public void benchmark() {
		ExecutorService exec = Executors.newFixedThreadPool(1, new ThreadFactory() {
			public Thread newThread(Runnable runnable) {
				Thread thread = new Thread(runnable);
				thread.setName("benchmark");
				thread.setDaemon(false);
				return thread;
			}
		});
		exec.submit(instantiateBenchmark());
	}

	public abstract Benchmark instantiateBenchmark();

	public void submit() {
		HttpParams httpParameters = new BasicHttpParams();
		int timeoutConnection = 20;
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection * 1000);
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutConnection * 1000);
		HttpClient httpclient = new DefaultHttpClient(httpParameters);
		try {
			// Create a response handler
			BasicResponseStatusHandler responseHandler = new BasicResponseStatusHandler();
			HttpPost req = new HttpPost("http://hwbot.org/submit/api?client=" + getClient() + "&clientVersion=" + getClientVersion());
			req.addHeader("Accept", "application/xml");
			MultipartEntity mpEntity = new MultipartEntity();
			byte[] bytes = getDataFile();
			// mpEntity.addPart("data", new StringBody(xml));
			mpEntity.addPart("data", new ByteArrayBody(bytes, "data"));
			req.setEntity(mpEntity);

			String response = httpclient.execute(req, responseHandler);
			String status = StringUtils.substringBetween(response, "<status>", "</status>");

			if ("success".equals(status)) {
				String url = StringUtils.substringBetween(response, "<url>", "</url>");
				try {
					Desktop.getDesktop().browse(new URI(url));
				} catch (Exception e) {
					output.write("Failed to open your browser, please open " + url + " to view your submission.");
				}
			} else {
				output.write("Failed to submit score. Status was: " + status);
				output.write(response);
			}

		} catch (HttpHostConnectException e) {
			output.write("Failed to connect to HWBOT server! Are you connected to the internet?");
			e.printStackTrace();
		} catch (Exception e) {
			output.write("Error communicating with online service. If this issue persists, please contact HWBOT crew. Error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			httpclient.getConnectionManager().shutdown();
		}
	}

	public byte[] getDataFile() {
		byte[] bytes = null;
		try {
			String xml = createXml(getClient(), version, processor, processorSpeed, score);
			if (key != null) {
				bytes = encrypt("AES/CBC/PKCS5Padding", xml.getBytes("utf8"));
			} else {
				bytes = xml.getBytes("utf8");
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return bytes;
	}

	protected abstract String getClient();

	protected abstract String getClientVersion();

	protected static String createXml(String client, String version, String processor, Float processorSpeed, long score) {
		StringBuffer xml = new StringBuffer();
		xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		xml.append("<submission xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://hwbot.org/submit/api\">");
		xml.append("<application>");
		xml.append("<name>" + client + "</name>");
		xml.append("<version>" + version + "</version>");
		xml.append("</application>");
		xml.append("<score><points>" + (score / 1000f) + "</points></score>");
		xml.append("<hardware>");
		xml.append("<processor>");
		xml.append("<name><![CDATA[" + processor + "]]></name>");
		if (processorSpeed != null) {
			xml.append("<coreClock><![CDATA[" + processorSpeed.intValue() + "]]></coreClock>");
		}
		xml.append("</processor>");
		xml.append("</hardware>");

		xml.append("<software>");
		xml.append("<os>");
		xml.append("<family>" + HardwareService.OS + "</family>");
		xml.append("</os>");
		xml.append("</software>");

		xml.append("<metadata name=\"java_environment\">");
		Map<String, String> env = System.getenv();
		for (String envName : env.keySet()) {
			xml.append(String.format("%s=%s%n\n", envName, env.get(envName)));

		}
		xml.append("</metadata>");

		xml.append("</submission>");
		return xml.toString();
	}

	/**
	 * Encrypt an array of bytes. Befor encrypting, you have to set the cipher to use, key and iv (if applicable)
	 * 
	 * @param data
	 * @return the encrypted data
	 */
	public byte[] encrypt(String cipher, byte[] data) {
		try {
			String[] config = cipher.split("/");
			Key encryptKey = new SecretKeySpec(key, config[0]);
			Cipher c = Cipher.getInstance(cipher);

			if ("CBC".equals(config[1])) {
				c.init(Cipher.ENCRYPT_MODE, encryptKey, new IvParameterSpec(iv));
			} else if ("ECB".equals(config[1])) {
				c.init(Cipher.ENCRYPT_MODE, encryptKey);
			}
			return c.doFinal(data);
		} catch (Exception e) {
			throw new RuntimeException("Failed to encrypt: " + e);
		}
	}

	public int getAvailableProcessors() {
		return availableProcessors;
	}

	public void setAvailableProcessors(int availableProcessors) {
		this.availableProcessors = availableProcessors;
	}

	public abstract String formatScore(Long score);
}
