package eu.kryocloud.api.plugin.event.network;

import eu.kryocloud.api.plugin.event.ICloudEvent;
import eu.kryocloud.api.plugin.network.NetworkCacheChangeType;
import eu.kryocloud.api.plugin.network.NetworkCacheEntry;

public record NetworkCacheChangedEvent(NetworkCacheChangeType type, NetworkCacheEntry entry) implements ICloudEvent {

    public NetworkCacheChangedEvent {
        if (type == null) {
            type = NetworkCacheChangeType.PUT;
        }

        if (entry == null) {
            throw new IllegalArgumentException("entry must not be null");
        }
    }

    public String key() {
        return entry.key();
    }

}
