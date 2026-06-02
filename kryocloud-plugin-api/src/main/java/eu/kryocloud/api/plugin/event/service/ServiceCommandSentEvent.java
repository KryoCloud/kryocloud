package eu.kryocloud.api.plugin.event.service;

import eu.kryocloud.api.plugin.event.ICloudEvent;

public record ServiceCommandSentEvent(String serviceName, String command) implements ICloudEvent {
}
