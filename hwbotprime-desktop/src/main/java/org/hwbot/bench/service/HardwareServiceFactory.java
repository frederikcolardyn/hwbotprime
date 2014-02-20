package org.hwbot.bench.service;

import org.hwbot.bench.prime.HardwareService;
import org.hwbot.bench.prime.Log;

public class HardwareServiceFactory {

    public static String OS = System.getProperty("os.name").toLowerCase();
    public static String OS_ARCH = System.getProperty("os.arch").toLowerCase();
    protected String version = this.getClass().getPackage().getImplementationVersion();
    protected boolean libraryLoaded;
    protected boolean processorSpeedReliable;

    private HardwareServiceFactory() {
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

    public static HardwareService getInstance() {
        if (isWindows()) {
            Log.debug("Using windows hardware detection.");
            return new HardwareServiceWindows();
        }
        if (isUnix()) {
            Log.debug("Using linux hardware detection.");
            return new HardwareServiceLinux();
        }
        if (isMac()) {
            Log.debug("Using mac hardware detection.");
            return new HardwareServiceMac();
        }

        Log.error("Can not detect hardware on platforn ", OS + " - " + OS_ARCH);
        return new HardwareServiceUnknown();
    }

}
