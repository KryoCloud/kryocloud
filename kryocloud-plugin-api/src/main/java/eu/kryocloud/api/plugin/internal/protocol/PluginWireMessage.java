package eu.kryocloud.api.plugin.internal.protocol;

import java.util.Map;
import java.util.UUID;

public record PluginWireMessage(UUID id, PluginWireMessageType type, String route, String pluginId, Map<String, String> payload) {

    public PluginWireMessage {
        id = id == null ? UUID.randomUUID() : id;

        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }

        route = route == null ? "" : route.trim();
        pluginId = pluginId == null ? "" : pluginId.trim();
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public static PluginWireMessage handshake(String pluginId, Map<String, String> payload) {
        return new PluginWireMessage(UUID.randomUUID(), PluginWireMessageType.HANDSHAKE, "plugin.handshake", pluginId, payload);
    }

    public static PluginWireMessage request(String pluginId, String route, Map<String, String> payload) {
        return new PluginWireMessage(UUID.randomUUID(), PluginWireMessageType.REQUEST, route, pluginId, payload);
    }

    public static PluginWireMessage subscribe(String pluginId, String route, Map<String, String> payload) {
        return new PluginWireMessage(UUID.randomUUID(), PluginWireMessageType.SUBSCRIBE, route, pluginId, payload);
    }

    public static PluginWireMessage unsubscribe(String pluginId, String route, Map<String, String> payload) {
        return new PluginWireMessage(UUID.randomUUID(), PluginWireMessageType.UNSUBSCRIBE, route, pluginId, payload);
    }

    public static PluginWireMessage message(String pluginId, String route, Map<String, String> payload) {
        return new PluginWireMessage(UUID.randomUUID(), PluginWireMessageType.MESSAGE, route, pluginId, payload);
    }

}
