package org.hwbot.bench.service;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.hwbot.bench.Benchmark;
import org.hwbot.bench.BenchmarkConfiguration;
import org.hwbot.bench.PrimeBenchmark;
import org.hwbot.bench.model.Hardware;
import org.hwbot.bench.model.Response;
import org.hwbot.bench.prime.HardwareService;
import org.hwbot.bench.prime.Log;
import org.hwbot.bench.prime.ProgressBar;
import org.hwbot.bench.remote.BasicResponseStatusHandler;
import org.hwbot.bench.security.EncryptionModule;
import org.hwbot.bench.ui.BenchUI;
import org.hwbot.bench.ui.Output;
import org.hwbot.bench.ui.console.BenchConsole;
import org.hwbot.bench.ui.console.SystemConsole;
import org.hwbot.bench.ui.console.SystemProgressBar;
import org.hwbot.bench.ui.swing.BenchSwingUI;
import org.hwbot.bench.ui.swing.JProgressBarProgressBar;
import org.hwbot.bench.util.DataServiceXml;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import static org.hwbot.bench.PrimeBenchmark.*;

public class BenchService implements Runnable {

    protected static final String version = "1.0.2";
    public static boolean headless;
    protected static EncryptionModule encryptionModule;

    public Number score;
    protected BenchUI benchUI;
    protected ProgressBar progressBar;
    protected Output output;
    private String server = System.getProperty("server", "https://hwbot.org");
    //    private String server = System.getProperty("server", "https://uat.hwbot.org");
    private Benchmark benchmark;
    private ScheduledFuture<?> processorFrequencyMonitorScheduler;
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private String checksum, checksumbase;
    private char[] checksumChars;
    private Hardware hardware;
    private ExecutorService exec;
    private BenchPhase currentPhase = BenchPhase.ready;

    public BenchService() {
        try {
            ServiceLoader<EncryptionModule> encryptionLoader = ServiceLoader.load(EncryptionModule.class);
            System.out.println("EncryptionLoader: " + encryptionLoader);
            for (EncryptionModule encryptionModule : encryptionLoader) {
                BenchService.encryptionModule = encryptionModule;
                System.out.println("Using encryption " + BenchService.encryptionModule);
            }
        } catch (Exception e) {
            // no encryption
            Log.error("No encryption module found.");
            // e.printStackTrace();
        }
    }

    public static String getProcessorFrequency(Float processorSpeed) {
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

    public static String toSHA1(byte[] string) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException();
        }
        return Hex.encodeHexString(md.digest(string));
    }

    public void initialize(boolean ui, String outputFile) throws Exception {
        // Class.forName(className)
        HardwareService hardwareService = HardwareServiceFactory.getInstance();
        hardware = hardwareService.gatherHardwareInfo();
        int availableProcessors = Runtime.getRuntime().availableProcessors();

        BenchService.headless = !ui;

        if (ui) {
            Log.info("Using UI mode.");
            try {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception noPreJdk9Laf) {
                try {
                    UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
                } catch (Exception noPostJdk9Laf) {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                }
            }

            JFrame frame = new JFrame("HWBOT Prime " + version);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setIconImage(new javax.swing.ImageIcon(getClass().getResource("/robot-small.png")).getImage());

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

            // output = new JTextAreaConsole(benchUI.getConsole());

            if (hardware.getProcessor() != null) {
                benchUI.getProcessor().setText(hardware.getProcessor().getName());
                benchUI.getFrequency().setText(getProcessorFrequency(hardware.getProcessor().getCoreClock()));
            } else {
                benchUI.getProcessor().setText("unknown");
            }
            benchUI.getThreads().setText(String.valueOf(availableProcessors));
            benchUI.getMemory().setText(String.valueOf(hardware.getMemory().getTotalSize()));

            output = new SystemConsole();

            frame.pack();

            progressBar = new JProgressBarProgressBar(progressbar);
            this.benchUI = benchUI;
        } else {
            BenchConsole benchUI = new BenchConsole(this, outputFile);

            output = new SystemConsole();
            progressBar = new SystemProgressBar(100);
            this.benchUI = benchUI;
            Float processorTemperature = hardwareService.getProcessorTemperature();

            output.write("--------- HWBOT Prime " + version + " - 10sec 'quick mode' ----------\n");
            output.write("Processor detected:\n" + hardware.getProcessor().getName());
            output.write("Estimating speed... ", false);
            output.write(((availableProcessors > 1) ? availableProcessors + "x " : "") + getProcessorFrequency(hardware.getProcessor().getCoreClock()) + "MHz"
                    + (processorTemperature != null ? " @ " + processorTemperature + " C" : ""));
            if (hardware.getMemory().getTotalSize() > 0) {
                output.write(hardware.getMemory().getTotalSize() + "MB memory");
            }
        }

        benchUI.waitForCommands();
    }

    public String getSubtitle() {
        return "Multithreaded Prime Bench - ARM/x86 - Win/Mac/Linux";
    }

    public String getTitle() {
        return "HWBOT Prime Benchmark";
    }

    public void benchmark(boolean quick) {
        exec = Executors.newFixedThreadPool(2, new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setPriority(Thread.MAX_PRIORITY);
                thread.setName("benchmark");
                thread.setDaemon(false);
                return thread;
            }
        });
        if (scheduledThreadPoolExecutor != null) {
            scheduledThreadPoolExecutor.shutdownNow();
            processorFrequencyMonitorScheduler.cancel(true);
        }
        benchmark = instantiateBenchmark(quick);
        new Thread(this).start();
    }

    @Override
    public void run() {
        Future<Number> submit = exec.submit(benchmark);
        this.currentPhase = BenchPhase.inprogress;
        try {
            score = submit.get();
            checksum = toSHA1((checksumbase + score).getBytes("UTF8"));
            checksumChars = checksum.toCharArray();
            this.currentPhase = BenchPhase.finished;
            benchUI.notifyBenchmarkFinished(benchmark);
        } catch (Exception e) {
            benchUI.notifyError(e.getMessage());
        }
    }

    public Benchmark instantiateBenchmark(boolean quick) {
        BenchmarkConfiguration configuration = new BenchmarkConfiguration();
        configuration.setValue(TIME_SPAN, quick ? QUICK_TEST_MS : STABILITY_TEST_MS);
        configuration.setValue(PrimeBenchmark.SILENT, false);
        return new PrimeBenchmark(configuration, Runtime.getRuntime().availableProcessors(), this.progressBar);
    }

    public void submit() {
        HttpParams httpParameters = new BasicHttpParams();
        int timeoutConnection = 20;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection * 1000);
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutConnection * 1000);
        HttpClient httpclient = new DefaultHttpClient(httpParameters);
        try {
            // Create a response handler
            byte[] bytes = getDataFile();

            BasicResponseStatusHandler responseHandler = new BasicResponseStatusHandler();
            String uri = server + "/submit/api?client=" + URLEncoder.encode(benchmark.getClient(), "ISO-8859-1") + "&clientVersion=" + getClientVersion();
            Log.info("URI: " + uri);
            HttpPost req = new HttpPost(uri);
            req.addHeader("Accept", "application/xml");
            MultipartEntity mpEntity = new MultipartEntity();
            mpEntity.addPart("data", new ByteArrayBody(bytes, "data"));
            req.setEntity(mpEntity);

            HttpResponse execute = httpclient.execute(req);
            System.out.println("status: " + execute.getStatusLine());
            String raw = responseHandler.handleResponse(execute);
            System.out.println(raw);
            Response response = DataServiceXml.parseResponse(raw);

            if ("success".equals(response.getStatus())) {
                String url = response.getUrl();
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (Exception e) {
                    output.write("Failed to open your browser, please open " + url + " to view your submission.");
                }
            } else if ("error".equals(response.getStatus())) {
                this.benchUI.notifyError(response.getMessage());
            } else {
                output.write("Failed to submit score. Status was: " + response);
                output.write(response.getMessage());
            }

        } catch (HttpHostConnectException e) {
            e.printStackTrace();
            output.write("Failed to connect to HWBOT server! Are you connected to the internet?");
        } catch (Exception e) {
            e.printStackTrace();
            output.write("Error communicating with online service. If this issue persists, please contact HWBOT crew.");
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
    }

    public byte[] getDataFile() throws UnsupportedEncodingException {
        byte[] bytes;
        String xml = DataServiceXml.createXml(benchmark.getClient(), version, hardware, this.formatScore(benchmark.getScore()), !headless,
                BenchService.encryptionModule);
        if (encryptionModule != null) {
            bytes = BenchService.encryptionModule.encrypt(xml.getBytes("utf8"), null);
        } else {
            bytes = xml.getBytes("utf8");
        }
        return bytes;
    }

    protected String getClientVersion() {
        return version;
    }

    public String formatScore(Number score) {
        if (QUICK_TEST_MS.equals(benchmark.getConfig().getValue(TIME_SPAN))) {
            return String.format(Locale.ENGLISH, "%.2f", score);
        } else if (STABILITY_TEST_MS.equals(benchmark.getConfig().getValue(TIME_SPAN))) {
            return String.valueOf(score);
        } else {
            return String.valueOf(score);
        }

    }

    public void saveToFile(File file) {
        if (file.isDirectory()) {
            file = new File(file, "HWBOT Prime - " + new SimpleDateFormat("dd-MM-yyyy HH'h'mm'm'") + ".hwbot");
        }
        byte[] dataFile;
        try {
            dataFile = getDataFile();
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(dataFile);
            fileOutputStream.close();
        } catch (Exception e) {
            Log.error("Failed to save the file: " + file.getAbsolutePath() + ". Reason: " + e.getMessage());
        }
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public void setProgressBar(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    public BenchPhase getCurrentPhase() {
        return this.currentPhase;
    }

    public enum BenchPhase {
        ready, warmup, inprogress, finished
    }

}
