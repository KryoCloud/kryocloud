package eu.kryocloud.wrapper.instance.readiness;

import eu.kryocloud.api.instance.ICloudInstance;
import eu.kryocloud.common.logging.KryoLogger;
import eu.kryocloud.network.protocol.CloudServiceType;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public final class InstanceReadinessProbe {

    private static final KryoLogger LOGGER = KryoLogger.logger("ReadinessProbe");

    private static final List<Pattern> SERVER_READY_PATTERNS = List.of(Pattern.compile("Done \\(.*\\)! For help, type", Pattern.CASE_INSENSITIVE), Pattern.compile("For help, type", Pattern.CASE_INSENSITIVE), Pattern.compile("Timings Reset", Pattern.CASE_INSENSITIVE));
    private static final List<Pattern> PROXY_READY_PATTERNS = List.of(Pattern.compile("Listening on", Pattern.CASE_INSENSITIVE), Pattern.compile("Listening for connections", Pattern.CASE_INSENSITIVE), Pattern.compile("Started proxy", Pattern.CASE_INSENSITIVE), Pattern.compile("Done", Pattern.CASE_INSENSITIVE));
    private static final List<Pattern> FAILURE_PATTERNS = List.of(Pattern.compile("Unrecognized VM option", Pattern.CASE_INSENSITIVE), Pattern.compile("Could not create the Java Virtual Machine", Pattern.CASE_INSENSITIVE), Pattern.compile("UnsupportedClassVersionError", Pattern.CASE_INSENSITIVE), Pattern.compile("You need to agree to the EULA", Pattern.CASE_INSENSITIVE), Pattern.compile("Failed to bind to port", Pattern.CASE_INSENSITIVE), Pattern.compile("Address already in use", Pattern.CASE_INSENSITIVE), Pattern.compile("OutOfMemoryError", Pattern.CASE_INSENSITIVE), Pattern.compile("Exception in thread", Pattern.CASE_INSENSITIVE));

    private final Duration pollInterval;

    public InstanceReadinessProbe(Duration pollInterval) {
        if (pollInterval == null) {
            throw new IllegalArgumentException("pollInterval must not be null");
        }

        if (pollInterval.isZero() || pollInterval.isNegative()) {
            throw new IllegalArgumentException("pollInterval must be positive");
        }

        this.pollInterval = pollInterval;
    }

    public InstanceReadinessResult awaitReady(ICloudInstance instance, CloudServiceType serviceType, Duration timeout) {
        if (instance == null) {
            throw new IllegalArgumentException("instance must not be null");
        }

        if (serviceType == null) {
            throw new IllegalArgumentException("serviceType must not be null");
        }

        if (timeout == null) {
            throw new IllegalArgumentException("timeout must not be null");
        }

        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }

        long deadline = System.currentTimeMillis() + timeout.toMillis();
        String logs = "";

        while (System.currentTimeMillis() <= deadline) {
            logs = safeLogs(instance);

            Optional<String> failure = find(FAILURE_PATTERNS, logs);

            if (failure.isPresent()) {
                return InstanceReadinessResult.failed("Minecraft startup failed: " + failure.get(), tail(logs, 350));
            }

            Optional<String> ready = find(readyPatterns(serviceType), logs);

            if (ready.isPresent()) {
                return InstanceReadinessResult.ready("Minecraft instance reached readiness: " + ready.get(), tail(logs, 350));
            }

            if (!online(instance)) {
                return InstanceReadinessResult.failed("Minecraft process stopped before readiness", tail(logs, 350));
            }

            sleep(pollInterval);
        }

        return InstanceReadinessResult.failed("Minecraft instance did not become ready within " + timeout.toSeconds() + "s", tail(logs, 350));
    }

    private List<Pattern> readyPatterns(CloudServiceType serviceType) {
        if (serviceType == CloudServiceType.PROXY) {
            return PROXY_READY_PATTERNS;
        }

        return SERVER_READY_PATTERNS;
    }

    private Optional<String> find(List<Pattern> patterns, String logs) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(logs).find()) {
                return Optional.of(pattern.pattern());
            }
        }

        return Optional.empty();
    }

    private boolean online(ICloudInstance instance) {
        try {
            return instance.isOnline();
        } catch (Exception exception) {
            LOGGER.warn("Failed to query Minecraft instance process state: " + exception.getMessage());
            return false;
        }
    }

    private String safeLogs(ICloudInstance instance) {
        try {
            return instance.logs();
        } catch (Exception exception) {
            return "";
        }
    }

    private String tail(String value, int maxCharacters) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = value.replace('\r', ' ').trim();

        if (normalized.length() <= maxCharacters) {
            return normalized;
        }

        return normalized.substring(normalized.length() - maxCharacters);
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
