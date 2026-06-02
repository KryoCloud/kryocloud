package eu.kryocloud.network.stream;

public final class StreamingProtocol {

    public static final int MAGIC = 0x4B535452;
    public static final int VERSION = 1;
    public static final int MAX_FRAME_SIZE = 8 * 1024 * 1024;
    public static final int MAX_PAYLOAD_BYTES = MAX_FRAME_SIZE - 128;
    public static final int MAX_CHANNEL_BYTES = 192;

    private StreamingProtocol() {}

    public static boolean compatible(int version) {
        return version == VERSION;
    }

}
