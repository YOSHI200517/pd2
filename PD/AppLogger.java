import java.util.logging.*;

/**
 * シンプルなロガーユーティリティ。コンソールに INFO/SEVERE を出力します。
 */
public final class AppLogger {
    private static final Logger logger = Logger.getLogger("PDApp");

    static {
        logger.setUseParentHandlers(false);
        ConsoleHandler h = new ConsoleHandler();
        h.setLevel(Level.ALL);
        h.setFormatter(new SimpleFormatter());
        logger.addHandler(h);
        logger.setLevel(Level.ALL);
    }

    private AppLogger() {}

    public static void info(String msg) { logger.log(Level.INFO, msg); }
    public static void warn(String msg) { logger.log(Level.WARNING, msg); }
    public static void error(String msg, Throwable t) { logger.log(Level.SEVERE, msg, t); }
    public static void error(String msg) { logger.log(Level.SEVERE, msg); }
}
