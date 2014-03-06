package org.hwbot.prime.service;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.regex.Pattern;

import org.hwbot.bench.model.Device;
import org.hwbot.bench.model.Hardware;
import org.hwbot.bench.model.Memory;
import org.hwbot.bench.model.Processor;
import org.hwbot.bench.prime.HardwareService;
import org.hwbot.prime.model.DeviceInfo;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

public class AndroidHardwareService extends Activity implements HardwareService, SensorEventListener {

    protected static AndroidHardwareService service;
    protected DeviceInfo deviceInfo;

    public static String OS = System.getProperty("os.name").toLowerCase();
    private SensorManager mSensorManager;
    private Sensor mTempSensor;
    private float temperature;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager) getSystemService(Activity.SENSOR_SERVICE);
        mTempSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
    }

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

    public Float getProcessorSpeed() {
        String fileContents = getFileContents("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");
        Log.i("Max freq", fileContents);
        return Float.parseFloat(fileContents) / 1000;
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
        processor.setCoreClock(getProcessorSpeed());
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
            ex.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
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
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i("sensor", "accuracy: " + accuracy);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mTempSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public void setDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

}
