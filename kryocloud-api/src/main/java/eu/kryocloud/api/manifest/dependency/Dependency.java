package eu.kryocloud.api.manifest.dependency;

public record Dependency(String groupId, String artifactId, String version, Repository repository) {
}
