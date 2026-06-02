package eu.kryocloud.api.plugin.event.group;

import eu.kryocloud.api.plugin.cloud.model.CloudGroupSnapshot;
import eu.kryocloud.api.plugin.event.ICloudEvent;

public record GroupScaledEvent(CloudGroupSnapshot group, int oldMinOnline, int newMinOnline) implements ICloudEvent {
}
