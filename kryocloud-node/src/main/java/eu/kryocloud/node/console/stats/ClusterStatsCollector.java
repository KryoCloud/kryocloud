package eu.kryocloud.node.console.stats;

import eu.kryocloud.api.group.IGroup;
import eu.kryocloud.network.protocol.CloudServiceState;
import eu.kryocloud.node.KryoNode;
import eu.kryocloud.node.service.runtime.NodeServiceSnapshot;
import eu.kryocloud.node.wrapper.WrapperSnapshot;

import java.util.Comparator;
import java.util.List;

public final class ClusterStatsCollector {

    private final KryoNode node;

    public ClusterStatsCollector(KryoNode node) {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }

        this.node = node;
    }

    public ClusterStatsSnapshot snapshot() {
        List<WrapperSnapshot> wrappers = node.wrapperRegistry().wrappers();
        List<NodeServiceSnapshot> services = node.serviceRegistry().services();
        List<GroupStatsSnapshot> groups = node.groupManager().groups().stream().map(group -> groupSnapshot(group, services)).sorted(Comparator.comparing(GroupStatsSnapshot::groupName)).toList();
        int memoryUsed = wrappers.stream().mapToInt(WrapperSnapshot::usedMemoryMb).sum();
        int memoryMax = wrappers.stream().mapToInt(WrapperSnapshot::maxMemoryMb).sum();
        int running = (int) services.stream().filter(service -> service.state() == CloudServiceState.RUNNING).count();
        int starting = (int) services.stream().filter(service -> service.state() == CloudServiceState.PREPARING || service.state() == CloudServiceState.STARTING).count();
        int failed = (int) services.stream().filter(service -> service.state() == CloudServiceState.FAILED).count();

        return new ClusterStatsSnapshot(System.currentTimeMillis(), wrappers.size(), node.wrapperRegistry().timedOutWrappers().size(), memoryUsed, memoryMax, services.size(), running, starting, failed, groups);
    }

    public GroupStatsSnapshot groupSnapshot(String groupName) {
        if (groupName == null || groupName.isBlank()) {
            throw new IllegalArgumentException("groupName must not be blank");
        }

        IGroup group = node.groupManager().groupByName(groupName);

        if (group == null) {
            throw new IllegalArgumentException("Unknown group: " + groupName);
        }

        return groupSnapshot(group, node.serviceRegistry().services(groupName));
    }

    private GroupStatsSnapshot groupSnapshot(IGroup group, List<NodeServiceSnapshot> allServices) {
        List<NodeServiceSnapshot> services = allServices.stream().filter(service -> service.groupName().equalsIgnoreCase(group.name())).toList();
        int running = (int) services.stream().filter(service -> service.state() == CloudServiceState.RUNNING).count();
        int failed = (int) services.stream().filter(service -> service.state() == CloudServiceState.FAILED).count();
        int configuredMemory = running * group.maxMemory();
        int capacityMemory = group.maxCount() * group.maxMemory();

        return new GroupStatsSnapshot(group.name(), group.serviceType(), group.software(), group.softwareVersion(), services.size(), running, failed, group.serviceCount(), group.minCount(), group.maxCount(), configuredMemory, capacityMemory, group.staticServices());
    }
}