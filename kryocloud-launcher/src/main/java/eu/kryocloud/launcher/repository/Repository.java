package eu.kryocloud.launcher.repository;

import eu.kryocloud.launcher.dependency.Artifact;

import java.net.URI;

public record Repository(String id, URI baseUri) {

    public Repository {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }

        if (baseUri == null) {
            throw new IllegalArgumentException("baseUri must not be null");
        }
    }

    public URI resolve(Artifact artifact) {
        if (artifact == null) {
            throw new IllegalArgumentException("artifact must not be null");
        }

        String base = baseUri.toString();

        if (!base.endsWith("/")) {
            base = base + "/";
        }

        return URI.create(base + artifact.path());
    }

}
