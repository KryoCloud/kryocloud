package eu.kryocloud.launcher.dependency;

import java.nio.file.Path;

public record Artifact(String groupId, String artifactId, String version) {

    public Artifact {
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("groupId must not be blank");
        }

        if (artifactId == null || artifactId.isBlank()) {
            throw new IllegalArgumentException("artifactId must not be blank");
        }

        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version must not be blank");
        }
    }

    public String fileName() {
        return artifactId + "-" + version + ".jar";
    }

    public String path() {
        return groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + fileName();
    }

    public Path mavenPath(Path repository) {
        return repository.resolve(path());
    }

    public String coordinates() {
        return groupId + ":" + artifactId + ":" + version;
    }

}
