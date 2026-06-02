package eu.kryocloud.api.plugin.event.service;

import eu.kryocloud.api.plugin.cloud.model.CloudServiceSnapshot;
import eu.kryocloud.api.plugin.event.ICloudEvent;

public record ServiceStoppedEvent(CloudServiceSnapshot service) implements ICloudEvent {
}
