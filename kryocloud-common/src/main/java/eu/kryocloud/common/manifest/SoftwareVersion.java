package eu.kryocloud.common.manifest;

import java.net.URI;
import java.util.List;

public record SoftwareVersion(String version, URI link, int javaVersion, List<String> javaFlags) {

    public SoftwareVersion {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version must not be blank");
        }

        if (link == null) {
            throw new IllegalArgumentException("link must not be null");
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