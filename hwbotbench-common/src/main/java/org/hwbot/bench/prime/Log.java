package org.hwbot.bench.prime;

public class Log {

    private static final String loglevel = System.getProperty("loglevel", "info");

    public static void debug(String msg, Object... args) {
        if (loglevel.equals("debug")) {
            System.out.println("[debug] " + String.format(msg, args));
        }
    }

    public static void info(String msg, Object... args) {
        System.out.println(String.format(msg, args));
    }

    public static void error(String msg, Object... args) {
        System.err.println("[error] " + String.format(msg, args));
    }

}
