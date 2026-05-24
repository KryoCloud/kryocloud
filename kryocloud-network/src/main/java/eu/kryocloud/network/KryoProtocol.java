package eu.kryocloud.network;

public final class KryoProtocol {

    public static final int VERSION = 1;

    public static final String NODE_IDENTITY = "kryocloud-node";

    public static final int MAX_FRAME_SIZE = 1024 * 1024;
    public static final int MAX_STRING_BYTES = 32_767;

    public static final long HEARTBEAT_INTERVAL_MILLIS = 10_000L;
    public static final long HEARTBEAT_TIMEOUT_MILLIS = 30_000L;

    public static final int HANDSHAKE_PACKET_ID = 0x01;
    public static final int HANDSHAKE_RESPONSE_PACKET_ID = 0x02;
    public static final int HEARTBEAT_PACKET_ID = 0x03;

    public static final int WRAPPER_REGISTER_PACKET_ID = 0x10;
    public static final int WRAPPER_HEARTBEAT_PACKET_ID = 0x11;

    public static final int SERVICE_START_REQUEST_PACKET_ID = 0x20;
    public static final int SERVICE_STOP_REQUEST_PACKET_ID = 0x21;
    public static final int SERVICE_STATE_PACKET_ID = 0x22;
    public static final int SERVICE_COMMAND_REQUEST_PACKET_ID = 0x23;
    public static final int SERVICE_COMMAND_RESPONSE_PACKET_ID = 0x24;
    public static final int SERVICE_LOGS_REQUEST_PACKET_ID = 0x25;
    public static final int SERVICE_LOGS_RESPONSE_PACKET_ID = 0x26;
    public static final int SERVICE_CLEANUP_REQUEST_PACKET_ID = 0x27;
    public static final int SERVICE_CLEANUP_RESPONSE_PACKET_ID = 0x28;
    public static final int SERVICE_METRICS_PACKET_ID = 0x29;

    @Deprecated
    public static final int LEGACY_AUTH_PACKET_ID = 0x7F;

    private KryoProtocol() {
    }

    public static boolean isCompatible(int protocolVersion) {
        return protocolVersion == VERSION;
    }
}
