package org.hwbot.bench.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.hwbot.bench.util.Util;
import org.hwbot.cpuid.CpuId;

import com.jezhumble.javasysmon.JavaSysMon;
import com.jezhumble.javasysmon.MemoryStats;

public class HardwareService {

    public static String OS = System.getProperty("os.name").toLowerCase();
    public static String OS_ARCH = System.getProperty("os.arch").toLowerCase();
    protected String version = this.getClass().getPackage().getImplementationVersion();
    protected boolean libraryLoaded;

    public HardwareService() {
        prepareCpuid();
    }

    public String getProcessorInfo() {
        String processor = null;
        try {

            if (libraryLoaded) {
                processor = CpuId.model();
            }

            if (StringUtils.isEmpty(processor)) {
                processor = readProcessorStringFromProcCpuInfo();
            }
        } catch (Exception e) {
            System.err.println("Failed to read processor info.");
            processor = "unknown";
        }

        return StringUtils.trim(processor);
    }

    public void prepareCpuid() {
        String libraryName = getLibraryName();

        if (libraryName == null) {
            System.err.println("No native cpu speed library for " + OS + " yet... falling back to OS tools.");
        } else {

            String libraryNameWithVersion = libraryName + "-" + version;
            java.io.File cpuid = new java.io.File(System.getProperty("java.io.tmpdir") + java.io.File.separator + libraryNameWithVersion
                    + getLibraryExtension());
            if (!cpuid.exists()) {
                // System.out.println("Installing " + libraryNameWithVersion + " in " + cpuid);
                Util.extractFile(libraryName + getLibraryExtension(), cpuid);
            } else {
                // System.out.println("Using existing " + cpuid.getAbsolutePath());
            }

            // load
            String libraryDirectory = cpuid.getParent();
            // System.setProperty("java.library.path", libraryDirectory);
            Util.addDirToJavaLibraryPath(libraryDirectory);

            String libraryShortName = (libraryNameWithVersion.startsWith("lib")) ? StringUtils.substringAfter(libraryNameWithVersion, "lib")
                    : libraryNameWithVersion;

            // System.out.println("loading library... " + libraryShortName + " in " + System.getProperty("java.library.path"));
            try {
                System.loadLibrary(libraryShortName);
                CpuId.model();
                // CpuId.sampleFrequency();
                libraryLoaded = true;
            } catch (UnsatisfiedLinkError e) {
                System.err.println("Failed to load native library on OS " + OS + ": " + e.getMessage());
                Properties properties = System.getProperties();

                Set<Entry<Object, Object>> entrySet = properties.entrySet();
                for (Entry<Object, Object> entry : entrySet) {
                    System.out.println(entry.getKey() + ": " + entry.getValue());
                }
            }

        }
    }

    public String getLibraryName() {
        String property = System.getProperty("sun.arch.data.model");
        String name;
        int bits;
        if ("32".equals(property)) {
            bits = 32;
        } else {
            bits = 64;
        }

        if (isArm()) {
            name = null;
        } else if (isWindows()) {
            name = "cpuid-" + bits;
        } else if (isMac()) {
            name = "libCpuId-osx" + bits;
        } else if (isUnix()) {
            name = "libCpuId-" + bits;
        } else {
            name = null;
            System.err.println("OS '" + OS + "' is not supported!!");
        }
        return name;
    }

    public String getLibraryExtension() {

        if (isWindows()) {
            return ".dll";
        } else if (isMac()) {
            return ".dylib";
        } else if (isUnix()) {
            return ".so";
        } else {
            System.err.println("OS '" + OS + "' is not supported!!");
            return null;
        }
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
        // System.out.println("Unable to dedect amount of cores.");
        return 1;
    }

    /**
     * 
     * @return memory in bytes
     */
    public long getMemorySize() {
        JavaSysMon sysMon = new JavaSysMon();
        if (sysMon.supportedPlatform()) {
            MemoryStats memoryStats = sysMon.physical();
            return memoryStats.getTotalBytes();
        }
        return 0;
    }

    /**
     * 
     * @return processor speed in MHz
     */
    public Float getEstimatedProcessorSpeed() {

        if (libraryLoaded) {
            return CpuId.sampleFrequency();
        }

        // utility class, reports stock speed mostly
        JavaSysMon sysMon = new JavaSysMon();
        if (sysMon.supportedPlatform()) {
            try {
                if (sysMon.cpuFrequencyInHz() > 0) {
                    System.out.println("using java sysmon");
                    return sysMon.cpuFrequencyInHz() / 1000f / 1000f;
                }
            } catch (Exception e) {
                // ignore
            }
        }

        // see if the scaling_cur_freq File is present (linux only)
        if (isUnix()) {
            // cat "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq
            File linuxFreqFile = new File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");
            if (linuxFreqFile.exists() && linuxFreqFile.canRead()) {
                try {
                    Float speed = Float.parseFloat(FileUtils.readFileToString(linuxFreqFile));
                    if (speed > 0) {
                        speed = speed / 1000f;
                        // System.out.println("detected speed cpuinfo_max_freq: "+speed);
                        return speed;
                    }
                } catch (IOException e) {
                }
            }
        }

        Float speed = null;

        // mac only
        if (isMac()) {
            try {
                speed = NumberUtils.createFloat(StringUtils.substringAfterLast(Util.execRuntime(new String[] { "sysctl", "hw.cpufrequency" }), ":").trim());
                speed = speed / 1000 / 1000;
            } catch (NumberFormatException e) {
                System.err.println("Failed to read processor speed on mac: " + e);
            }
        }

        // calculate based on bogomips on unix
        if (isUnix()) {
            File linuxFreqFile = new File("/proc/cpuinfo");
            if (linuxFreqFile.exists() && linuxFreqFile.canRead()) {
                try {
                    List<String> lines = FileUtils.readLines(linuxFreqFile);
                    speed = 0f;
                    String proc = null;
                    for (String line : lines) {
                        if (line.contains("Processor")) {
                            proc = StringUtils.substringAfterLast(line, ":").trim();
                        }
                        if (line.contains("BogoMIPS")) {
                            Float mips = Float.parseFloat(StringUtils.substringAfterLast(line, ":").trim());
                            speed = mipsToSpeed(proc, mips);
                        }
                    }
                    if (speed > 0) {
                        return speed;
                    }
                } catch (Exception e) {
                }
            }
        }

        return null;
    }

    private static float mipsToSpeed(String proc, Float mips) {
        if (proc == null) {
            return 0;
        } else {
            proc = proc.toLowerCase();
            if (proc.contains("arm")) {
                // very uneducated guess
                return mips / 2;
            }
        }
        return 0;
    }

    public static boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

    public static boolean isArm() {
        return (OS_ARCH.indexOf("arm") >= 0);
    }

    public static boolean isMac() {
        return (OS.indexOf("mac") >= 0);
    }

    public static boolean isUnix() {
        return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);
    }

    public String readProcessorStringFromProcCpuInfo() throws IOException {
        File source = new File("/proc/cpuinfo");
        if (source.exists() && source.canRead()) {
            for (String line : FileUtils.readLines(source)) {
                if (line.startsWith("model name")) {
                    return line.substring(line.indexOf(':') + 1).trim();
                } else if (line.startsWith("Processor")) {
                    return line.substring(line.indexOf(':') + 1).trim();
                }
            }
        }
        return null;
    }
}
