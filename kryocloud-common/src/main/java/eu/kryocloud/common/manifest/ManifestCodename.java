package eu.kryocloud.common.manifest;

import java.util.List;

public record ManifestCodename(String name, List<String> versions) {

    public ManifestCodename {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        if (versions == null || versions.isEmpty()) {
            throw new IllegalArgumentException("versions must not be empty");
        }

        name = name.trim();
        versions = versions.stream()
                .filter(version -> version != null && !version.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        if (versions.isEmpty()) {
            throw new IllegalArgumentException("versions must contain at least one valid entry");
        }
    }
}
