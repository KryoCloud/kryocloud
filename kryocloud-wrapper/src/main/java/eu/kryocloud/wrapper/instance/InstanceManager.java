package eu.kryocloud.wrapper.instance;

import eu.kryocloud.api.instance.ICloudInstance;
import eu.kryocloud.api.instance.IInstanceManager;
import eu.kryocloud.api.screen.IScreen;
import eu.kryocloud.api.screen.IScreenManager;
import eu.kryocloud.common.logging.KryoLogger;
import eu.kryocloud.network.connection.KryoConnection;
import eu.kryocloud.network.packet.type.service.ServiceCleanupRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceCleanupResponsePacket;
import eu.kryocloud.network.packet.type.service.ServiceCommandRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceCommandResponsePacket;
import eu.kryocloud.network.packet.type.service.ServiceLogsRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceLogsResponsePacket;
import eu.kryocloud.network.packet.type.service.ServiceStartRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceStatePacket;
import eu.kryocloud.network.packet.type.service.ServiceStopRequestPacket;
import eu.kryocloud.network.protocol.CloudServiceState;
import eu.kryocloud.wrapper.instance.metrics.InstanceMetrics;
import eu.kryocloud.wrapper.instance.metrics.InstanceMetricsCollector;
import eu.kryocloud.wrapper.instance.process.InstanceProcessSpec;
import eu.kryocloud.wrapper.instance.readiness.InstanceReadinessProbe;
import eu.kryocloud.wrapper.instance.readiness.InstanceReadinessResult;
import eu.kryocloud.wrapper.instance.runtime.JavaRuntime;
import eu.kryocloud.wrapper.instance.runtime.JavaRuntimeResolver;
import eu.kryocloud.wrapper.instance.workspace.InstanceCleanupResult;
import eu.kryocloud.wrapper.instance.workspace.InstanceWorkspace;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class InstanceManager implements IInstanceManager {

    private static final KryoLogger LOGGER = KryoLogger.logger("InstanceManager");

    private final String cloudName;
    private final String wrapperId;
    private final String advertisedAddress;
    private final IScreenManager screenManager;
    private final InstanceWorkspace workspace;
    private final JavaRuntimeResolver javaRuntimeResolver;
    private final Duration startupProbeTimeout;
    private final Duration shutdownTimeout;
    private final InstanceReadinessProbe readinessProbe;
    private final InstanceMetricsCollector metricsCollector;
    private final ScheduledExecutorService watcherExecutor;
    private final ConcurrentMap<String, ICloudInstance> cloudInstances = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> screenSessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, InstanceSnapshot> snapshots = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, KryoConnection> nodeConnections = new ConcurrentHashMap<>();

    public InstanceManager(String cloudName, String wrapperId, String advertisedAddress, IScreenManager screenManager, InstanceWorkspace workspace, JavaRuntimeResolver javaRuntimeResolver, int startupProbeSeconds, int shutdownTimeoutSeconds) {
        if (cloudName == null || cloudName.isBlank()) {
            throw new IllegalArgumentException("cloudName must not be blank");
        }

        if (!cloudName.matches("[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("cloudName contains unsupported characters: " + cloudName);
        }

        if (wrapperId == null || wrapperId.isBlank()) {
            throw new IllegalArgumentException("wrapperId must not be blank");
        }

        if (advertisedAddress == null || advertisedAddress.isBlank()) {
            throw new IllegalArgumentException("advertisedAddress must not be blank");
        }

        if (screenManager == null) {
            throw new IllegalArgumentException("screenManager must not be null");
        }

        if (workspace == null) {
            throw new IllegalArgumentException("workspace must not be null");
        }

        if (javaRuntimeResolver == null) {
            throw new IllegalArgumentException("javaRuntimeResolver must not be null");
        }

        if (startupProbeSeconds < 1) {
            throw new IllegalArgumentException("startupProbeSeconds must be greater than 0");
        }

        if (shutdownTimeoutSeconds < 1) {
            throw new IllegalArgumentException("shutdownTimeoutSeconds must be greater than 0");
        }

        this.cloudName = cloudName;
        this.wrapperId = wrapperId;
        this.advertisedAddress = advertisedAddress;
        this.screenManager = screenManager;
        this.workspace = workspace;
        this.javaRuntimeResolver = javaRuntimeResolver;
        this.startupProbeTimeout = Duration.ofSeconds(startupProbeSeconds);
        this.shutdownTimeout = Duration.ofSeconds(shutdownTimeoutSeconds);
        this.readinessProbe = new InstanceReadinessProbe(Duration.ofMillis(500));
        this.metricsCollector = new InstanceMetricsCollector();
        this.watcherExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "kryocloud-instance-watchdog");
            thread.setDaemon(true);
            return thread;
        });
        this.watcherExecutor.scheduleAtFixedRate(this::watchInstancesSafely, 1L, 1L, TimeUnit.SECONDS);
    }

    @Override
    public ICloudInstance create(String name, String javaExecutable, Path workingDirectory, int minMemory, int maxMemory, List<String> jvmArgs) {
        return create(name, name, javaExecutable, workingDirectory, minMemory, maxMemory, jvmArgs);
    }

    private ICloudInstance create(String serviceId, String screenSession, String javaExecutable, Path workingDirectory, int minMemory, int maxMemory, List<String> jvmArgs) {
        validateName(serviceId);
        validateName(screenSession);

        if (javaExecutable == null || javaExecutable.isBlank()) {
            throw new IllegalArgumentException("javaExecutable must not be blank");
        }

        if (workingDirectory == null) {
            throw new IllegalArgumentException("workingDirectory must not be null");
        }

        if (minMemory < 1) {
            throw new IllegalArgumentException("minMemory must be greater than 0");
        }

        if (maxMemory < minMemory) {
            throw new IllegalArgumentException("maxMemory must be greater than or equal to minMemory");
        }

        if (jvmArgs == null) {
            throw new IllegalArgumentException("jvmArgs must not be null");
        }

        IScreen screen = screenManager.create(screenSession, workingDirectory);
        InstanceProcessSpec processSpec = new InstanceProcessSpec(serviceId, javaExecutable, workingDirectory, minMemory, maxMemory, jvmArgs, "server.jar");
        ICloudInstance instance = new CloudInstance(processSpec, screen);
        ICloudInstance existing = cloudInstances.putIfAbsent(serviceId, instance);

        if (existing != null) {
            screenManager.remove(screenSession);
            throw new IllegalStateException("Minecraft instance already exists: " + serviceId);
        }

        screenSessions.put(serviceId, screenSession);
        return instance;
    }

    @Override
    public ICloudInstance get(String name) {
        validateName(name);
        return cloudInstances.get(name);
    }

    @Override
    public void remove(String name) {
        validateName(name);

        ICloudInstance instance = cloudInstances.get(name);

        if (instance != null) {
            stopSafely(name, instance);
        }

        unregister(name);
    }

    public Optional<InstanceSnapshot> start(KryoConnection nodeConnection, ServiceStartRequestPacket packet) {
        if (nodeConnection == null) {
            throw new IllegalArgumentException("nodeConnection must not be null");
        }

        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        if (snapshots.containsKey(packet.serviceId())) {
            sendState(nodeConnection, packet, CloudServiceState.FAILED, "Minecraft instance is already running or reserved");
            return Optional.empty();
        }

        sendState(nodeConnection, packet, CloudServiceState.PREPARING, "Preparing Minecraft workspace");

        Path workingDirectory = null;

        try {
            workingDirectory = workspace.prepare(packet);
            JavaRuntime javaRuntime = javaRuntimeResolver.resolve(workspace.javaVersion(workingDirectory), workspace.javaFlags(workingDirectory));
            sendState(nodeConnection, packet, CloudServiceState.STARTING, "Starting Minecraft instance with Java " + javaRuntime.majorVersion());

            String screenSession = screenSessionName(packet);
            ICloudInstance instance = create(packet.serviceId(), screenSession, javaRuntime.executable(), workingDirectory, packet.maxMemoryMb(), packet.maxMemoryMb(), finalJvmArgs(packet, javaRuntime.acceptedFlags()));
            InstanceSnapshot snapshot = new InstanceSnapshot(packet.requestId(), packet.serviceId(), packet.groupName(), packet.templateName(), packet.serviceType(), packet.port(), packet.maxMemoryMb(), packet.staticService(), workingDirectory, instance, CloudServiceState.STARTING, Instant.now());
            InstanceSnapshot existing = snapshots.putIfAbsent(packet.serviceId(), snapshot);

            if (existing != null) {
                unregister(packet.serviceId());
                sendState(nodeConnection, packet, CloudServiceState.FAILED, "Minecraft instance became reserved while starting");
                workspace.cleanupTemporary(packet, workingDirectory);
                return Optional.empty();
            }

            instance.start();

            InstanceReadinessResult readiness = readinessProbe.awaitReady(instance, packet.serviceType(), startupProbeTimeout);

            if (!readiness.ready()) {
                snapshots.remove(packet.serviceId());
                workspace.archive(packet, workingDirectory, readiness.logs());
                unregister(packet.serviceId());
                workspace.cleanupTemporary(packet, workingDirectory);
                sendState(nodeConnection, packet, CloudServiceState.FAILED, readiness.message() + ". Logs: " + readiness.logs());
                LOGGER.warn("Minecraft instance " + packet.serviceId() + " failed readiness. " + readiness.message());
                return Optional.empty();
            }

            InstanceSnapshot runningSnapshot = snapshot.withState(CloudServiceState.RUNNING);
            snapshots.put(packet.serviceId(), runningSnapshot);
            nodeConnections.put(packet.serviceId(), nodeConnection);
            sendState(nodeConnection, packet, CloudServiceState.RUNNING, readiness.message());
            LOGGER.success("Started Minecraft instance " + packet.serviceId() + " in screen " + screenSession + " on port " + packet.port());

            return Optional.of(runningSnapshot);
        } catch (Exception exception) {
            snapshots.remove(packet.serviceId());
            String logs = message(exception, "Minecraft instance start failed");

            if (workingDirectory != null) {
                workspace.archive(packet, workingDirectory, logs);
            }

            unregister(packet.serviceId());

            if (workingDirectory != null) {
                workspace.cleanupTemporary(packet, workingDirectory);
            }

            sendState(nodeConnection, packet, CloudServiceState.FAILED, logs);
            LOGGER.warn("Minecraft instance " + packet.serviceId() + " failed to start: " + logs);
            return Optional.empty();
        }
    }

    public Optional<InstanceSnapshot> stop(KryoConnection nodeConnection, ServiceStopRequestPacket packet) {
        if (nodeConnection == null) {
            throw new IllegalArgumentException("nodeConnection must not be null");
        }

        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        InstanceSnapshot snapshot = snapshots.remove(packet.serviceId());

        if (snapshot == null) {
            return Optional.empty();
        }

        sendState(nodeConnection, snapshot, CloudServiceState.STOPPING, packet.reason());
        stopSnapshot(packet.serviceId(), snapshot, packet.force());
        sendState(nodeConnection, snapshot, CloudServiceState.STOPPED, packet.force() ? "Minecraft instance killed and deleted" : "Minecraft instance stopped and deleted");
        LOGGER.success((packet.force() ? "Killed" : "Stopped") + " Minecraft instance " + packet.serviceId());

        return Optional.of(snapshot.withState(CloudServiceState.STOPPED));
    }

    public void command(KryoConnection nodeConnection, ServiceCommandRequestPacket packet) {
        if (nodeConnection == null) {
            throw new IllegalArgumentException("nodeConnection must not be null");
        }

        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        InstanceSnapshot snapshot = snapshots.get(packet.serviceId());

        if (snapshot == null) {
            nodeConnection.send(new ServiceCommandResponsePacket(packet.requestId(), packet.serviceId(), false, "Minecraft instance is not running"));
            return;
        }

        try {
            snapshot.instance().command(packet.command());
            nodeConnection.send(new ServiceCommandResponsePacket(packet.requestId(), packet.serviceId(), true, "Command sent"));
        } catch (Exception exception) {
            nodeConnection.send(new ServiceCommandResponsePacket(packet.requestId(), packet.serviceId(), false, message(exception, "Command failed")));
        }
    }

    public void logs(KryoConnection nodeConnection, ServiceLogsRequestPacket packet) {
        if (nodeConnection == null) {
            throw new IllegalArgumentException("nodeConnection must not be null");
        }

        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        InstanceSnapshot snapshot = snapshots.get(packet.serviceId());

        if (snapshot != null) {
            respondLogs(nodeConnection, packet, logsFromInstance(snapshot.instance(), packet.tailLines()), "Live instance logs");
            return;
        }

        String archivedLogs = workspace.archivedLogs(packet.serviceId());

        if (!archivedLogs.isBlank()) {
            respondLogs(nodeConnection, packet, tail(archivedLogs, packet.tailLines()), "Archived failed-start logs");
            return;
        }

        nodeConnection.send(new ServiceLogsResponsePacket(packet.requestId(), packet.serviceId(), false, "", "No live or failed-start logs found"));
    }

    public void cleanup(KryoConnection nodeConnection, ServiceCleanupRequestPacket packet) {
        if (nodeConnection == null) {
            throw new IllegalArgumentException("nodeConnection must not be null");
        }

        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        InstanceCleanupResult result = workspace.cleanupOrphans(snapshots.keySet(), packet.dryRun());
        nodeConnection.send(new ServiceCleanupResponsePacket(packet.requestId(), wrapperId, packet.dryRun(), result.scanned(), result.deleted(), result.skipped(), result.failed(), result.details()));
        LOGGER.success("Cleanup scan finished for " + wrapperId + ": scanned=" + result.scanned() + ", deleted=" + result.deleted() + ", skipped=" + result.skipped() + ", failed=" + result.failed());
    }

    public Optional<InstanceSnapshot> instance(String serviceId) {
        validateName(serviceId);
        return Optional.ofNullable(snapshots.get(serviceId));
    }

    public List<InstanceSnapshot> instances() {
        return snapshots.values().stream().sorted(Comparator.comparing(InstanceSnapshot::serviceId)).toList();
    }

    public Map<String, ICloudInstance> cloudInstances() {
        return Map.copyOf(cloudInstances);
    }

    public int runningInstanceCount() {
        return snapshots.size();
    }

    public int reservedMemoryMb() {
        return snapshots.values().stream().mapToInt(InstanceSnapshot::maxMemoryMb).sum();
    }

    public int processMemoryMb() {
        return metrics().stream().mapToInt(InstanceMetrics::memoryMb).sum();
    }

    public int cpuLoadPermille() {
        List<InstanceMetrics> metrics = metrics();

        if (metrics.isEmpty()) {
            return 0;
        }

        return Math.min(1000, metrics.stream().mapToInt(InstanceMetrics::cpuLoadPermille).sum());
    }

    public List<InstanceMetrics> metrics() {
        return snapshots.keySet().stream().map(metricsCollector::collect).toList();
    }

    public InstanceMetrics metrics(String serviceId) {
        validateName(serviceId);
        return metricsCollector.collect(serviceId);
    }

    public void clear() {
        for (String name : cloudInstances.keySet()) {
            remove(name);
        }

        snapshots.clear();
        screenSessions.clear();
    }

    public void shutdown() {
        watcherExecutor.shutdownNow();

        for (InstanceSnapshot snapshot : snapshots.values()) {
            stopSnapshot(snapshot.serviceId(), snapshot, false);
        }

        snapshots.clear();
        cloudInstances.clear();
        screenSessions.clear();
        nodeConnections.clear();
    }

    private void watchInstancesSafely() {
        try {
            watchInstances();
        } catch (Exception exception) {
            LOGGER.warn("Minecraft instance watchdog failed: " + message(exception, "watchdog failed"));
        }
    }

    private void watchInstances() {
        for (InstanceSnapshot snapshot : snapshots.values()) {
            if (!watchable(snapshot)) {
                continue;
            }

            if (isOnline(snapshot.instance())) {
                continue;
            }

            handleExternalStop(snapshot);
        }
    }

    private boolean watchable(InstanceSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }

        return snapshot.state() == CloudServiceState.RUNNING;
    }

    private void handleExternalStop(InstanceSnapshot snapshot) {
        if (!snapshots.remove(snapshot.serviceId(), snapshot)) {
            return;
        }

        KryoConnection connection = nodeConnections.remove(snapshot.serviceId());
        unregister(snapshot.serviceId());
        boolean cleaned = workspace.cleanupTemporary(snapshot.staticService(), snapshot.workingDirectory());
        String message = cleaned ? "Minecraft instance stopped outside KryoCloud and was deleted" : "Minecraft instance stopped outside KryoCloud, but temporary cleanup failed";

        if (connection != null && connection.isActive()) {
            try {
                sendState(connection, snapshot, CloudServiceState.STOPPED, message);
            } catch (Exception exception) {
                LOGGER.warn("Failed to notify node about stopped Minecraft instance " + snapshot.serviceId() + ": " + exception.getMessage());
            }
        }

        if (!cleaned) {
            LOGGER.warn("Temporary Minecraft workspace was not deleted after external stop: " + snapshot.workingDirectory());
            return;
        }

        LOGGER.success("Detected stopped Minecraft instance " + snapshot.serviceId() + " and cleaned its workspace.");
    }

    private void respondLogs(KryoConnection nodeConnection, ServiceLogsRequestPacket packet, String logs, String message) {
        nodeConnection.send(new ServiceLogsResponsePacket(packet.requestId(), packet.serviceId(), true, logs, message));
    }

    private String logsFromInstance(ICloudInstance instance, int tailLines) {
        try {
            return tail(instance.logs(), tailLines);
        } catch (Exception exception) {
            return "Failed to read logs: " + exception.getMessage();
        }
    }

    private String tail(String value, int lines) {
        if (value == null || value.isBlank()) {
            return "";
        }

        if (lines < 1) {
            return value;
        }

        String[] split = value.split("\\R");
        int from = Math.max(0, split.length - lines);
        StringBuilder output = new StringBuilder();

        for (int index = from; index < split.length; index++) {
            output.append(split[index]).append(System.lineSeparator());
        }

        return output.toString().stripTrailing();
    }

    private String screenSessionName(ServiceStartRequestPacket packet) {
        return sanitizeScreenPart(cloudName) + "-" + sanitizeScreenPart(packet.groupName()) + "-" + serviceNumber(packet) + "-" + UUID.randomUUID();
    }

    private String serviceNumber(ServiceStartRequestPacket packet) {
        String prefix = packet.groupName() + "-";

        if (packet.serviceId().startsWith(prefix)) {
            String suffix = packet.serviceId().substring(prefix.length());

            if (!suffix.isBlank()) {
                return sanitizeScreenPart(suffix);
            }
        }

        int separator = packet.serviceId().lastIndexOf('-');

        if (separator >= 0 && separator < packet.serviceId().length() - 1) {
            return sanitizeScreenPart(packet.serviceId().substring(separator + 1));
        }

        return sanitizeScreenPart(packet.serviceId());
    }

    private String sanitizeScreenPart(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        String sanitized = value.toLowerCase().replaceAll("[^a-z0-9_.-]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");

        if (!sanitized.isBlank()) {
            return sanitized;
        }

        return "unknown";
    }

    private List<String> finalJvmArgs(ServiceStartRequestPacket packet, List<String> javaFlags) {
        List<String> arguments = new ArrayList<>(javaFlags);
        arguments.add("-Dkryocloud.service.id=" + packet.serviceId());
        arguments.add("-Dkryocloud.group=" + packet.groupName());
        arguments.add("-Dkryocloud.template=" + packet.templateName());
        arguments.add("-Dkryocloud.static=" + packet.staticService());
        arguments.add("-Dkryocloud.bind.address=" + packet.bindAddress());
        arguments.add("-Dserver.ip=" + packet.bindAddress());
        return List.copyOf(arguments);
    }

    private void stopSnapshot(String serviceId, InstanceSnapshot snapshot, boolean force) {
        try {
            if (force) {
                unregister(serviceId);
                return;
            }

            gracefulStop(serviceId, snapshot);
        } catch (Exception exception) {
            LOGGER.warn("Failed while stopping Minecraft instance " + serviceId + ": " + exception.getMessage());
        } finally {
            unregister(serviceId);

            if (!workspace.cleanupTemporary(snapshot.staticService(), snapshot.workingDirectory())) {
                LOGGER.warn("Temporary Minecraft workspace was not deleted: " + snapshot.workingDirectory());
            }
        }
    }

    private void gracefulStop(String serviceId, InstanceSnapshot snapshot) throws Exception {
        snapshot.instance().stop();

        if (waitOffline(snapshot.instance(), shutdownTimeout)) {
            return;
        }

        LOGGER.warn("Minecraft instance " + serviceId + " did not stop within " + shutdownTimeout.toSeconds() + "s. Forcing screen shutdown.");
        unregister(serviceId);
    }

    private boolean waitOffline(ICloudInstance instance, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();

        while (System.currentTimeMillis() <= deadline) {
            if (!isOnline(instance)) {
                return true;
            }

            sleep(500L);
        }

        return false;
    }

    private boolean isOnline(ICloudInstance instance) {
        try {
            return instance.isOnline();
        } catch (Exception exception) {
            LOGGER.warn("Failed to query Minecraft instance state: " + exception.getMessage());
            return false;
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void stopSafely(String name, ICloudInstance instance) {
        try {
            instance.stop();
        } catch (Exception exception) {
            LOGGER.warn("Failed to stop Minecraft instance " + name + ": " + exception.getMessage());
        }
    }

    private void unregister(String serviceId) {
        nodeConnections.remove(serviceId);
        cloudInstances.remove(serviceId);
        String screenSession = screenSessions.remove(serviceId);

        if (screenSession == null || screenSession.isBlank()) {
            screenSession = serviceId;
        }

        try {
            screenManager.remove(screenSession);
        } catch (Exception exception) {
            LOGGER.warn("Failed to unregister Minecraft screen " + serviceId + ": " + exception.getMessage());
        }
    }

    private void sendState(KryoConnection connection, ServiceStartRequestPacket packet, CloudServiceState state, String message) {
        connection.send(new ServiceStatePacket(packet.serviceId(), packet.groupName(), packet.serviceType(), state, wrapperId, advertisedAddress, packet.port(), normalizeMessage(message)));
    }

    private void sendState(KryoConnection connection, InstanceSnapshot snapshot, CloudServiceState state, String message) {
        connection.send(new ServiceStatePacket(snapshot.serviceId(), snapshot.groupName(), snapshot.serviceType(), state, wrapperId, advertisedAddress, snapshot.port(), normalizeMessage(message)));
    }

    private String normalizeMessage(String message) {
        if (message == null) {
            return "";
        }

        return message;
    }

    private String message(Exception exception, String fallback) {
        if (exception == null) {
            return fallback;
        }

        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return fallback;
        }

        return message;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        if (!name.matches("[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("name contains unsupported characters: " + name);
        }
    }
}
