package eu.kryocloud.network.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthManager {

    private static final int MIN_TOKEN_LENGTH = 32;
    private static final Set<String> VALID_TOKENS = ConcurrentHashMap.newKeySet();

    private AuthManager() {
    }

    public static String issueToken() {
        String token = TokenGenerator.generate();
        VALID_TOKENS.add(token);
        return token;
    }

    public static void registerToken(String token) {
        validateTokenFormat(token);
        VALID_TOKENS.add(token);
    }

    public static boolean validate(String token) {
        if (!isValidFormat(token)) {
            return false;
        }

        for (String validToken : VALID_TOKENS) {
            if (constantTimeEquals(validToken, token)) {
                return true;
            }
        }

        return false;
    }

    public static int registeredTokenCount() {
        return VALID_TOKENS.size();
    }

    public static void clearTokens() {
        VALID_TOKENS.clear();
    }

    private static void validateTokenFormat(String token) {
        if (!isValidFormat(token)) {
            throw new IllegalArgumentException("token must not be blank and must contain at least " + MIN_TOKEN_LENGTH + " characters");
        }
    }

    private static boolean isValidFormat(String token) {
        if (token == null) {
            return false;
        }

        if (token.isBlank()) {
            return false;
        }

        return token.length() >= MIN_TOKEN_LENGTH;
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }
}