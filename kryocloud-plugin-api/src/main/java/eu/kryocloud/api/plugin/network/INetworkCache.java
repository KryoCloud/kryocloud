package eu.kryocloud.api.plugin.network;

import eu.kryocloud.api.plugin.event.IEventListener;
import eu.kryocloud.api.plugin.event.network.NetworkCacheChangedEvent;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface INetworkCache {

    CompletableFuture<NetworkCacheEntry> put(String key, byte[] value);

    CompletableFuture<NetworkCacheEntry> put(String key, byte[] value, Duration ttl);

    default CompletableFuture<NetworkCacheEntry> putText(String key, String value) {
        return put(key, (value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    default CompletableFuture<NetworkCacheEntry> putText(String key, String value, Duration ttl) {
        return put(key, (value == null ? "" : value).getBytes(StandardCharsets.UTF_8), ttl);
    }

    CompletableFuture<Optional<NetworkCacheEntry>> get(String key);

    default CompletableFuture<Optional<String>> getText(String key) {
        return get(key).thenApply(entry -> entry.map(NetworkCacheEntry::text));
    }

    CompletableFuture<Boolean> remove(String key);

    CompletableFuture<List<String>> keys(String prefix);

    AutoCloseable watch(String key, IEventListener<NetworkCacheChangedEvent> listener);

    AutoCloseable watchPrefix(String prefix, IEventListener<NetworkCacheChangedEvent> listener);

}
