package eu.kryocloud.network.packet.type.plugin;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketByteBuffer;
import eu.kryocloud.network.packet.PacketValidation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PluginGatewayRequestPacket extends Packet {

    private UUID requestId;
    private String pluginId;
    private String serviceId;
    private String wrapperId;
    private String route;
    private String type;
    private Map<String, String> payload = Map.of();

    public PluginGatewayRequestPacket() {
    }

    public PluginGatewayRequestPacket(UUID requestId, String pluginId, String serviceId, String wrapperId, String route, String type, Map<String, String> payload) {
        this.requestId = PacketValidation.value(requestId, "requestId");
        this.pluginId = PacketValidation.nonBlankString(pluginId, "pluginId");
        this.serviceId = PacketValidation.string(serviceId, "serviceId");
        this.wrapperId = PacketValidation.nonBlankString(wrapperId, "wrapperId");
        this.route = PacketValidation.nonBlankString(route, "route");
        this.type = PacketValidation.nonBlankString(type, "type");
        this.payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    @Override
    public int getId() {
        return KryoProtocol.PLUGIN_GATEWAY_REQUEST_PACKET_ID;
    }

    @Override
    public void write(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");
        validateWritable();

        buffer.writeUuid(requestId);
        buffer.writeString(pluginId);
        buffer.writeString(serviceId);
        buffer.writeString(wrapperId);
        buffer.writeString(route);
        buffer.writeString(type);
        writeMap(buffer, payload);
    }

    @Override
    public void read(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");

        requestId = buffer.readUuid();
        pluginId = buffer.readString();
        serviceId = buffer.readString();
        wrapperId = buffer.readString();
        route = buffer.readString();
        type = buffer.readString();
        payload = readMap(buffer);
    }

    public UUID requestId() {
        return requestId;
    }

    public String pluginId() {
        return pluginId;
    }

    public String serviceId() {
        return serviceId;
    }

    public String wrapperId() {
        return wrapperId;
    }

    public String route() {
        return route;
    }

    public String type() {
        return type;
    }

    public Map<String, String> payload() {
        return payload;
    }

    private void validateWritable() {
        PacketValidation.value(requestId, "requestId");
        PacketValidation.nonBlankString(pluginId, "pluginId");
        PacketValidation.string(serviceId, "serviceId");
        PacketValidation.nonBlankString(wrapperId, "wrapperId");
        PacketValidation.nonBlankString(route, "route");
        PacketValidation.nonBlankString(type, "type");
    }

    static void writeMap(PacketByteBuffer buffer, Map<String, String> payload) {
        Map<String, String> values = payload == null ? Map.of() : payload;
        buffer.writeInt(values.size());

        for (Map.Entry<String, String> entry : values.entrySet()) {
            buffer.writeString(entry.getKey());
            buffer.writeString(entry.getValue());
        }
    }

    static Map<String, String> readMap(PacketByteBuffer buffer) {
        int size = buffer.readInt();

        if (size < 0 || size > 16_384) {
            throw new IllegalStateException("Invalid plugin payload size: " + size);
        }

        Map<String, String> values = new LinkedHashMap<>();

        for (int index = 0; index < size; index++) {
            values.put(buffer.readString(), buffer.readString());
        }

        return Map.copyOf(values);
    }

}
