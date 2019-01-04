package org.hwbot.bench.prime;

import org.hwbot.bench.model.Device;
import org.hwbot.bench.model.Hardware;
import org.hwbot.bench.model.Memory;
import org.hwbot.bench.model.Processor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;

public abstract class AbstractHardwareService implements HardwareService {

    public static String OS = System.getProperty("os.name").toLowerCase();
    public static String OS_ARCH = System.getProperty("os.arch").toLowerCase();
    protected boolean libraryLoaded;
    protected String version = this.getClass().getPackage().getImplementationVersion();

    @Override
    public abstract Float getProcessorTemperature();

    public abstract String getProcessorInfo();

    public abstract String getLibraryName();

    public abstract String getLibraryExtension();

    public abstract Float getEstimatedProcessorSpeed();

    @Override
    public Integer getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
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
        memory.setTotalSize((int) (getMemorySize() / 1024 / 1024));
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

    public long getMemorySize() {
        return ((com.sun.management.OperatingSystemMXBean) ManagementFactory
                .getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
    }

    public int getNumberOfProcessorCores() {
        String command = "";
        if(OperatingSystemUtil.isMac()){
            command = "sysctl -n machdep.cpu.core_count";
        }else if(OperatingSystemUtil.isUnix()){
            command = "lscpu";
        }else if(OperatingSystemUtil.isWindows()){
            command = "cmd /C WMIC CPU Get /Format:List";
        }
        Process process = null;
        int numberOfCores = 0;
        int sockets = 0;
        try {
            if(OperatingSystemUtil.isMac()){
                String[] cmd = { "/bin/sh", "-c", command};
                process = Runtime.getRuntime().exec(cmd);
            }else{
                process = Runtime.getRuntime().exec(command);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        String line;

        try {
            while ((line = reader.readLine()) != null) {
                if(OperatingSystemUtil.isMac()){
                    numberOfCores = line.length() > 0 ? Integer.parseInt(line) : 0;
                }else if (OperatingSystemUtil.isUnix()) {
                    if (line.contains("Core(s) per socket:")) {
                        numberOfCores = Integer.parseInt(line.split("\\s+")[line.split("\\s+").length - 1]);
                    }
                    if(line.contains("Socket(s):")){
                        sockets = Integer.parseInt(line.split("\\s+")[line.split("\\s+").length - 1]);
                    }
                } else if (OperatingSystemUtil.isWindows()) {
                    if (line.contains("NumberOfCores")) {
                        numberOfCores = Integer.parseInt(line.split("=")[1]);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(OperatingSystemUtil.isUnix()){
            return numberOfCores * sockets;
        }
        return numberOfCores;
    }


}
