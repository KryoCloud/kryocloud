package eu.kryocloud.api.plugin.bootstrap;

import eu.kryocloud.api.plugin.identity.CloudServiceIdentity;
import java.util.Locale;

public record CloudPluginEndpoint(String host, int port) {

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 7070;

    private static final String HOST_PROPERTY = "kryocloud.plugin.host";
    private static final String PORT_PROPERTY = "kryocloud.plugin.port";
    private static final String API_HOST_PROPERTY = "kryocloud.api.host";
    private static final String API_PORT_PROPERTY = "kryocloud.api.port";
    private static final String HOST_ENV = "KRYOCLOUD_PLUGIN_HOST";
    private static final String PORT_ENV = "KRYOCLOUD_PLUGIN_PORT";
    private static final String API_HOST_ENV = "KRYOCLOUD_API_HOST";
    private static final String API_PORT_ENV = "KRYOCLOUD_API_PORT";

    public CloudPluginEndpoint {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }

        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }

        host = host.trim().toLowerCase(Locale.ROOT);
    }

    public static CloudPluginEndpoint local() {
        return new CloudPluginEndpoint(DEFAULT_HOST, DEFAULT_PORT);
    }

    public static CloudPluginEndpoint configured() {
        CloudServiceIdentity identity = CloudServiceIdentity.resolve();
        String host = value(API_HOST_PROPERTY, API_HOST_ENV, value(HOST_PROPERTY, HOST_ENV, identity.apiHost()));
        int port = port(API_PORT_PROPERTY, API_PORT_ENV, port(PORT_PROPERTY, PORT_ENV, identity.apiPort()));

        return new CloudPluginEndpoint(host, port);
    }

    public static CloudPluginEndpoint of(String host, int port) {
        return new CloudPluginEndpoint(host, port);
    }

    private static String value(String property, String environment, String fallback) {
        String propertyValue = System.getProperty(property);

        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        String environmentValue = System.getenv(environment);

        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }

        return fallback;
    }

    private static int port(String property, String environment, int fallback) {
        String value = value(property, environment, String.valueOf(fallback));

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

}
