package eu.kryocloud.node.plugin;

import eu.kryocloud.api.group.IGroup;
import eu.kryocloud.api.template.ITemplate;
import eu.kryocloud.api.plugin.event.network.NetworkCacheChangedEvent;
import eu.kryocloud.api.plugin.event.network.NetworkChannelMessageEvent;
import eu.kryocloud.common.layout.KryoDirectoryLayout;
import eu.kryocloud.common.manifest.ManifestCodename;
import eu.kryocloud.common.manifest.SoftwareManifest;
import eu.kryocloud.common.manifest.SoftwareVersion;
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
import eu.kryocloud.node.config.network.NetworkAddressConfig;
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
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NodePluginGateway implements AutoCloseable {

    private static final KryoLogger LOGGER = KryoLogger.logger("PluginGateway");

    private final NodeWrapperRegistry wrapperRegistry;
    private final eu.kryocloud.node.service.runtime.NodeServiceRegistry serviceRegistry;
    private final NodeServiceScheduler serviceScheduler;
    private final GroupManager groupManager;
    private final TemplateManager templateManager;
    private final NodeVersionStorage versionStorage;
    private final NetworkAddressConfig networkAddressConfig;
    private final AtomicBoolean maintenance = new AtomicBoolean(false);
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private final AtomicLong networkCacheVersions = new AtomicLong();
    private final ConcurrentMap<String, NetworkCacheValue> networkCache = new ConcurrentHashMap<>();
    private final List<PacketSubscription> subscriptions = new ArrayList<>();

    public NodePluginGateway(NodeWrapperRegistry wrapperRegistry, eu.kryocloud.node.service.runtime.NodeServiceRegistry serviceRegistry, NodeServiceScheduler serviceScheduler, GroupManager groupManager, TemplateManager templateManager, NodeVersionStorage versionStorage, NetworkAddressConfig networkAddressConfig) {
        this.wrapperRegistry = wrapperRegistry;
        this.serviceRegistry = serviceRegistry;
        this.serviceScheduler = serviceScheduler;
        this.groupManager = groupManager;
        this.templateManager = templateManager;
        this.versionStorage = versionStorage;
        this.networkAddressConfig = networkAddressConfig;
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
            case "network.message.publish" -> networkMessagePublish(packet);
            case "network.cache.put" -> networkCachePut(packet);
            case "network.cache.get" -> networkCacheGet(packet.payload());
            case "network.cache.remove" -> networkCacheRemove(packet);
            case "network.cache.keys" -> networkCacheKeys(packet.payload());
            case "console.commands" -> consoleCommands();
            case "console.suggest" -> consoleSuggest(packet.payload());
            case "console.execute" -> consoleExecute(packet.payload());
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

    private Map<String, String> consoleCommands() {
        List<Map<String, String>> commands = List.of(
                commandSpec("help", "Shows ingame cloud command help", "help", true, false, List.of("?")),
                commandSpec("cloud", "Shows KryoCloud runtime information", "cloud info", true, false, List.of("kryo", "kryocloud", "home")),
                commandSpec("service", "Controls Minecraft services", "service <list|running|cleanup|service> ...", true, false, List.of("services", "server", "servers", "instance", "instances")),
                commandSpec("group", "Controls Minecraft groups", "group <list|group> ...", true, false, List.of("groups", "task", "tasks")),
                commandSpec("wrapper", "Lists and inspects wrappers", "wrapper <list|timedout|wrapper> [info]", true, false, List.of("wrappers", "node", "nodes")),
                commandSpec("ip", "Manages Minecraft bind addresses", "ip <list|add|remove|default> ...", true, false, List.of("ips", "address", "addresses", "bind")),
                commandSpec("version", "Manages Minecraft software versions", "version <list|refresh|software> ...", true, false, List.of("versions", "software")),
                commandSpec("stats", "Shows KryoCloud runtime stats", "stats [groups|group <name>]", true, false, List.of("usage", "metrics")),
                commandSpec("shutdown", "Stops KryoCloud from the Cloud CLI", "shutdown", false, true, List.of("stop", "exit", "quit"))
        );

        return list("commands", commands);
    }

    private Map<String, String> consoleSuggest(Map<String, String> payload) {
        return stringList("suggestions", suggestions(consoleArguments(payload)));
    }

    private Map<String, String> consoleExecute(Map<String, String> payload) {
        List<String> arguments = consoleArguments(payload);

        if (arguments.isEmpty()) {
            return consoleHelp();
        }

        String root = arguments.getFirst().toLowerCase(java.util.Locale.ROOT);
        List<String> tail = arguments.size() <= 1 ? List.of() : arguments.subList(1, arguments.size());

        return switch (root) {
            case "help", "?" -> consoleHelp();
            case "shutdown", "stop", "exit", "quit" -> consoleCliOnly("shutdown");
            case "cloud", "kryo", "kryocloud", "home" -> consoleCloud(tail);
            case "service", "services", "server", "servers", "instance", "instances" -> consoleService(tail);
            case "group", "groups", "task", "tasks" -> consoleGroup(tail);
            case "wrapper", "wrappers", "node", "nodes" -> consoleWrapper(tail);
            case "ip", "ips", "address", "addresses", "bind" -> consoleIp(tail);
            case "version", "versions", "software" -> consoleVersion(tail);
            case "stats", "usage", "metrics" -> consoleStats(tail);
            default -> consoleFail("Unknown command. Use /cloud help.");
        };
    }

    private Map<String, String> consoleCloud(List<String> arguments) {
        if (arguments.isEmpty() || keyword(arguments.getFirst(), "info")) {
            return consoleOk("KryoCloud runtime information", List.of(
                    "Home: " + KryoDirectoryLayout.ROOT,
                    "Source: " + KryoDirectoryLayout.homeSource(),
                    "Config: " + KryoDirectoryLayout.CONFIG,
                    "Templates: " + KryoDirectoryLayout.TEMPLATES,
                    "Storage: " + KryoDirectoryLayout.STORAGE,
                    "Static: " + KryoDirectoryLayout.STATIC,
                    "Temporary: " + KryoDirectoryLayout.TMP,
                    "JDK: " + KryoDirectoryLayout.JDK
            ));
        }

        if (keyword(arguments.getFirst(), "home", "set", "reset")) {
            return consoleCliOnly("cloud home");
        }

        return consoleFail("Usage: cloud info");
    }

    private Map<String, String> consoleService(List<String> arguments) {
        if (arguments.isEmpty() || keyword(arguments.getFirst(), "list")) {
            return consoleServices(serviceRegistry.services(), "Minecraft services");
        }

        if (keyword(arguments.getFirst(), "running")) {
            return consoleServices(serviceRegistry.runningServices(), "Running Minecraft services");
        }

        if (keyword(arguments.getFirst(), "cleanup")) {
            return consoleServiceCleanup(arguments);
        }

        String service = arguments.getFirst();
        String action = arguments.size() >= 2 ? arguments.get(1) : "info";

        if (keyword(action, "info")) {
            return consoleServiceInfo(service);
        }

        if (keyword(action, "stop")) {
            serviceStop(Map.of("service", service), false);
            return consoleOk("Stop request sent", List.of("Stop request sent to " + service + "."));
        }

        if (keyword(action, "kill")) {
            serviceStop(Map.of("service", service), true);
            return consoleOk("Kill request sent", List.of("Kill request sent to " + service + "."));
        }

        if (keyword(action, "logs", "log")) {
            int lines = arguments.size() >= 3 ? positiveInteger(arguments.get(2), "lines") : 80;
            serviceLogs(Map.of("service", service, "tail", String.valueOf(lines)));
            return consoleOk("Logs request sent", List.of("Requested last " + lines + " log line(s) from " + service + "."));
        }

        if (keyword(action, "cmd", "command", "write")) {
            String command = join(arguments, 2);

            if (command.isBlank()) {
                return consoleFail("Usage: service " + service + " cmd <command>");
            }

            serviceCommand(Map.of("service", service, "command", command));
            return consoleOk("Command sent", List.of("Command sent to " + service + ": " + command));
        }

        return consoleFail("Usage: service " + service + " <info|stop|kill|logs|cmd>");
    }

    private Map<String, String> consoleServiceCleanup(List<String> arguments) {
        boolean dryRun = arguments.stream().anyMatch(argument -> keyword(argument, "--dry-run", "dry", "preview"));
        String target = cleanupTarget(arguments);

        if ("all".equalsIgnoreCase(target)) {
            Map<String, String> response = wrapperCleanupAll(Map.of("dryRun", String.valueOf(dryRun)));
            return consoleOk("Cleanup request sent", List.of((dryRun ? "Cleanup preview" : "Cleanup") + " requested on " + response.getOrDefault("sent", "0") + " wrapper(s)."));
        }

        wrapperCleanup(Map.of("wrapper", target, "dryRun", String.valueOf(dryRun)));
        return consoleOk("Cleanup request sent", List.of((dryRun ? "Cleanup preview" : "Cleanup") + " requested on " + target + "."));
    }

    private Map<String, String> consoleServiceInfo(String service) {
        Optional<NodeServiceSnapshot> snapshot = serviceRegistry.service(service);

        if (snapshot.isEmpty()) {
            return consoleFail("Unknown Minecraft service: " + service);
        }

        NodeServiceSnapshot current = snapshot.get();
        return consoleOk("Service " + current.serviceId(), List.of(
                "Service: " + current.serviceId(),
                "Group: " + current.groupName(),
                "Type: " + current.serviceType().name(),
                "State: " + current.state().name(),
                "Wrapper: " + current.wrapperId(),
                "Address: " + current.host() + ":" + current.port(),
                "Memory: " + current.processMemoryMb() + "MB",
                "CPU: " + String.format(java.util.Locale.ROOT, "%.1f%%", current.cpuLoadPermille() / 10.0D),
                "Uptime: " + current.uptimeMillis() + "ms",
                "Message: " + current.message()
        ));
    }

    private Map<String, String> consoleServices(List<NodeServiceSnapshot> services, String title) {
        if (services.isEmpty()) {
            return consoleOk(title, List.of("No Minecraft services known."));
        }

        List<String> lines = new ArrayList<>();
        lines.add(title + ":");

        for (NodeServiceSnapshot service : services) {
            lines.add(service.serviceId() + " • " + service.state().name() + " • " + service.groupName() + " • " + service.wrapperId() + " • " + service.host() + ":" + service.port());
        }

        return consoleOk(title, lines);
    }

    private Map<String, String> consoleGroup(List<String> arguments) {
        if (arguments.isEmpty() || keyword(arguments.getFirst(), "list")) {
            return consoleGroups();
        }

        if (keyword(arguments.getFirst(), "setup", "create")) {
            return consoleDisabled("group setup");
        }

        String group = arguments.getFirst();
        String action = arguments.size() >= 2 ? arguments.get(1) : "info";

        if (keyword(action, "info")) {
            return consoleGroupInfo(group);
        }

        if (keyword(action, "start")) {
            int count = arguments.size() >= 3 ? positiveInteger(arguments.get(2), "count") : missingMinimum(group);
            List<ServiceStartResult> results = serviceScheduler.startGroup(group, Math.max(1, count));
            return consoleStartResults("Started " + results.size() + " service(s) for " + group + ".", results, group);
        }

        if (keyword(action, "stop")) {
            int sent = stopGroup(group, false);
            return consoleOk("Stop request sent", List.of("Stop request sent to " + sent + " service(s) in " + group + "."));
        }

        if (keyword(action, "kill")) {
            int sent = stopGroup(group, true);
            return consoleOk("Kill request sent", List.of("Kill request sent to " + sent + " service(s) in " + group + "."));
        }

        if (keyword(action, "restart")) {
            int stopped = stopGroup(group, false);
            List<ServiceStartResult> results = serviceScheduler.startGroup(group, missingMinimum(group));
            List<String> lines = new ArrayList<>();
            lines.add("Stop request sent to " + stopped + " service(s) in " + group + ".");
            lines.add("Start requested for " + results.size() + " service(s).");
            addStartResultLines(lines, results, group);
            return consoleOk("Restart request sent", lines);
        }

        return consoleFail("Usage: group " + group + " <info|start|stop|kill|restart>");
    }

    private Map<String, String> consoleGroups() {
        Collection<IGroup> groups = groupManager.groups();

        if (groups.isEmpty()) {
            return consoleOk("Minecraft groups", List.of("No groups configured."));
        }

        List<String> lines = new ArrayList<>();
        lines.add("Minecraft groups:");

        for (IGroup group : groups) {
            lines.add(group.name() + " • " + group.serviceType().name() + " • " + group.minCount() + "/" + group.maxCount() + " • " + group.maxMemory() + "MB • " + group.templateName());
        }

        return consoleOk("Minecraft groups", lines);
    }

    private Map<String, String> consoleGroupInfo(String name) {
        IGroup group = groupManager.groupByName(name);

        if (group == null) {
            return consoleFail("Unknown group: " + name);
        }

        return consoleOk("Group " + group.name(), List.of(
                "Group: " + group.name(),
                "Type: " + group.serviceType().name(),
                "Min online: " + group.minCount(),
                "Max online: " + group.maxCount(),
                "Memory: " + group.maxMemory() + "MB",
                "Template: " + group.templateName(),
                "Version: " + group.softwareVersion(),
                "Online mode: " + group.onlineMode(),
                "Forwarding: " + group.forwardingMode()
        ));
    }

    private Map<String, String> consoleWrapper(List<String> arguments) {
        if (arguments.isEmpty() || keyword(arguments.getFirst(), "list")) {
            return consoleWrappers(wrapperRegistry.wrappers(), "Connected wrappers");
        }

        if (keyword(arguments.getFirst(), "timedout")) {
            return consoleWrappers(wrapperRegistry.timedOutWrappers(), "Timed out wrappers");
        }

        if (keyword(arguments.getFirst(), "cleanup")) {
            return consoleWrapperCleanup(arguments);
        }

        String wrapper = arguments.getFirst();
        String action = arguments.size() >= 2 ? arguments.get(1) : "info";

        if (keyword(action, "info")) {
            return consoleWrapperInfo(wrapper);
        }

        return consoleFail("Usage: wrapper " + wrapper + " info");
    }

    private Map<String, String> consoleWrapperCleanup(List<String> arguments) {
        boolean dryRun = arguments.stream().anyMatch(argument -> keyword(argument, "--dry-run", "dry", "preview"));
        String target = cleanupTarget(arguments);

        if ("all".equalsIgnoreCase(target)) {
            Map<String, String> response = wrapperCleanupAll(Map.of("dryRun", String.valueOf(dryRun)));
            return consoleOk("Cleanup request sent", List.of((dryRun ? "Cleanup preview" : "Cleanup") + " requested on " + response.getOrDefault("sent", "0") + " wrapper(s)."));
        }

        wrapperCleanup(Map.of("wrapper", target, "dryRun", String.valueOf(dryRun)));
        return consoleOk("Cleanup request sent", List.of((dryRun ? "Cleanup preview" : "Cleanup") + " requested on " + target + "."));
    }

    private Map<String, String> consoleWrappers(List<WrapperSnapshot> wrappers, String title) {
        if (wrappers.isEmpty()) {
            return consoleOk(title, List.of("No wrappers found."));
        }

        List<String> lines = new ArrayList<>();
        lines.add(title + ":");

        for (WrapperSnapshot wrapper : wrappers) {
            lines.add(wrapper.wrapperId() + " • " + wrapper.state().name() + " • services " + wrapper.runningServices() + " • RAM " + wrapper.usedMemoryMb() + "/" + wrapper.maxMemoryMb() + "MB • " + wrapper.remoteAddress());
        }

        return consoleOk(title, lines);
    }

    private Map<String, String> consoleWrapperInfo(String wrapper) {
        Optional<WrapperSnapshot> snapshot = wrapperRegistry.wrapper(wrapper);

        if (snapshot.isEmpty()) {
            return consoleFail("Unknown wrapper: " + wrapper);
        }

        WrapperSnapshot current = snapshot.get();
        return consoleOk("Wrapper " + current.wrapperId(), List.of(
                "Wrapper: " + current.wrapperId(),
                "State: " + current.state().name(),
                "Hostname: " + current.hostname(),
                "Address: " + current.address(),
                "OS: " + current.osName(),
                "Cores: " + current.availableProcessors(),
                "Process RAM: " + current.usedMemoryMb() + "/" + current.maxMemoryMb() + "MB",
                "Services: " + current.runningServices(),
                "Remote: " + current.remoteAddress()
        ));
    }

    private Map<String, String> consoleIp(List<String> arguments) {
        if (arguments.isEmpty() || keyword(arguments.getFirst(), "list")) {
            return consoleIpList();
        }

        String action = arguments.getFirst();

        if (keyword(action, "add")) {
            return consoleIpAdd(arguments);
        }

        if (keyword(action, "remove", "delete")) {
            return consoleIpRemove(arguments);
        }

        if (keyword(action, "default")) {
            return consoleIpDefault(arguments);
        }

        return consoleFail("Usage: ip list | ip add <server|proxy> <address> | ip remove <server|proxy> <address> | ip default <server|proxy> <address>");
    }

    private Map<String, String> consoleIpList() {
        List<String> lines = new ArrayList<>();
        lines.add("Minecraft bind addresses:");
        lines.add("Server default: " + networkAddressConfig.getDefaultServerAddress());
        lines.add("Proxy default: " + networkAddressConfig.getDefaultProxyAddress());
        lines.add("Server addresses: " + String.join(", ", networkAddressConfig.getServerAddresses()));
        lines.add("Proxy addresses: " + String.join(", ", networkAddressConfig.getProxyAddresses()));
        return consoleOk("Minecraft bind addresses", lines);
    }

    private Map<String, String> consoleIpAdd(List<String> arguments) {
        if (arguments.size() < 3) {
            return consoleFail("Usage: ip add <server|proxy> <address>");
        }

        String type = arguments.get(1);
        String address = arguments.get(2);

        if (serverType(type)) {
            networkAddressConfig.addServerAddress(address);
            networkAddressConfig.save();
            return consoleOk("Address added", List.of("Added server bind address " + address + "."));
        }

        if (proxyType(type)) {
            networkAddressConfig.addProxyAddress(address);
            networkAddressConfig.save();
            return consoleOk("Address added", List.of("Added proxy bind address " + address + "."));
        }

        return consoleFail("Type must be server or proxy.");
    }

    private Map<String, String> consoleIpRemove(List<String> arguments) {
        if (arguments.size() < 3) {
            return consoleFail("Usage: ip remove <server|proxy> <address>");
        }

        String type = arguments.get(1);
        String address = arguments.get(2);

        if (serverType(type)) {
            boolean removed = networkAddressConfig.removeServerAddress(address);
            networkAddressConfig.save();
            return consoleOk("Address removed", List.of(removed ? "Removed server bind address " + address + "." : "Server bind address was not registered: " + address));
        }

        if (proxyType(type)) {
            boolean removed = networkAddressConfig.removeProxyAddress(address);
            networkAddressConfig.save();
            return consoleOk("Address removed", List.of(removed ? "Removed proxy bind address " + address + "." : "Proxy bind address was not registered: " + address));
        }

        return consoleFail("Type must be server or proxy.");
    }

    private Map<String, String> consoleIpDefault(List<String> arguments) {
        if (arguments.size() < 3) {
            return consoleFail("Usage: ip default <server|proxy> <address>");
        }

        String type = arguments.get(1);
        String address = arguments.get(2);

        if (serverType(type)) {
            networkAddressConfig.setDefaultServerAddress(address);
            networkAddressConfig.save();
            return consoleOk("Default address updated", List.of("Default server bind address is now " + address + "."));
        }

        if (proxyType(type)) {
            networkAddressConfig.setDefaultProxyAddress(address);
            networkAddressConfig.save();
            return consoleOk("Default address updated", List.of("Default proxy bind address is now " + address + "."));
        }

        return consoleFail("Type must be server or proxy.");
    }

    private Map<String, String> consoleVersion(List<String> arguments) {
        if (arguments.isEmpty() || keyword(arguments.getFirst(), "list", "available")) {
            return consoleVersionSoftware();
        }

        if (keyword(arguments.getFirst(), "refresh", "reload")) {
            int refreshed = versionStorage.refreshManifests();
            publish(event("version.VersionRefreshedEvent"), "", "", "", Map.of("refreshed", String.valueOf(refreshed)));
            return consoleOk("Versions refreshed", List.of("Loaded " + refreshed + " Minecraft software manifest(s)."));
        }

        if (keyword(arguments.getFirst(), "create", "scan")) {
            return consoleCliOnly("version " + arguments.getFirst());
        }

        if (keyword(arguments.getFirst(), "install")) {
            if (arguments.size() < 2) {
                return consoleFail("Usage: version install <software> [version|latest]");
            }

            String software = arguments.get(1);
            String version = arguments.size() >= 3 ? arguments.get(2) : "latest";
            return consoleVersionInstall(software, version);
        }

        String software = arguments.getFirst();
        String action = arguments.size() >= 2 ? arguments.get(1) : "info";

        if (keyword(action, "list", "versions")) {
            return consoleVersionList(software);
        }

        if (keyword(action, "info")) {
            String version = arguments.size() >= 3 ? arguments.get(2) : "latest";
            return consoleVersionInfo(software, version);
        }

        if (keyword(action, "install")) {
            String version = arguments.size() >= 3 ? arguments.get(2) : "latest";
            return consoleVersionInstall(software, version);
        }

        return consoleFail("Usage: version " + software + " <list|info [version]|install [version]>");
    }

    private Map<String, String> consoleVersionSoftware() {
        List<String> software = versionStorage.availableSoftware();

        if (software.isEmpty()) {
            return consoleOk("Minecraft software manifests", List.of("No Minecraft software manifests are registered. Try /cloud version refresh."));
        }

        List<String> lines = new ArrayList<>();
        lines.add("Minecraft software manifests:");
        lines.add("Index: " + versionStorage.manifestIndexSource());
        lines.add("Cloud codename: " + versionStorage.latestCodename());

        if (!versionStorage.channels().isEmpty()) {
            lines.add("Channels: " + String.join(", ", versionStorage.channels()));
        }

        if (!versionStorage.codenames().isEmpty()) {
            lines.add("Cloud codenames:");

            for (ManifestCodename codename : versionStorage.codenames()) {
                lines.add("- " + codename.name() + " • " + String.join(", ", codename.versions()));
            }
        }

        lines.add("Minecraft software:");

        for (String entry : software) {
            lines.add(entry + " • " + versionStorage.manifestSource(entry));
        }

        return consoleOk("Minecraft software manifests", lines);
    }

    private Map<String, String> consoleVersionList(String software) {
        SoftwareManifest manifest = versionStorage.manifest(software);
        List<String> lines = new ArrayList<>();
        lines.add("Versions for " + software + ":");
        lines.add("Type: " + manifest.type().name());
        lines.add("Latest: " + manifest.latestVersion());

        for (String version : manifest.versions().keySet().stream().sorted(java.util.Comparator.reverseOrder()).toList()) {
            SoftwareVersion softwareVersion = manifest.versions().get(version);
            lines.add(version + " • Java " + softwareVersion.javaVersion() + " • " + softwareVersion.javaFlags().size() + " JVM flag(s)");
        }

        return consoleOk("Versions for " + software, lines);
    }

    private Map<String, String> consoleVersionInfo(String software, String requestedVersion) {
        SoftwareManifest manifest = versionStorage.manifest(software);
        SoftwareVersion version = manifest.resolve(requestedVersion);
        List<String> lines = new ArrayList<>();
        lines.add(software + " " + version.version());
        lines.add("Type: " + manifest.type().name());
        lines.add("Latest: " + manifest.latestVersion());
        lines.add("Java: " + version.javaVersion());
        lines.add("Manifest: " + versionStorage.manifestSource(software));
        lines.add("Download: " + version.link());

        if (!version.javaFlags().isEmpty()) {
            lines.add("JVM flags: " + String.join(" ", version.javaFlags()));
        }

        return consoleOk(software + " " + version.version(), lines);
    }

    private Map<String, String> consoleVersionInstall(String software, String version) {
        versionStorage.installFromManifest(software, version, true);
        publish(event("version.VersionInstalledEvent"), "", "", "", Map.of("version", version, "type", software, "path", versionStorage.rootDirectory().resolve(software).resolve(version).toString(), "installedAt", Instant.now().toString()));
        return consoleOk("Version installed", List.of("Installed " + software + " " + version + "."));
    }

    private Map<String, String> consoleStats(List<String> arguments) {
        if (arguments.isEmpty()) {
            Map<String, String> snapshot = stats();
            return consoleOk("KryoCloud stats", List.of(
                    "Wrappers: " + snapshot.getOrDefault("onlineWrappers", "0") + "/" + snapshot.getOrDefault("wrappers", "0"),
                    "Services: " + snapshot.getOrDefault("runningServices", "0") + "/" + snapshot.getOrDefault("services", "0"),
                    "Groups: " + snapshot.getOrDefault("groups", "0"),
                    "RAM: " + snapshot.getOrDefault("usedMemoryMb", "0") + "/" + snapshot.getOrDefault("maxMemoryMb", "0") + "MB"
            ));
        }

        if (keyword(arguments.getFirst(), "live")) {
            return consoleCliOnly("stats live");
        }

        if (keyword(arguments.getFirst(), "groups")) {
            return consoleStatsGroups();
        }

        if (keyword(arguments.getFirst(), "group")) {
            if (arguments.size() < 2) {
                return consoleFail("Usage: stats group <name>");
            }

            return consoleStatsGroup(arguments.get(1));
        }

        return consoleFail("Usage: stats [groups|group <name>]");
    }

    private Map<String, String> consoleStatsGroups() {
        Collection<IGroup> groups = groupManager.groups();

        if (groups.isEmpty()) {
            return consoleOk("Group stats", List.of("No groups configured."));
        }

        List<String> lines = new ArrayList<>();
        lines.add("Group stats:");

        for (IGroup group : groups) {
            long running = serviceRegistry.services(group.name()).stream().filter(service -> service.state() == CloudServiceState.RUNNING).count();
            lines.add(group.name() + " • " + running + "/" + group.maxCount() + " services • " + group.maxMemory() + "MB configured");
        }

        return consoleOk("Group stats", lines);
    }

    private Map<String, String> consoleStatsGroup(String name) {
        IGroup group = groupManager.groupByName(name);

        if (group == null) {
            return consoleFail("Unknown group: " + name);
        }

        List<NodeServiceSnapshot> services = serviceRegistry.services(group.name());
        long running = services.stream().filter(service -> service.state() == CloudServiceState.RUNNING).count();
        int memory = services.stream().mapToInt(NodeServiceSnapshot::processMemoryMb).sum();
        return consoleOk("Group " + group.name(), List.of(
                "Group: " + group.name(),
                "Type: " + group.serviceType().name(),
                "Services: " + running + "/" + group.maxCount(),
                "Known services: " + services.size(),
                "Process RAM: " + memory + "MB",
                "Configured RAM: " + group.maxMemory() + "MB",
                "Static: " + group.staticServices()
        ));
    }

    private Map<String, String> consoleHelp() {
        return consoleOk("KryoCloud ingame commands", List.of(
                "/cloud service list",
                "/cloud service <service> <info|stop|kill|logs|cmd>",
                "/cloud group list",
                "/cloud group <group> <info|start|stop|kill|restart>",
                "/cloud wrapper list",
                "/cloud wrapper <wrapper> info",
                "/cloud ip list",
                "/cloud version list",
                "/cloud stats",
                "/cloud shutdown"
        ));
    }

    private Map<String, String> consoleCliOnly(String command) {
        return consoleFail("The command '" + command + "' can only be executed in the Cloud CLI.");
    }

    private Map<String, String> consoleDisabled(String command) {
        return consoleFail("The command '" + command + "' is disabled ingame because it opens an interactive setup wizard.");
    }

    private Map<String, String> consoleOk(String message, List<String> lines) {
        return consoleResult(true, message, lines);
    }

    private Map<String, String> consoleFail(String message) {
        return consoleResult(false, message, List.of(message));
    }

    private Map<String, String> consoleStartResults(String message, List<ServiceStartResult> results, String group) {
        List<String> lines = new ArrayList<>();
        lines.add(message);
        addStartResultLines(lines, results, group);
        return consoleOk(message, lines);
    }

    private void addStartResultLines(List<String> lines, List<ServiceStartResult> results, String group) {
        for (ServiceStartResult result : results) {
            lines.add(result.serviceId() + " • " + group + " • " + result.wrapperId());
        }
    }

    private Map<String, String> consoleResult(boolean success, String message, List<String> lines) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("success", String.valueOf(success));
        values.put("message", message == null ? "" : message);
        values.putAll(stringList("lines", lines == null || lines.isEmpty() ? List.of(message == null ? "" : message) : lines));
        return Map.copyOf(values);
    }

    private Map<String, String> commandSpec(String name, String description, String usage, boolean executable, boolean cliOnly, List<String> aliases) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("name", name);
        values.put("description", description);
        values.put("usage", usage);
        values.put("executable", String.valueOf(executable));
        values.put("cliOnly", String.valueOf(cliOnly));
        values.putAll(stringList("aliases", aliases));
        return Map.copyOf(values);
    }

    private Map<String, String> stringList(String prefix, List<String> values) {
        Map<String, String> payload = new LinkedHashMap<>();
        List<String> safeValues = values == null ? List.of() : values;
        payload.put(prefix + ".size", String.valueOf(safeValues.size()));

        for (int index = 0; index < safeValues.size(); index++) {
            payload.put(prefix + "." + index, safeValues.get(index));
        }

        return Map.copyOf(payload);
    }

    private List<String> suggestions(List<String> arguments) {
        if (arguments.isEmpty()) {
            return rootSuggestions();
        }

        String root = arguments.getFirst().toLowerCase(java.util.Locale.ROOT);
        int index = arguments.size() - 1;

        if (arguments.size() == 1) {
            return filtered(rootSuggestions(), root);
        }

        return switch (root) {
            case "cloud", "kryo", "kryocloud", "home" -> filtered(List.of("info", "home"), arguments.get(index));
            case "service", "services", "server", "servers", "instance", "instances" -> serviceSuggestions(arguments, index);
            case "group", "groups", "task", "tasks" -> groupSuggestions(arguments, index);
            case "wrapper", "wrappers", "node", "nodes" -> wrapperSuggestions(arguments, index);
            case "ip", "ips", "address", "addresses", "bind" -> ipSuggestions(arguments, index);
            case "version", "versions", "software" -> versionSuggestions(arguments, index);
            case "stats", "usage", "metrics" -> statsSuggestions(arguments, index);
            default -> List.of();
        };
    }

    private List<String> rootSuggestions() {
        return List.of("help", "cloud", "service", "group", "wrapper", "ip", "version", "stats", "shutdown");
    }

    private List<String> serviceSuggestions(List<String> arguments, int index) {
        if (index == 1) {
            List<String> values = new ArrayList<>(List.of("list", "running", "cleanup"));
            serviceRegistry.services().forEach(service -> values.add(service.serviceId()));
            return filtered(values, arguments.get(index));
        }

        if (index == 2 && !keyword(arguments.get(1), "list", "running", "cleanup")) {
            return filtered(List.of("info", "stop", "kill", "logs", "cmd", "command"), arguments.get(index));
        }

        if (index == 2 && keyword(arguments.get(1), "cleanup")) {
            List<String> values = new ArrayList<>(List.of("all", "--dry-run"));
            wrapperRegistry.wrappers().forEach(wrapper -> values.add(wrapper.wrapperId()));
            return filtered(values, arguments.get(index));
        }

        if (index == 3 && keyword(arguments.get(1), "cleanup")) {
            return filtered(List.of("--dry-run"), arguments.get(index));
        }

        return List.of();
    }

    private List<String> groupSuggestions(List<String> arguments, int index) {
        if (index == 1) {
            List<String> values = new ArrayList<>(List.of("list"));
            groupManager.groups().forEach(group -> values.add(group.name()));
            return filtered(values, arguments.get(index));
        }

        if (index == 2 && !keyword(arguments.get(1), "list")) {
            return filtered(List.of("info", "start", "stop", "kill", "restart"), arguments.get(index));
        }

        return List.of();
    }

    private List<String> wrapperSuggestions(List<String> arguments, int index) {
        if (index == 1) {
            List<String> values = new ArrayList<>(List.of("list", "timedout", "cleanup"));
            wrapperRegistry.wrappers().forEach(wrapper -> values.add(wrapper.wrapperId()));
            return filtered(values, arguments.get(index));
        }

        if (index == 2 && !keyword(arguments.get(1), "list", "timedout", "cleanup")) {
            return filtered(List.of("info"), arguments.get(index));
        }

        if (index == 2 && keyword(arguments.get(1), "cleanup")) {
            List<String> values = new ArrayList<>(List.of("all", "--dry-run"));
            wrapperRegistry.wrappers().forEach(wrapper -> values.add(wrapper.wrapperId()));
            return filtered(values, arguments.get(index));
        }

        return List.of();
    }

    private List<String> ipSuggestions(List<String> arguments, int index) {
        if (index == 1) {
            return filtered(List.of("list", "add", "remove", "default"), arguments.get(index));
        }

        if (index == 2 && keyword(arguments.get(1), "add", "remove", "default")) {
            return filtered(List.of("server", "proxy"), arguments.get(index));
        }

        if (index == 3 && keyword(arguments.get(1), "remove", "default")) {
            return filtered(addresses(arguments.get(2)), arguments.get(index));
        }

        return List.of();
    }

    private List<String> versionSuggestions(List<String> arguments, int index) {
        if (index == 1) {
            List<String> values = new ArrayList<>(List.of("list", "refresh", "install"));
            values.addAll(versionStorage.availableSoftware());
            return filtered(values, arguments.get(index));
        }

        if (index == 2 && keyword(arguments.get(1), "install")) {
            return filtered(versionStorage.availableSoftware(), arguments.get(index));
        }

        if (index == 2 && !keyword(arguments.get(1), "list", "refresh", "install")) {
            return filtered(List.of("list", "versions", "info", "install"), arguments.get(index));
        }

        if (index == 3 && !keyword(arguments.get(1), "list", "refresh", "install")) {
            return filtered(versions(arguments.get(1)), arguments.get(index));
        }

        if (index == 3 && keyword(arguments.get(1), "install")) {
            return filtered(versions(arguments.get(2)), arguments.get(index));
        }

        return List.of();
    }

    private List<String> statsSuggestions(List<String> arguments, int index) {
        if (index == 1) {
            return filtered(List.of("groups", "group", "live"), arguments.get(index));
        }

        if (index == 2 && keyword(arguments.get(1), "group")) {
            List<String> values = groupManager.groups().stream().map(IGroup::name).toList();
            return filtered(values, arguments.get(index));
        }

        return List.of();
    }

    private List<String> versions(String software) {
        try {
            List<String> versions = new ArrayList<>(List.of("latest"));
            versions.addAll(versionStorage.availableVersions(software));
            return versions;
        } catch (Exception exception) {
            return List.of("latest");
        }
    }

    private List<String> addresses(String type) {
        if (proxyType(type)) {
            return networkAddressConfig.getProxyAddresses();
        }

        return networkAddressConfig.getServerAddresses();
    }

    private List<String> filtered(List<String> values, String prefix) {
        String value = prefix == null ? "" : prefix.toLowerCase(java.util.Locale.ROOT);
        return values.stream().filter(entry -> entry.toLowerCase(java.util.Locale.ROOT).startsWith(value)).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private List<String> consoleArguments(Map<String, String> payload) {
        int size = integer(payload, "arguments.size", -1);

        if (size >= 0) {
            List<String> arguments = new ArrayList<>();

            for (int index = 0; index < size; index++) {
                String value = payload.getOrDefault("arguments." + index, "");

                if (value.isBlank()) {
                    continue;
                }

                arguments.add(value);
            }

            return List.copyOf(arguments);
        }

        return tokenize(payload.getOrDefault("input", ""));
    }

    private List<String> tokenize(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);

            if (character == '"') {
                quoted = !quoted;
                continue;
            }

            if (Character.isWhitespace(character) && !quoted) {
                if (buffer.isEmpty()) {
                    continue;
                }

                values.add(buffer.toString());
                buffer.setLength(0);
                continue;
            }

            buffer.append(character);
        }

        if (!buffer.isEmpty()) {
            values.add(buffer.toString());
        }

        return List.copyOf(values);
    }

    private String cleanupTarget(List<String> arguments) {
        for (String argument : arguments.subList(1, arguments.size())) {
            if (keyword(argument, "--dry-run", "dry", "preview")) {
                continue;
            }

            return argument;
        }

        return "all";
    }

    private int stopGroup(String group, boolean force) {
        int sent = 0;

        for (NodeServiceSnapshot service : serviceRegistry.services(group)) {
            if (service.state() == CloudServiceState.STOPPED || service.state() == CloudServiceState.FAILED) {
                continue;
            }

            serviceStop(Map.of("service", service.serviceId()), force);
            sent++;
        }

        return sent;
    }

    private int missingMinimum(String groupName) {
        IGroup group = groupManager.groupByName(groupName);

        if (group == null) {
            throw new IllegalArgumentException("Unknown group: " + groupName);
        }

        return Math.max(1, group.minCount());
    }

    private int positiveInteger(String input, String name) {
        try {
            int value = Integer.parseInt(input);

            if (value < 1) {
                throw new IllegalArgumentException(name + " must be greater than 0");
            }

            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a valid number", exception);
        }
    }

    private String join(List<String> arguments, int start) {
        if (arguments.size() <= start) {
            return "";
        }

        return String.join(" ", arguments.subList(start, arguments.size()));
    }

    private boolean serverType(String type) {
        return "server".equalsIgnoreCase(type) || "local".equalsIgnoreCase(type) || "backend".equalsIgnoreCase(type);
    }

    private boolean proxyType(String type) {
        return "proxy".equalsIgnoreCase(type) || "public".equalsIgnoreCase(type);
    }

    private boolean keyword(String value, String... keywords) {
        for (String keyword : keywords) {
            if (keyword.equalsIgnoreCase(value)) {
                return true;
            }
        }

        return false;
    }



    private Map<String, String> networkMessagePublish(PluginGatewayRequestPacket packet) {
        Map<String, String> payload = packet.payload();
        String channel = required(payload, "channel");
        String data = payload.getOrDefault("payload", "");
        validateBase64(data, "payload");

        Map<String, String> values = new LinkedHashMap<>();
        values.put("channel", channel);
        values.put("payload", data);
        values.put("sourcePlugin", packet.pluginId());
        values.put("sourceService", packet.serviceId());
        values.put("sourceGroup", sourceGroup(packet));
        values.put("sourceWrapper", packet.wrapperId());
        values.put("createdAt", Instant.now().toString());

        int receivers = publish(NetworkChannelMessageEvent.class.getName(), target(payload, "targetService"), target(payload, "targetGroup"), target(payload, "targetWrapper"), values);
        return Map.of("published", "true", "receivers", String.valueOf(receivers));
    }

    private Map<String, String> networkCachePut(PluginGatewayRequestPacket packet) {
        Map<String, String> payload = packet.payload();
        String key = networkKey(required(payload, "key"));
        String value = payload.getOrDefault("value", "");
        validateBase64(value, "value");

        long ttlMillis = Math.max(0L, longValue(payload, "ttlMillis", 0L));
        Instant now = Instant.now();
        String expiresAt = ttlMillis <= 0L ? "" : now.plusMillis(ttlMillis).toString();
        NetworkCacheValue entry = new NetworkCacheValue(
                key,
                value,
                payload.getOrDefault("contentType", "application/octet-stream"),
                networkCacheVersions.incrementAndGet(),
                now.toString(),
                expiresAt,
                packet.pluginId(),
                packet.serviceId(),
                sourceGroup(packet),
                packet.wrapperId()
        );

        networkCache.put(key, entry);
        Map<String, String> values = entry.payload("true", "PUT");
        publish(NetworkCacheChangedEvent.class.getName(), target(payload, "targetService"), target(payload, "targetGroup"), target(payload, "targetWrapper"), values);
        return values;
    }

    private Map<String, String> networkCacheGet(Map<String, String> payload) {
        String key = networkKey(required(payload, "key"));
        NetworkCacheValue entry = cacheEntry(key);

        if (entry == null) {
            return Map.of("present", "false", "key", key);
        }

        return entry.payload("true", "PUT");
    }

    private Map<String, String> networkCacheRemove(PluginGatewayRequestPacket packet) {
        Map<String, String> payload = packet.payload();
        String key = networkKey(required(payload, "key"));
        NetworkCacheValue removed = networkCache.remove(key);

        if (removed == null) {
            return Map.of("removed", "false", "key", key);
        }

        Map<String, String> values = removed.payload("false", "REMOVE");
        publish(NetworkCacheChangedEvent.class.getName(), target(payload, "targetService"), target(payload, "targetGroup"), target(payload, "targetWrapper"), values);
        return Map.of("removed", "true", "key", key, "version", String.valueOf(removed.version()));
    }

    private Map<String, String> networkCacheKeys(Map<String, String> payload) {
        String prefix = payload.getOrDefault("prefix", "");
        List<String> keys = networkCache.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .filter(key -> cacheEntry(key) != null)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        return stringList("keys", keys);
    }

    private NetworkCacheValue cacheEntry(String key) {
        NetworkCacheValue entry = networkCache.get(key);

        if (entry == null) {
            return null;
        }

        if (!entry.expired()) {
            return entry;
        }

        if (networkCache.remove(key, entry)) {
            publish(NetworkCacheChangedEvent.class.getName(), "", "", "", entry.payload("false", "EXPIRE"));
        }

        return null;
    }

    private String sourceGroup(PluginGatewayRequestPacket packet) {
        if (packet.serviceId() == null || packet.serviceId().isBlank()) {
            return "";
        }

        return serviceRegistry.service(packet.serviceId()).map(NodeServiceSnapshot::groupName).orElse("");
    }

    private String target(Map<String, String> payload, String key) {
        String value = payload.get(key);
        return value == null ? "" : value.trim();
    }

    private String networkKey(String key) {
        String value = key == null ? "" : key.trim();

        if (value.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }

        if (value.length() > 256) {
            throw new IllegalArgumentException("key must not be longer than 256 characters");
        }

        return value;
    }

    private void validateBase64(String value, String field) {
        try {
            Base64.getDecoder().decode(value == null ? "" : value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(field + " must be valid base64", exception);
        }
    }

    private long longValue(Map<String, String> payload, String key, long fallback) {
        try {
            return Long.parseLong(payload.getOrDefault(key, String.valueOf(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int publish(String route, String service, String group, String wrapper, Map<String, String> payload) {
        PluginGatewayEventPacket packet = new PluginGatewayEventPacket(UUID.randomUUID(), route, service == null ? "" : service, group == null ? "" : group, wrapper == null ? "" : wrapper, payload);

        if (wrapper != null && !wrapper.isBlank()) {
            Optional<KryoConnection> connection = wrapperRegistry.connection(wrapper);

            if (connection.isEmpty()) {
                return 0;
            }

            connection.get().send(packet);
            return 1;
        }

        int sent = 0;

        for (WrapperSnapshot snapshot : wrapperRegistry.wrappers()) {
            Optional<KryoConnection> connection = wrapperRegistry.connection(snapshot.wrapperId());

            if (connection.isEmpty()) {
                continue;
            }

            connection.get().send(packet);
            sent++;
        }

        return sent;
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
            return Map.of("group", "unknown", "type", "UNKNOWN", "minOnline", "0", "maxOnline", "0", "memoryMb", "0", "template", "", "version", "", "onlineMode", "AUTO", "forwardingMode", "AUTO");
        }

        return Map.of("group", group.name(), "type", group.serviceType().name(), "minOnline", String.valueOf(group.minCount()), "maxOnline", String.valueOf(group.maxCount()), "memoryMb", String.valueOf(group.maxMemory()), "template", group.templateName(), "version", group.softwareVersion(), "onlineMode", group.onlineMode(), "forwardingMode", group.forwardingMode());
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


    private record NetworkCacheValue(String key, String value, String contentType, long version, String updatedAt, String expiresAt, String sourcePlugin, String sourceService, String sourceGroup, String sourceWrapper) {

        boolean expired() {
            if (expiresAt == null || expiresAt.isBlank()) {
                return false;
            }

            try {
                return Instant.parse(expiresAt).isBefore(Instant.now());
            } catch (Exception ignored) {
                return false;
            }
        }

        Map<String, String> payload(String present, String changeType) {
            Map<String, String> values = new LinkedHashMap<>();
            values.put("present", present == null ? "true" : present);
            values.put("changeType", changeType == null || changeType.isBlank() ? "PUT" : changeType);
            values.put("key", key);
            values.put("value", value);
            values.put("contentType", contentType);
            values.put("version", String.valueOf(version));
            values.put("updatedAt", updatedAt);
            values.put("expiresAt", expiresAt == null ? "" : expiresAt);
            values.put("sourcePlugin", sourcePlugin);
            values.put("sourceService", sourceService);
            values.put("sourceGroup", sourceGroup);
            values.put("sourceWrapper", sourceWrapper);
            return Map.copyOf(values);
        }

    }

}
