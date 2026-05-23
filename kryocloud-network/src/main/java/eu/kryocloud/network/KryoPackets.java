package eu.kryocloud.network;

import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketRegistry;
import eu.kryocloud.network.packet.type.AuthPacket;
import eu.kryocloud.network.packet.type.protocol.HandshakePacket;
import eu.kryocloud.network.packet.type.protocol.HandshakeResponsePacket;
import eu.kryocloud.network.packet.type.protocol.HeartbeatPacket;
import eu.kryocloud.network.packet.type.service.ServiceCleanupRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceCleanupResponsePacket;
import eu.kryocloud.network.packet.type.service.ServiceCommandRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceCommandResponsePacket;
import eu.kryocloud.network.packet.type.service.ServiceLogsRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceLogsResponsePacket;
import eu.kryocloud.network.packet.type.service.ServiceStartRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceStatePacket;
import eu.kryocloud.network.packet.type.service.ServiceStopRequestPacket;
import eu.kryocloud.network.packet.type.wrapper.WrapperHeartbeatPacket;
import eu.kryocloud.network.packet.type.wrapper.WrapperRegisterPacket;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public final class KryoPackets {

    private static final AtomicBoolean DEFAULTS_REGISTERED = new AtomicBoolean(false);

    private KryoPackets() {
    }

    public static void registerDefaults() {
        if (!DEFAULTS_REGISTERED.compareAndSet(false, true)) {
            return;
        }

        try {
            register(KryoProtocol.HANDSHAKE_PACKET_ID, HandshakePacket.class, HandshakePacket::new);
            register(KryoProtocol.HANDSHAKE_RESPONSE_PACKET_ID, HandshakeResponsePacket.class, HandshakeResponsePacket::new);
            register(KryoProtocol.HEARTBEAT_PACKET_ID, HeartbeatPacket.class, HeartbeatPacket::new);

            register(KryoProtocol.WRAPPER_REGISTER_PACKET_ID, WrapperRegisterPacket.class, WrapperRegisterPacket::new);
            register(KryoProtocol.WRAPPER_HEARTBEAT_PACKET_ID, WrapperHeartbeatPacket.class, WrapperHeartbeatPacket::new);

            register(KryoProtocol.SERVICE_START_REQUEST_PACKET_ID, ServiceStartRequestPacket.class, ServiceStartRequestPacket::new);
            register(KryoProtocol.SERVICE_STOP_REQUEST_PACKET_ID, ServiceStopRequestPacket.class, ServiceStopRequestPacket::new);
            register(KryoProtocol.SERVICE_STATE_PACKET_ID, ServiceStatePacket.class, ServiceStatePacket::new);
            register(KryoProtocol.SERVICE_COMMAND_REQUEST_PACKET_ID, ServiceCommandRequestPacket.class, ServiceCommandRequestPacket::new);
            register(KryoProtocol.SERVICE_COMMAND_RESPONSE_PACKET_ID, ServiceCommandResponsePacket.class, ServiceCommandResponsePacket::new);
            register(KryoProtocol.SERVICE_LOGS_REQUEST_PACKET_ID, ServiceLogsRequestPacket.class, ServiceLogsRequestPacket::new);
            register(KryoProtocol.SERVICE_LOGS_RESPONSE_PACKET_ID, ServiceLogsResponsePacket.class, ServiceLogsResponsePacket::new);
            register(KryoProtocol.SERVICE_CLEANUP_REQUEST_PACKET_ID, ServiceCleanupRequestPacket.class, ServiceCleanupRequestPacket::new);
            register(KryoProtocol.SERVICE_CLEANUP_RESPONSE_PACKET_ID, ServiceCleanupResponsePacket.class, ServiceCleanupResponsePacket::new);

            register(KryoProtocol.LEGACY_AUTH_PACKET_ID, AuthPacket.class, AuthPacket::new);
        } catch (RuntimeException exception) {
            DEFAULTS_REGISTERED.set(false);
            throw exception;
        }
    }

    public static <T extends Packet> void register(int id, Class<T> type, Supplier<T> supplier) {
        PacketRegistry.register(id, type, supplier);
    }
}
