package eu.kryocloud.wrapper.instance.runtime;

import eu.kryocloud.common.logging.KryoLogger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public final class TemurinJavaInstaller {

    private static final KryoLogger LOGGER = KryoLogger.logger("TemurinInstaller");

    private final Path runtimeDirectory;
    private final HttpClient httpClient;
    private final Duration timeout;
    private final JdkArchiveExtractor archiveExtractor;

    public TemurinJavaInstaller(Path runtimeDirectory, Duration timeout) {
        if (runtimeDirectory == null) {
            throw new IllegalArgumentException("runtimeDirectory must not be null");
        }

        if (timeout == null) {
            throw new IllegalArgumentException("timeout must not be null");
        }

        this.runtimeDirectory = runtimeDirectory;
        this.timeout = timeout;
        this.archiveExtractor = new JdkArchiveExtractor();
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    public Path ensureInstalled(int majorVersion) {
        if (majorVersion < 1) {
            throw new IllegalArgumentException("majorVersion must be greater than 0");
        }

        Path targetDirectory = runtimeDirectory.resolve("java-" + majorVersion).toAbsolutePath().normalize();
        Path executable = targetDirectory.resolve("bin").resolve(executableName());

        if (Files.exists(executable)) {
            return executable;
        }

        synchronized (lock(majorVersion)) {
            if (Files.exists(executable)) {
                return executable;
            }

            install(majorVersion, targetDirectory);
            return executable;
        }
    }

    private void install(int majorVersion, Path targetDirectory) {
        try {
            Files.createDirectories(runtimeDirectory);

            Path downloadFile = runtimeDirectory.resolve("temurin-" + majorVersion + "-" + os() + "-" + arch() + archiveSuffix() + ".download").toAbsolutePath().normalize();
            Files.deleteIfExists(downloadFile);

            URI uri = downloadUri(majorVersion);
            LOGGER.warn("Downloading Temurin Java " + majorVersion + " from " + uri);
            download(uri, downloadFile);

            Path temporaryDirectory = runtimeDirectory.resolve(".extract-java-" + majorVersion + "-" + System.nanoTime()).toAbsolutePath().normalize();
            archiveExtractor.extract(downloadFile, temporaryDirectory);
            Path extractedRuntime = archiveExtractor.findJavaHome(temporaryDirectory, executableName());

            archiveExtractor.deleteRecursively(targetDirectory);
            Files.createDirectories(targetDirectory.getParent());
            Files.move(extractedRuntime, targetDirectory);

            archiveExtractor.deleteRecursively(temporaryDirectory);
            Files.deleteIfExists(downloadFile);

            LOGGER.success("Installed Temurin Java " + majorVersion + " into " + targetDirectory);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to install Temurin Java " + majorVersion + " into " + targetDirectory + ": " + exception.getMessage(), exception);
        }
    }

    private void download(URI uri, Path target) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).GET().build();
        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(target));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            Files.deleteIfExists(target);
            throw new IllegalStateException("Temurin download failed with HTTP " + response.statusCode() + " for " + uri);
        }
    }

    private URI downloadUri(int majorVersion) {
        return URI.create("https://api.adoptium.net/v3/binary/latest/" + majorVersion + "/ga/" + os() + "/" + arch() + "/jdk/hotspot/normal/eclipse");
    }

    private String os() {
        String osName = System.getProperty("os.name", "").toLowerCase();

        if (osName.contains("win")) {
            return "windows";
        }

        if (osName.contains("mac") || osName.contains("darwin")) {
            return "mac";
        }

        return "linux";
    }

    private String arch() {
        String architecture = System.getProperty("os.arch", "").toLowerCase();

        if ("amd64".equals(architecture) || "x86_64".equals(architecture)) {
            return "x64";
        }

        if ("aarch64".equals(architecture) || "arm64".equals(architecture)) {
            return "aarch64";
        }

        if (architecture.startsWith("arm")) {
            return "arm";
        }

        if (architecture.contains("ppc64le")) {
            return "ppc64le";
        }

        if (architecture.contains("s390x")) {
            return "s390x";
        }

        return architecture;
    }

    private String archiveSuffix() {
        if ("windows".equals(os())) {
            return ".zip";
        }

        return ".tar.gz";
    }

    private String executableName() {
        if ("windows".equals(os())) {
            return "java.exe";
        }

        return "java";
    }

    private Object lock(int majorVersion) {
        return ("temurin-java-" + majorVersion).intern();
    }
}
