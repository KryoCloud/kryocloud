package eu.kryocloud.network;

import eu.kryocloud.network.auth.AuthManager;
import eu.kryocloud.network.connection.KryoConnection;
import eu.kryocloud.network.packet.bus.KryoPacketBus;
import eu.kryocloud.network.packet.bus.PacketContext;
import eu.kryocloud.network.packet.type.AuthPacket;
import eu.kryocloud.network.packet.type.protocol.HandshakePacket;
import eu.kryocloud.network.packet.type.protocol.HandshakeResponsePacket;
import eu.kryocloud.network.packet.type.protocol.HeartbeatPacket;
import eu.kryocloud.network.protocol.HandshakeStatus;
import eu.kryocloud.network.protocol.PeerType;
import io.netty.channel.ChannelFutureListener;

import java.util.concurrent.atomic.AtomicBoolean;

public final class KryoProtocolHandlers {

    private static final AtomicBoolean DEFAULTS_REGISTERED = new AtomicBoolean(false);

    private KryoProtocolHandlers() {
    }

    public static void registerDefaults() {
        if (!DEFAULTS_REGISTERED.compareAndSet(false, true)) {
            return;
        }

        KryoPacketBus.listen(HandshakePacket.class, KryoProtocolHandlers::handleHandshake);
        KryoPacketBus.listen(HandshakeResponsePacket.class, KryoProtocolHandlers::handleHandshakeResponse);
        KryoPacketBus.listen(HeartbeatPacket.class, KryoProtocolHandlers::handleHeartbeat);
        KryoPacketBus.listen(AuthPacket.class, KryoProtocolHandlers::handleLegacyAuth);
    }

    private static void handleHandshake(PacketContext context, HandshakePacket packet) {
        HandshakeStatus status = validateHandshake(packet);

        if (status != HandshakeStatus.ACCEPTED) {
            context.reply(HandshakeResponsePacket.rejected(status, KryoProtocol.NODE_IDENTITY)).addListener(ChannelFutureListener.CLOSE);
            return;
        }

        KryoConnection connection = context.connection();
        connection.markAuthenticated(packet.peerType(), packet.identity(), packet.protocolVersion());
        context.reply(HandshakeResponsePacket.accepted(KryoProtocol.NODE_IDENTITY));

        System.out.println("Accepted Kryo handshake: " + connection);
    }

    private static void handleHandshakeResponse(PacketContext context, HandshakeResponsePacket packet) {
        KryoConnection connection = context.connection();

        if (!packet.accepted()) {
            System.out.println("Kryo handshake rejected by " + packet.remoteIdentity() + ": " + packet.status());
            connection.close();
            return;
        }

        connection.markAuthenticated(PeerType.NODE, packet.remoteIdentity(), packet.protocolVersion());
        System.out.println("Kryo handshake accepted by " + packet.remoteIdentity() + " using protocol " + packet.protocolVersion());
    }

    private static void handleHeartbeat(PacketContext context, HeartbeatPacket packet) {
        context.connection().markHeartbeat();
    }

    private static void handleLegacyAuth(PacketContext context, AuthPacket packet) {
        if (!AuthManager.validate(packet.token())) {
            context.connection().close();
            return;
        }

        context.connection().markAuthenticated(PeerType.WRAPPER, "legacy-peer", KryoProtocol.VERSION);
        System.out.println("Authenticated legacy Kryo protocol connection: " + context.connection().id());
    }

    private static HandshakeStatus validateHandshake(HandshakePacket packet) {
        if (!KryoProtocol.isCompatible(packet.protocolVersion())) {
            return HandshakeStatus.UNSUPPORTED_PROTOCOL;
        }

        if (packet.peerType() == null) {
            return HandshakeStatus.INVALID_PEER_TYPE;
        }

        if (packet.identity() == null || packet.identity().isBlank()) {
            return HandshakeStatus.INVALID_IDENTITY;
        }

        if (!AuthManager.validate(packet.token())) {
            return HandshakeStatus.INVALID_TOKEN;
        }

        return HandshakeStatus.ACCEPTED;
    }
}