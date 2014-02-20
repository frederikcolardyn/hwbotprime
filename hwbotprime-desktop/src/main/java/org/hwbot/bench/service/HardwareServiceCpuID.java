package org.hwbot.bench.service;

import org.apache.commons.lang.StringUtils;
import org.hwbot.bench.prime.AbstractHardwareService;
import org.hwbot.bench.prime.FileSystemUtil;
import org.hwbot.bench.prime.Log;
import org.hwbot.cpuid.CpuId;

public abstract class HardwareServiceCpuID extends AbstractHardwareService {

    public HardwareServiceCpuID() {
        if (!libraryLoaded) {
            prepareCpuid();
        }
    }

    public String getProcessorInfo() {
        String processor = null;
        try {
            if (libraryLoaded) {
                processor = CpuId.model();
            }
        } catch (Exception e) {
            processor = null;
        }
        return StringUtils.trim(processor);
    }

    public Float getEstimatedProcessorSpeed() {
        if (libraryLoaded) {
            return CpuId.sampleFrequency();
        }
        return null;
    }

    protected void prepareCpuid() {
        String libraryName = getLibraryName();
        if (libraryName == null) {
            Log.error("No native cpu speed library for " + AbstractHardwareService.OS + " yet... falling back to OS tools.");
        } else {
            String libraryNameWithVersion = libraryName + "-" + version;
            java.io.File cpuid = new java.io.File(System.getProperty("java.io.tmpdir") + java.io.File.separator + libraryNameWithVersion
                    + getLibraryExtension());
            if (!cpuid.exists()) {
                // Log.info("Installing " + libraryNameWithVersion + " in " + cpuid);
                FileSystemUtil.extractFile(libraryName + getLibraryExtension(), cpuid);
            } else {
                // Log.info("Using existing " + cpuid.getAbsolutePath());
                FileSystemUtil.extractFile(libraryName + getLibraryExtension(), cpuid);
            }

            // load
            String libraryDirectory = cpuid.getParent();
            // System.setProperty("java.library.path", libraryDirectory);
            FileSystemUtil.addDirToJavaLibraryPath(libraryDirectory);

            String libraryShortName = (libraryNameWithVersion.startsWith("lib")) ? StringUtils.substringAfter(libraryNameWithVersion, "lib")
                    : libraryNameWithVersion;

            try {
                System.loadLibrary(libraryShortName);
                CpuId.model();
                // CpuId.sampleFrequency();
                libraryLoaded = true;
            } catch (UnsatisfiedLinkError e) {
                Log.error("Failed to load native library " + libraryShortName + " on OS " + AbstractHardwareService.OS + ": " + e.getMessage());
            }

        }
    }

}
