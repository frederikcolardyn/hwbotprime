package org.hwbot.bench.service;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.hwbot.bench.prime.FileSystemUtil;
import org.hwbot.bench.prime.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HardwareServiceMac extends HardwareServiceCpuID {

    protected String version = this.getClass().getPackage().getImplementationVersion();

    @Override
    public Float getProcessorTemperature() {
        Float temp = null;
        try {
            java.io.File targetFile = prepareTemperaturMac();
            String output = FileSystemUtil.execRuntime(new String[] { targetFile.getAbsolutePath(), "-c", "-l", "-a" });
            Pattern p = Pattern.compile("CPU[A-Za-z0-9 ]{0,20}: ([0-9]{1,3}) C");
            Matcher matcher = p.matcher(output);
            if (matcher.find()) {
                temp = Float.parseFloat(matcher.group(1));
            }
        } catch (Exception e) {
            Log.error("Failed to measure temperature. Reason: " + e.getMessage());
            e.printStackTrace();
        }
        return temp;
    }

    protected java.io.File prepareTemperaturMac() {
        java.io.File targetFile = new java.io.File(System.getProperty("java.io.tmpdir") + java.io.File.separator + "temperature");
        if (!targetFile.exists()) {
            FileSystemUtil.extractFile("tempmonitor", targetFile);
        }
        return targetFile;
    }

    @Override
    public String getProcessorInfo() {
        String processor = null;
        try {
            processor = super.getProcessorInfo();
        } catch (Exception e) {
            Log.error("Failed to read processor info.");
            processor = "unknown";
        }

        return StringUtils.trim(processor);
    }

    @Override
    public String getLibraryName() {
        String property = System.getProperty("sun.arch.data.model");
        int bits;
        if ("32".equals(property)) {
            bits = 32;
        } else {
            bits = 64;
        }

        return "libCpuId-osx" + bits;
    }

    @Override
    public String getLibraryExtension() {
        return ".dylib";
    }

    public Float getEstimatedProcessorSpeed() {

        Float estimatedProcessorSpeed = super.getEstimatedProcessorSpeed();
        if (estimatedProcessorSpeed != null) {
            return estimatedProcessorSpeed;
        }

        Float speed = null;
        // mac only
        try {
            speed = NumberUtils.createFloat(StringUtils.substringAfterLast(FileSystemUtil.execRuntime(new String[] { "sysctl", "hw.cpufrequency" }), ":")
                    .trim());
            speed = speed / 1000 / 1000;
            // processorSpeedReliable = true;
        } catch (NumberFormatException e) {
            Log.error("Failed to read processor speed on mac: " + e);
        }
        return null;
    }

}
