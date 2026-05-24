package eu.kryocloud.wrapper.instance.runtime;

import eu.kryocloud.common.logging.KryoLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class JavaRuntimeResolver {

    private static final KryoLogger LOGGER = KryoLogger.logger("JavaRuntime");
    private static final Pattern VERSION_PATTERN = Pattern.compile("version \"([^\"]+)\"");

    private final Path runtimeDirectory;
    private final Duration probeTimeout;
    private final TemurinJavaInstaller installer;

    public JavaRuntimeResolver(Path runtimeDirectory, Duration probeTimeout) {
        if (runtimeDirectory == null) {
            throw new IllegalArgumentException("runtimeDirectory must not be null");
        }

        if (probeTimeout == null) {
            throw new IllegalArgumentException("probeTimeout must not be null");
        }

        this.runtimeDirectory = runtimeDirectory;
        this.probeTimeout = probeTimeout;
        this.installer = new TemurinJavaInstaller(runtimeDirectory, Duration.ofMinutes(10));
    }

    public JavaRuntime resolve(int requiredMajorVersion, List<String> requestedFlags) {
        if (requiredMajorVersion < 1) {
            throw new IllegalArgumentException("requiredMajorVersion must be greater than 0");
        }

        if (requestedFlags == null) {
            throw new IllegalArgumentException("requestedFlags must not be null");
        }

        for (String executable : managedCandidates(requiredMajorVersion)) {
            JavaRuntime runtime = runtime(executable, requiredMajorVersion, requestedFlags);

            if (runtime != null) {
                return runtime;
            }
        }

        try {
            Path managedExecutable = installer.ensureInstalled(requiredMajorVersion);
            JavaRuntime managedRuntime = runtime(managedExecutable.toAbsolutePath().toString(), requiredMajorVersion, requestedFlags);

            if (managedRuntime != null) {
                return managedRuntime;
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("Managed Temurin Java " + requiredMajorVersion + " installation failed: " + exception.getMessage());
        }

        for (String executable : fallbackCandidates(requiredMajorVersion)) {
            JavaRuntime runtime = runtime(executable, requiredMajorVersion, requestedFlags);

            if (runtime != null) {
                return runtime;
            }
        }

        throw new IllegalStateException("No exact Java " + requiredMajorVersion + " runtime found. KryoCloud tried managed Temurin under " + runtimeDirectory + " and exact-version environment candidates.");
    }

    private JavaRuntime runtime(String executable, int requiredMajorVersion, List<String> requestedFlags) {
        int majorVersion = probeMajorVersion(executable);

        if (majorVersion != requiredMajorVersion) {
            LOGGER.warn("Skipping Java runtime " + executable + " because it is Java " + majorVersion + " but this Minecraft version requires Java " + requiredMajorVersion);
            return null;
        }

        FlagProbeResult flags = sanitizeFlags(executable, requestedFlags);
        return new JavaRuntime(executable, majorVersion, flags.acceptedFlags(), flags.rejectedFlags());
    }

    private List<String> managedCandidates(int requiredMajorVersion) {
        List<String> candidates = new ArrayList<>();
        addDirectoryCandidate(candidates, runtimeDirectory.resolve("java-" + requiredMajorVersion));
        addDirectoryCandidate(candidates, runtimeDirectory.resolve("jdk-" + requiredMajorVersion));
        addMatchingRuntimeDirectories(candidates, requiredMajorVersion);
        return candidates.stream().distinct().toList();
    }

    private List<String> fallbackCandidates(int requiredMajorVersion) {
        List<String> candidates = new ArrayList<>();

        addEnvironmentCandidate(candidates, "JAVA_" + requiredMajorVersion + "_HOME");
        addEnvironmentCandidate(candidates, "JAVA" + requiredMajorVersion + "_HOME");
        addDirectoryCandidate(candidates, runtimeDirectory.resolve("java-" + requiredMajorVersion));
        addDirectoryCandidate(candidates, runtimeDirectory.resolve("jdk-" + requiredMajorVersion));
        addMatchingRuntimeDirectories(candidates, requiredMajorVersion);

        return candidates.stream().distinct().toList();
    }

    private void addEnvironmentCandidate(List<String> candidates, String environmentName) {
        String value = System.getenv(environmentName);

        if (value == null || value.isBlank()) {
            return;
        }

        addDirectoryCandidate(candidates, Path.of(value));
    }

    private void addDirectoryCandidate(List<String> candidates, Path directory) {
        Path executable = directory.resolve("bin").resolve(executableName());

        if (!Files.exists(executable)) {
            return;
        }

        candidates.add(executable.toAbsolutePath().toString());
    }

    private void addMatchingRuntimeDirectories(List<String> candidates, int requiredMajorVersion) {
        if (!Files.isDirectory(runtimeDirectory)) {
            return;
        }

        try (Stream<Path> paths = Files.list(runtimeDirectory)) {
            paths.filter(Files::isDirectory).filter(path -> path.getFileName().toString().contains(String.valueOf(requiredMajorVersion))).forEach(path -> addDirectoryCandidate(candidates, path));
        } catch (Exception exception) {
            LOGGER.warn("Failed to scan Java runtime directory " + runtimeDirectory + ": " + exception.getMessage());
        }
    }

    private int probeMajorVersion(String executable) {
        try {
            Process process = new ProcessBuilder(executable, "-version").redirectErrorStream(true).start();
            boolean finished = process.waitFor(probeTimeout.toMillis(), TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                return -1;
            }

            String output = new String(process.getInputStream().readAllBytes());
            return parseMajorVersion(output);
        } catch (Exception exception) {
            LOGGER.warn("Failed to probe Java runtime " + executable + ": " + exception.getMessage());
            return -1;
        }
    }

    private int parseMajorVersion(String output) {
        Matcher matcher = VERSION_PATTERN.matcher(output);

        if (!matcher.find()) {
            return -1;
        }

        String version = matcher.group(1);

        if (version.startsWith("1.")) {
            return parseLeadingNumber(version.substring(2));
        }

        return parseLeadingNumber(version);
    }

    private int parseLeadingNumber(String version) {
        StringBuilder number = new StringBuilder();

        for (char character : version.toCharArray()) {
            if (!Character.isDigit(character)) {
                break;
            }

            number.append(character);
        }

        if (number.isEmpty()) {
            return -1;
        }

        return Integer.parseInt(number.toString());
    }

    private FlagProbeResult sanitizeFlags(String executable, List<String> requestedFlags) {
        List<String> normalizedFlags = normalizeFlags(requestedFlags);
        List<String> accepted = new ArrayList<>();
        List<String> rejected = new ArrayList<>();

        for (String flag : normalizedFlags) {
            List<String> candidate = new ArrayList<>(accepted);
            candidate.add(flag);

            if (supportsFlags(executable, candidate)) {
                accepted.add(flag);
                continue;
            }

            rejected.add(flag);
        }

        if (!rejected.isEmpty()) {
            LOGGER.warn("Skipped unsupported JVM flags for Java " + executable + ": " + rejected);
        }

        return new FlagProbeResult(accepted, rejected);
    }

    private List<String> normalizeFlags(List<String> flags) {
        Set<String> unique = new LinkedHashSet<>();

        if (flags.contains("-XX:+UnlockExperimentalVMOptions")) {
            unique.add("-XX:+UnlockExperimentalVMOptions");
        }

        for (String flag : flags) {
            if (flag == null || flag.isBlank()) {
                continue;
            }

            if ("-XX:+UnlockExperimentalVMOptions".equals(flag)) {
                continue;
            }

            unique.add(flag);
        }

        return List.copyOf(unique);
    }

    private boolean supportsFlags(String executable, List<String> flags) {
        try {
            List<String> command = new ArrayList<>();
            command.add(executable);
            command.addAll(flags);
            command.add("-version");

            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean finished = process.waitFor(probeTimeout.toMillis(), TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                return false;
            }

            return process.exitValue() == 0;
        } catch (Exception exception) {
            return false;
        }
    }

    private String executableName() {
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return "java.exe";
        }

        return "java";
    }

    private record FlagProbeResult(List<String> acceptedFlags, List<String> rejectedFlags) {
    }
}
