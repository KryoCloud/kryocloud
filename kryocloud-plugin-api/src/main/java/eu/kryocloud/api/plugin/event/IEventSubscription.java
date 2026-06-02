package eu.kryocloud.api.plugin.event;

public interface IEventSubscription extends AutoCloseable {

    Class<? extends ICloudEvent> eventType();

    boolean active();

    void unsubscribe();

    @Override
    default void close() {
        unsubscribe();
    }

}
