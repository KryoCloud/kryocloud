package eu.kryocloud.common.manifest;

import eu.kryocloud.common.logging.KryoLogger;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ManifestRepository {

    private static final KryoLogger LOGGER = KryoLogger.logger("ManifestRepository");
    private static final URI DEFAULT_INDEX_URI = URI.create("https://raw.githubusercontent.com/KryoCloud/manifest/master/manifest.yaml");
    private static final URI DEFAULT_VERSIONS_BASE_URI = URI.create("https://raw.githubusercontent.com/KryoCloud/manifest/master/versions/");
    private static final List<String> FALLBACK_SOFTWARE = List.of("bungeecord", "folia", "leaf", "paper", "purpur", "spigot", "velocity");

    private final ConcurrentMap<String, URI> manifests = new ConcurrentHashMap<>();
    private volatile ManifestIndex index;

    private final ManifestIndexClient indexClient;
    private final URI indexUri;
    private final URI versionsBaseUri;

    public ManifestRepository() {
        this(DEFAULT_INDEX_URI, DEFAULT_VERSIONS_BASE_URI, Duration.ofSeconds(8));
    }

    public ManifestRepository(URI indexUri, URI versionsBaseUri, Duration timeout) {
        if (indexUri == null) {
            throw new IllegalArgumentException("indexUri must not be null");
        }

        if (versionsBaseUri == null) {
            throw new IllegalArgumentException("versionsBaseUri must not be null");
        }

        if (timeout == null) {
            throw new IllegalArgumentException("timeout must not be null");
        }

        this.indexUri = indexUri;
        this.versionsBaseUri = versionsBaseUri;
        this.indexClient = new ManifestIndexClient(timeout);
    }

    public static ManifestRepository defaults() {
        ManifestRepository repository = new ManifestRepository();
        repository.refreshFromIndex();

        if (repository.manifests.isEmpty()) {
            repository.registerFallbacks();
        }

        return repository;
    }

    public int refreshFromIndex() {
        try {
            ManifestIndex index = indexClient.fetch(indexUri);

            for (String software : index.availableSoftware()) {
                registerOrUpdate(software, manifestUri(software));
            }

            this.index = index;

            String suffix = index.channels().isEmpty() ? "" : " with " + index.channels().size() + " channel(s)";
            LOGGER.success("Loaded " + index.availableSoftware().size() + " Minecraft software manifest(s) from " + index.source() + suffix);
            return index.availableSoftware().size();
        } catch (Exception exception) {
            LOGGER.warn("Failed to fetch dynamic manifest index from " + indexUri + ": " + exception.getMessage());
            registerFallbacks();
            return 0;
        }
    }

    public void register(String software, URI manifestUri) {
        validateSoftware(software);

        if (manifestUri == null) {
            throw new IllegalArgumentException("manifestUri must not be null");
        }

        String normalized = software.toLowerCase();
        URI previous = manifests.putIfAbsent(normalized, manifestUri);

        if (previous == null) {
            return;
        }

        if (previous.equals(manifestUri)) {
            return;
        }

        throw new IllegalStateException("Manifest already registered for software: " + software);
    }

    public void registerOrUpdate(String software, URI manifestUri) {
        validateSoftware(software);

        if (manifestUri == null) {
            throw new IllegalArgumentException("manifestUri must not be null");
        }

        manifests.put(software.toLowerCase(), manifestUri);
    }

    public URI resolve(String software) {
        validateSoftware(software);

        URI uri = manifests.get(software.toLowerCase());

        if (uri != null) {
            return uri;
        }

        refreshFromIndex();

        URI refreshedUri = manifests.get(software.toLowerCase());

        if (refreshedUri != null) {
            return refreshedUri;
        }

        throw new IllegalArgumentException("Unknown software manifest: " + software + ". Available: " + String.join(", ", availableSoftware()));
    }

    public List<String> availableSoftware() {
        return manifests.keySet().stream().sorted().toList();
    }

    public List<String> channels() {
        ManifestIndex current = index;

        if (current == null) {
            return List.of();
        }

        return current.channels();
    }

    public List<ManifestCodename> codenames() {
        ManifestIndex current = index;

        if (current == null) {
            return List.of();
        }

        return current.codenames();
    }

    public String latestCodename() {
        ManifestIndex current = index;

        if (current == null) {
            return "unknown";
        }

        return current.latestCodename();
    }

    public URI indexUri() {
        return indexUri;
    }

    public URI versionsBaseUri() {
        return versionsBaseUri;
    }

    public Map<String, URI> manifests() {
        return Map.copyOf(manifests);
    }

    private void registerFallbacks() {
        for (String software : FALLBACK_SOFTWARE) {
            manifests.putIfAbsent(software, manifestUri(software));
        }
    }

    private URI manifestUri(String software) {
        return versionsBaseUri.resolve(software.toLowerCase() + ".yaml");
    }

    private void validateSoftware(String software) {
        if (software == null || software.isBlank()) {
            throw new IllegalArgumentException("software must not be blank");
        }

        if (!software.matches("[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("software contains unsupported characters: " + software);
        }
    }
}
