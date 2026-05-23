package eu.kryocloud.node.version;

import eu.kryocloud.common.logging.KryoLogger;
import eu.kryocloud.common.manifest.ManifestClient;
import eu.kryocloud.common.manifest.ManifestRepository;
import eu.kryocloud.common.manifest.SoftwareManifest;
import eu.kryocloud.common.manifest.SoftwareVersion;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

public final class NodeVersionStorage {

    private static final KryoLogger LOGGER = KryoLogger.logger("VersionStorage");

    private final Path rootDirectory;
    private final Path templatesDirectory;
    private final ManifestRepository manifestRepository;
    private final ManifestClient manifestClient;
    private final HttpClient httpClient;
    private final Duration timeout;

    public NodeVersionStorage(Path rootDirectory, Path templatesDirectory, ManifestRepository manifestRepository, Duration timeout) {
        if (rootDirectory == null) {
            throw new IllegalArgumentException("rootDirectory must not be null");
        }

        if (templatesDirectory == null) {
            throw new IllegalArgumentException("templatesDirectory must not be null");
        }

        if (manifestRepository == null) {
            throw new IllegalArgumentException("manifestRepository must not be null");
        }

        if (timeout == null) {
            throw new IllegalArgumentException("timeout must not be null");
        }

        this.rootDirectory = rootDirectory;
        this.templatesDirectory = templatesDirectory;
        this.manifestRepository = manifestRepository;
        this.timeout = timeout;
        this.manifestClient = new ManifestClient(timeout);
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    public VersionInstallResult installFromManifest(String software, String requestedVersion, boolean updateLatest) {
        validateSoftware(software);

        try {
            Files.createDirectories(rootDirectory);

            URI manifestUri = manifestRepository.resolve(software);
            SoftwareManifest manifest = manifestClient.fetch(manifestUri);
            SoftwareVersion version = manifest.resolve(requestedVersion);
            Path softwareDirectory = softwareDirectory(software);
            Path versionDirectory = versionDirectory(software, version.version());
            Path serverJar = versionDirectory.resolve("server.jar");
            Path flatJar = softwareDirectory.resolve(software.toLowerCase() + "-" + version.version() + ".jar");

            Files.createDirectories(versionDirectory);
            download(version.link(), serverJar);
            Files.copy(serverJar, flatJar, StandardCopyOption.REPLACE_EXISTING);
            writeArguments(versionDirectory, version.javaFlags());
            writeVersionProperties(versionDirectory, software, version.version(), version.javaVersion(), version.link().toString());

            boolean latestUpdated = updateLatestIfNeeded(software, version.version(), serverJar, updateLatest);
            LOGGER.success("Installed " + software + " " + version.version() + " into " + versionDirectory);

            return new VersionInstallResult(software.toLowerCase(), version.version(), versionDirectory, serverJar, flatJar, version.javaVersion(), version.javaFlags(), latestUpdated);
        } catch (Exception exception) {
            LOGGER.error("Failed to install " + software + ": " + exception.getMessage(), exception);
            throw new RuntimeException("Failed to install " + software, exception);
        }
    }

    public VersionInstallResult installLocalJar(String software, String version, Path jarFile, boolean updateLatest) {
        validateSoftware(software);
        validateVersion(version);

        if (jarFile == null) {
            throw new IllegalArgumentException("jarFile must not be null");
        }

        try {
            if (!Files.exists(jarFile)) {
                throw new IllegalStateException("Jar file does not exist: " + jarFile);
            }

            Files.createDirectories(rootDirectory);

            Path softwareDirectory = softwareDirectory(software);
            Path versionDirectory = versionDirectory(software, version);
            Path serverJar = versionDirectory.resolve("server.jar");
            Path flatJar = softwareDirectory.resolve(software.toLowerCase() + "-" + version + ".jar");

            Files.createDirectories(versionDirectory);
            Files.copy(jarFile, serverJar, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(jarFile, flatJar, StandardCopyOption.REPLACE_EXISTING);
            writeArguments(versionDirectory, List.of());
            writeVersionProperties(versionDirectory, software, version, 21, jarFile.toAbsolutePath().toString());

            boolean latestUpdated = updateLatestIfNeeded(software, version, serverJar, updateLatest);
            LOGGER.success("Imported " + jarFile + " as " + software + " " + version);

            return new VersionInstallResult(software.toLowerCase(), version, versionDirectory, serverJar, flatJar, 21, List.of(), latestUpdated);
        } catch (Exception exception) {
            LOGGER.error("Failed to import local jar " + jarFile + ": " + exception.getMessage(), exception);
            throw new RuntimeException("Failed to import local jar", exception);
        }
    }

    public void materializeTemplate(String software, String version, String templateName) {
        validateSoftware(software);
        validateVersion(version);
        validateTemplate(templateName);

        try {
            Path templateDirectory = templatesDirectory.resolve(templateName);
            Path sourceJar = resolveServerJar(software, version);
            Path sourceArguments = resolveArguments(software, version);
            Path sourceVersionProperties = resolveVersionProperties(software, version);

            Files.createDirectories(templateDirectory);
            Files.copy(sourceJar, templateDirectory.resolve("server.jar"), StandardCopyOption.REPLACE_EXISTING);

            if (Files.exists(sourceArguments)) {
                Files.copy(sourceArguments, templateDirectory.resolve("java-flags.txt"), StandardCopyOption.REPLACE_EXISTING);
            }

            if (!Files.exists(sourceArguments)) {
                Files.write(templateDirectory.resolve("java-flags.txt"), List.of());
            }

            if (Files.exists(sourceVersionProperties)) {
                Files.copy(sourceVersionProperties, templateDirectory.resolve("version.properties"), StandardCopyOption.REPLACE_EXISTING);
                writeJavaVersionFile(templateDirectory, sourceVersionProperties);
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to materialize Minecraft template " + templateName + ": " + exception.getMessage(), exception);
            throw new RuntimeException("Failed to materialize Minecraft template " + templateName, exception);
        }
    }

    public boolean installed(String software, String version) {
        validateSoftware(software);
        validateVersion(version);

        if ("latest".equalsIgnoreCase(version)) {
            return Files.exists(latestJar(software));
        }

        return Files.exists(versionDirectory(software, version).resolve("server.jar"));
    }

    public List<VersionImportCandidate> pendingImports() {
        try {
            Files.createDirectories(rootDirectory);

            try (Stream<Path> paths = Files.list(rootDirectory)) {
                return paths.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith(".jar")).sorted(Comparator.comparing(Path::getFileName)).map(this::candidateFromJar).toList();
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to scan pending imports: " + exception.getMessage(), exception);
            return List.of();
        }
    }

    public Optional<String> latestVersion(String software) {
        validateSoftware(software);

        Path propertiesPath = softwareDirectory(software).resolve("latest.properties");

        if (!Files.exists(propertiesPath)) {
            return Optional.empty();
        }

        try (var input = Files.newInputStream(propertiesPath)) {
            Properties properties = new Properties();
            properties.load(input);

            String version = properties.getProperty("version");

            if (version == null || version.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(version);
        } catch (Exception exception) {
            LOGGER.warn("Failed to read latest for " + software + ": " + exception.getMessage());
            return Optional.empty();
        }
    }

    public Path rootDirectory() {
        return rootDirectory;
    }

    private VersionImportCandidate candidateFromJar(Path jarFile) {
        String fileName = jarFile.getFileName().toString();
        String baseName = fileName.substring(0, fileName.length() - ".jar".length());
        int separator = baseName.indexOf('-');

        if (separator < 1 || separator >= baseName.length() - 1) {
            return new VersionImportCandidate(jarFile, baseName, "custom");
        }

        return new VersionImportCandidate(jarFile, baseName.substring(0, separator), baseName.substring(separator + 1));
    }

    private Path resolveServerJar(String software, String version) {
        if ("latest".equalsIgnoreCase(version)) {
            Path latest = latestJar(software);

            if (!Files.exists(latest)) {
                throw new IllegalStateException("Latest jar is not installed for " + software);
            }

            return latest;
        }

        Path serverJar = versionDirectory(software, version).resolve("server.jar");

        if (!Files.exists(serverJar)) {
            throw new IllegalStateException("Version is not installed: " + software + " " + version);
        }

        return serverJar;
    }

    private Path resolveArguments(String software, String version) {
        if ("latest".equalsIgnoreCase(version)) {
            Optional<String> latestVersion = latestVersion(software);

            if (latestVersion.isPresent()) {
                return versionDirectory(software, latestVersion.get()).resolve("arguments.txt");
            }

            return softwareDirectory(software).resolve("arguments.txt");
        }

        return versionDirectory(software, version).resolve("arguments.txt");
    }

    private Path resolveVersionProperties(String software, String version) {
        if ("latest".equalsIgnoreCase(version)) {
            Optional<String> latestVersion = latestVersion(software);

            if (latestVersion.isPresent()) {
                return versionDirectory(software, latestVersion.get()).resolve("version.properties");
            }

            return softwareDirectory(software).resolve("version.properties");
        }

        return versionDirectory(software, version).resolve("version.properties");
    }

    private void writeJavaVersionFile(Path templateDirectory, Path versionProperties) throws Exception {
        Properties properties = new Properties();

        try (var input = Files.newInputStream(versionProperties)) {
            properties.load(input);
        }

        String javaVersion = properties.getProperty("javaVersion");

        if (javaVersion == null || javaVersion.isBlank()) {
            return;
        }

        Files.writeString(templateDirectory.resolve("java-version.txt"), javaVersion + "\n");
    }

    private boolean updateLatestIfNeeded(String software, String version, Path serverJar, boolean updateLatest) throws Exception {
        Path latest = latestJar(software);

        if (!Files.exists(latest)) {
            writeLatest(software, version, serverJar);
            return true;
        }

        if (!updateLatest) {
            return false;
        }

        writeLatest(software, version, serverJar);
        return true;
    }

    private void writeLatest(String software, String version, Path serverJar) throws Exception {
        Path softwareDirectory = softwareDirectory(software);
        Path latest = latestJar(software);
        Path propertiesPath = softwareDirectory.resolve("latest.properties");

        Files.copy(serverJar, latest, StandardCopyOption.REPLACE_EXISTING);

        Properties properties = new Properties();
        properties.setProperty("software", software.toLowerCase());
        properties.setProperty("version", version);

        try (var output = Files.newOutputStream(propertiesPath)) {
            properties.store(output, "KryoCloud latest version");
        }
    }

    private void download(URI source, Path target) throws Exception {
        Path temporaryFile = target.resolveSibling(target.getFileName() + ".download");

        Files.deleteIfExists(temporaryFile);

        HttpRequest request = HttpRequest.newBuilder(source).timeout(timeout).GET().build();
        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(temporaryFile));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            Files.deleteIfExists(temporaryFile);
            throw new IllegalStateException("Download failed with HTTP " + response.statusCode() + " for " + source);
        }

        Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private void writeArguments(Path versionDirectory, List<String> arguments) throws Exception {
        Files.write(versionDirectory.resolve("arguments.txt"), arguments);
    }

    private void writeVersionProperties(Path versionDirectory, String software, String version, int javaVersion, String source) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("software", software.toLowerCase());
        properties.setProperty("version", version);
        properties.setProperty("javaVersion", String.valueOf(javaVersion));
        properties.setProperty("source", source);

        try (var output = Files.newOutputStream(versionDirectory.resolve("version.properties"))) {
            properties.store(output, "KryoCloud version metadata");
        }
    }

    private Path softwareDirectory(String software) {
        return rootDirectory.resolve(software.toLowerCase());
    }

    private Path versionDirectory(String software, String version) {
        return softwareDirectory(software).resolve(version);
    }

    private Path latestJar(String software) {
        return softwareDirectory(software).resolve(software.toLowerCase() + "-latest.jar");
    }

    private void validateSoftware(String software) {
        if (software == null || software.isBlank()) {
            throw new IllegalArgumentException("software must not be blank");
        }

        if (!software.matches("[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("software contains unsupported characters: " + software);
        }
    }

    private void validateVersion(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version must not be blank");
        }

        if (!version.matches("[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("version contains unsupported characters: " + version);
        }
    }

    private void validateTemplate(String templateName) {
        if (templateName == null || templateName.isBlank()) {
            throw new IllegalArgumentException("templateName must not be blank");
        }

        if (!templateName.matches("[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("templateName contains unsupported characters: " + templateName);
        }
    }
}
