package eu.kryocloud.network.packet.type.plugin;

import eu.kryocloud.network.KryoProtocol;
import eu.kryocloud.network.packet.Packet;
import eu.kryocloud.network.packet.PacketByteBuffer;
import eu.kryocloud.network.packet.PacketValidation;
import java.util.Map;
import java.util.UUID;

public final class PluginGatewayEventPacket extends Packet {

    private UUID eventId;
    private String route;
    private String targetService;
    private String targetGroup;
    private String targetWrapper;
    private Map<String, String> payload = Map.of();

    public PluginGatewayEventPacket() {
    }

    public PluginGatewayEventPacket(UUID eventId, String route, String targetService, String targetGroup, String targetWrapper, Map<String, String> payload) {
        this.eventId = PacketValidation.value(eventId, "eventId");
        this.route = PacketValidation.nonBlankString(route, "route");
        this.targetService = PacketValidation.string(targetService, "targetService");
        this.targetGroup = PacketValidation.string(targetGroup, "targetGroup");
        this.targetWrapper = PacketValidation.string(targetWrapper, "targetWrapper");
        this.payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    @Override
    public int getId() {
        return KryoProtocol.PLUGIN_GATEWAY_EVENT_PACKET_ID;
    }

    @Override
    public void write(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");
        validateWritable();

        buffer.writeUuid(eventId);
        buffer.writeString(route);
        buffer.writeString(targetService);
        buffer.writeString(targetGroup);
        buffer.writeString(targetWrapper);
        PluginGatewayRequestPacket.writeMap(buffer, payload);
    }

    @Override
    public void read(PacketByteBuffer buffer) {
        PacketValidation.value(buffer, "buffer");

        eventId = buffer.readUuid();
        route = buffer.readString();
        targetService = buffer.readString();
        targetGroup = buffer.readString();
        targetWrapper = buffer.readString();
        payload = PluginGatewayRequestPacket.readMap(buffer);
    }

    public UUID eventId() {
        return eventId;
    }

    public String route() {
        return route;
    }

    public String targetService() {
        return targetService;
    }

    public String targetGroup() {
        return targetGroup;
    }

    public String targetWrapper() {
        return targetWrapper;
    }

    public Map<String, String> payload() {
        return payload;
    }

    private void validateWritable() {
        PacketValidation.value(eventId, "eventId");
        PacketValidation.nonBlankString(route, "route");
        PacketValidation.string(targetService, "targetService");
        PacketValidation.string(targetGroup, "targetGroup");
        PacketValidation.string(targetWrapper, "targetWrapper");
    }

}
