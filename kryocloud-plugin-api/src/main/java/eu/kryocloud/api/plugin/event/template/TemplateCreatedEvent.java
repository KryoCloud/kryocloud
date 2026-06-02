package eu.kryocloud.api.plugin.event.template;

import eu.kryocloud.api.plugin.cloud.model.CloudTemplateSnapshot;
import eu.kryocloud.api.plugin.event.ICloudEvent;

public record TemplateCreatedEvent(CloudTemplateSnapshot template) implements ICloudEvent {
}
