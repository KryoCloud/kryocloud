package eu.kryocloud.launcher.repository;

import eu.kryocloud.launcher.dependency.Artifact;
import eu.kryocloud.launcher.download.Downloader;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RepositoryResolver {

    private final Path projectRoot;
    private final Path cacheDirectory;
    private final Path localRepository;
    private final Downloader downloader;
    private final List<Repository> repositories;

    public RepositoryResolver(Path projectRoot, Path cacheDirectory) {
        if (projectRoot == null) {
            throw new IllegalArgumentException("projectRoot must not be null");
        }

        if (cacheDirectory == null) {
            throw new IllegalArgumentException("cacheDirectory must not be null");
        }

        this.projectRoot = projectRoot.toAbsolutePath().normalize();
        this.cacheDirectory = cacheDirectory.toAbsolutePath().normalize();
        this.localRepository = Path.of(System.getProperty("user.home"), ".m2", "repository").toAbsolutePath().normalize();
        this.downloader = new Downloader();
        this.repositories = List.of(
                new Repository("central", URI.create("https://repo.maven.apache.org/maven2/")),
                new Repository("sonatype-snapshots", URI.create("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        );
    }

    public List<Path> resolveAll(List<Artifact> artifacts) {
        if (artifacts == null) {
            throw new IllegalArgumentException("artifacts must not be null");
        }

        List<Path> files = new ArrayList<>();

        for (Artifact artifact : artifacts) {
            files.add(resolve(artifact));
        }

        return List.copyOf(files);
    }

    public Path resolve(Artifact artifact) {
        if (artifact == null) {
            throw new IllegalArgumentException("artifact must not be null");
        }

        Optional<Path> projectJar = projectJar(artifact);

        if (projectJar.isPresent()) {
            return projectJar.get();
        }

        Path cached = cacheDirectory.resolve(artifact.path());

        if (Files.isRegularFile(cached)) {
            return cached;
        }

        Path local = artifact.mavenPath(localRepository);

        if (Files.isRegularFile(local)) {
            return local;
        }

        for (Repository repository : repositories) {
            try {
                System.out.println("Downloading " + artifact.coordinates() + " from " + repository.id());
                return downloader.download(repository.resolve(artifact), cached);
            } catch (RuntimeException ignored) {
            }
        }

        throw new IllegalStateException("Could not resolve dependency " + artifact.coordinates() + ". Run mvn clean package once or install the artifact into ~/.m2/repository.");
    }

    private Optional<Path> projectJar(Artifact artifact) {
        if (!artifact.groupId().equals("eu.kryocloud")) {
            return Optional.empty();
        }

        Path module = projectRoot.resolve(artifact.artifactId());
        Path target = module.resolve("target");
        List<Path> candidates = List.of(
                target.resolve(artifact.fileName()),
                target.resolve(artifact.artifactId() + ".jar")
        );

        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return Optional.of(candidate);
            }
        }

        return newestProjectJar(target, artifact);
    }

    private Optional<Path> newestProjectJar(Path target, Artifact artifact) {
        if (!Files.isDirectory(target)) {
            return Optional.empty();
        }

        try (var paths = Files.list(target)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(artifact.artifactId()))
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .filter(path -> !path.getFileName().toString().contains("sources"))
                    .filter(path -> !path.getFileName().toString().contains("javadoc"))
                    .findFirst();
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

}
