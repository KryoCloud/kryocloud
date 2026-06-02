package eu.kryocloud.api.plugin.event;

public interface ICancellableEvent extends ICloudEvent {

    boolean cancelled();

    void cancelled(boolean cancelled);

}
