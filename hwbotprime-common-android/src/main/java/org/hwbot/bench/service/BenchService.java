package org.hwbot.bench.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.hwbot.bench.BenchmarkConfiguration;
import org.hwbot.bench.PrimeBenchmark;
import org.hwbot.bench.model.Hardware;
import org.hwbot.bench.prime.ProgressBar;
import org.hwbot.bench.ui.BenchUI;
import org.hwbot.bench.ui.Output;
import org.hwbot.bench.ui.android.AndroidProgressBar;
import org.hwbot.bench.ui.android.BenchConsole;
import org.hwbot.bench.ui.android.ViewConsole;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

public class BenchService implements Runnable {

    public String processor;
    public String deviceName, socName, deviceVendor;
    public Number score;
    private String version = this.getClass().getPackage().getImplementationVersion();
    protected int availableProcessors;

    protected BenchUI benchUI;
    protected ProgressBar progressBar;
    protected Output output;
    protected Button compareButton;

    private String checksum, checksumbase;
    private char[] checksumChars;

    protected byte[] key;
    protected byte[] iv;
    protected Float processorSpeed;

    public void initialize(TextView console, android.widget.ProgressBar progressbar, Button compareButton) throws IOException {
        this.compareButton = compareButton;
        AndroidHardwareService hardwareService = new AndroidHardwareService();
        Hardware gatherHardwareInfo = hardwareService.gatherHardwareInfo();
        Log.i("hw info", gatherHardwareInfo.toString());
        processor = hardwareService.getProcessorInfo();
        deviceName = hardwareService.getDeviceName();
        socName = hardwareService.getSocName();
        deviceVendor = hardwareService.getDeviceVendor();
        availableProcessors = Runtime.getRuntime().availableProcessors();

        BenchConsole benchUI = new BenchConsole(this);

        output = new ViewConsole(console);
        this.progressBar = new AndroidProgressBar(progressbar, 100);
        this.benchUI = benchUI;

        output.write("Processor detected:\n" + processor);
        output.write("Temperature:\n" + hardwareService.getProcessorTemperature() + "C");
        output.write("Estimated freq: " + hardwareService.getProcessorSpeed() + "MHz ", false);
        // processorSpeed = hardwareService.getProcessorSpeed();
        // output.write((new JavaSysMon().numCpus() + "x ") + getProcessorFrequency() + " Ghz");

        benchUI.waitForCommands();
    }

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

    public enum BenchPhase {
        ready, warmup, inprogress, finished
    }

    private ExecutorService exec;
    private BenchPhase currentPhase = BenchPhase.ready;
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private ScheduledFuture<?> processorFrequencyMonitorScheduler;
    private PrimeBenchmark benchmark;

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
        this.currentPhase = BenchPhase.inprogress;
        // wait for outout
        try {
            score = submit.get();
            checksum = toSHA1((checksumbase + score).getBytes("UTF8"));
            checksumChars = checksum.toCharArray();
            this.currentPhase = BenchPhase.finished;
            benchUI.notifyBenchmarkFinished(score);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void benchmarkold() {
        Log.i(this.getClass().getName(), "Benchmarking...");
        ExecutorService exec = Executors.newFixedThreadPool(1, new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setName("benchmark");
                thread.setDaemon(false);
                thread.setPriority(Thread.MAX_PRIORITY);
                return thread;
            }
        });
        Future<Number> submit = exec.submit(instantiateBenchmark());
        //
        // while(!submit.isDone()){
        // try {
        // Thread.sleep(100);
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // }
        // }
        //
        // try {
        // Long score = submit.get();
        // benchUI.notifyBenchmarkFinished(score);
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // } catch (ExecutionException e) {
        // e.printStackTrace();
        // }
    }

    public PrimeBenchmark instantiateBenchmark() {
        BenchmarkConfiguration configuration = new BenchmarkConfiguration();
        configuration.setValue(PrimeBenchmark.TIME_SPAN, TimeUnit.SECONDS.toMillis(10));
        configuration.setValue(PrimeBenchmark.SILENT, false);
        // return new PrimeBenchmark(this, super.benchUI, super.availableProcessors, super.progressBar, super.compareButton);
        return new PrimeBenchmark(configuration, Runtime.getRuntime().availableProcessors(), this.progressBar);
    }

    public Intent submit() {
        Log.i(this.getClass().getName(), "submitting");
        ExecutorService exec = Executors.newFixedThreadPool(1);
        SubmitWorker worker = new SubmitWorker(createXml(version, processor, processorSpeed, score, deviceName, socName, deviceVendor));
        Future<String> submit = exec.submit(worker);

        try {
            String response = submit.get();
            Log.i(this.getClass().getName(), response);
            if (response != null) {
                return new Intent(Intent.ACTION_VIEW, Uri.parse(response));
            } else {
                this.output.write("Failed to submit to HWBOT. Sorry!");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String createXml(String version, String processor, Float processorSpeed, Number score, String deviceName, String deviceSoc,
            String deviceBrand) {
        StringBuffer xml = new StringBuffer();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        xml.append("<submission xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://hwbot.org/submit/api\">");
        xml.append("<application>");
        xml.append("<name>HWBOT Prime</name>");
        xml.append("<version>" + version + "</version>");
        xml.append("</application>");
        xml.append("<score><points>" + score + "</points></score>");
        xml.append("<hardware>");
        xml.append("<device>");
        if (deviceName != null) {
            xml.append("<name><![CDATA[" + deviceName + "]]></name>");
        }
        if (deviceSoc != null) {
            xml.append("<soc><![CDATA[" + deviceSoc + "]]></soc>");
        }
        if (deviceBrand != null) {
            xml.append("<vendor><![CDATA[" + deviceBrand + "]]></vendor>");
        }
        xml.append("</device>");
        xml.append("<processor>");
        xml.append("<name><![CDATA[" + processor + "]]></name>");
        if (processorSpeed != null) {
            xml.append("<coreClock><![CDATA[" + (processorSpeed.intValue()) + "]]></coreClock>");
        }
        xml.append("</processor>");
        xml.append("</hardware>");

        xml.append("<software>");
        xml.append("<os>");
        xml.append("<family>" + AndroidHardwareService.OS + "</family>");
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

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public void setProgressBar(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    public Output getOutput() {
        return output;
    }

    public void setOutput(Output output) {
        this.output = output;
    }

    public String formatScore(Number score) {
        return String.format(Locale.ENGLISH, "%.2f", score);
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
}
