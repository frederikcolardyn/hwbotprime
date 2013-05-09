package org.hwbot.bench.service;

import java.io.IOException;
import java.security.Key;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.hwbot.bench.api.Benchmark;
import org.hwbot.bench.ui.BenchUI;
import org.hwbot.bench.ui.Output;
import org.hwbot.bench.ui.ProgressBar;
import org.hwbot.bench.ui.android.AndroidProgressBar;
import org.hwbot.bench.ui.android.BenchConsole;
import org.hwbot.bench.ui.android.ViewConsole;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

public abstract class BenchService {

    public String processor;
    public long score;
    private String version = this.getClass().getPackage().getImplementationVersion();
    protected int availableProcessors;

    protected BenchUI benchUI;
    protected ProgressBar progressBar;
    protected Output output;
    protected Button compareButton;

    protected byte[] key;
    protected byte[] iv;
    protected Float processorSpeed;

    public void initialize(TextView console, android.widget.ProgressBar progressbar, Button compareButton) throws IOException {
        this.compareButton = compareButton;
        processor = new HardwareService().getProcessorInfo();
        availableProcessors = Runtime.getRuntime().availableProcessors();

        BenchConsole benchUI = new BenchConsole(this);

        output = new ViewConsole(console);
        this.progressBar = new AndroidProgressBar(progressbar, 100);
        this.benchUI = benchUI;

        output.write("Processor detected:\n" + processor);
        output.write("Estimated speed: ", false);
        processorSpeed = new HardwareService().getProcessorSpeed();
//        output.write((new JavaSysMon().numCpus() + "x ") + getProcessorFrequency() + " Ghz");

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

    public void benchmark() {
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
        Future<Long> submit = exec.submit(instantiateBenchmark());
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

    public abstract Benchmark instantiateBenchmark();

    public Intent submit() {
        Log.i(this.getClass().getName(), "submitting");
        ExecutorService exec = Executors.newFixedThreadPool(1);
        SubmitWorker worker = new SubmitWorker(createXml(version, processor, processorSpeed, score));
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

    private static String createXml(String version, String processor, Float processorSpeed, long score) {
        StringBuffer xml = new StringBuffer();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        xml.append("<submission xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://hwbot.org/submit/api\">");
        xml.append("<application>");
        xml.append("<name>BenchBot</name>");
        xml.append("<version>" + version + "</version>");
        xml.append("</application>");
        xml.append("<score><points>" + (score / 1000f) + "</points></score>");
        xml.append("<hardware>");
        xml.append("<processor>");
        xml.append("<name><![CDATA[" + processor + "]]></name>");
        if (processorSpeed != null) {
            xml.append("<coreClock><![CDATA[" + (processorSpeed.intValue()) + "]]></coreClock>");
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

    public abstract String formatScore(Long score);
}
