package eu.kryocloud.node.service.runtime;

import eu.kryocloud.network.packet.type.service.ServiceStatePacket;
import eu.kryocloud.network.protocol.CloudServiceState;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class NodeServiceRegistry {

    private final ConcurrentMap<String, NodeServiceSnapshot> servicesById = new ConcurrentHashMap<>();

    public NodeServiceSnapshot update(ServiceStatePacket packet) {
        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        NodeServiceSnapshot snapshot = new NodeServiceSnapshot(packet.serviceId(), packet.groupName(), packet.serviceType(), packet.state(), packet.wrapperId(), packet.host(), packet.port(), packet.timestamp(), packet.message());
        servicesById.put(snapshot.serviceId(), snapshot);
        return snapshot;
    }

    public Optional<NodeServiceSnapshot> service(String serviceId) {
        validateServiceId(serviceId);
        return Optional.ofNullable(servicesById.get(serviceId));
    }

    public List<NodeServiceSnapshot> services() {
        return servicesById.values().stream().sorted(Comparator.comparing(NodeServiceSnapshot::serviceId)).toList();
    }

    public List<NodeServiceSnapshot> runningServices() {
        return servicesById.values().stream().filter(NodeServiceSnapshot::running).sorted(Comparator.comparing(NodeServiceSnapshot::serviceId)).toList();
    }

    public List<NodeServiceSnapshot> services(String groupName) {
        if (groupName == null || groupName.isBlank()) {
            throw new IllegalArgumentException("groupName must not be blank");
        }

        return servicesById.values().stream().filter(service -> service.groupName().equalsIgnoreCase(groupName)).sorted(Comparator.comparing(NodeServiceSnapshot::serviceId)).toList();
    }

    public void remove(String serviceId) {
        validateServiceId(serviceId);
        servicesById.remove(serviceId);
    }

    public void clear() {
        servicesById.clear();
    }

    public int size() {
        return servicesById.size();
    }

    private void validateServiceId(String serviceId) {
        if (serviceId == null) {
            throw new IllegalArgumentException("serviceId must not be null");
        }

        if (serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }
    }
}