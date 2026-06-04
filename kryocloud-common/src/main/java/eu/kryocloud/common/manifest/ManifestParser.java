package eu.kryocloud.common.manifest;

import org.yaml.snakeyaml.Yaml;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ManifestParser {

    public SoftwareManifest parse(String yamlContent) {
        if (yamlContent == null || yamlContent.isBlank()) {
            throw new IllegalArgumentException("yamlContent must not be blank");
        }

        Object loaded = new Yaml().load(stripBom(yamlContent));

        if (!(loaded instanceof Map<?, ?> root)) {
            throw new IllegalArgumentException("Manifest root must be a YAML object");
        }

        SoftwareType type = SoftwareType.valueOf(string(root, "type").toUpperCase());
        String latestVersion = optionalString(root, "latestVersion");
        Map<?, ?> rawVersions = map(root, "versions");
        Map<String, SoftwareVersion> versions = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : rawVersions.entrySet()) {
            String version = String.valueOf(entry.getKey());

            if (!(entry.getValue() instanceof Map<?, ?> versionRoot)) {
                throw new IllegalArgumentException("Version entry must be an object: " + version);
            }

            versions.put(version, new SoftwareVersion(version, URI.create(string(versionRoot, "link")), integer(versionRoot, "javaVersion"), stringList(versionRoot, "javaFlags")));
        }

        return new SoftwareManifest(type, effectiveLatestVersion(latestVersion, versions), versions);
    }

    private String stripBom(String value) {
        if (value.startsWith("\uFEFF")) {
            return value.substring(1);
        }

        return value;
    }

    private String effectiveLatestVersion(String latestVersion, Map<String, SoftwareVersion> versions) {
        if (latestVersion != null && !latestVersion.isBlank() && versions.containsKey(latestVersion)) {
            return latestVersion;
        }

        return ManifestVersionComparator.newest(versions.keySet());
    }

    private String optionalString(Map<?, ?> map, String key) {
        Object value = map.get(key);

        if (value == null) {
            return "";
        }

        return String.valueOf(value).trim();
    }

    private String string(Map<?, ?> map, String key) {
        Object value = map.get(key);

        if (value == null) {
            throw new IllegalArgumentException("Manifest key is missing: " + key);
        }

        String result = String.valueOf(value);

        if (result.isBlank()) {
            throw new IllegalArgumentException("Manifest key must not be blank: " + key);
        }

        return result;
    }

    private int integer(Map<?, ?> map, String key) {
        Object value = map.get(key);

        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value == null) {
            throw new IllegalArgumentException("Manifest key is missing: " + key);
        }

        return Integer.parseInt(String.valueOf(value));
    }

    private Map<?, ?> map(Map<?, ?> map, String key) {
        Object value = map.get(key);

        if (value instanceof Map<?, ?> result) {
            return result;
        }

        throw new IllegalArgumentException("Manifest key must be an object: " + key);
    }

    private List<String> stringList(Map<?, ?> map, String key) {
        Object value = map.get(key);

        if (value == null) {
            return List.of();
        }

        if (!(value instanceof List<?> rawList)) {
            throw new IllegalArgumentException("Manifest key must be a list: " + key);
        }

        List<String> result = new ArrayList<>();

        for (Object entry : rawList) {
            result.add(String.valueOf(entry));
        }

        return List.copyOf(result);
    }
}