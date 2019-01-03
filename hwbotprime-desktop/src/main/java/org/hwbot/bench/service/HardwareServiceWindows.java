package org.hwbot.bench.service;

import com.jezhumble.javasysmon.JavaSysMon;
import com.jezhumble.javasysmon.MemoryStats;
import org.hwbot.bench.model.Device;
import org.hwbot.bench.model.Hardware;
import org.hwbot.bench.model.Memory;
import org.hwbot.bench.model.Processor;

public class HardwareServiceWindows extends HardwareServiceCpuID {

    @Override
    public Float getProcessorTemperature() {
        return null;
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
        return null;
    }

    private Memory gatherMemoryInfo() {
        Memory memory = new Memory();
        memory.setTotalSize(((int) getMemorySizeInBytes() / 1024 / 1024));
        return memory;
    }

    private Processor gatherProcessorInfo() {
        Processor processor = new Processor();
        processor.setCoreClock(getEstimatedProcessorSpeed());
        processor.setName(getProcessorInfo());
        processor.setEffectiveCores(getNumberOfProcessorCores());
        processor.setIdleTemp(getProcessorTemperature());
        return processor;
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

    public int getNumberOfProcessorCores() {
        JavaSysMon sysMon = new JavaSysMon();
        if (sysMon.supportedPlatform()) {
            try {
                if (sysMon.numCpus() > 0) {
                    return sysMon.numCpus();
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return 1;
    }

    public long getMemorySizeInBytes() {
        JavaSysMon sysMon = new JavaSysMon();
        if (sysMon.supportedPlatform()) {
            MemoryStats memoryStats = sysMon.physical();
            return memoryStats.getTotalBytes();
        }
        return 0;
    }
}
