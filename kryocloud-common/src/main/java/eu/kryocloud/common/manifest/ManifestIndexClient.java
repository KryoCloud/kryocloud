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

        Object available = root.get("available");

        if (!(available instanceof List<?> rawAvailable)) {
            throw new IllegalArgumentException("Manifest index key 'available' must be a list");
        }

        List<String> software = new ArrayList<>();

        for (Object entry : rawAvailable) {
            if (entry == null) {
                continue;
            }

            String value = String.valueOf(entry).trim();

            if (value.isBlank()) {
                continue;
            }

            software.add(value);
        }

        return new ManifestIndex(source, software);
    }

    private String stripBom(String value) {
        if (value.startsWith("\uFEFF")) {
            return value.substring(1);
        }

        return value;
    }
}
