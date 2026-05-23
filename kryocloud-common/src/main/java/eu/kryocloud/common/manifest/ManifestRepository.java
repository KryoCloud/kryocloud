package eu.kryocloud.common.manifest;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ManifestRepository {

    private final ConcurrentMap<String, URI> manifests = new ConcurrentHashMap<>();

    public static ManifestRepository defaults() {
        ManifestRepository repository = new ManifestRepository();
        repository.register("paper", URI.create("https://raw.githubusercontent.com/KryoCloud/manifest/master/versions/paper.yaml"));
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