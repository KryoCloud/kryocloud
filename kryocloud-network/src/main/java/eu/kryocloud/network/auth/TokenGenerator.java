package eu.kryocloud.network.auth;

import java.util.UUID;

public class TokenGenerator {

    public static String generate() {
        long time = System.currentTimeMillis();
        String base = UUID.randomUUID().toString();
        return base + "-" + Long.toHexString(time);
    }
}
