package eu.kryocloud.common.manifest;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ManifestRepository {

    private final ConcurrentMap<String, URI> manifests = new ConcurrentHashMap<>();

    public static ManifestRepository defaults() {
        ManifestRepository repository = new ManifestRepository();

        Yaml yaml = new Yaml();

        try (InputStream in = URI.create("https://raw.githubusercontent.com/KryoCloud/manifest/master/versions.yaml").toURL().openStream()) {
            Map<String, Object> root = yaml.load(in);

            @SuppressWarnings("unchecked")
            List<String> availableVersions = (List<String>) root.get("available");

            for (String version : availableVersions) {
                repository.register(version, URI.create("https://raw.githubusercontent.com/KryoCloud/manifest/master/versions/%s.yaml".formatted(version)));
            }
        } catch (IOException exception) {
            throw new RuntimeException("can't fetch versions");
        }

        return repository;
    }

    public void register(String software, URI manifestUri) {
        if (software == null || software.isBlank()) {
            throw new IllegalArgumentException("software must not be blank");
        }

        if (manifestUri == null) {
            throw new IllegalArgumentException("manifestUri must not be null");
        }

        URI previous = manifests.putIfAbsent(software.toLowerCase(), manifestUri);

        if (previous != null) {
            throw new IllegalStateException("Manifest already registered for software: " + software);
        }
    }

    public URI resolve(String software) {
        if (software == null || software.isBlank()) {
            throw new IllegalArgumentException("software must not be blank");
        }

        URI uri = manifests.get(software.toLowerCase());

        if (uri == null) {
            throw new IllegalArgumentException("Unknown software manifest: " + software);
        }

        return uri;
    }

    public Map<String, URI> manifests() {
        return Map.copyOf(manifests);
    }
}