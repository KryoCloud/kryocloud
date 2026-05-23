package eu.kryocloud.node.version;

import java.nio.file.Path;
import java.util.List;

public record VersionInstallResult(String software, String version, Path versionDirectory, Path serverJar, Path flatJar, int javaVersion, List<String> javaFlags, boolean latestUpdated) {

    public VersionInstallResult {
        if (software == null || software.isBlank()) {
            throw new IllegalArgumentException("software must not be blank");
        }

        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version must not be blank");
        }

        if (versionDirectory == null) {
            throw new IllegalArgumentException("versionDirectory must not be null");
        }

        if (serverJar == null) {
            throw new IllegalArgumentException("serverJar must not be null");
        }

        if (flatJar == null) {
            throw new IllegalArgumentException("flatJar must not be null");
        }

        if (javaVersion < 1) {
            throw new IllegalArgumentException("javaVersion must be greater than 0");
        }

        if (javaFlags == null) {
            throw new IllegalArgumentException("javaFlags must not be null");
        }

        javaFlags = List.copyOf(javaFlags);
    }
}