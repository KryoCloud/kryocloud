package eu.kryocloud.api.plugin.event.version;

import eu.kryocloud.api.plugin.cloud.model.CloudVersionSnapshot;
import eu.kryocloud.api.plugin.event.ICloudEvent;

public record VersionInstalledEvent(CloudVersionSnapshot version) implements ICloudEvent {
}
