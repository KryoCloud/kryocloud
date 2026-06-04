package eu.kryocloud.common.manifest;

import org.yaml.snakeyaml.Yaml;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ManifestIndexClient {

    private final HttpClient httpClient;
    private final Duration timeout;

    public ManifestIndexClient(Duration timeout) {
        if (timeout == null) {
            throw new IllegalArgumentException("timeout must not be null");
        }

        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    public ManifestIndex fetch(URI uri) throws Exception {
        if (uri == null) {
            throw new IllegalArgumentException("uri must not be null");
        }

        HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Manifest index request failed with HTTP " + response.statusCode() + " for " + uri);
        }

        return parse(uri, response.body());
    }

    private ManifestIndex parse(URI source, String yamlContent) {
        if (yamlContent == null || yamlContent.isBlank()) {
            throw new IllegalArgumentException("Manifest index content must not be blank");
        }

        Object loaded = new Yaml().load(stripBom(yamlContent));

        if (!(loaded instanceof Map<?, ?> root)) {
            throw new IllegalArgumentException("Manifest index root must be a YAML object");
        }

        List<String> versions = stringList(root, "versions");

        if (versions.isEmpty()) {
            versions = stringList(root, "available");
        }

        if (versions.isEmpty()) {
            throw new IllegalArgumentException("Manifest index must contain a 'versions' list");
        }

        List<String> channels = stringList(root, "channels");
        List<ManifestCodename> codenames = codenames(root.get("codenames"));

        return new ManifestIndex(source, versions, channels, codenames);
    }

    private List<ManifestCodename> codenames(Object value) {
        if (value == null) {
            return List.of();
        }

        if (!(value instanceof List<?> rawList)) {
            throw new IllegalArgumentException("Manifest index key 'codenames' must be a list");
        }

        List<ManifestCodename> result = new ArrayList<>();

        for (Object entry : rawList) {
            if (!(entry instanceof Map<?, ?> codenameRoot)) {
                throw new IllegalArgumentException("Manifest codename entry must be an object");
            }

            Object rawName = codenameRoot.get("name");

            if (rawName == null || String.valueOf(rawName).isBlank()) {
                throw new IllegalArgumentException("Manifest codename entry is missing name");
            }

            result.add(new ManifestCodename(String.valueOf(rawName), stringList(codenameRoot, "versions")));
        }

        return List.copyOf(result);
    }

    private List<String> stringList(Map<?, ?> map, String key) {
        Object value = map.get(key);

        if (value == null) {
            return List.of();
        }

        if (!(value instanceof List<?> rawList)) {
            throw new IllegalArgumentException("Manifest index key '" + key + "' must be a list");
        }

        List<String> result = new ArrayList<>();

        for (Object entry : rawList) {
            if (entry == null) {
                continue;
            }

            String valueString = String.valueOf(entry).trim();

            if (valueString.isBlank()) {
                continue;
            }

            result.add(valueString);
        }

        return List.copyOf(result);
    }

    private String stripBom(String value) {
        if (value.startsWith("\uFEFF")) {
            return value.substring(1);
        }

        return value;
    }
}
