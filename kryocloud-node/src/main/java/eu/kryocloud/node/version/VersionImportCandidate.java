package eu.kryocloud.node.version;

import java.nio.file.Path;

public record VersionImportCandidate(Path source, String suggestedSoftware, String suggestedVersion) {

    public VersionImportCandidate {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }

        if (suggestedSoftware == null || suggestedSoftware.isBlank()) {
            throw new IllegalArgumentException("suggestedSoftware must not be blank");
        }

        if (suggestedVersion == null || suggestedVersion.isBlank()) {
            throw new IllegalArgumentException("suggestedVersion must not be blank");
        }
    }
}