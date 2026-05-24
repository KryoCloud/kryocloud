package eu.kryocloud.node.wrapper;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.connection.KryoConnection;
import eu.kryocloud.network.packet.type.wrapper.WrapperHeartbeatPacket;
import eu.kryocloud.network.packet.type.wrapper.WrapperRegisterPacket;
import eu.kryocloud.network.protocol.WrapperState;

import java.net.SocketAddress;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class NodeWrapperRegistry {

    private final ConcurrentMap<String, WrapperSnapshot> wrappersById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, KryoConnection> connectionsByWrapperId = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, String> wrapperIdByConnectionId = new ConcurrentHashMap<>();

    public WrapperSnapshot register(KryoConnection connection, WrapperRegisterPacket packet) {
        validateConnection(connection);
        validateRegisterPacket(packet);

        long now = System.currentTimeMillis();
        String remoteAddress = remoteAddress(connection.remoteAddress());
        WrapperSnapshot snapshot = new WrapperSnapshot(packet.wrapperId(), connection.id(), packet.hostname(), packet.address(), packet.osName(), packet.availableProcessors(), packet.maxMemoryMb(), 0, 0, 0, WrapperState.AVAILABLE, now, now, remoteAddress);

        wrappersById.put(packet.wrapperId(), snapshot);
        connectionsByWrapperId.put(packet.wrapperId(), connection);
        wrapperIdByConnectionId.put(connection.id(), packet.wrapperId());

        System.out.println("Registered wrapper " + packet.wrapperId() + " from " + remoteAddress);
        return snapshot;
    }

    public Optional<WrapperSnapshot> heartbeat(KryoConnection connection, WrapperHeartbeatPacket packet) {
        validateConnection(connection);
        validateHeartbeatPacket(packet);

        WrapperSnapshot updatedSnapshot = wrappersById.compute(packet.wrapperId(), (wrapperId, currentSnapshot) -> {
            if (currentSnapshot == null) {
                return null;
            }

            if (!currentSnapshot.connectionId().equals(connection.id())) {
                throw new IllegalStateException("Wrapper " + wrapperId + " heartbeat came from unexpected connection " + connection.id());
            }

            return currentSnapshot.withHeartbeat(packet.state(), packet.timestamp(), packet.usedMemoryMb(), packet.maxMemoryMb(), packet.runningServices(), packet.cpuLoadPermille());
        });

        if (updatedSnapshot == null) {
            return Optional.empty();
        }

        connection.markHeartbeat();
        return Optional.of(updatedSnapshot);
    }

    public Optional<WrapperSnapshot> unregisterConnection(KryoConnection connection) {
        validateConnectionReference(connection);

        String wrapperId = wrapperIdByConnectionId.remove(connection.id());

        if (wrapperId == null) {
            return Optional.empty();
        }

        return unregister(wrapperId);
    }

    public Optional<WrapperSnapshot> unregister(String wrapperId) {
        validateWrapperId(wrapperId);

        WrapperSnapshot removedSnapshot = wrappersById.remove(wrapperId);

        if (removedSnapshot == null) {
            return Optional.empty();
        }

        connectionsByWrapperId.remove(wrapperId);
        wrapperIdByConnectionId.remove(removedSnapshot.connectionId());

        System.out.println("Unregistered wrapper " + wrapperId);
        return Optional.of(removedSnapshot.offline());
    }

    public Optional<WrapperSnapshot> wrapper(String wrapperId) {
        validateWrapperId(wrapperId);
        return Optional.ofNullable(wrappersById.get(wrapperId));
    }

    public Optional<KryoConnection> connection(String wrapperId) {
        validateWrapperId(wrapperId);

        KryoConnection connection = connectionsByWrapperId.get(wrapperId);

        if (connection == null || !connection.isActive()) {
            return Optional.empty();
        }

        return Optional.of(connection);
    }

    public Optional<WrapperSnapshot> wrapperByConnection(UUID connectionId) {
        if (connectionId == null) {
            throw new IllegalArgumentException("connectionId must not be null");
        }

        String wrapperId = wrapperIdByConnectionId.get(connectionId);

        if (wrapperId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(wrappersById.get(wrapperId));
    }

    public List<WrapperSnapshot> wrappers() {
        return wrappersById.values().stream().sorted(Comparator.comparing(WrapperSnapshot::wrapperId)).toList();
    }

    public List<WrapperSnapshot> availableWrappers() {
        return wrappersById.values().stream().filter(snapshot -> snapshot.state() == WrapperState.AVAILABLE).filter(snapshot -> connection(snapshot.wrapperId()).isPresent()).sorted(Comparator.comparingInt(WrapperSnapshot::availableMemoryMb).reversed()).toList();
    }

    public List<WrapperSnapshot> availableWrappersForMemory(int requiredMemoryMb) {
        if (requiredMemoryMb < 1) {
            throw new IllegalArgumentException("requiredMemoryMb must be greater than 0");
        }

        return availableWrappers().stream().filter(snapshot -> snapshot.availableMemoryMb() >= requiredMemoryMb).toList();
    }

    public List<WrapperSnapshot> timedOutWrappers() {
        return timedOutWrappers(KryoProtocol.HEARTBEAT_TIMEOUT_MILLIS);
    }

    public List<WrapperSnapshot> timedOutWrappers(long timeoutMillis) {
        if (timeoutMillis < 1) {
            throw new IllegalArgumentException("timeoutMillis must be greater than 0");
        }

        return wrappersById.values().stream().filter(snapshot -> snapshot.timedOut(timeoutMillis)).sorted(Comparator.comparing(WrapperSnapshot::lastHeartbeatAtMillis)).toList();
    }

    public int size() {
        return wrappersById.size();
    }

    public boolean empty() {
        return wrappersById.isEmpty();
    }

    public void clear() {
        wrappersById.clear();
        connectionsByWrapperId.clear();
        wrapperIdByConnectionId.clear();
    }

    private void validateConnection(KryoConnection connection) {
        validateConnectionReference(connection);

        if (!connection.isAuthenticated()) {
            throw new IllegalStateException("connection must be authenticated");
        }
    }

    private void validateConnectionReference(KryoConnection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null");
        }
    }

    private void validateWrapperId(String wrapperId) {
        if (wrapperId == null) {
            throw new IllegalArgumentException("wrapperId must not be null");
        }

        if (wrapperId.isBlank()) {
            throw new IllegalArgumentException("wrapperId must not be blank");
        }
    }

    private void validateRegisterPacket(WrapperRegisterPacket packet) {
        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        if (packet.wrapperId() == null || packet.wrapperId().isBlank()) {
            throw new IllegalArgumentException("packet.wrapperId must not be blank");
        }

        if (packet.hostname() == null || packet.hostname().isBlank()) {
            throw new IllegalArgumentException("packet.hostname must not be blank");
        }

        if (packet.address() == null || packet.address().isBlank()) {
            throw new IllegalArgumentException("packet.address must not be blank");
        }

        if (packet.osName() == null || packet.osName().isBlank()) {
            throw new IllegalArgumentException("packet.osName must not be blank");
        }

        if (packet.availableProcessors() < 1) {
            throw new IllegalArgumentException("packet.availableProcessors must be greater than 0");
        }

        if (packet.maxMemoryMb() < 1) {
            throw new IllegalArgumentException("packet.maxMemoryMb must be greater than 0");
        }
    }

    private void validateHeartbeatPacket(WrapperHeartbeatPacket packet) {
        if (packet == null) {
            throw new IllegalArgumentException("packet must not be null");
        }

        if (packet.wrapperId() == null || packet.wrapperId().isBlank()) {
            throw new IllegalArgumentException("packet.wrapperId must not be blank");
        }

        if (packet.state() == null) {
            throw new IllegalArgumentException("packet.state must not be null");
        }

        if (packet.timestamp() < 1) {
            throw new IllegalArgumentException("packet.timestamp must be greater than 0");
        }

        if (packet.usedMemoryMb() < 0) {
            throw new IllegalArgumentException("packet.usedMemoryMb must not be negative");
        }

        if (packet.maxMemoryMb() < 1) {
            throw new IllegalArgumentException("packet.maxMemoryMb must be greater than 0");
        }

        if (packet.runningServices() < 0) {
            throw new IllegalArgumentException("packet.runningServices must not be negative");
        }

        if (packet.cpuLoadPermille() < 0) {
            throw new IllegalArgumentException("packet.cpuLoadPermille must not be negative");
        }
    }

    private String remoteAddress(SocketAddress socketAddress) {
        if (socketAddress == null) {
            return "unknown";
        }

        return socketAddress.toString();
    }
}