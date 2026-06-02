package eu.kryocloud.api.plugin.event.wrapper;

import eu.kryocloud.api.plugin.cloud.model.CloudWrapperSnapshot;
import eu.kryocloud.api.plugin.event.ICloudEvent;

public record WrapperConnectedEvent(CloudWrapperSnapshot wrapper) implements ICloudEvent {
}
