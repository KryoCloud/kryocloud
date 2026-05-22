package eu.kryocloud.wrapper.service;

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

    private final String wrapperId;
    private final Path templatesDirectory;
    private final Path servicesDirectory;
    private final ConcurrentMap<String, WrapperServiceInstance> services = new ConcurrentHashMap<>();

    public WrapperServiceManager(String wrapperId, Path templatesDirectory, Path servicesDirectory) {
        if (wrapperId == null || wrapperId.isBlank()) {
            throw new IllegalArgumentException("wrapperId must not be blank");
        }

        if (templatesDirectory == null) {
            throw new IllegalArgumentException("templatesDirectory must not be null");
        }

        if (servicesDirectory == null) {
            throw new IllegalArgumentException("servicesDirectory must not be null");
        }

        this.wrapperId = wrapperId;
        this.templatesDirectory = templatesDirectory;
        this.servicesDirectory = servicesDirectory;
    }

    public WrapperServiceInstance start(KryoConnection nodeConnection, ServiceStartRequestPacket packet) {
        if (nodeConnection == null) {
            throw new IllegalArgumentException("nodeConnection must not be null");
        }

        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        if (services.containsKey(packet.serviceId())) {
            throw new IllegalStateException("Service is already running or reserved: " + packet.serviceId());
        }

        sendState(nodeConnection, packet, CloudServiceState.PREPARING, "Preparing service workspace");

        Path workingDirectory = servicesDirectory.resolve(packet.serviceId());
        Path templateDirectory = templatesDirectory.resolve(packet.templateName());

        try {
            prepareWorkspace(templateDirectory, workingDirectory);
            sendState(nodeConnection, packet, CloudServiceState.STARTING, "Starting Minecraft process");

            Process process = startProcess(packet, workingDirectory);
            WrapperServiceInstance instance = new WrapperServiceInstance(packet.requestId(), packet.serviceId(), packet.groupName(), packet.templateName(), packet.serviceType(), packet.port(), packet.maxMemoryMb(), packet.staticService(), workingDirectory, process, CloudServiceState.STARTING, Instant.now());

            WrapperServiceInstance existing = services.putIfAbsent(packet.serviceId(), instance);

            if (existing != null) {
                process.destroy();
                throw new IllegalStateException("Service became reserved while starting: " + packet.serviceId());
            }

            sendState(nodeConnection, packet, CloudServiceState.RUNNING, "Service process started");
            return instance.withState(CloudServiceState.RUNNING);
        } catch (Exception exception) {
            services.remove(packet.serviceId());
            sendState(nodeConnection, packet, CloudServiceState.FAILED, exception.getMessage() == null ? "Service start failed" : exception.getMessage());
            throw new RuntimeException("Failed to start service " + packet.serviceId(), exception);
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

        Process process = instance.process();

        if (process != null && process.isAlive()) {
            if (packet.force()) {
                process.destroyForcibly();
            }
            if(!packet.force()) {
                process.destroy();
            }
        }

        sendState(nodeConnection, instance, CloudServiceState.STOPPED, "Service stopped");
        return Optional.of(instance.withState(CloudServiceState.STOPPED));
    }

    public Optional<WrapperServiceInstance> service(String serviceId) {
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }

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
            Process process = instance.process();

            if (process != null && process.isAlive()) {
                process.destroy();
            }
        }

        services.clear();
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

    private Process startProcess(ServiceStartRequestPacket packet, Path workingDirectory) throws IOException {
        Path jarFile = workingDirectory.resolve("server.jar");

        if (!Files.exists(jarFile)) {
            throw new IllegalStateException("Missing server.jar in service workspace: " + jarFile);
        }

        ProcessBuilder processBuilder = new ProcessBuilder("java", "-Xmx" + packet.maxMemoryMb() + "M", "-Dkryocloud.service.id=" + packet.serviceId(), "-Dkryocloud.group=" + packet.groupName(), "-Dserver.port=" + packet.port(), "-jar", "server.jar", "nogui");
        processBuilder.directory(workingDirectory.toFile());
        processBuilder.redirectErrorStream(true);

        return processBuilder.start();
    }

    private void sendState(KryoConnection connection, ServiceStartRequestPacket packet, CloudServiceState state, String message) {
        connection.send(new ServiceStatePacket(packet.serviceId(), packet.groupName(), packet.serviceType(), state, wrapperId, "127.0.0.1", packet.port(), message == null ? "" : message));
    }

    private void sendState(KryoConnection connection, WrapperServiceInstance instance, CloudServiceState state, String message) {
        connection.send(new ServiceStatePacket(instance.serviceId(), instance.groupName(), instance.serviceType(), state, wrapperId, "127.0.0.1", instance.port(), message == null ? "" : message));
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