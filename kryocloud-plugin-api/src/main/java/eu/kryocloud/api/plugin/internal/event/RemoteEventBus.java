package eu.kryocloud.api.plugin.internal.event;

import eu.kryocloud.api.plugin.event.EventPriority;
import eu.kryocloud.api.plugin.event.ICloudEvent;
import eu.kryocloud.api.plugin.event.IEventBus;
import eu.kryocloud.api.plugin.event.IEventListener;
import eu.kryocloud.api.plugin.event.IEventSubscription;
import eu.kryocloud.api.plugin.event.lifecycle.CloudDisconnectedEvent;
import eu.kryocloud.api.plugin.internal.client.PluginRequestClient;
import eu.kryocloud.api.plugin.logging.IPluginLogger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RemoteEventBus implements IEventBus, AutoCloseable {

    private final IPluginLogger logger;
    private final ConcurrentMap<Class<? extends ICloudEvent>, CopyOnWriteArrayList<Subscription<? extends ICloudEvent>>> subscriptions = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private PluginRequestClient client;

    public RemoteEventBus(IPluginLogger logger) {
        this.logger = logger;
    }

    public void client(PluginRequestClient client) {
        this.client = client;
    }

    @Override
    public <T extends ICloudEvent> IEventSubscription listen(Class<T> eventType, IEventListener<T> listener) {
        return listen(eventType, EventPriority.NORMAL, listener);
    }

    @Override
    public <T extends ICloudEvent> IEventSubscription listen(Class<T> eventType, EventPriority priority, IEventListener<T> listener) {
        if (eventType == null) {
            throw new IllegalArgumentException("eventType must not be null");
        }

        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }

        Subscription<T> subscription = new Subscription<>(eventType, priority == null ? EventPriority.NORMAL : priority, listener, this);
        subscriptions.computeIfAbsent(eventType, key -> new CopyOnWriteArrayList<>()).add(subscription);
        subscriptions.get(eventType).sort(Comparator.comparing(Subscription::priority));

        if (client != null) {
            client.subscribe(eventType.getName());
        }

        return subscription;
    }

    @Override
    public CompletableFuture<Void> publish(ICloudEvent event) {
        if (event == null) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> dispatch(event), executor);
    }

    public void dispatchRemote(String route, Map<String, String> payload) {
        publish(RemoteEventMapper.map(route, payload));
    }

    public void dispatchDisconnected(String reason) {
        publish(new CloudDisconnectedEvent(reason));
    }

    public void remove(Subscription<? extends ICloudEvent> subscription) {
        CopyOnWriteArrayList<Subscription<? extends ICloudEvent>> listeners = subscriptions.get(subscription.eventType());

        if (listeners == null) {
            return;
        }

        listeners.remove(subscription);

        if (!listeners.isEmpty() || client == null) {
            return;
        }

        client.unsubscribe(subscription.eventType().getName());
    }

    @Override
    public void close() {
        subscriptions.clear();
        executor.shutdownNow();
    }

    private void dispatch(ICloudEvent event) {
        List<Subscription<? extends ICloudEvent>> listeners = new ArrayList<>();
        subscriptions.forEach((type, entries) -> {
            if (!type.isAssignableFrom(event.getClass())) {
                return;
            }

            listeners.addAll(entries);
        });

        listeners.sort(Comparator.comparing(Subscription::priority));

        for (Subscription<? extends ICloudEvent> subscription : listeners) {
            subscription.dispatch(event, logger);
        }
    }

    private record Subscription<T extends ICloudEvent>(Class<T> eventType, EventPriority priority, IEventListener<T> listener, RemoteEventBus bus) implements IEventSubscription {

        @Override
        public boolean active() {
            CopyOnWriteArrayList<Subscription<? extends ICloudEvent>> listeners = bus.subscriptions.get(eventType);
            return listeners != null && listeners.contains(this);
        }

        @Override
        public void unsubscribe() {
            bus.remove(this);
        }

        void dispatch(ICloudEvent event, IPluginLogger logger) {
            try {
                listener.handle(eventType.cast(event));
            } catch (Throwable throwable) {
                logger.error("Cloud event listener failed for " + eventType.getName(), throwable);
            }
        }

    }

}
