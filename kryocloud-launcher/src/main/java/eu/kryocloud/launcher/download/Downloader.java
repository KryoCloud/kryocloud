package eu.kryocloud.launcher.download;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public final class Downloader {

    private final HttpClient client;

    public Downloader() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public Path download(URI uri, Path target) {
        if (uri == null) {
            throw new IllegalArgumentException("uri must not be null");
        }

        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }

        if (Files.isRegularFile(target)) {
            return target;
        }

        try {
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMinutes(2))
                    .GET()
                    .build();
            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tmp));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                Files.deleteIfExists(tmp);
                throw new IllegalStateException("Download failed with HTTP " + response.statusCode() + ": " + uri);
            }

            Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            return target;
        } catch (IOException exception) {
            throw new RuntimeException("Failed to download " + uri + " to " + target, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while downloading " + uri, exception);
        }
    }

}
