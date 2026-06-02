package eu.kryocloud.plugins.proxybridge;

import java.net.InetSocketAddress;
import java.util.Objects;

public record ProxyRegistration(String name, String groupName, String host, int port) {

    public ProxyRegistration {
        name = requireText(name, "name");
        groupName = clean(groupName);
        host = requireText(host, "host");

        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }

    public InetSocketAddress address() {
        return InetSocketAddress.createUnresolved(host, port);
    }

    public boolean sameAddress(ProxyRegistration other) {
        if (other == null) {
            return false;
        }

        if (!Objects.equals(host, other.host)) {
            return false;
        }

        return port == other.port;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }

        return value.trim();
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

}
