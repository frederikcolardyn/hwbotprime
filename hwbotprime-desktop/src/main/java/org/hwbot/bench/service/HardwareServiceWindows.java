package org.hwbot.bench.service;

public class HardwareServiceWindows extends HardwareServiceCpuID {

    @Override
    public Float getProcessorTemperature() {
        return null;
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

        return "cpuid-" + bits;
    }

    @Override
    public String getLibraryExtension() {
        return ".dll";
    }

}
