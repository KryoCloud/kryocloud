package eu.kryocloud.api.plugin.event.service;

import eu.kryocloud.api.plugin.cloud.model.CloudServiceSnapshot;
import eu.kryocloud.api.plugin.event.ICloudEvent;

public record ServiceFailedEvent(CloudServiceSnapshot service, String reason) implements ICloudEvent {
}
