package eu.kryocloud.api.plugin.internal.network;

import eu.kryocloud.api.plugin.event.EventPriority;
import eu.kryocloud.api.plugin.event.IEventListener;
import eu.kryocloud.api.plugin.event.network.NetworkCacheChangedEvent;
import eu.kryocloud.api.plugin.internal.client.PluginRequestClient;
import eu.kryocloud.api.plugin.internal.event.RemoteEventBus;
import eu.kryocloud.api.plugin.internal.protocol.PluginWireMessage;
import eu.kryocloud.api.plugin.internal.protocol.payload.Payload;
import eu.kryocloud.api.plugin.network.INetworkCache;
import eu.kryocloud.api.plugin.network.NetworkCacheEntry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class RemoteNetworkCache implements INetworkCache {

    private final PluginRequestClient client;
    private final RemoteEventBus events;

    public RemoteNetworkCache(PluginRequestClient client, RemoteEventBus events) {
        this.client = client;
        this.events = events;
    }

    @Override
    public CompletableFuture<NetworkCacheEntry> put(String key, byte[] value) {
        return put(key, value, Duration.ZERO);
    }

    @Override
    public CompletableFuture<NetworkCacheEntry> put(String key, byte[] value, Duration ttl) {
        byte[] data = value == null ? new byte[0] : value;
        long ttlMillis = ttl == null || ttl.isNegative() ? 0L : ttl.toMillis();

        return client.request("network.cache.put", Payload.create()
                .put("key", key)
                .put("value", Base64.getEncoder().encodeToString(data))
                .put("ttlMillis", String.valueOf(ttlMillis))
                .map()).thenApply(response -> NetworkCacheEntry.fromPayload(response.payload()));
    }

    @Override
    public CompletableFuture<Optional<NetworkCacheEntry>> get(String key) {
        return client.request("network.cache.get", Payload.create()
                .put("key", key)
                .map()).thenApply(response -> {
                    if (!Boolean.parseBoolean(response.payload().getOrDefault("present", "false"))) {
                        return Optional.empty();
                    }

                    return Optional.of(NetworkCacheEntry.fromPayload(response.payload()));
                });
    }

    @Override
    public CompletableFuture<Boolean> remove(String key) {
        return client.request("network.cache.remove", Payload.create()
                .put("key", key)
                .map()).thenApply(response -> Boolean.parseBoolean(response.payload().getOrDefault("removed", "false")));
    }

    @Override
    public CompletableFuture<List<String>> keys(String prefix) {
        return client.request("network.cache.keys", Payload.create()
                .put("prefix", prefix == null ? "" : prefix)
                .map()).thenApply(this::keys);
    }

    @Override
    public AutoCloseable watch(String key, IEventListener<NetworkCacheChangedEvent> listener) {
        return events.listen(NetworkCacheChangedEvent.class, EventPriority.NORMAL, event -> {
            if (!event.key().equalsIgnoreCase(key)) {
                return;
            }

            listener.handle(event);
        });
    }

    @Override
    public AutoCloseable watchPrefix(String prefix, IEventListener<NetworkCacheChangedEvent> listener) {
        String value = prefix == null ? "" : prefix;

        return events.listen(NetworkCacheChangedEvent.class, EventPriority.NORMAL, event -> {
            if (!event.key().startsWith(value)) {
                return;
            }

            listener.handle(event);
        });
    }

    private List<String> keys(PluginWireMessage response) {
        Map<String, String> payload = response.payload();
        int size = integer(payload.get("keys.size"));
        List<String> values = new ArrayList<>();

        for (int index = 0; index < size; index++) {
            String value = payload.get("keys." + index);

            if (value == null || value.isBlank()) {
                continue;
            }

            values.add(value);
        }

        return List.copyOf(values);
    }

    private int integer(String value) {
        try {
            return Integer.parseInt(value == null ? "0" : value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

}
