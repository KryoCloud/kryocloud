package eu.kryocloud.common.manifest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class ManifestClient {

    private final HttpClient httpClient;
    private final ManifestParser parser;
    private final Duration timeout;

    public ManifestClient(Duration timeout) {
        if (timeout == null) {
            throw new IllegalArgumentException("timeout must not be null");
        }

        this.timeout = timeout;
        this.parser = new ManifestParser();
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    public SoftwareManifest fetch(URI uri) throws Exception {
        if (uri == null) {
            throw new IllegalArgumentException("uri must not be null");
        }

        HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Manifest request failed with HTTP " + response.statusCode() + " for " + uri);
        }

        return parser.parse(response.body());
    }
}