package eu.kryocloud.node.service.schedule;

import eu.kryocloud.network.connection.KryoConnection;
import eu.kryocloud.network.packet.type.service.ServiceStartRequestPacket;
import eu.kryocloud.node.wrapper.NodeWrapperRegistry;
import eu.kryocloud.node.wrapper.WrapperSnapshot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class NodeServiceScheduler {

    private final NodeWrapperRegistry wrapperRegistry;

    public NodeServiceScheduler(NodeWrapperRegistry wrapperRegistry) {
        if (wrapperRegistry == null) {
            throw new IllegalArgumentException("wrapperRegistry must not be null");
        }

        this.wrapperRegistry = wrapperRegistry;
    }

    public ServiceStartResult start(ServiceStartPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("plan must not be null");
        }

        WrapperSnapshot wrapper = selectWrapper(plan).orElseThrow(() -> new IllegalStateException("No available wrapper found for service " + plan.serviceId() + " with " + plan.maxMemoryMb() + "MB memory"));
        KryoConnection connection = wrapperRegistry.connection(wrapper.wrapperId()).orElseThrow(() -> new IllegalStateException("Selected wrapper " + wrapper.wrapperId() + " has no active connection"));
        UUID requestId = UUID.randomUUID();

        connection.send(new ServiceStartRequestPacket(requestId, plan.serviceId(), plan.groupName(), plan.templateName(), plan.serviceType(), plan.port(), plan.maxMemoryMb(), plan.staticService()));

        System.out.println("Scheduled service " + plan.serviceId() + " on wrapper " + wrapper.wrapperId() + " with request " + requestId);
        return new ServiceStartResult(requestId, plan.serviceId(), wrapper.wrapperId());
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
}