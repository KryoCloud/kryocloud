package eu.kryocloud.api.plugin.network;

import eu.kryocloud.api.plugin.messaging.PluginChannel;

public interface ICloudNetwork {

    ICloudNetworkChannel channel(PluginChannel channel);

    default ICloudNetworkChannel channel(String namespace, String name) {
        return channel(PluginChannel.of(namespace, name));
    }

    INetworkCache cache();

}
