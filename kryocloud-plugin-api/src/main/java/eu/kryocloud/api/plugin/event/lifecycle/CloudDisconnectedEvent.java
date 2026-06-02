package eu.kryocloud.api.plugin.event.lifecycle;

import eu.kryocloud.api.plugin.event.ICloudEvent;

public record CloudDisconnectedEvent(String reason) implements ICloudEvent {

    public CloudDisconnectedEvent {
        reason = reason == null ? "" : reason;
    }

}
