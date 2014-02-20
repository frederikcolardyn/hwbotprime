package org.hwbot.bench.prime;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.jar.JarEntry;

import org.hwbot.bench.model.Hardware;

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

    public static void extractFile(String fileToExtract, File targetFile, boolean permissions) throws IOException {

        if (targetFile.exists()) {
            // ok!
            // Log.info("Using CPU executable: " + getCpuIdExecutable().getAbsolutePath());
        } else {
            String path = AbstractHardwareService.class.getProtectionDomain().getCodeSource().getLocation().getPath();
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
                    // Log.info("cpuid executable written to " + f);
                    // Log.info("Prepared: " + targetFile.getAbsolutePath());
                    if (permissions) {
                        Runtime.getRuntime().exec("chmod +x " + f.getAbsolutePath());
                    }
                    installed = true;
                    break;
                }
            }
            if (!installed) {
                Log.error("Sorry, we can not run the bechmark on this platform. Please inform HWBOT crew this does not work on " + AbstractHardwareService.OS
                        + " - " + AbstractHardwareService.OS_ARCH);
                throw new RuntimeException("OS not supported!");
            }
        }
    }

    @Override
    public Integer getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    @Override
    public Hardware gatherHardwareInfo() {
        return null;
    }

}
