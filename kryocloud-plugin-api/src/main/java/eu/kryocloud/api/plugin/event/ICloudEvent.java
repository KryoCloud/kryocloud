package eu.kryocloud.api.plugin.event;

public interface ICloudEvent {

    default String name() {
        return getClass().getName();
    }

}
