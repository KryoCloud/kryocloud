package eu.kryocloud.common.manifest;

import java.util.Map;

public record SoftwareManifest(SoftwareType type, String latestVersion, Map<String, SoftwareVersion> versions) {

    public SoftwareManifest {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }

        if (latestVersion == null || latestVersion.isBlank()) {
            throw new IllegalArgumentException("latestVersion must not be blank");
        }

        if (versions == null || versions.isEmpty()) {
            throw new IllegalArgumentException("versions must not be empty");
        }

        versions = Map.copyOf(versions);
    }

    public SoftwareVersion resolve(String requestedVersion) {
        String version = requestedVersion;

        if (version == null || version.isBlank()) {
            version = latestVersion;
        }

        if ("latest".equalsIgnoreCase(version)) {
            version = latestVersion;
        }

        SoftwareVersion softwareVersion = versions.get(version);

        if (softwareVersion == null) {
            throw new IllegalArgumentException("Version is not available in manifest: " + version);
        }

        return softwareVersion;
    }
}