package org.hwbot.bench.service;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
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
import org.hwbot.bench.model.Response;
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

public abstract class BenchService implements Runnable {

    public String processor;
    public Number score;
    protected String version = this.getClass().getPackage().getImplementationVersion();
    protected int availableProcessors, availableProcessorThreads, memoryInMB;

    protected BenchUI benchUI;
    protected ProgressBar progressBar;
    protected Output output;

    protected byte[] key;
    protected byte[] iv;
    // processor speed in Mhz
    protected Float processorSpeed;
    private String server = System.getProperty("server", "http://uat.hwbot.org");
    public static boolean headless;
    protected static EncryptionModule encryptionModule;
    private Benchmark benchmark;
    private ScheduledFuture<?> processorFrequencyMonitorScheduler;
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private boolean processorSpeedReliable;
    private String outputFile;
    private HardwareService hardwareService;
    private String checksum = "";
    private char[] checksumChars;

    public BenchService() {
        try {
            ServiceLoader<EncryptionModule> encryptionLoader = ServiceLoader.load(EncryptionModule.class);
            for (EncryptionModule encryptionModule : encryptionLoader) {
                BenchService.encryptionModule = encryptionModule;
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

    public void initialize(boolean ui, String outputFile) throws IOException {
        this.outputFile = outputFile;
        hardwareService = new HardwareService();
        processor = hardwareService.getProcessorInfo();
        availableProcessors = Runtime.getRuntime().availableProcessors();
        checksum += processor + availableProcessors;

        processorSpeed = hardwareService.getEstimatedProcessorSpeed();
        processorSpeedReliable = hardwareService.isProcessorSpeedReliable();

        availableProcessorThreads = hardwareService.getNumberOfProcessorCores();
        memoryInMB = (int) (hardwareService.getMemorySize() / 1024 / 1024);

        BenchService.headless = !ui;

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

            JFrame frame = new JFrame("HWBOT Prime " + version);
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

            // output = new JTextAreaConsole(benchUI.getConsole());

            benchUI.getProcessor().setText(processor);
            benchUI.getFrequency().setText(getProcessorFrequency(processorSpeed));
            benchUI.getThreads().setText("" + availableProcessorThreads);
            benchUI.getMemory().setText("" + memoryInMB);

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

            output.write("--------- HWBOT Prime " + version + " ----------\n");
            output.write("Processor detected:\n" + processor);
            output.write("Estimating speed... ", false);
            output.write(((availableProcessorThreads > 1) ? availableProcessorThreads + "x " : "") + getProcessorFrequency(processorSpeed) + "MHz"
                    + (processorTemperature != null ? " @ " + processorTemperature + " C" : ""));
            if (memoryInMB > 0) {
                output.write(memoryInMB + " MB memory");
            }
        }

        benchUI.waitForCommands();
    }

    public abstract String getSubtitle();

    public abstract String getTitle();

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

    private ExecutorService exec;

    public void benchmark() {
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
        benchmark = instantiateBenchmark();
        new Thread(this).start();
    }

    public void run() {
        Future<Number> submit = exec.submit(benchmark);
        // wait for outout
        try {
            score = submit.get();
            checksum += score;
            checksum = toSHA1(checksum.getBytes("UTF8"));
            checksumChars = checksum.toCharArray();
            benchUI.notifyBenchmarkFinished(benchmark);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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
            byte[] bytes = getDataFile();
            // checksum can only be used in case of online submission, not saved files
            // String checksum = toSHA1(bytes);

            BasicResponseStatusHandler responseHandler = new BasicResponseStatusHandler();
            HttpPost req = new HttpPost(server + "/submit/api?client=" + URLEncoder.encode(benchmark.getClient(), "ISO-8859-1") + "&clientVersion="
                    + getClientVersion());
            req.addHeader("Accept", "application/xml");
            MultipartEntity mpEntity = new MultipartEntity();
            mpEntity.addPart("data", new ByteArrayBody(bytes, "data"));
            req.setEntity(mpEntity);

            Response response = DataServiceXml.parseResponse(httpclient.execute(req, responseHandler));

            if ("success".equals(response.getStatus())) {
                String url = response.getUrl();
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (Exception e) {
                    output.write("Failed to open your browser, please open " + url + " to view your submission.");
                }
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
        byte[] bytes = null;
        // processor speed ignored, not reliable enough...
        verifyMemoryUnaltered();
        String xml = DataServiceXml.createXml(benchmark.getClient(), version, processor, (processorSpeedReliable ? processorSpeed : null), memoryInMB,
                this.formatScore(benchmark.getScore()), !headless, BenchService.encryptionModule);
        if (key != null) {
            bytes = encrypt("AES/CBC/PKCS5Padding", xml.getBytes("utf8"));
        } else {
            bytes = xml.getBytes("utf8");
        }
        return bytes;
    }

    private void verifyMemoryUnaltered() {
        try {
            checksum = processor + availableProcessors + score;
            checksum = toSHA1(checksum.getBytes("UTF8"));
            if (!ArrayUtils.isEquals(checksumChars, checksum.toCharArray())) {
                throw new SecurityException("Memory has been altered. Bad hacker! Shoo!");
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException();
        }
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

    protected abstract String getClientVersion();

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

    public abstract String formatScore(Number score);

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
            System.err.println("Failed to save the file: " + file.getAbsolutePath() + ". Reason: " + e.getMessage());
        }
    }

    public HardwareService getHardwareService() {
        return this.hardwareService;
    }

}
