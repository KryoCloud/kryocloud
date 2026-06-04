package eu.kryocloud.launcher.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class StartupWarningSilencer {

    private StartupWarningSilencer() {
    }

    public static void install() {
        silence("org.jline");
        silence("org.jline.utils");
        silence("org.antlr");
        silence("org.slf4j");
        System.setProperty("slf4j.provider", "org.slf4j.nop.NOPServiceProvider");
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off");
        System.setProperty("org.jline.terminal.disableDeprecatedProviderWarning", "true");
    }

    private static void silence(String name) {
        Logger logger = Logger.getLogger(name);
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);
    }
}
