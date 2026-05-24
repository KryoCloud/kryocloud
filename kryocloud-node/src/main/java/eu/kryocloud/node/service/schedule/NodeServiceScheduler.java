package eu.kryocloud.node.service.schedule;

import eu.kryocloud.api.group.IGroup;
import eu.kryocloud.api.group.IGroupManager;
import eu.kryocloud.api.service.ServiceType;
import eu.kryocloud.common.logging.KryoLogger;
import eu.kryocloud.network.connection.KryoConnection;
import eu.kryocloud.network.packet.type.service.ServiceStartRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceStopRequestPacket;
import eu.kryocloud.network.protocol.CloudServiceType;
import eu.kryocloud.node.service.runtime.NodeServiceRegistry;
import eu.kryocloud.node.service.runtime.NodeServiceSnapshot;
import eu.kryocloud.node.version.NodeVersionStorage;
import eu.kryocloud.node.wrapper.NodeWrapperRegistry;
import eu.kryocloud.node.wrapper.WrapperSnapshot;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NodeServiceScheduler {

    private static final KryoLogger LOGGER = KryoLogger.logger("Scheduler");

    private final NodeWrapperRegistry wrapperRegistry;
    private final IGroupManager groupManager;
    private final NodeServiceRegistry serviceRegistry;
    private final NodeVersionStorage versionStorage;
    private final AtomicBoolean reconciling = new AtomicBoolean(false);

    public NodeServiceScheduler(NodeWrapperRegistry wrapperRegistry, IGroupManager groupManager, NodeServiceRegistry serviceRegistry, NodeVersionStorage versionStorage) {
        if (wrapperRegistry == null) {
            throw new IllegalArgumentException("wrapperRegistry must not be null");
        }

        if (groupManager == null) {
            throw new IllegalArgumentException("groupManager must not be null");
        }

        if (serviceRegistry == null) {
            throw new IllegalArgumentException("serviceRegistry must not be null");
        }

        if (versionStorage == null) {
            throw new IllegalArgumentException("versionStorage must not be null");
        }

        this.wrapperRegistry = wrapperRegistry;
        this.groupManager = groupManager;
        this.serviceRegistry = serviceRegistry;
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

        List<ServiceStartPlan> plans = plansFromGroup(group, count);
        List<ServiceStartResult> results = new ArrayList<>();

        for (ServiceStartPlan plan : plans) {
            results.add(start(plan));
        }

        return List.copyOf(results);
    }

    public List<ServiceStartResult> reconcileMinimumServices() {
        if (!reconciling.compareAndSet(false, true)) {
            return List.of();
        }

        try {
            List<ServiceStartResult> results = new ArrayList<>();

            for (IGroup group : groupManager.groups()) {
                results.addAll(reconcileGroup(group.name()));
            }

            return List.copyOf(results);
        } finally {
            reconciling.set(false);
        }
    }

    public List<ServiceStartResult> reconcileGroup(String groupName) {
        validateGroupName(groupName);

        IGroup group = groupManager.groupByName(groupName);

        if (group == null) {
            throw new IllegalArgumentException("Unknown group: " + groupName);
        }

        int activeServices = serviceRegistry.activeServiceCount(group.name());
        int missingServices = Math.max(0, group.minCount() - activeServices);

        if (missingServices < 1) {
            return List.of();
        }

        LOGGER.info("Auto-starting " + missingServices + " Minecraft service(s) for group " + group.name());
        return startGroup(group.name(), missingServices);
    }

    public int stopGroup(String groupName, String reason, boolean force) {
        validateGroupName(groupName);

        String safeReason = reason == null || reason.isBlank() ? "KryoCloud group stop" : reason;
        int sent = 0;

        for (NodeServiceSnapshot service : serviceRegistry.services(groupName)) {
            Optional<KryoConnection> connection = wrapperRegistry.connection(service.wrapperId());

            if (connection.isEmpty()) {
                continue;
            }

            connection.get().send(new ServiceStopRequestPacket(UUID.randomUUID(), service.serviceId(), force, safeReason));
            sent++;
        }

        return sent;
    }

    public int stopAll(String reason, boolean force) {
        String safeReason = reason == null || reason.isBlank() ? "KryoCloud shutdown" : reason;
        int sent = 0;

        for (NodeServiceSnapshot service : serviceRegistry.services()) {
            Optional<KryoConnection> connection = wrapperRegistry.connection(service.wrapperId());

            if (connection.isEmpty()) {
                continue;
            }

            connection.get().send(new ServiceStopRequestPacket(UUID.randomUUID(), service.serviceId(), force, safeReason));
            sent++;
        }

        return sent;
    }

    public boolean waitForServiceDrain(Duration timeout) {
        if (timeout == null) {
            throw new IllegalArgumentException("timeout must not be null");
        }

        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }

        long deadline = System.currentTimeMillis() + timeout.toMillis();

        while (System.currentTimeMillis() <= deadline) {
            if (serviceRegistry.runningServices().isEmpty()) {
                return true;
            }

            sleep(250L);
        }

        return serviceRegistry.runningServices().isEmpty();
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

    private List<ServiceStartPlan> plansFromGroup(IGroup group, int count) {
        List<ServiceStartPlan> plans = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            int serviceNumber = nextFreeServiceNumber(group, plans);
            plans.add(planFromGroup(group, serviceNumber));
        }

        return List.copyOf(plans);
    }

    private int nextFreeServiceNumber(IGroup group, List<ServiceStartPlan> pendingPlans) {
        int maxSlots = Math.max(group.maxCount(), group.serviceCount());
        int slot = 1;

        while (slot <= maxSlots) {
            int candidate = slot;
            String serviceId = group.name() + "-" + candidate;

            if (!serviceRegistry.activeServiceId(serviceId) && pendingPlans.stream().noneMatch(plan -> plan.serviceId().equalsIgnoreCase(serviceId))) {
                return candidate;
            }

            slot++;
        }

        throw new IllegalStateException("No free service slot available for group " + group.name());
    }

    private ServiceStartPlan planFromGroup(IGroup group, int serviceNumber) {
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

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
