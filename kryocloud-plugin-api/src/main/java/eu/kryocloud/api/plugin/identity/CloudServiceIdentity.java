package eu.kryocloud.api.plugin.identity;

import eu.kryocloud.api.plugin.bootstrap.CloudPluginEndpoint;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record CloudServiceIdentity(String serviceId, String serviceName, String groupName, String serviceType, String templateName, String wrapperId, String apiHost, int apiPort, String bindAddress, int servicePort, boolean staticService, CloudServiceIdentitySource source) {

    public static final String DEFAULT_API_HOST = "127.0.0.1";
    public static final int DEFAULT_API_PORT = 7070;

    public CloudServiceIdentity {
        serviceId = clean(serviceId);
        serviceName = clean(serviceName);
        groupName = clean(groupName);
        serviceType = clean(serviceType);
        templateName = clean(templateName);
        wrapperId = clean(wrapperId);
        apiHost = clean(apiHost);
        bindAddress = clean(bindAddress);
        apiPort = validPort(apiPort) ? apiPort : DEFAULT_API_PORT;
        servicePort = validPort(servicePort) ? servicePort : 0;
        source = source == null ? CloudServiceIdentitySource.MISSING : source;

        if (apiHost.isBlank()) {
            apiHost = DEFAULT_API_HOST;
        }

        if (serviceName.isBlank()) {
            serviceName = serviceId;
        }
    }

    public static CloudServiceIdentity resolve() {
        Map<String, String> properties = fromProperties();

        if (containsIdentity(properties)) {
            return from(properties, CloudServiceIdentitySource.SYSTEM_PROPERTY);
        }

        Map<String, String> file = fromRuntimeFile(Path.of(".kryocloud", "service.json"));

        if (containsIdentity(file)) {
            return from(file, CloudServiceIdentitySource.RUNTIME_FILE);
        }

        Map<String, String> environment = fromEnvironment();

        if (containsIdentity(environment)) {
            return from(environment, CloudServiceIdentitySource.ENVIRONMENT);
        }

        return missing();
    }

    public static CloudServiceIdentity missing() {
        return new CloudServiceIdentity("", "", "", "", "", "", DEFAULT_API_HOST, DEFAULT_API_PORT, "", 0, false, CloudServiceIdentitySource.MISSING);
    }

    public boolean available() {
        return !serviceId.isBlank() || !serviceName.isBlank() || !groupName.isBlank();
    }

    public boolean runningInCloud() {
        return available();
    }

    public CloudPluginEndpoint endpoint() {
        return CloudPluginEndpoint.of(apiHost, apiPort);
    }

    public Map<String, String> payload() {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("serviceId", serviceId);
        payload.put("serviceName", serviceName);
        payload.put("groupName", groupName);
        payload.put("serviceType", serviceType);
        payload.put("templateName", templateName);
        payload.put("wrapperId", wrapperId);
        payload.put("apiHost", apiHost);
        payload.put("apiPort", String.valueOf(apiPort));
        payload.put("bindAddress", bindAddress);
        payload.put("servicePort", String.valueOf(servicePort));
        payload.put("staticService", String.valueOf(staticService));
        payload.put("identitySource", source.name());
        return Map.copyOf(payload);
    }

    private static CloudServiceIdentity from(Map<String, String> values, CloudServiceIdentitySource source) {
        String serviceId = first(values, "serviceId", "id", "name");
        String serviceName = first(values, "serviceName", "name", "serviceId", "id");
        String groupName = first(values, "groupName", "group");
        String serviceType = first(values, "serviceType", "type");
        String templateName = first(values, "templateName", "template");
        String wrapperId = first(values, "wrapperId", "wrapper");
        String apiHost = first(values, "apiHost", "api.host", "pluginHost", "plugin.host");
        String bindAddress = first(values, "bindAddress", "bind.address");
        int apiPort = integer(first(values, "apiPort", "api.port", "pluginPort", "plugin.port"), DEFAULT_API_PORT);
        int servicePort = integer(first(values, "servicePort", "port"), 0);
        boolean staticService = Boolean.parseBoolean(first(values, "staticService", "static"));

        return new CloudServiceIdentity(serviceId, serviceName, groupName, serviceType, templateName, wrapperId, apiHost, apiPort, bindAddress, servicePort, staticService, source);
    }

    private static Map<String, String> fromProperties() {
        Map<String, String> values = new LinkedHashMap<>();
        put(values, "serviceId", System.getProperty("kryocloud.service.id"));
        put(values, "serviceName", System.getProperty("kryocloud.service.name"));
        put(values, "groupName", System.getProperty("kryocloud.service.group"));
        put(values, "group", System.getProperty("kryocloud.group"));
        put(values, "serviceType", System.getProperty("kryocloud.service.type"));
        put(values, "templateName", System.getProperty("kryocloud.service.template"));
        put(values, "template", System.getProperty("kryocloud.template"));
        put(values, "wrapperId", System.getProperty("kryocloud.wrapper.id"));
        put(values, "apiHost", System.getProperty("kryocloud.api.host"));
        put(values, "apiPort", System.getProperty("kryocloud.api.port"));
        put(values, "pluginHost", System.getProperty("kryocloud.plugin.host"));
        put(values, "pluginPort", System.getProperty("kryocloud.plugin.port"));
        put(values, "bindAddress", System.getProperty("kryocloud.service.bind.address"));
        put(values, "bind.address", System.getProperty("kryocloud.bind.address"));
        put(values, "servicePort", System.getProperty("kryocloud.service.port"));
        put(values, "staticService", System.getProperty("kryocloud.service.static"));
        put(values, "static", System.getProperty("kryocloud.static"));
        return values;
    }

    private static Map<String, String> fromEnvironment() {
        Map<String, String> values = new LinkedHashMap<>();
        put(values, "serviceId", System.getenv("KRYOCLOUD_SERVICE_ID"));
        put(values, "serviceName", System.getenv("KRYOCLOUD_SERVICE_NAME"));
        put(values, "groupName", System.getenv("KRYOCLOUD_SERVICE_GROUP"));
        put(values, "group", System.getenv("KRYOCLOUD_GROUP"));
        put(values, "serviceType", System.getenv("KRYOCLOUD_SERVICE_TYPE"));
        put(values, "templateName", System.getenv("KRYOCLOUD_SERVICE_TEMPLATE"));
        put(values, "template", System.getenv("KRYOCLOUD_TEMPLATE"));
        put(values, "wrapperId", System.getenv("KRYOCLOUD_WRAPPER_ID"));
        put(values, "apiHost", System.getenv("KRYOCLOUD_API_HOST"));
        put(values, "apiPort", System.getenv("KRYOCLOUD_API_PORT"));
        put(values, "pluginHost", System.getenv("KRYOCLOUD_PLUGIN_HOST"));
        put(values, "pluginPort", System.getenv("KRYOCLOUD_PLUGIN_PORT"));
        put(values, "bindAddress", System.getenv("KRYOCLOUD_SERVICE_BIND_ADDRESS"));
        put(values, "servicePort", System.getenv("KRYOCLOUD_SERVICE_PORT"));
        put(values, "staticService", System.getenv("KRYOCLOUD_SERVICE_STATIC"));
        return values;
    }

    private static Map<String, String> fromRuntimeFile(Path path) {
        if (!Files.exists(path)) {
            return Map.of();
        }

        try {
            return parseFlatJson(Files.readString(path));
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private static Map<String, String> parseFlatJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }

        Map<String, String> values = new LinkedHashMap<>();
        int index = 0;

        while (index < json.length()) {
            int keyStart = json.indexOf('"', index);

            if (keyStart < 0) {
                break;
            }

            int keyEnd = findStringEnd(json, keyStart + 1);

            if (keyEnd < 0) {
                break;
            }

            String key = unescape(json.substring(keyStart + 1, keyEnd));
            int colon = json.indexOf(':', keyEnd + 1);

            if (colon < 0) {
                break;
            }

            int valueStart = skipWhitespace(json, colon + 1);

            if (valueStart >= json.length()) {
                break;
            }

            if (json.charAt(valueStart) == '"') {
                int valueEnd = findStringEnd(json, valueStart + 1);

                if (valueEnd < 0) {
                    break;
                }

                put(values, key, unescape(json.substring(valueStart + 1, valueEnd)));
                index = valueEnd + 1;
                continue;
            }

            int valueEnd = valueStart;

            while (valueEnd < json.length() && json.charAt(valueEnd) != ',' && json.charAt(valueEnd) != '}') {
                valueEnd++;
            }

            put(values, key, json.substring(valueStart, valueEnd).trim());
            index = valueEnd + 1;
        }

        return values;
    }

    private static boolean containsIdentity(Map<String, String> values) {
        return !first(values, "serviceId", "id", "name", "serviceName", "groupName", "group").isBlank();
    }

    private static String first(Map<String, String> values, String... keys) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        for (String key : keys) {
            String value = values.get(key);

            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        return "";
    }

    private static void put(Map<String, String> values, String key, String value) {
        if (key == null || key.isBlank()) {
            return;
        }

        if (value == null || value.isBlank()) {
            return;
        }

        values.put(key.trim(), value.trim());
    }

    private static int integer(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static boolean validPort(int port) {
        return port >= 1 && port <= 65_535;
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private static int skipWhitespace(String text, int index) {
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }

        return index;
    }

    private static int findStringEnd(String text, int index) {
        boolean escaped = false;

        for (int position = index; position < text.length(); position++) {
            char character = text.charAt(position);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (character == '\\') {
                escaped = true;
                continue;
            }

            if (character == '"') {
                return position;
            }
        }

        return -1;
    }

    private static String unescape(String value) {
        return value.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    @Override
    public String serviceType() {
        return serviceType.toUpperCase(Locale.ROOT);
    }
}
