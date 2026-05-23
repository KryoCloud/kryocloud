package eu.kryocloud.node.service.schedule;

import eu.kryocloud.api.group.IGroup;
import eu.kryocloud.api.group.IGroupManager;
import eu.kryocloud.api.service.ServiceType;
import eu.kryocloud.network.connection.KryoConnection;
import eu.kryocloud.network.packet.type.service.ServiceStartRequestPacket;
import eu.kryocloud.network.protocol.CloudServiceType;
import eu.kryocloud.node.version.NodeVersionStorage;
import eu.kryocloud.node.wrapper.NodeWrapperRegistry;
import eu.kryocloud.node.wrapper.WrapperSnapshot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class NodeServiceScheduler {

    private final NodeWrapperRegistry wrapperRegistry;
    private final IGroupManager groupManager;
    private final NodeVersionStorage versionStorage;
    private final ConcurrentMap<String, Integer> nextServiceNumberByGroup = new ConcurrentHashMap<>();

    public NodeServiceScheduler(NodeWrapperRegistry wrapperRegistry, IGroupManager groupManager, NodeVersionStorage versionStorage) {
        if (wrapperRegistry == null) {
            throw new IllegalArgumentException("wrapperRegistry must not be null");
        }

        if (groupManager == null) {
            throw new IllegalArgumentException("groupManager must not be null");
        }

        if (versionStorage == null) {
            throw new IllegalArgumentException("versionStorage must not be null");
        }

        this.wrapperRegistry = wrapperRegistry;
        this.groupManager = groupManager;
        this.versionStorage = versionStorage;
    }

    public ServiceStartResult start(ServiceStartPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("plan must not be null");
        }

        Optional<WrapperSnapshot> optionalWrapper = selectWrapper(plan);

        if (optionalWrapper.isEmpty()) {
            throw new IllegalStateException("No available wrapper found for service " + plan.serviceId());
        }

        WrapperSnapshot wrapper = optionalWrapper.get();
        Optional<KryoConnection> optionalConnection = wrapperRegistry.connection(wrapper.wrapperId());

        if (optionalConnection.isEmpty()) {
            throw new IllegalStateException("Selected wrapper " + wrapper.wrapperId() + " has no active connection");
        }

        UUID requestId = UUID.randomUUID();
        optionalConnection.get().send(new ServiceStartRequestPacket(requestId, plan.serviceId(), plan.groupName(), plan.templateName(), plan.serviceType(), plan.port(), plan.maxMemoryMb(), plan.staticService()));

        return new ServiceStartResult(requestId, plan.serviceId(), wrapper.wrapperId());
    }

    public List<ServiceStartResult> startGroup(String groupName, int count) {
        validateGroupName(groupName);

        if (count < 1) {
            throw new IllegalArgumentException("count must be greater than 0");
        }

        IGroup group = groupManager.groupByName(groupName);

        if (group == null) {
            throw new IllegalArgumentException("Unknown group: " + groupName);
        }

        ensureGroupSoftware(group);

        return java.util.stream.IntStream.range(0, count).mapToObj(index -> start(planFromGroup(group))).toList();
    }

    public Optional<WrapperSnapshot> selectWrapper(ServiceStartPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("plan must not be null");
        }

        List<WrapperSnapshot> candidates = wrapperRegistry.availableWrappersForMemory(plan.maxMemoryMb());

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(candidates.getFirst());
    }

    private void ensureGroupSoftware(IGroup group) {
        if (!group.installOnStart()) {
            return;
        }

        if (!versionStorage.installed(group.software(), group.softwareVersion())) {
            versionStorage.installFromManifest(group.software(), group.softwareVersion(), false);
        }

        versionStorage.materializeTemplate(group.software(), group.softwareVersion(), group.templateName());
    }

    private ServiceStartPlan planFromGroup(IGroup group) {
        int serviceNumber = nextServiceNumberByGroup.compute(normalize(group.name()), (name, current) -> current == null ? 1 : current + 1);
        int port = group.basePort() + serviceNumber - 1;
        return new ServiceStartPlan(group.name() + "-" + serviceNumber, group.name(), group.templateName(), mapType(group.serviceType()), port, group.maxMemory(), group.staticServices());
    }

    private CloudServiceType mapType(ServiceType type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }

        if (type == ServiceType.PROXY) {
            return CloudServiceType.PROXY;
        }

        return CloudServiceType.SERVER;
    }

    private void validateGroupName(String groupName) {
        if (groupName == null || groupName.isBlank()) {
            throw new IllegalArgumentException("groupName must not be blank");
        }
    }

    private String normalize(String value) {
        return value.toLowerCase();
    }
}
