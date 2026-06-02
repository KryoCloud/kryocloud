package eu.kryocloud.wrapper.plugin;

import eu.kryocloud.api.plugin.event.ICloudEvent;
import eu.kryocloud.api.plugin.event.remote.RemoteCloudEvent;
import eu.kryocloud.api.plugin.internal.protocol.PluginWireCodec;
import eu.kryocloud.api.plugin.internal.protocol.PluginWireMessage;
import eu.kryocloud.api.plugin.internal.protocol.PluginWireMessageType;
import eu.kryocloud.api.plugin.internal.transport.frame.StreamFrame;
import eu.kryocloud.api.plugin.internal.transport.frame.StreamFrameCodec;
import eu.kryocloud.api.plugin.internal.transport.frame.StreamFrameType;
import eu.kryocloud.common.logging.KryoLogger;
import eu.kryocloud.network.KryoProtocolClient;
import eu.kryocloud.network.packet.bus.KryoPacketBus;
import eu.kryocloud.network.packet.bus.PacketContext;
import eu.kryocloud.network.packet.bus.PacketSubscription;
import eu.kryocloud.network.packet.type.plugin.PluginGatewayEventPacket;
import eu.kryocloud.network.packet.type.plugin.PluginGatewayRequestPacket;
import eu.kryocloud.network.packet.type.plugin.PluginGatewayResponsePacket;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WrapperPluginGatewayServer implements AutoCloseable {

    private static final KryoLogger LOGGER = KryoLogger.logger("PluginGateway");
    private static final String CHANNEL = "kryocloud:plugin";

    private final String host;
    private final int port;
    private final String wrapperId;
    private final KryoProtocolClient protocolClient;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Set<PluginSession> sessions = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<UUID, PluginSession> pending = new ConcurrentHashMap<>();
    private final List<PacketSubscription> subscriptions = new ArrayList<>();

    private ServerSocket serverSocket;

    public WrapperPluginGatewayServer(String host, int port, String wrapperId, KryoProtocolClient protocolClient) {
        this.host = host;
        this.port = port;
        this.wrapperId = wrapperId;
        this.protocolClient = protocolClient;
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        subscriptions.add(KryoPacketBus.listen(PluginGatewayResponsePacket.class, this::handleResponse));
        subscriptions.add(KryoPacketBus.listen(PluginGatewayEventPacket.class, this::handleEvent));
        executor.execute(this::acceptLoop);
    }

    @Override
    public void close() {
        if (!running.getAndSet(false)) {
            return;
        }

        for (PacketSubscription subscription : subscriptions) {
            subscription.close();
        }

        subscriptions.clear();

        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Exception ignored) {
        }

        for (PluginSession session : sessions) {
            session.close();
        }

        sessions.clear();
        pending.clear();
        executor.shutdownNow();
    }

    private void acceptLoop() {
        try (ServerSocket server = new ServerSocket()) {
            server.bind(new InetSocketAddress(host, port));
            serverSocket = server;
            LOGGER.success("Plugin API listening on " + host + ":" + port);

            while (running.get()) {
                Socket socket = server.accept();
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                PluginSession session = new PluginSession(socket);
                sessions.add(session);
                executor.execute(session::readLoop);
            }
        } catch (Throwable throwable) {
            if (running.get()) {
                LOGGER.error("Plugin API gateway failed", throwable);
            }
        }
    }

    private void handleResponse(PacketContext context, PluginGatewayResponsePacket packet) {
        PluginSession session = pending.remove(packet.requestId());

        if (session == null) {
            return;
        }

        PluginWireMessageType type = packet.success() ? PluginWireMessageType.RESPONSE : PluginWireMessageType.ERROR;
        Map<String, String> payload = packet.success() ? packet.payload() : merge(packet.payload(), Map.of("message", packet.message() == null ? "Request failed" : packet.message()));
        session.send(new PluginWireMessage(packet.requestId(), type, packet.route(), packet.pluginId(), payload));
    }

    private void handleEvent(PacketContext context, PluginGatewayEventPacket packet) {
        for (PluginSession session : sessions) {
            if (!session.accepts(packet)) {
                continue;
            }

            session.send(new PluginWireMessage(packet.eventId(), PluginWireMessageType.EVENT, packet.route(), session.pluginId(), packet.payload()));
        }
    }

    private void forward(PluginSession session, PluginWireMessage message) {
        pending.put(message.id(), session);

        try {
            protocolClient.send(new PluginGatewayRequestPacket(message.id(), session.pluginId(), session.serviceId(), wrapperId, message.route(), message.type().name(), message.payload()));
        } catch (Throwable throwable) {
            pending.remove(message.id());
            session.send(new PluginWireMessage(message.id(), PluginWireMessageType.ERROR, message.route(), session.pluginId(), Map.of("message", throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage())));
        }
    }

    private Map<String, String> merge(Map<String, String> left, Map<String, String> right) {
        java.util.LinkedHashMap<String, String> values = new java.util.LinkedHashMap<>();
        values.putAll(left == null ? Map.of() : left);
        values.putAll(right == null ? Map.of() : right);
        return Map.copyOf(values);
    }

    private final class PluginSession implements AutoCloseable {

        private final Socket socket;
        private final Object writeLock = new Object();
        private final Set<String> subscriptions = ConcurrentHashMap.newKeySet();

        private DataInputStream input;
        private DataOutputStream output;
        private volatile String pluginId = "unknown";
        private volatile String serviceId = "";
        private volatile String groupName = "";

        private PluginSession(Socket socket) {
            this.socket = socket;
        }

        private String pluginId() {
            return pluginId;
        }

        private String serviceId() {
            return serviceId;
        }

        private void readLoop() {
            try (socket) {
                input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                while (running.get() && !socket.isClosed()) {
                    StreamFrame frame = StreamFrameCodec.read(input);
                    handleFrame(frame);
                }
            } catch (Throwable throwable) {
                if (running.get()) {
                    LOGGER.warn("Plugin connection closed: " + throwable.getMessage());
                }
            } finally {
                sessions.remove(this);
                pending.entrySet().removeIf(entry -> entry.getValue() == this);
            }
        }

        private void handleFrame(StreamFrame frame) {
            if (frame.type() == StreamFrameType.HEARTBEAT || frame.payload().length == 0) {
                return;
            }

            PluginWireMessage message = PluginWireCodec.decode(frame.payload());

            if (message.type() == PluginWireMessageType.ACK) {
                return;
            }

            if (message.type() == PluginWireMessageType.HANDSHAKE) {
                handshake(message);
                return;
            }

            if (message.type() == PluginWireMessageType.SUBSCRIBE) {
                subscriptions.add(message.payload().getOrDefault("event", message.route()));
                send(new PluginWireMessage(message.id(), PluginWireMessageType.RESPONSE, message.route(), pluginId, Map.of("subscribed", "true")));
                return;
            }

            if (message.type() == PluginWireMessageType.UNSUBSCRIBE) {
                subscriptions.remove(message.payload().getOrDefault("event", message.route()));
                send(new PluginWireMessage(message.id(), PluginWireMessageType.RESPONSE, message.route(), pluginId, Map.of("unsubscribed", "true")));
                return;
            }

            forward(this, message);
        }

        private void handshake(PluginWireMessage message) {
            pluginId = text(message.payload(), "id", message.pluginId());
            serviceId = text(message.payload(), "serviceId", text(message.payload(), "service.id", text(message.payload(), "serviceName", text(message.payload(), "service.name", ""))));
            groupName = text(message.payload(), "groupName", text(message.payload(), "service.group", ""));
            subscriptions.add(RemoteCloudEvent.class.getName());
            send(new PluginWireMessage(message.id(), PluginWireMessageType.RESPONSE, message.route(), pluginId, Map.of("accepted", "true", "wrapper", wrapperId)));
        }

        private boolean accepts(PluginGatewayEventPacket packet) {
            if (!matches(packet.targetWrapper(), wrapperId)) {
                return false;
            }

            if (!matches(packet.targetService(), serviceId)) {
                return false;
            }

            if (!matches(packet.targetGroup(), groupName)) {
                return false;
            }

            if (subscriptions.contains(packet.route())) {
                return true;
            }

            if (subscriptions.contains(ICloudEvent.class.getName())) {
                return true;
            }

            return subscriptions.contains(RemoteCloudEvent.class.getName());
        }

        private boolean matches(String target, String local) {
            return target == null || target.isBlank() || target.equalsIgnoreCase(local == null ? "" : local);
        }

        private void send(PluginWireMessage message) {
            synchronized (writeLock) {
                try {
                    StreamFrameCodec.write(output, StreamFrame.open(message.id(), CHANNEL, PluginWireCodec.encode(message)));
                } catch (Throwable throwable) {
                    close();
                }
            }
        }

        @Override
        public void close() {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }

        private String text(Map<String, String> payload, String key, String fallback) {
            String value = payload.get(key);

            if (value == null || value.isBlank()) {
                return fallback == null ? "" : fallback;
            }

            return value;
        }

    }

}
