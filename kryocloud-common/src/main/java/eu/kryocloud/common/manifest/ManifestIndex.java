package eu.kryocloud.common.manifest;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record ManifestIndex(URI source, List<String> versions, List<String> channels, List<ManifestCodename> codenames) {

    public ManifestIndex {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }

        versions = normalizeVersions(versions);
        channels = normalizeChannels(channels);

        if (codenames == null) {
            codenames = List.of();
        }

        codenames = List.copyOf(codenames);
    }

    public List<String> availableSoftware() {
        return versions;
    }

    public String latestCodename() {
        if (codenames.isEmpty()) {
            return "unknown";
        }

        ManifestCodename codename = codenames.getFirst();
        String version = codename.versions().isEmpty() ? "" : " " + codename.versions().getFirst();

        return codename.name() + version;
    }

    private static List<String> normalizeVersions(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("versions must not be empty");
        }

        Set<String> normalized = new LinkedHashSet<>();

        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }

            normalized.add(value.trim().toLowerCase());
        }

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("versions must contain at least one valid entry");
        }

        return List.copyOf(normalized);
    }

    private static List<String> normalizeChannels(List<String> values) {
        if (values == null) {
            return List.of();
        }

        Set<String> normalized = new LinkedHashSet<>();

        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }

            normalized.add(value.trim().toLowerCase());
        }

        return List.copyOf(normalized);
    }
}
