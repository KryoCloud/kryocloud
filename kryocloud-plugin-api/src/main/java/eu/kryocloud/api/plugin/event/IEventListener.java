package eu.kryocloud.api.plugin.event;

@FunctionalInterface
public interface IEventListener<T extends ICloudEvent> {

    void handle(T event) throws Exception;

}
