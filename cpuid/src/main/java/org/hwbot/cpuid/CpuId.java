package org.hwbot.cpuid;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

public class CpuId {

    // print something
    public static native String model();

    // read sdtsc cpu clock
    public static native long rdtsc();

    public static void main(String[] args) throws IOException {
        String libraryDirectory = new File("target").getAbsolutePath() + System.getProperty("path.separator") + System.getProperty("java.library.path");
        System.setProperty("java.library.path", libraryDirectory);
        addDir(new File("target").getAbsolutePath());

        String libraryShortName = "CpuId-osx64";

        String mapLibraryName = System.mapLibraryName("CpuId-osx64");

        System.out.println("loading library... " + mapLibraryName);
        System.loadLibrary(libraryShortName);

        try {
            CpuId.model();
            CpuId.sampleFrequency();
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load native library: " + e.getMessage());
        }
    }

    public static void addDir(String s) throws IOException {
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
            throw new IOException("Failed to get permissions to set library path");
        } catch (NoSuchFieldException e) {
            throw new IOException("Failed to get field handle to set library path");
        }
    }

    public static float sampleFrequency() {
        try {
            float seconds = 0.5f;
            long t1 = System.nanoTime();
            long lp1 = rdtsc();
            Thread.sleep((int) (1000 * seconds));
            long lp2 = rdtsc();
            long t2 = System.nanoTime();

            long diff = lp2 - lp1;
            long timespan = t2 - t1;

            float mhz = diff / (timespan / 1000000 * 1000f);
            return mhz;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}