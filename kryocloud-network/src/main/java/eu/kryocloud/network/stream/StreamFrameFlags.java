package eu.kryocloud.network.stream;

public final class StreamFrameFlags {

    public static final int NONE = 0;
    public static final int COMPRESSED = 1;
    public static final int FINAL = 1 << 1;

    private StreamFrameFlags() {}

    public static boolean compressed(int flags) {
        return (flags & COMPRESSED) == COMPRESSED;
    }

    public static boolean finalFrame(int flags) {
        return (flags & FINAL) == FINAL;
    }

}
