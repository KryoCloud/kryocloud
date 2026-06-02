package eu.kryocloud.common.manifest;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record ManifestIndex(URI source, List<String> availableSoftware) {

    public ManifestIndex {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }

        if (availableSoftware == null || availableSoftware.isEmpty()) {
            throw new IllegalArgumentException("availableSoftware must not be empty");
        }

        Set<String> normalized = new LinkedHashSet<>();

        for (String software : availableSoftware) {
            if (software == null || software.isBlank()) {
                continue;
            }

            normalized.add(software.trim().toLowerCase());
        }

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("availableSoftware must contain at least one valid entry");
        }

        availableSoftware = List.copyOf(normalized);
    }
}
