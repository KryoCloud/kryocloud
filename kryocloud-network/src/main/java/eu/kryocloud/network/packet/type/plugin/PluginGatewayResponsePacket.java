package eu.kryocloud.network.packet.type.plugin;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketByteBuffer;
import eu.kryocloud.network.packet.PacketValidation;
import java.util.Map;
import java.util.UUID;

public final class PluginGatewayResponsePacket extends Packet {

    private UUID requestId;
    private String pluginId;
    private String route;
    private boolean success;
    private String message;
    private Map<String, String> payload = Map.of();

    public PluginGatewayResponsePacket() {
    }

    public PluginGatewayResponsePacket(UUID requestId, String pluginId, String route, boolean success, String message, Map<String, String> payload) {
        this.requestId = PacketValidation.value(requestId, "requestId");
        this.pluginId = PacketValidation.nonBlankString(pluginId, "pluginId");
        this.route = PacketValidation.nonBlankString(route, "route");
        this.success = success;
        this.message = PacketValidation.string(message, "message");
        this.payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    @Override
    public int getId() {
        return KryoProtocol.PLUGIN_GATEWAY_RESPONSE_PACKET_ID;
    }

    @Override
    public void write(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");
        validateWritable();

        buffer.writeUuid(requestId);
        buffer.writeString(pluginId);
        buffer.writeString(route);
        buffer.writeBoolean(success);
        buffer.writeString(message);
        PluginGatewayRequestPacket.writeMap(buffer, payload);
    }

    @Override
    public void read(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");

        requestId = buffer.readUuid();
        pluginId = buffer.readString();
        route = buffer.readString();
        success = buffer.readBoolean();
        message = buffer.readString();
        payload = PluginGatewayRequestPacket.readMap(buffer);
    }

    public UUID requestId() {
        return requestId;
    }

    public String pluginId() {
        return pluginId;
    }

    public String route() {
        return route;
    }

    public boolean success() {
        return success;
    }

    public String message() {
        return message;
    }

    public Map<String, String> payload() {
        return payload;
    }

    private void validateWritable() {
        PacketValidation.value(requestId, "requestId");
        PacketValidation.nonBlankString(pluginId, "pluginId");
        PacketValidation.nonBlankString(route, "route");
        PacketValidation.string(message, "message");
    }

}
