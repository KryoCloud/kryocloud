package eu.kryocloud.node.plugin;

import eu.kryocloud.api.group.IGroup;
import eu.kryocloud.api.template.ITemplate;
import eu.kryocloud.common.logging.KryoLogger;
import eu.kryocloud.network.connection.KryoConnection;
import eu.kryocloud.network.packet.bus.KryoPacketBus;
import eu.kryocloud.network.packet.bus.PacketContext;
import eu.kryocloud.network.packet.bus.PacketSubscription;
import eu.kryocloud.network.packet.type.plugin.PluginGatewayEventPacket;
import eu.kryocloud.network.packet.type.plugin.PluginGatewayRequestPacket;
import eu.kryocloud.network.packet.type.plugin.PluginGatewayResponsePacket;
import eu.kryocloud.network.packet.type.service.ServiceCleanupRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceCommandRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceLogsRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceStopRequestPacket;
import eu.kryocloud.network.protocol.CloudServiceState;
import eu.kryocloud.network.protocol.PeerType;
import eu.kryocloud.network.protocol.WrapperState;
import eu.kryocloud.node.config.group.GroupConfig;
import eu.kryocloud.node.group.GroupManager;
import eu.kryocloud.node.service.runtime.NodeServiceSnapshot;
import eu.kryocloud.node.service.schedule.NodeServiceScheduler;
import eu.kryocloud.node.service.schedule.ServiceStartResult;
import eu.kryocloud.node.template.Template;
import eu.kryocloud.node.template.TemplateManager;
import eu.kryocloud.node.version.NodeVersionStorage;
import eu.kryocloud.node.wrapper.NodeWrapperRegistry;
import eu.kryocloud.node.wrapper.WrapperSnapshot;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NodePluginGateway implements AutoCloseable {

    private static final KryoLogger LOGGER = KryoLogger.logger("PluginGateway");

    private final NodeWrapperRegistry wrapperRegistry;
    private final eu.kryocloud.node.service.runtime.NodeServiceRegistry serviceRegistry;
    private final NodeServiceScheduler serviceScheduler;
    private final GroupManager groupManager;
    private final TemplateManager templateManager;
    private final NodeVersionStorage versionStorage;
    private final AtomicBoolean maintenance = new AtomicBoolean(false);
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private final List<PacketSubscription> subscriptions = new ArrayList<>();

    public NodePluginGateway(NodeWrapperRegistry wrapperRegistry, eu.kryocloud.node.service.runtime.NodeServiceRegistry serviceRegistry, NodeServiceScheduler serviceScheduler, GroupManager groupManager, TemplateManager templateManager, NodeVersionStorage versionStorage) {
        this.wrapperRegistry = wrapperRegistry;
        this.serviceRegistry = serviceRegistry;
        this.serviceScheduler = serviceScheduler;
        this.groupManager = groupManager;
        this.templateManager = templateManager;
        this.versionStorage = versionStorage;
    }

    public void register() {
        if (!registered.compareAndSet(false, true)) {
            return;
        }

        subscriptions.add(KryoPacketBus.listen(PluginGatewayRequestPacket.class, this::handleRequest));
    }

    public void publishServiceState(NodeServiceSnapshot previous, NodeServiceSnapshot current) {
        Map<String, String> payload = servicePayload(current);
        payload = mutable(payload);
        payload.put("oldState", previous == null ? "UNKNOWN" : previous.state().name());
        payload.put("newState", current.state().name());

        publish(event("service.ServiceStateChangedEvent"), current.serviceId(), current.groupName(), current.wrapperId(), payload);

        String specificEvent = switch (current.state()) {
            case PREPARING -> event("service.ServicePreparingEvent");
            case STARTING -> event("service.ServiceStartingEvent");
            case RUNNING -> event("service.ServiceStartedEvent");
            case STOPPING -> event("service.ServiceStoppingEvent");
            case STOPPED -> event("service.ServiceStoppedEvent");
            case FAILED -> event("service.ServiceFailedEvent");
        };

        publish(specificEvent, current.serviceId(), current.groupName(), current.wrapperId(), payload);
    }

    public void publishServiceMetrics(NodeServiceSnapshot snapshot) {
        publish(event("service.ServiceMetricsUpdatedEvent"), snapshot.serviceId(), snapshot.groupName(), snapshot.wrapperId(), servicePayload(snapshot));
    }

    public void publishWrapperConnected(WrapperSnapshot snapshot) {
        publish(event("wrapper.WrapperConnectedEvent"), "", "", snapshot.wrapperId(), wrapperPayload(snapshot));
        publish(event("wrapper.WrapperStateChangedEvent"), "", "", snapshot.wrapperId(), wrapperPayload(snapshot));
    }

    public void publishWrapperHeartbeat(WrapperSnapshot snapshot) {
        publish(event("wrapper.WrapperHeartbeatEvent"), "", "", snapshot.wrapperId(), wrapperPayload(snapshot));
    }

    public void publishWrapperState(WrapperSnapshot snapshot) {
        publish(event("wrapper.WrapperStateChangedEvent"), "", "", snapshot.wrapperId(), wrapperPayload(snapshot));
    }

    public void publishCloudReady() {
        publish(event("lifecycle.CloudReadyEvent"), "", "", "", Map.of());
    }

    public void publishCloudStopping(String reason) {
        publish(event("lifecycle.CloudStoppingEvent"), "", "", "", Map.of("reason", reason == null ? "" : reason));
    }

    @Override
    public void close() {
        if (!registered.compareAndSet(true, false)) {
            return;
        }

        for (PacketSubscription subscription : subscriptions) {
            subscription.close();
        }

        subscriptions.clear();
    }

    private void handleRequest(PacketContext context, PluginGatewayRequestPacket packet) {
        if (!validWrapper(context)) {
            context.connection().close();
            return;
        }

        try {
            Map<String, String> response = route(packet);
            context.connection().send(new PluginGatewayResponsePacket(packet.requestId(), packet.pluginId(), packet.route(), true, "OK", response));
        } catch (Throwable throwable) {
            String message = throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
            context.connection().send(new PluginGatewayResponsePacket(packet.requestId(), packet.pluginId(), packet.route(), false, message, Map.of("message", message)));
        }
    }

    private Map<String, String> route(PluginGatewayRequestPacket packet) {
        return switch (packet.route()) {
            case "service.start" -> serviceStart(packet.payload());
            case "service.stop" -> serviceStop(packet.payload(), false);
            case "service.kill" -> serviceStop(packet.payload(), true);
            case "service.restart" -> serviceRestart(packet.payload());
            case "service.command" -> serviceCommand(packet.payload());
            case "service.logs" -> serviceLogs(packet.payload());
            case "service.list" -> serviceList(packet.payload());
            case "service.info" -> serviceInfo(packet.payload());
            case "group.list" -> groupList();
            case "group.info" -> groupInfo(packet.payload());
            case "group.exists" -> Map.of("exists", String.valueOf(groupManager.existsGroup(required(packet.payload(), "group"))));
            case "group.scale" -> groupScale(packet.payload());
            case "group.reconcile" -> groupReconcile(packet.payload());
            case "group.reconcile.all" -> groupReconcileAll();
            case "wrapper.list" -> wrapperList(wrapperRegistry.wrappers());
            case "wrapper.available" -> wrapperList(wrapperRegistry.availableWrappers());
            case "wrapper.info" -> wrapperInfo(packet.payload());
            case "wrapper.cleanup" -> wrapperCleanup(packet.payload());
            case "wrapper.cleanup.all" -> wrapperCleanupAll(packet.payload());
            case "template.list" -> templateList();
            case "template.create" -> templateCreate(packet.payload());
            case "template.delete" -> templateDelete(packet.payload());
            case "template.copy" -> templateCopy(packet.payload());
            case "template.sync" -> templateSync(packet.payload());
            case "version.list" -> versionList();
            case "version.install" -> versionInstall(packet.payload());
            case "version.refresh" -> versionRefresh();
            case "maintenance.enabled" -> Map.of("enabled", String.valueOf(maintenance.get()));
            case "maintenance.enable" -> maintenanceEnable(packet.payload());
            case "maintenance.disable" -> maintenanceDisable();
            case "cloud.stats" -> stats();
            case "cloud.shutdown" -> cloudShutdown(packet.payload());
            default -> throw new IllegalArgumentException("Unknown plugin route: " + packet.route());
        };
    }

    private Map<String, String> serviceStart(Map<String, String> payload) {
        String group = required(payload, "group");
        int count = integer(payload, "count", 1);
        List<ServiceStartResult> results = serviceScheduler.startGroup(group, count);

        if (count == 1 && !results.isEmpty()) {
            ServiceStartResult result = results.getFirst();
            return Map.of("service", result.serviceId(), "group", group, "wrapper", result.wrapperId(), "success", "true", "message", "Service start requested");
        }

        return resultList(results, group);
    }

    private Map<String, String> serviceStop(Map<String, String> payload, boolean force) {
        String service = required(payload, "service");
        NodeServiceSnapshot snapshot = serviceRegistry.service(service).orElseThrow(() -> new IllegalArgumentException("Unknown service: " + service));
        KryoConnection connection = wrapperRegistry.connection(snapshot.wrapperId()).orElseThrow(() -> new IllegalStateException("Wrapper not connected: " + snapshot.wrapperId()));
        connection.send(new ServiceStopRequestPacket(UUID.randomUUID(), service, force, force ? "KryoCloud plugin kill" : "KryoCloud plugin stop"));
        return Map.of("sent", "true");
    }

    private Map<String, String> serviceRestart(Map<String, String> payload) {
        String service = required(payload, "service");
        NodeServiceSnapshot snapshot = serviceRegistry.service(service).orElseThrow(() -> new IllegalArgumentException("Unknown service: " + service));
        serviceStop(payload, false);
        List<ServiceStartResult> results = serviceScheduler.startGroup(snapshot.groupName(), 1);
        return resultList(results, snapshot.groupName());
    }

    private Map<String, String> serviceCommand(Map<String, String> payload) {
        String service = required(payload, "service");
        String command = required(payload, "command");
        NodeServiceSnapshot snapshot = serviceRegistry.service(service).orElseThrow(() -> new IllegalArgumentException("Unknown service: " + service));
        KryoConnection connection = wrapperRegistry.connection(snapshot.wrapperId()).orElseThrow(() -> new IllegalStateException("Wrapper not connected: " + snapshot.wrapperId()));
        connection.send(new ServiceCommandRequestPacket(UUID.randomUUID(), service, command));
        publish(event("service.ServiceCommandSentEvent"), service, snapshot.groupName(), snapshot.wrapperId(), Map.of("service", service, "command", command));
        return Map.of("sent", "true");
    }

    private Map<String, String> serviceLogs(Map<String, String> payload) {
        String service = required(payload, "service");
        int tail = integer(payload, "tail", 100);
        NodeServiceSnapshot snapshot = serviceRegistry.service(service).orElseThrow(() -> new IllegalArgumentException("Unknown service: " + service));
        KryoConnection connection = wrapperRegistry.connection(snapshot.wrapperId()).orElseThrow(() -> new IllegalStateException("Wrapper not connected: " + snapshot.wrapperId()));
        connection.send(new ServiceLogsRequestPacket(UUID.randomUUID(), service, tail));
        return Map.of("logs", "Logs request sent to wrapper. Use the console response stream for live output.");
    }

    private Map<String, String> serviceList(Map<String, String> payload) {
        String group = payload.getOrDefault("group", "");
        List<NodeServiceSnapshot> services = group == null || group.isBlank() ? serviceRegistry.services() : serviceRegistry.services(group);
        return list("services", services.stream().map(this::servicePayload).toList());
    }

    private Map<String, String> serviceInfo(Map<String, String> payload) {
        Optional<NodeServiceSnapshot> service = serviceRegistry.service(required(payload, "service"));

        if (service.isEmpty()) {
            return Map.of("present", "false");
        }

        Map<String, String> values = mutable(servicePayload(service.get()));
        values.put("present", "true");
        return values;
    }

    private Map<String, String> groupList() {
        return list("groups", groupManager.groups().stream().map(this::groupPayload).toList());
    }

    private Map<String, String> groupInfo(Map<String, String> payload) {
        IGroup group = groupManager.groupByName(required(payload, "group"));

        if (group == null) {
            return Map.of("present", "false");
        }

        Map<String, String> values = mutable(groupPayload(group));
        values.put("present", "true");
        return values;
    }

    private Map<String, String> groupScale(Map<String, String> payload) {
        String name = required(payload, "group");
        int minOnline = integer(payload, "minOnline", 1);
        IGroup oldGroup = groupManager.groupByName(name);

        if (oldGroup == null) {
            throw new IllegalArgumentException("Unknown group: " + name);
        }

        GroupConfig config = groupManager.config(name).orElseThrow(() -> new IllegalStateException("No config for group: " + name));
        int oldMin = oldGroup.minCount();
        config.setMinCount(minOnline);
        config.save();
        groupManager.deleteGroup(oldGroup.uniqueId());
        IGroup group = groupManager.createGroup(config.toGroup());
        Map<String, String> eventPayload = mutable(groupPayload(group));
        eventPayload.put("oldMinOnline", String.valueOf(oldMin));
        eventPayload.put("newMinOnline", String.valueOf(minOnline));
        publish(event("group.GroupScaledEvent"), "", group.name(), "", eventPayload);
        publish(event("group.GroupUpdatedEvent"), "", group.name(), "", groupPayload(group));
        return groupPayload(group);
    }

    private Map<String, String> groupReconcile(Map<String, String> payload) {
        String groupName = required(payload, "group");
        List<ServiceStartResult> results = serviceScheduler.reconcileGroup(groupName);
        IGroup group = groupManager.groupByName(groupName);
        Map<String, String> eventPayload = mutable(groupPayload(group));
        eventPayload.put("startedServices", String.valueOf(results.size()));
        publish(event("group.GroupReconciledEvent"), "", groupName, "", eventPayload);
        return resultList(results, groupName);
    }

    private Map<String, String> groupReconcileAll() {
        List<ServiceStartResult> results = serviceScheduler.reconcileMinimumServices();
        publish(event("group.GroupReconcileAllEvent"), "", "", "", Map.of("startedServices", String.valueOf(results.size())));
        return resultList(results, "");
    }

    private Map<String, String> wrapperList(List<WrapperSnapshot> wrappers) {
        return list("wrappers", wrappers.stream().map(this::wrapperPayload).toList());
    }

    private Map<String, String> wrapperInfo(Map<String, String> payload) {
        Optional<WrapperSnapshot> wrapper = wrapperRegistry.wrapper(required(payload, "wrapper"));

        if (wrapper.isEmpty()) {
            return Map.of("present", "false");
        }

        Map<String, String> values = mutable(wrapperPayload(wrapper.get()));
        values.put("present", "true");
        return values;
    }

    private Map<String, String> wrapperCleanup(Map<String, String> payload) {
        String wrapper = required(payload, "wrapper");
        boolean dryRun = Boolean.parseBoolean(payload.getOrDefault("dryRun", "false"));
        KryoConnection connection = wrapperRegistry.connection(wrapper).orElseThrow(() -> new IllegalStateException("Wrapper not connected: " + wrapper));
        connection.send(new ServiceCleanupRequestPacket(UUID.randomUUID(), dryRun));
        return Map.of("sent", "true");
    }

    private Map<String, String> wrapperCleanupAll(Map<String, String> payload) {
        boolean dryRun = Boolean.parseBoolean(payload.getOrDefault("dryRun", "false"));
        int sent = 0;

        for (WrapperSnapshot wrapper : wrapperRegistry.wrappers()) {
            Optional<KryoConnection> connection = wrapperRegistry.connection(wrapper.wrapperId());

            if (connection.isEmpty()) {
                continue;
            }

            connection.get().send(new ServiceCleanupRequestPacket(UUID.randomUUID(), dryRun));
            sent++;
        }

        return Map.of("sent", String.valueOf(sent));
    }

    private Map<String, String> templateList() {
        return list("templates", templateManager.templates().values().stream().map(this::templatePayload).toList());
    }

    private Map<String, String> templateCreate(Map<String, String> payload) {
        String name = required(payload, "template");
        Template template = new Template(name, eu.kryocloud.common.layout.KryoDirectoryLayout.TEMPLATES.resolve(name));
        templateManager.createTemplate(template);
        Map<String, String> values = templatePayload(template);
        publish(event("template.TemplateCreatedEvent"), "", "", "", values);
        return values;
    }

    private Map<String, String> templateDelete(Map<String, String> payload) {
        String name = required(payload, "template");
        ITemplate template = templateManager.template(name);
        Map<String, String> values = template == null ? Map.of("template", name, "path", "", "updatedAt", Instant.EPOCH.toString()) : templatePayload(template);
        templateManager.deleteTemplate(name);
        publish(event("template.TemplateDeletedEvent"), "", "", "", values);
        return values;
    }

    private Map<String, String> templateCopy(Map<String, String> payload) {
        String sourceName = required(payload, "source");
        String targetName = required(payload, "target");
        ITemplate source = templateManager.template(sourceName);

        if (source == null) {
            throw new IllegalArgumentException("Unknown template: " + sourceName);
        }

        Template target = new Template(targetName, eu.kryocloud.common.layout.KryoDirectoryLayout.TEMPLATES.resolve(targetName));
        templateManager.createTemplate(target);
        copyDirectory(source.path(), target.path());
        Map<String, String> values = new LinkedHashMap<>();
        prefixed(values, "source.", templatePayload(source));
        prefixed(values, "target.", templatePayload(target));
        publish(event("template.TemplateCopiedEvent"), "", "", "", values);
        return values;
    }

    private Map<String, String> templateSync(Map<String, String> payload) {
        String name = required(payload, "template");
        ITemplate template = templateManager.template(name);

        if (template == null) {
            throw new IllegalArgumentException("Unknown template: " + name);
        }

        Map<String, String> values = templatePayload(template);
        publish(event("template.TemplateSyncedEvent"), "", "", "", values);
        return values;
    }

    private Map<String, String> versionList() {
        List<Map<String, String>> versions = versionStorage.availableSoftware().stream().map(software -> Map.of("version", versionStorage.latestVersion(software).orElse("latest"), "type", software, "path", versionStorage.rootDirectory().resolve(software).toString(), "installedAt", Instant.EPOCH.toString())).toList();
        return list("versions", versions);
    }

    private Map<String, String> versionInstall(Map<String, String> payload) {
        String version = required(payload, "version");
        String software = payload.getOrDefault("software", payload.getOrDefault("type", "paper"));
        versionStorage.installFromManifest(software, version, true);
        Map<String, String> values = Map.of("version", version, "type", software, "path", versionStorage.rootDirectory().resolve(software).resolve(version).toString(), "installedAt", Instant.now().toString());
        publish(event("version.VersionInstalledEvent"), "", "", "", values);
        return values;
    }

    private Map<String, String> versionRefresh() {
        int refreshed = versionStorage.refreshManifests();
        publish(event("version.VersionRefreshedEvent"), "", "", "", Map.of("refreshed", String.valueOf(refreshed)));
        return Map.of("refreshed", String.valueOf(refreshed));
    }

    private Map<String, String> maintenanceEnable(Map<String, String> payload) {
        maintenance.set(true);
        String reason = payload.getOrDefault("reason", "");
        publish(event("maintenance.MaintenanceEnabledEvent"), "", "", "", Map.of("reason", reason));
        return Map.of("enabled", "true", "reason", reason);
    }

    private Map<String, String> maintenanceDisable() {
        maintenance.set(false);
        publish(event("maintenance.MaintenanceDisabledEvent"), "", "", "", Map.of());
        return Map.of("enabled", "false");
    }

    private Map<String, String> stats() {
        int usedMemory = wrapperRegistry.wrappers().stream().mapToInt(WrapperSnapshot::usedMemoryMb).sum();
        int maxMemory = wrapperRegistry.wrappers().stream().mapToInt(WrapperSnapshot::maxMemoryMb).sum();
        int onlineWrappers = (int) wrapperRegistry.wrappers().stream().filter(wrapper -> wrapper.state() != WrapperState.OFFLINE).count();
        return Map.of("wrappers", String.valueOf(wrapperRegistry.size()), "onlineWrappers", String.valueOf(onlineWrappers), "services", String.valueOf(serviceRegistry.size()), "runningServices", String.valueOf(serviceRegistry.runningServices().size()), "groups", String.valueOf(groupManager.groups().size()), "usedMemoryMb", String.valueOf(usedMemory), "maxMemoryMb", String.valueOf(maxMemory));
    }

    private Map<String, String> cloudShutdown(Map<String, String> payload) {
        String reason = payload.getOrDefault("reason", "KryoCloud plugin requested shutdown");
        publishCloudStopping(reason);
        return Map.of("accepted", "true", "reason", reason);
    }

    private void publish(String route, String service, String group, String wrapper, Map<String, String> payload) {
        PluginGatewayEventPacket packet = new PluginGatewayEventPacket(UUID.randomUUID(), route, service == null ? "" : service, group == null ? "" : group, wrapper == null ? "" : wrapper, payload);

        for (WrapperSnapshot snapshot : wrapperRegistry.wrappers()) {
            wrapperRegistry.connection(snapshot.wrapperId()).ifPresent(connection -> connection.send(packet));
        }
    }

    private Map<String, String> servicePayload(NodeServiceSnapshot service) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("service", service.serviceId());
        values.put("group", service.groupName());
        values.put("type", service.serviceType().name());
        values.put("state", service.state().name());
        values.put("wrapper", service.wrapperId());
        values.put("host", service.host());
        values.put("port", String.valueOf(service.port()));
        values.put("memoryMb", String.valueOf(service.processMemoryMb()));
        values.put("maxMemoryMb", String.valueOf(service.processMemoryMb()));
        values.put("startedAt", Instant.ofEpochMilli(service.timestamp()).toString());
        values.put("message", service.message() == null ? "" : service.message());
        values.put("cpuLoadPermille", String.valueOf(service.cpuLoadPermille()));
        values.put("uptimeMillis", String.valueOf(service.uptimeMillis()));
        return Map.copyOf(values);
    }

    private Map<String, String> groupPayload(IGroup group) {
        if (group == null) {
            return Map.of("group", "unknown", "type", "UNKNOWN", "minOnline", "0", "maxOnline", "0", "memoryMb", "0", "template", "", "version", "");
        }

        return Map.of("group", group.name(), "type", group.serviceType().name(), "minOnline", String.valueOf(group.minCount()), "maxOnline", String.valueOf(group.maxCount()), "memoryMb", String.valueOf(group.maxMemory()), "template", group.templateName(), "version", group.softwareVersion());
    }

    private Map<String, String> wrapperPayload(WrapperSnapshot wrapper) {
        return Map.of("wrapper", wrapper.wrapperId(), "state", wrapper.state().name(), "host", wrapper.address(), "services", String.valueOf(wrapper.runningServices()), "maxServices", "0", "usedMemoryMb", String.valueOf(wrapper.usedMemoryMb()), "maxMemoryMb", String.valueOf(wrapper.maxMemoryMb()), "lastHeartbeat", Instant.ofEpochMilli(wrapper.lastHeartbeatAtMillis()).toString());
    }

    private Map<String, String> templatePayload(ITemplate template) {
        return Map.of("template", template.name(), "path", template.path().toString(), "updatedAt", Instant.EPOCH.toString());
    }

    private Map<String, String> list(String prefix, List<Map<String, String>> items) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(prefix + ".size", String.valueOf(items.size()));

        for (int index = 0; index < items.size(); index++) {
            prefixed(values, prefix + "." + index + ".", items.get(index));
        }

        return Map.copyOf(values);
    }

    private Map<String, String> resultList(List<ServiceStartResult> results, String group) {
        List<Map<String, String>> items = results.stream().map(result -> Map.of("service", result.serviceId(), "group", group == null ? "" : group, "wrapper", result.wrapperId(), "success", "true", "message", "Service start requested")).toList();
        return list("results", items);
    }

    private void prefixed(Map<String, String> target, String prefix, Map<String, String> source) {
        source.forEach((key, value) -> target.put(prefix + key, value));
    }

    private String required(Map<String, String> payload, String key) {
        String value = payload.get(key);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " must not be blank");
        }

        return value;
    }

    private int integer(Map<String, String> payload, String key, int fallback) {
        try {
            return Integer.parseInt(payload.getOrDefault(key, String.valueOf(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private Map<String, String> mutable(Map<String, String> source) {
        return new LinkedHashMap<>(source);
    }

    private String event(String shortName) {
        return "eu.kryocloud.api.plugin.event." + shortName;
    }

    private boolean validWrapper(PacketContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        return context.authenticated() && context.connection().peerType() == PeerType.WRAPPER;
    }

    private void copyDirectory(java.nio.file.Path source, java.nio.file.Path target) {
        try {
            Files.createDirectories(target);

            try (var paths = Files.walk(source)) {
                for (java.nio.file.Path path : paths.toList()) {
                    java.nio.file.Path relative = source.relativize(path);
                    java.nio.file.Path destination = target.resolve(relative);

                    if (Files.isDirectory(path)) {
                        Files.createDirectories(destination);
                        continue;
                    }

                    Files.copy(path, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (Exception exception) {
            throw new RuntimeException("Could not copy template directory", exception);
        }
    }

}
