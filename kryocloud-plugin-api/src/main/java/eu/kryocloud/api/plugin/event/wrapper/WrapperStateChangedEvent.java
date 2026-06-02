package eu.kryocloud.api.plugin.event.wrapper;

import eu.kryocloud.api.plugin.cloud.model.CloudWrapperSnapshot;
import eu.kryocloud.api.plugin.cloud.model.CloudWrapperState;
import eu.kryocloud.api.plugin.event.ICloudEvent;

public record WrapperStateChangedEvent(CloudWrapperSnapshot wrapper, CloudWrapperState oldState, CloudWrapperState newState) implements ICloudEvent {
}
