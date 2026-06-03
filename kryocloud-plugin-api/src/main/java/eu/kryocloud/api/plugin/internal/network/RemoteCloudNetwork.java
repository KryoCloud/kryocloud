package eu.kryocloud.api.plugin.internal.network;

import eu.kryocloud.api.plugin.internal.client.PluginRequestClient;
import eu.kryocloud.api.plugin.internal.event.RemoteEventBus;
import eu.kryocloud.api.plugin.messaging.PluginChannel;
import eu.kryocloud.api.plugin.network.ICloudNetwork;
import eu.kryocloud.api.plugin.network.ICloudNetworkChannel;
import eu.kryocloud.api.plugin.network.INetworkCache;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class RemoteCloudNetwork implements ICloudNetwork {

    private final PluginRequestClient client;
    private final RemoteEventBus events;
    private final RemoteNetworkCache cache;
    private final ConcurrentMap<PluginChannel, RemoteCloudNetworkChannel> channels = new ConcurrentHashMap<>();

    public RemoteCloudNetwork(PluginRequestClient client, RemoteEventBus events) {
        this.client = client;
        this.events = events;
        this.cache = new RemoteNetworkCache(client, events);
    }

    @Override
    public ICloudNetworkChannel channel(PluginChannel channel) {
        return channels.computeIfAbsent(channel, key -> new RemoteCloudNetworkChannel(key, client, events));
    }

    @Override
    public INetworkCache cache() {
        return cache;
    }

}
