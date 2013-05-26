package org.hwbot.bench.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.jar.JarEntry;

import org.apache.commons.io.IOUtils;
import org.hwbot.bench.service.HardwareService;

public class Util {

    public static boolean extractFile(String fileToExtract, File targetFile) {
        boolean installed = false;
        try {
            if (targetFile.exists()) {
                installed = true;
                // ok!
                // System.out.println("Using CPU executable: " + getCpuIdExecutable().getAbsolutePath());
            } else {
                String path = HardwareService.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                String decodedPath;
                decodedPath = URLDecoder.decode(path, "UTF-8");
                java.util.jar.JarFile jar = new java.util.jar.JarFile(decodedPath);
                Enumeration<JarEntry> entries = jar.entries();
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
                        installed = true;
                        break;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            // try loading as stream
            System.out.println("Jar file not found, loading as resource.");
            InputStream resourceAsStream = Util.class.getClassLoader().getResourceAsStream(fileToExtract);
            if (resourceAsStream != null) {
                try {
                    IOUtils.copy(resourceAsStream, new FileWriter(targetFile));
                } catch (IOException e1) {
                    throw new RuntimeException(e1);
                }
            } else {
                System.out.println("Resource " + fileToExtract + " not found.");
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            System.out.println("Jar file not found, " + e);
            throw new RuntimeException(e);
        }
        return installed;
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

    public static void addDirToJavaLibraryPath(String s) {
        try {
            // This enables the java.library.path to be modified at runtime
            // From a Sun engineer at http://forums.sun.com/thread.jspa?threadID=707176
            //
            Field field = ClassLoader.class.getDeclaredField("usr_paths");
            field.setAccessible(true);
            String[] paths = (String[]) field.get(null);
            for (int i = 0; i < paths.length; i++) {
                if (s.equals(paths[i])) {
                    return;
                }
            }
            String[] tmp = new String[paths.length + 1];
            System.arraycopy(paths, 0, tmp, 0, paths.length);
            tmp[paths.length] = s;
            field.set(null, tmp);
            System.setProperty("java.library.path", System.getProperty("java.library.path") + File.pathSeparator + s);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get permissions to set library path");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to get field handle to set library path");
        }
    }

}
