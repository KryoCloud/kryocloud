package eu.kryocloud.api.plugin.event.remote;

import eu.kryocloud.api.plugin.event.ICloudEvent;
import java.util.Map;

public record RemoteCloudEvent(String name, Map<String, String> payload) implements ICloudEvent {

    public RemoteCloudEvent {
        name = name == null || name.isBlank() ? RemoteCloudEvent.class.getName() : name.trim();
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

}
