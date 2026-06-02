package eu.kryocloud.node.service;

import eu.kryocloud.api.group.IGroup;
import eu.kryocloud.api.player.IPlayer;
import eu.kryocloud.api.service.IService;
import eu.kryocloud.api.service.ServiceType;
import eu.kryocloud.api.template.ITemplate;
import eu.kryocloud.api.wrapper.IWrapper;
import eu.kryocloud.network.protocol.CloudServiceType;
import eu.kryocloud.node.service.runtime.NodeServiceSnapshot;
import eu.kryocloud.node.service.schedule.NodeServiceScheduler;
import eu.kryocloud.node.template.TemplateManager;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class CloudService implements IService {

    private final String name;
    private final IGroup group;
    private final NodeServiceScheduler scheduler;
    private final TemplateManager templateManager;
    private final Optional<NodeServiceSnapshot> snapshot;

    public CloudService(String name, IGroup group, NodeServiceScheduler scheduler, TemplateManager templateManager, Optional<NodeServiceSnapshot> snapshot) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        if (group == null) {
            throw new IllegalArgumentException("group must not be null");
        }

        if (scheduler == null) {
            throw new IllegalArgumentException("scheduler must not be null");
        }

        if (templateManager == null) {
            throw new IllegalArgumentException("templateManager must not be null");
        }

        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null");
        }

        this.name = name;
        this.group = group;
        this.scheduler = scheduler;
        this.templateManager = templateManager;
        this.snapshot = snapshot;
    }

    @Override
    public UUID uniqueId() {
        return UUID.nameUUIDFromBytes(("kryocloud:service:" + name).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String javaVersion() {
        return group.javaVersion();
    }

    @Override
    public String groupName() {
        return group.name();
    }

    @Override
    public String templateName() {
        return group.templateName();
    }

    @Override
    public String host() {
        return snapshot.map(NodeServiceSnapshot::host).orElse(group.bindAddress());
    }

    @Override
    public ServiceType serviceType() {
        return snapshot.map(NodeServiceSnapshot::serviceType).map(this::mapType).orElse(group.serviceType());
    }

    @Override
    public IGroup group() {
        return group;
    }

    @Override
    public ITemplate template() {
        return templateManager.template(group.templateName());
    }

    @Override
    public IWrapper worker() {
        return snapshot.map(value -> new CloudWorker(value.wrapperId())).orElse(null);
    }

    @Override
    public Collection<IPlayer> onlinePlayers() {
        return List.of();
    }

    @Override
    public int serviceNumber() {
        String prefix = group.name() + "-";

        if (name.startsWith(prefix)) {
            return parseServiceNumber(name.substring(prefix.length()));
        }

        int separator = name.lastIndexOf('-');

        if (separator >= 0 && separator < name.length() - 1) {
            return parseServiceNumber(name.substring(separator + 1));
        }

        return 0;
    }

    @Override
    public int minMemory() {
        return group.minMemory();
    }

    @Override
    public int maxMemory() {
        return group.maxMemory();
    }

    @Override
    public int maxPlayers() {
        return group.maxPlayers();
    }

    @Override
    public int port() {
        return snapshot.map(NodeServiceSnapshot::port).orElse(group.basePort());
    }

    @Override
    public boolean staticDirectory() {
        return group.staticServices();
    }

    @Override
    public void start() {
        if (snapshot.isPresent()) {
            return;
        }

        scheduler.startGroup(group.name(), 1);
    }

    @Override
    public void stop() {
        scheduler.stopService(name, "KryoCloud API service stop", false);
    }

    @Override
    public void restart() {
        stop();
        start();
    }

    private ServiceType mapType(CloudServiceType type) {
        if (type == CloudServiceType.PROXY) {
            return ServiceType.PROXY;
        }

        return group.serviceType();
    }

    private int parseServiceNumber(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

}
