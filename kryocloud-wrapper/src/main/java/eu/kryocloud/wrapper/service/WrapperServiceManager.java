package eu.kryocloud.wrapper.service;

import eu.kryocloud.api.server.ICloudServer;
import eu.kryocloud.api.server.IServerManager;
import eu.kryocloud.common.logging.KryoLogger;
import eu.kryocloud.network.connection.KryoConnection;
import eu.kryocloud.network.packet.type.service.ServiceStartRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceStatePacket;
import eu.kryocloud.network.packet.type.service.ServiceStopRequestPacket;
import eu.kryocloud.network.protocol.CloudServiceState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class WrapperServiceManager {

    private static final KryoLogger LOGGER = KryoLogger.logger("WrapperServiceManager");

    private final String wrapperId;
    private final String advertisedAddress;
    private final Path templatesDirectory;
    private final Path servicesDirectory;
    private final IServerManager serverManager;
    private final ConcurrentMap<String, WrapperServiceInstance> services = new ConcurrentHashMap<>();

    public WrapperServiceManager(String wrapperId, String advertisedAddress, Path templatesDirectory, Path servicesDirectory, IServerManager serverManager) {
        if (wrapperId == null || wrapperId.isBlank()) {
            throw new IllegalArgumentException("wrapperId must not be blank");
        }

        if (advertisedAddress == null || advertisedAddress.isBlank()) {
            throw new IllegalArgumentException("advertisedAddress must not be blank");
        }

        if (templatesDirectory == null) {
            throw new IllegalArgumentException("templatesDirectory must not be null");
        }

        if (servicesDirectory == null) {
            throw new IllegalArgumentException("servicesDirectory must not be null");
        }

        if (serverManager == null) {
            throw new IllegalArgumentException("serverManager must not be null");
        }

        this.wrapperId = wrapperId;
        this.advertisedAddress = advertisedAddress;
        this.templatesDirectory = templatesDirectory;
        this.servicesDirectory = servicesDirectory;
        this.serverManager = serverManager;
    }

    public Optional<WrapperServiceInstance> start(KryoConnection nodeConnection, ServiceStartRequestPacket packet) {
        if (nodeConnection == null) {
            throw new IllegalArgumentException("nodeConnection must not be null");
        }

        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        if (services.containsKey(packet.serviceId())) {
            sendState(nodeConnection, packet, CloudServiceState.FAILED, "Service is already running or reserved");
            return Optional.empty();
        }

        sendState(nodeConnection, packet, CloudServiceState.PREPARING, "Preparing service workspace");

        Path workingDirectory = servicesDirectory.resolve(packet.serviceId());
        Path templateDirectory = templatesDirectory.resolve(packet.templateName());

        try {
            prepareWorkspace(templateDirectory, workingDirectory);
            sendState(nodeConnection, packet, CloudServiceState.STARTING, "Starting screen session");

            ICloudServer server = serverManager.create(packet.serviceId(), workingDirectory, packet.maxMemoryMb(), packet.maxMemoryMb(), jvmArgs(packet));
            WrapperServiceInstance instance = new WrapperServiceInstance(packet.requestId(), packet.serviceId(), packet.groupName(), packet.templateName(), packet.serviceType(), packet.port(), packet.maxMemoryMb(), packet.staticService(), workingDirectory, server, CloudServiceState.STARTING, Instant.now());
            WrapperServiceInstance existing = services.putIfAbsent(packet.serviceId(), instance);

            if (existing != null) {
                serverManager.remove(packet.serviceId());
                sendState(nodeConnection, packet, CloudServiceState.FAILED, "Service became reserved while starting");
                return Optional.empty();
            }

            server.start();

            WrapperServiceInstance runningInstance = instance.withState(CloudServiceState.RUNNING);
            services.put(packet.serviceId(), runningInstance);
            sendState(nodeConnection, packet, CloudServiceState.RUNNING, "Screen session started");
            LOGGER.success("Started service " + packet.serviceId() + " on port " + packet.port());

            return Optional.of(runningInstance);
        } catch (Exception exception) {
            services.remove(packet.serviceId());
            safeRemoveServer(packet.serviceId());
            sendState(nodeConnection, packet, CloudServiceState.FAILED, message(exception, "Service start failed"));
            LOGGER.warn("Service " + packet.serviceId() + " failed to start: " + message(exception, "Service start failed"));
            return Optional.empty();
        }
    }

    public Optional<WrapperServiceInstance> stop(KryoConnection nodeConnection, ServiceStopRequestPacket packet) {
        if (nodeConnection == null) {
            throw new IllegalArgumentException("nodeConnection must not be null");
        }

        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        WrapperServiceInstance instance = services.remove(packet.serviceId());

        if (instance == null) {
            return Optional.empty();
        }

        sendState(nodeConnection, instance, CloudServiceState.STOPPING, packet.reason());
        stopInstance(packet.serviceId(), instance);
        sendState(nodeConnection, instance, CloudServiceState.STOPPED, "Service stopped");
        LOGGER.success("Stopped service " + packet.serviceId());

        return Optional.of(instance.withState(CloudServiceState.STOPPED));
    }

    public Optional<WrapperServiceInstance> service(String serviceId) {
        validateServiceId(serviceId);
        return Optional.ofNullable(services.get(serviceId));
    }

    public List<WrapperServiceInstance> services() {
        return services.values().stream().sorted(Comparator.comparing(WrapperServiceInstance::serviceId)).toList();
    }

    public int runningServiceCount() {
        return services.size();
    }

    public void shutdown() {
        for (WrapperServiceInstance instance : services.values()) {
            stopInstance(instance.serviceId(), instance);
        }

        services.clear();
    }

    private List<String> jvmArgs(ServiceStartRequestPacket packet) {
        return List.of("-Dkryocloud.service.id=" + packet.serviceId(), "-Dkryocloud.group=" + packet.groupName(), "-Dkryocloud.template=" + packet.templateName(), "-Dkryocloud.static=" + packet.staticService(), "-Dserver.port=" + packet.port());
    }

    private void stopInstance(String serviceId, WrapperServiceInstance instance) {
        try {
            instance.server().stop();
            safeRemoveServer(serviceId);
        } catch (Exception exception) {
            safeRemoveServer(serviceId);
            LOGGER.warn("Failed to stop service " + serviceId + ": " + exception.getMessage());
        }
    }

    private void prepareWorkspace(Path templateDirectory, Path workingDirectory) throws IOException {
        Files.createDirectories(servicesDirectory);

        if (!Files.exists(templateDirectory)) {
            throw new IllegalStateException("Template does not exist: " + templateDirectory);
        }

        if (Files.exists(workingDirectory)) {
            deleteRecursively(workingDirectory);
        }

        copyRecursively(templateDirectory, workingDirectory);
    }

    private void sendState(KryoConnection connection, ServiceStartRequestPacket packet, CloudServiceState state, String message) {
        connection.send(new ServiceStatePacket(packet.serviceId(), packet.groupName(), packet.serviceType(), state, wrapperId, advertisedAddress, packet.port(), normalizeMessage(message)));
    }

    private void sendState(KryoConnection connection, WrapperServiceInstance instance, CloudServiceState state, String message) {
        connection.send(new ServiceStatePacket(instance.serviceId(), instance.groupName(), instance.serviceType(), state, wrapperId, advertisedAddress, instance.port(), normalizeMessage(message)));
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

    private void validateServiceId(String serviceId) {
        if (serviceId == null) {
            throw new IllegalArgumentException("serviceId must not be null");
        }

        if (serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }
    }

    private void safeRemoveServer(String serviceId) {
        try {
            serverManager.remove(serviceId);
        } catch (Exception exception) {
            LOGGER.warn("Failed to remove server " + serviceId + ": " + exception.getMessage());
        }
    }

    private void copyRecursively(Path source, Path target) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);

                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                    continue;
                }

                Files.copy(path, destination);
            }
        }
    }

    private void deleteRecursively(Path directory) throws IOException {
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}