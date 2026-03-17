package eu.kryocloud.network.auth;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {

    private static final Set<String> validTokens = ConcurrentHashMap.newKeySet();

    public static boolean validate(String token) {
        if (token == null) {
            return false;
        }
        if (!isValidFormat(token)) {
            return false;
        }
        return validTokens.add(token);
    }

    private static boolean isValidFormat(String token) {
        return token.length() > 10;
    }
}
