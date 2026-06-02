package eu.kryocloud.network;

import eu.kryocloud.common.logging.KryoLogger;
import eu.kryocloud.network.auth.AuthManager;
import eu.kryocloud.network.connection.KryoConnection;
import eu.kryocloud.network.connection.ProtocolSide;
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

    private static final KryoLogger LOGGER = KryoLogger.logger("Protocol");
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
        if (context.connection().side() != ProtocolSide.SERVER) {
            context.connection().close();
            return;
        }

        HandshakeStatus status = validateHandshake(packet);

        if (status != HandshakeStatus.ACCEPTED) {
            context.reply(HandshakeResponsePacket.rejected(status, KryoProtocol.NODE_IDENTITY)).addListener(ChannelFutureListener.CLOSE);
            LOGGER.warn("Handshake rejected for " + safeIdentity(packet.identity()) + ": " + status);
            return;
        }

        KryoConnection connection = context.connection();
        connection.markAuthenticated(packet.peerType(), packet.identity(), packet.protocolVersion());
        context.reply(HandshakeResponsePacket.accepted(KryoProtocol.NODE_IDENTITY));

        LOGGER.debug("Accepted handshake from " + packet.identity() + " using protocol " + packet.protocolVersion());
    }

    private static void handleHandshakeResponse(PacketContext context, HandshakeResponsePacket packet) {
        KryoConnection connection = context.connection();

        if (connection.side() != ProtocolSide.CLIENT) {
            connection.close();
            return;
        }

        if (!packet.accepted()) {
            LOGGER.warn("Handshake rejected by " + packet.remoteIdentity() + ": " + packet.status());
            connection.close();
            return;
        }

        connection.markAuthenticated(PeerType.NODE, packet.remoteIdentity(), packet.protocolVersion());
        LOGGER.debug("Handshake accepted by " + packet.remoteIdentity() + " using protocol " + packet.protocolVersion());
    }

    private static void handleHeartbeat(PacketContext context, HeartbeatPacket packet) {
        context.connection().markHeartbeat();
    }

    private static void handleLegacyAuth(PacketContext context, AuthPacket packet) {
        if (context.connection().side() != ProtocolSide.SERVER) {
            context.connection().close();
            return;
        }

        if (!AuthManager.validate(packet.token())) {
            context.connection().close();
            return;
        }

        context.connection().markAuthenticated(PeerType.WRAPPER, "legacy-peer", KryoProtocol.VERSION);
        LOGGER.debug("Authenticated legacy protocol connection " + context.connection().id());
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

    private static String safeIdentity(String identity) {
        if (identity == null || identity.isBlank()) {
            return "unknown";
        }

        return identity;
    }
}
