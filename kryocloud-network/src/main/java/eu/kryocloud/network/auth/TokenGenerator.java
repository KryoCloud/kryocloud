package eu.kryocloud.network.auth;

import java.security.SecureRandom;
import java.util.Base64;

public final class TokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private TokenGenerator() {
    }

    public static String generate() {
        byte[] token = new byte[32];
        RANDOM.nextBytes(token);
        return TOKEN_ENCODER.encodeToString(token);
    }
}