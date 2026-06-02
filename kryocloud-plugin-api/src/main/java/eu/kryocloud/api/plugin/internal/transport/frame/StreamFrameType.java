package eu.kryocloud.api.plugin.internal.transport.frame;

import java.util.Arrays;

public enum StreamFrameType {

    OPEN(1),
    DATA(2),
    END(3),
    RESET(4),
    HEARTBEAT(5);

    private final int wireId;

    StreamFrameType(int wireId) {
        this.wireId = wireId;
    }

    public int wireId() {
        return wireId;
    }

    public static StreamFrameType fromWireId(int wireId) {
        return Arrays.stream(values())
                .filter(type -> type.wireId == wireId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown stream frame type: " + wireId));
    }

}
