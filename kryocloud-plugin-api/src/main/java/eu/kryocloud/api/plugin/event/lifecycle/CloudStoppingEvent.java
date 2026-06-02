package eu.kryocloud.api.plugin.event.lifecycle;

import eu.kryocloud.api.plugin.event.ICloudEvent;

public record CloudStoppingEvent(String reason) implements ICloudEvent {

    public CloudStoppingEvent {
        reason = reason == null ? "" : reason;
    }

}
