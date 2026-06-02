package eu.kryocloud.plugins.proxybridge;

import eu.kryocloud.api.plugin.cloud.model.CloudServiceSnapshot;
import eu.kryocloud.api.plugin.cloud.model.CloudServiceState;
import eu.kryocloud.api.plugin.cloud.model.CloudServiceType;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ProxyServiceMapper {

    private ProxyServiceMapper() {
    }

    public static boolean registerable(CloudServiceSnapshot service) {
        if (service == null) {
            return false;
        }

        if (service.type() == CloudServiceType.PROXY) {
            return false;
        }

        if (service.type() == CloudServiceType.UNKNOWN) {
            return false;
        }

        if (service.state() != CloudServiceState.RUNNING) {
            return false;
        }

        if (service.port() <= 0) {
            return false;
        }

        return host(service).isPresent();
    }

    public static Optional<ProxyRegistration> registration(CloudServiceSnapshot service) {
        if (!registerable(service)) {
            return Optional.empty();
        }

        return host(service).map(host -> new ProxyRegistration(service.name(), service.groupName(), host, service.port()));
    }

    private static Optional<String> host(CloudServiceSnapshot service) {
        String host = clean(service.host());

        if (!host.isBlank() && !wildcard(host)) {
            return Optional.of(host);
        }

        Map<String, String> properties = service.properties();
        String propertyHost = first(properties, "proxyHost", "publicHost", "advertisedHost", "host", "address", "bindAddress", "bind.address");

        if (!propertyHost.isBlank() && !wildcard(propertyHost)) {
            return Optional.of(propertyHost);
        }

        if (!host.isBlank()) {
            return Optional.of("127.0.0.1");
        }

        return Optional.empty();
    }

    private static boolean wildcard(String host) {
        String value = host.toLowerCase(Locale.ROOT);

        if (value.equals("0.0.0.0")) {
            return true;
        }

        if (value.equals("::")) {
            return true;
        }

        return value.equals("*");
    }

    private static String first(Map<String, String> values, String... keys) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        for (String key : keys) {
            String value = clean(values.get(key));

            if (!value.isBlank()) {
                return value;
            }
        }

        return "";
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

}
