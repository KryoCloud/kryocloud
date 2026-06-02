package eu.kryocloud.api.plugin.internal.client;

import eu.kryocloud.api.plugin.bootstrap.CloudPluginEndpoint;
import eu.kryocloud.api.plugin.description.PluginDescription;
import eu.kryocloud.api.plugin.identity.CloudServiceIdentity;
import eu.kryocloud.api.plugin.internal.event.RemoteEventBus;
import eu.kryocloud.api.plugin.internal.protocol.PluginWireCodec;
import eu.kryocloud.api.plugin.internal.protocol.PluginWireMessage;
import eu.kryocloud.api.plugin.internal.protocol.PluginWireMessageType;
import eu.kryocloud.api.plugin.internal.protocol.payload.Payload;
import eu.kryocloud.api.plugin.internal.transport.PluginStreamTransport;
import eu.kryocloud.api.plugin.internal.transport.frame.StreamFrame;
import eu.kryocloud.api.plugin.internal.transport.frame.StreamFrameType;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PluginRequestClient implements AutoCloseable {

    private final PluginDescription description;
    private final CloudServiceIdentity identity;
    private final PluginStreamTransport transport;
    private final ConcurrentMap<UUID, CompletableFuture<PluginWireMessage>> pending = new ConcurrentHashMap<>();
    private final RemoteEventBus eventBus;

    public PluginRequestClient(PluginDescription description, CloudServiceIdentity identity, CloudPluginEndpoint endpoint, Duration timeout, RemoteEventBus eventBus) {
        this.description = description;
        this.identity = identity;
        this.eventBus = eventBus;
        this.transport = new PluginStreamTransport(endpoint.host(), endpoint.port(), timeout, this::handleFrame, this::handleError);
    }

    public CompletableFuture<Void> connect() {
        return transport.connect()
                .thenCompose(value -> handshake())
                .thenApply(value -> null);
    }

    public CompletableFuture<PluginWireMessage> handshake() {
        return send(PluginWireMessage.handshake(description.id(), Payload.create()
                .put("id", description.id())
                .put("name", description.name())
                .put("version", description.version())
                .putAll(identity.payload())
                .map()));
    }

    public CompletableFuture<PluginWireMessage> request(String route, Map<String, String> payload) {
        return send(PluginWireMessage.request(description.id(), route, payload));
    }

    public CompletableFuture<PluginWireMessage> subscribe(String eventName) {
        return send(PluginWireMessage.subscribe(description.id(), "event.subscribe", Payload.create()
                .put("event", eventName)
                .map()));
    }

    public CompletableFuture<PluginWireMessage> unsubscribe(String eventName) {
        return send(PluginWireMessage.unsubscribe(description.id(), "event.unsubscribe", Payload.create()
                .put("event", eventName)
                .map()));
    }

    public CompletableFuture<PluginWireMessage> message(String route, Map<String, String> payload) {
        return send(PluginWireMessage.message(description.id(), route, payload));
    }

    public boolean connected() {
        return transport.connected();
    }

    @Override
    public void close() {
        transport.close();
        pending.forEach((id, future) -> future.completeExceptionally(new IllegalStateException("Plugin connection closed")));
        pending.clear();
    }

    private CompletableFuture<PluginWireMessage> send(PluginWireMessage message) {
        CompletableFuture<PluginWireMessage> future = new CompletableFuture<>();
        pending.put(message.id(), future);

        try {
            transport.send(message);
        } catch (RuntimeException exception) {
            pending.remove(message.id());
            future.completeExceptionally(exception);
        }

        return future;
    }

    private void handleFrame(StreamFrame frame) {
        if (frame.type() == StreamFrameType.HEARTBEAT) {
            return;
        }

        if (frame.payload().length == 0) {
            return;
        }

        PluginWireMessage message = PluginWireCodec.decode(frame.payload());

        if (message.type() == PluginWireMessageType.EVENT) {
            eventBus.dispatchRemote(message.route(), message.payload());
            transport.ack(frame.streamId());
            return;
        }

        CompletableFuture<PluginWireMessage> future = pending.remove(message.id());

        if (future == null) {
            transport.ack(frame.streamId());
            return;
        }

        if (message.type() == PluginWireMessageType.ERROR) {
            future.completeExceptionally(new IllegalStateException(message.payload().getOrDefault("message", "KryoCloud request failed")));
            transport.ack(frame.streamId());
            return;
        }

        future.complete(message);
        transport.ack(frame.streamId());
    }

    private void handleError(Throwable throwable) {
        pending.forEach((id, future) -> future.completeExceptionally(throwable));
        pending.clear();
        eventBus.dispatchDisconnected(throwable.getMessage());
    }

}
