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
        System.setProperty("slf4j.internal.verbosity", "ERROR");
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off");
        System.setProperty("org.jline.terminal.disableDeprecatedProviderWarning", "true");
        System.setProperty("org.jline.terminal.provider", "exec");
        System.setProperty("org.jline.terminal.providers", "exec,dumb");
        System.setProperty("org.jline.terminal.ffm", "false");
        System.setProperty("org.jline.terminal.jni", "false");
        System.setProperty("org.jline.terminal.jna", "false");
        System.setProperty("org.jline.terminal.jansi", "false");
    }

    private static void silence(String name) {
        Logger logger = Logger.getLogger(name);
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);
    }
}
