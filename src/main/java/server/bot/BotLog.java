package server.bot;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class BotLog {
    private BotLog() {}
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");

    private static String now() {
        return "[" + TS.format(LocalDateTime.now()) + "]";
    }

    public static void info(String msg) {
        System.out.println(now() + " [INFO] " + msg);
    }

    public static void complete(String msg) {
        System.out.println(now() + " [DISCORD] " + msg);
    }

    public static void warn(String msg) {
        System.out.println(now() + " [WARN] " + msg);
    }

    public static void error(String msg) {
        System.err.println(now() + " [ERROR] " + msg);
    }
}
