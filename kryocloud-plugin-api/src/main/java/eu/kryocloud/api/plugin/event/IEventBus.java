package eu.kryocloud.api.plugin.event;

import java.util.concurrent.CompletableFuture;

public interface IEventBus {

    <T extends ICloudEvent> IEventSubscription listen(Class<T> eventType, IEventListener<T> listener);

    <T extends ICloudEvent> IEventSubscription listen(Class<T> eventType, EventPriority priority, IEventListener<T> listener);

    CompletableFuture<Void> publish(ICloudEvent event);

}
