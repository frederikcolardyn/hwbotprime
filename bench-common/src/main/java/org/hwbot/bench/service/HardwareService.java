package org.hwbot.bench.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.jezhumble.javasysmon.JavaSysMon;
import com.jezhumble.javasysmon.MemoryStats;

public class HardwareService {

    public static String OS = System.getProperty("os.name").toLowerCase();
    protected String version = this.getClass().getPackage().getImplementationVersion();

    public String getProcessorInfo() {
        String processor = null;
        try {

            processor = execRuntime(new String[] { getCpuIdExecutable().getAbsolutePath(), "-b" });

            if (StringUtils.isEmpty(processor)) {
                processor = readProcessorStringFromProcCpuInfo();
            }
        } catch (Exception e) {
            System.err.println("Failed to read processor info.");
            processor = "unknown";
        }

        return StringUtils.trim(processor);
    }

    public void prepareCpuid(File targetFile) {
        String property = System.getProperty("sun.arch.data.model");
        int bits;
        if ("32".equals(property)) {
            bits = 32;
        } else {
            bits = 64;
        }

        if (isWindows()) {
            extractFile("cpuid-win" + bits + ".exe", targetFile, false);
        } else if (isMac()) {
            extractFile("cpuid-osx" + bits, targetFile, true);
        } else if (isUnix()) {
            extractFile("cpuid-linux" + bits, targetFile, true);
        } else {
            System.out.println("Your OS is not supported!!");
        }
    }

    public static void extractFile(String fileToExtract, File targetFile, boolean permissions) {

        try {
            if (targetFile.exists()) {
                // ok!
                // System.out.println("Using CPU executable: " + getCpuIdExecutable().getAbsolutePath());
            } else {
                String path = HardwareService.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                String decodedPath;
                decodedPath = URLDecoder.decode(path, "UTF-8");

                java.util.jar.JarFile jar = new java.util.jar.JarFile(decodedPath);
                Enumeration<JarEntry> entries = jar.entries();
                boolean installed = false;
                while (entries.hasMoreElements()) {
                    java.util.jar.JarEntry file = (java.util.jar.JarEntry) entries.nextElement();
                    java.io.File f = targetFile;
                    if (file.getName().equals(fileToExtract)) {
                        if (file.isDirectory()) { // if its a directory, create it
                            f.mkdir();
                            continue;
                        }
                        java.io.InputStream is = jar.getInputStream(file); // get the input stream
                        java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
                        while (is.available() > 0) { // write contents of 'is' to 'fos'
                            fos.write(is.read());
                        }
                        fos.close();
                        is.close();
                        // System.out.println("cpuid executable written to " + f);
                        // System.out.println("Prepared: " + targetFile.getAbsolutePath());
                        if (permissions) {
                            Runtime.getRuntime().exec("chmod +x " + f.getAbsolutePath());
                        }
                        installed = true;
                        break;
                    }
                }
                if (!installed) {
                    System.err.println("Sorry, we can not run the bechmark on this platform. Please inform HWBOT crew this does not work on " + OS);
                    throw new RuntimeException("OS not supported!");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
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
        System.out.println("Unable to dedect amount of cores.");
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
    public Float getDefaultProcessorSpeed() {

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
                speed = NumberUtils.createFloat(StringUtils.substringAfterLast(execRuntime(new String[] { "sysctl", "hw.cpufrequency" }), ":").trim());
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

    public Float measureCpuSpeed() {
        String processorInMhz = execRuntime(new String[] { getCpuIdExecutable().getAbsolutePath(), "-s" });

        if (NumberUtils.isDigits(processorInMhz)) {
            return NumberUtils.createFloat(processorInMhz);
        } else {
            System.err.println("Not a frequency: " + processorInMhz);
            return null;
        }
    }

    public File getCpuIdExecutable() {
        java.io.File cpuid = new java.io.File(System.getProperty("java.io.tmpdir") + java.io.File.separator + "cpuid-" + version);
        if (!cpuid.exists()) {
            prepareCpuid(cpuid);
        }
        return cpuid;
    }

    public static boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

    public static boolean isMac() {
        return (OS.indexOf("mac") >= 0);
    }

    public static boolean isUnix() {
        return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);
    }

    public static String execRuntime(String[] strings) {

        Process proc = null;
        int inBuffer, errBuffer;
        StringBuffer outputReport = new StringBuffer();
        StringBuffer errorBuffer = new StringBuffer();

        try {
            proc = Runtime.getRuntime().exec(strings);
        } catch (IOException e) {
            return "";
        }
        try {
            proc.waitFor();
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

            proc.destroy();
            proc = null;
        }

        if (errorBuffer.length() > 0) {
            // System.err.println("could not finish execution because of error(s): " + strings[0]);
            // System.err.println("*** Error : " + errorBuffer.toString());
            return null;
        }

        return outputReport.toString();
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
