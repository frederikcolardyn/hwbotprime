package org.hwbot.bench.service;

public class HardwareServiceLinux extends HardwareServiceCpuID {


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

        return "libCpuId-" + bits;
    }

    @Override
    public String getLibraryExtension() {
        return ".so";
    }

}
