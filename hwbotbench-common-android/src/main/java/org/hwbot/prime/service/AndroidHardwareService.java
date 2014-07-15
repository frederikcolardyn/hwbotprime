package org.hwbot.prime.service;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.hwbot.api.bench.dto.DeviceInfoDTO;
import org.hwbot.bench.model.Device;
import org.hwbot.bench.model.Hardware;
import org.hwbot.bench.model.Memory;
import org.hwbot.bench.model.Processor;
import org.hwbot.bench.prime.HardwareService;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Build;
import android.util.Log;
import android.widget.TextSwitcher;

public class AndroidHardwareService implements HardwareService, SensorEventListener, Runnable {

    protected static AndroidHardwareService service;
    protected DeviceInfoDTO deviceInfo;

    public static String OS = System.getProperty("os.name").toLowerCase();
    private float temperature;
    private TextSwitcher temperatureLabel;
    private ScheduledExecutorService monitorThread;
    private int loadTemperature;
    private int idleTemperature = Integer.MAX_VALUE;
    private int maxProcessorFrequency;

    private AndroidHardwareService() {
    }

    public static AndroidHardwareService getInstance() {
        if (service == null) {
            service = new AndroidHardwareService();
        }
        return service;
    }

    public String getProcessorInfo() {
        String processor;

        Log.i(this.getClass().getName(), "BOARD: " + Build.BOARD);
        Log.i(this.getClass().getName(), "BRAND: " + Build.BRAND);
        Log.i(this.getClass().getName(), "DEVICE: " + Build.DEVICE);
        Log.i(this.getClass().getName(), "DISPLAY: " + Build.DISPLAY);
        Log.i(this.getClass().getName(), "HARDWARE: " + Build.HARDWARE);
        Log.i(this.getClass().getName(), "TYPE: " + Build.TYPE);
        Log.i(this.getClass().getName(), "PRODUCT: " + Build.PRODUCT);
        Log.i(this.getClass().getName(), "MODEL: " + Build.MODEL);

        return Build.MODEL;
    }

    public int getProcessorSpeed() {
        String fileContents = getFileContents("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");
        int maxFreqMHz = Integer.parseInt(fileContents) / 1000;
        Log.i(this.getClass().getSimpleName(), "Max freq: " + maxFreqMHz + " MHz");
        return maxFreqMHz;
    }

    public int getMaxRecordedProcessorSpeed() {
        return this.maxProcessorFrequency;
    }

    public String execRuntime(String... strings) {

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

    public String getDeviceName() {
        return Build.MODEL;
    }

    public String getSocName() {
        return Build.BOARD;
    }

    public String getDeviceVendor() {
        return Build.BRAND;
    }

    @Override
    public Float getProcessorTemperature() {
        return temperature;
    }

    @Override
    public Hardware gatherHardwareInfo() {

        Processor processor = gatherProcessorInfo();
        Memory memory = gatherMemoryInfo();
        Device device = gatherDeviceInfo();

        Hardware hardware = new Hardware();
        hardware.setProcessor(processor);
        hardware.setMemory(memory);
        hardware.setDevice(device);

        return hardware;
    }

    private Device gatherDeviceInfo() {
        Device device = new Device();
        device.setDeviceName(Build.MODEL);
        device.setSocName(Build.BOARD);
        device.setDeviceVendor(Build.MANUFACTURER);

        return device;
    }

    private Memory gatherMemoryInfo() {
        Memory memory = new Memory();
        memory.setTotalSize(((int) getMemorySizeInBytes() / 1024 / 1024));
        return memory;
    }

    private int getMemorySizeInBytes() {
        String fileContents = getFileContents("/proc/meminfo");
        Log.i("memory: ", fileContents);
        return 0;
    }

    private Processor gatherProcessorInfo() {
        Processor processor = new Processor();
        processor.setCoreClock((float) getProcessorSpeed());
        processor.setName(getProcessorInfo());
        processor.setEffectiveCores(getNumberOfProcessorCores());
        processor.setIdleTemp(getProcessorTemperature());
        return processor;
    }

    private int getNumberOfProcessorCores() {
        return getNumCores();
    }

    @Override
    public Integer getAvailableProcessors() {
        return getNumberOfProcessorCores();
    }

    public static String getFileContents(String file) {
        RandomAccessFile reader = null;
        String load = null;
        try {
            reader = new RandomAccessFile(file, "r");
            load = reader.readLine();
        } catch (IOException ex) {
            // Log.w("io error", ex.getMessage());
            // ex.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return load;
    }

    /**
     * Gets the number of cores available in this device, across all processors. Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
     * 
     * @return The number of cores, or 1 if failed to get result
     */
    private int getNumCores() {
        // Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                // Check if filename is "cpu", followed by a single digit number
                if (Pattern.matches("cpu[0-9]+", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }

        try {
            // Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            // Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            // Return the number of cores (virtual CPU devices)
            return files.length;
        } catch (Exception e) {
            // Default to return 1 core
            return 1;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.i("sensor", "" + event.values);
        temperature = event.values[event.values.length - 1];
        if (this.temperatureLabel != null) {
            this.temperatureLabel.setText(String.format(Locale.ENGLISH, "%.0f °C", temperature));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i("sensor", "accuracy: " + accuracy);
    }

    // @Override
    // protected void onResume() {
    // super.onResume();
    // mSensorManager.registerListener(this, mTempSensor, SensorManager.SENSOR_DELAY_NORMAL);
    // }
    //
    // @Override
    // protected void onPause() {
    // super.onPause();
    // mSensorManager.unregisterListener(this);
    // }

    public void setDeviceInfo(DeviceInfoDTO deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public DeviceInfoDTO getDeviceInfo() {
        return deviceInfo;
    }

    public int getProcessorCores() {
        return getNumCores();
    }

    public int getLoadTemperature() {
        return this.loadTemperature;
    }

    public int getIdleTemperature() {
        return this.idleTemperature;
    }

    int maxFreq0 = 0;

    public void restartMonitor() {
        monitorThread = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setPriority(Thread.MIN_PRIORITY);
                thread.setName("monitor");
                thread.setDaemon(false);
                return thread;
            }
        });
    }

    public void stopMonitorCpuFrequency() {
        try {
            if (monitorThread != null) {
                monitorThread.shutdownNow();
                monitorThread = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startMonitorCpuFrequency() {
        try {
            if (monitorThread == null) {
                restartMonitor();
            }
            monitorThread.scheduleAtFixedRate(this, 0, 500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<Integer, TextSwitcher> cpuFreqMonitors = new HashMap<>();

    public void monitorCpuFrequency(final int core, final TextSwitcher textSwitcher) {
        cpuFreqMonitors.put(core, textSwitcher);

        // initialize
        Log.i(this.getClass().getSimpleName(), "Adding monitor for core " + core);
        String curFreq = getFileContents("/sys/devices/system/cpu/cpu" + core + "/cpufreq/scaling_cur_freq");
        if (curFreq != null) {
            if (NumberUtils.isDigits(curFreq)) {
                int mhz = Integer.parseInt(curFreq) / 1000;
                // Log.i(this.getClass().getSimpleName(), "Progress MHz: " + mhz);
                textSwitcher.setText(mhz + " MHz");
            } else {
                textSwitcher.setText("down");
            }
        }
    }

    public void monitorTemperature(TextSwitcher temperatureLabel) {
        this.temperatureLabel = temperatureLabel;
    }

    @Override
    public void run() {
        try {
            Set<Integer> cores = cpuFreqMonitors.keySet();
            for (Integer core : cores) {
                TextSwitcher textSwitcher = cpuFreqMonitors.get(core);
                String fileContents = getFileContents("/sys/devices/system/cpu/cpu" + core + "/cpufreq/scaling_cur_freq");
                if (NumberUtils.isDigits(fileContents)) {
                    int mhz = Integer.parseInt(fileContents) / 1000;
                    this.maxProcessorFrequency = Math.max(this.maxProcessorFrequency, mhz);
                    textSwitcher.setText(mhz + " MHz");
                } else {
                    textSwitcher.setText("down");
                }
            }
            if (this.temperatureLabel != null) {
                String fileContents = getFileContents("/sys/class/thermal/thermal_zone0/temp");
                if (NumberUtils.isDigits(fileContents)) {
                    int celcius = Integer.parseInt(fileContents);
                    if (celcius > 150) {
                        celcius /= 1000;
                    }
                    if (celcius < -200 || celcius > 150) {
                        this.temperatureLabel.setText("out of range: " + celcius + " ºC");
                    } else {
                        this.idleTemperature = Math.min(this.idleTemperature, celcius);
                        this.loadTemperature = Math.max(this.loadTemperature, celcius);
                        this.temperatureLabel.setText(celcius + " ºC");
                    }
                } else {
                    this.temperatureLabel.setText("not available");
                }
            }
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), e.getMessage());
            e.printStackTrace();
        }
    }

    public String getKernel() {
        return getFileContents("/proc/version");
    }

    public String getOsBuild() {
        return Build.VERSION.RELEASE;
    }

    public String getOsDebugInfo() {
        return execRuntime("getprop");
    }

    public String getHardwareFromCpuInfo() {
        RandomAccessFile reader = null;
        String info = null;
        try {
            reader = new RandomAccessFile(new File("/proc/cpuinfo"), "r");
            String line = null;
            do {
                line = reader.readLine();
                if (line != null && line.startsWith("Hardware")) {
                    String hardware = StringUtils.substringAfter(line, ":").trim();
                    Log.i(this.getClass().getSimpleName(), "Hardware: " + hardware);
                    info = hardware;
                    break;
                }
            } while (line != null);
        } catch (IOException ex) {
            // Log.w("io error", ex.getMessage());
            // ex.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return info != null ? info : "unknown";
    }
}
