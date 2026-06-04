package eu.kryocloud.sphere;

import java.util.Locale;

public enum KryoSpherePlatform {

    LINUX,
    WINDOWS,
    MACOS,
    UNKNOWN;

    public static KryoSpherePlatform current() {
        String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        if (name.contains("linux")) {
            return LINUX;
        }

        if (name.contains("windows")) {
            return WINDOWS;
        }

        if (name.contains("mac") || name.contains("darwin")) {
            return MACOS;
        }

        return UNKNOWN;
    }

    public boolean unix() {
        return this == LINUX || this == MACOS;
    }

}
