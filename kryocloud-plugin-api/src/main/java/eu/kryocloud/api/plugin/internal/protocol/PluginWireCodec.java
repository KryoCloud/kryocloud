package eu.kryocloud.api.plugin.internal.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PluginWireCodec {

    private static final int MAGIC = 0x4B504D47;
    private static final int VERSION = 1;

    private PluginWireCodec() {
    }

    public static byte[] encode(PluginWireMessage message) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(buffer);
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.writeLong(message.id().getMostSignificantBits());
            output.writeLong(message.id().getLeastSignificantBits());
            output.writeInt(message.type().ordinal());
            writeString(output, message.route());
            writeString(output, message.pluginId());
            output.writeInt(message.payload().size());

            for (Map.Entry<String, String> entry : message.payload().entrySet()) {
                writeString(output, entry.getKey());
                writeString(output, entry.getValue());
            }

            output.flush();
            return buffer.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not encode plugin message", exception);
        }
    }

    public static PluginWireMessage decode(byte[] payload) {
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
            int magic = input.readInt();

            if (magic != MAGIC) {
                throw new IllegalStateException("Invalid plugin message magic: " + Integer.toHexString(magic));
            }

            int version = input.readInt();

            if (version != VERSION) {
                throw new IllegalStateException("Unsupported plugin message version: " + version);
            }

            UUID id = new UUID(input.readLong(), input.readLong());
            PluginWireMessageType type = PluginWireMessageType.values()[input.readInt()];
            String route = readString(input);
            String pluginId = readString(input);
            int entries = input.readInt();
            Map<String, String> map = new LinkedHashMap<>();

            if (entries < 0 || entries > 16_384) {
                throw new IllegalStateException("Invalid payload entry count: " + entries);
            }

            for (int index = 0; index < entries; index++) {
                map.put(readString(input), readString(input));
            }

            return new PluginWireMessage(id, type, route, pluginId, map);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not decode plugin message", exception);
        }
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static String readString(DataInputStream input) throws IOException {
        int length = input.readInt();

        if (length < 0 || length > 8 * 1024 * 1024) {
            throw new IllegalStateException("Invalid string length: " + length);
        }

        byte[] bytes = input.readNBytes(length);

        if (bytes.length != length) {
            throw new IllegalStateException("Unexpected end of plugin message");
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

}
