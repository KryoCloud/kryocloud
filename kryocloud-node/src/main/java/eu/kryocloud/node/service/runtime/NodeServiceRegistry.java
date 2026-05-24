package eu.kryocloud.node.service.runtime;

import eu.kryocloud.network.packet.type.service.ServiceMetricsPacket;
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

        NodeServiceSnapshot previous = servicesById.get(packet.serviceId());
        int memory = previous == null ? 0 : previous.processMemoryMb();
        int cpu = previous == null ? 0 : previous.cpuLoadPermille();
        long uptime = previous == null ? 0L : previous.uptimeMillis();
        NodeServiceSnapshot snapshot = new NodeServiceSnapshot(packet.serviceId(), packet.groupName(), packet.serviceType(), packet.state(), packet.wrapperId(), packet.host(), packet.port(), packet.timestamp(), packet.message(), memory, cpu, uptime);

        if (snapshot.state() == CloudServiceState.STOPPED) {
            servicesById.remove(snapshot.serviceId());
            return snapshot;
        }

        servicesById.put(snapshot.serviceId(), snapshot);
        return snapshot;
    }

    public Optional<NodeServiceSnapshot> updateMetrics(ServiceMetricsPacket packet) {
        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        return Optional.ofNullable(servicesById.computeIfPresent(packet.serviceId(), (serviceId, snapshot) -> snapshot.withMetrics(packet.processMemoryMb(), packet.cpuLoadPermille(), packet.uptimeMillis())));
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

    public boolean activeServiceId(String serviceId) {
        validateServiceId(serviceId);
        NodeServiceSnapshot snapshot = servicesById.get(serviceId);

        if (snapshot == null) {
            return false;
        }

        return snapshot.state() == CloudServiceState.PREPARING || snapshot.state() == CloudServiceState.STARTING || snapshot.state() == CloudServiceState.RUNNING || snapshot.state() == CloudServiceState.STOPPING;
    }

    public boolean activeGroupSlot(String groupName, int serviceNumber) {
        if (groupName == null || groupName.isBlank()) {
            throw new IllegalArgumentException("groupName must not be blank");
        }

        if (serviceNumber < 1) {
            throw new IllegalArgumentException("serviceNumber must be greater than 0");
        }

        return activeServiceId(groupName + "-" + serviceNumber);
    }

    public int activeServiceCount(String groupName) {
        if (groupName == null || groupName.isBlank()) {
            throw new IllegalArgumentException("groupName must not be blank");
        }

        return (int) servicesById.values().stream().filter(service -> service.groupName().equalsIgnoreCase(groupName)).filter(service -> activeServiceId(service.serviceId())).count();
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
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }
    }
}
