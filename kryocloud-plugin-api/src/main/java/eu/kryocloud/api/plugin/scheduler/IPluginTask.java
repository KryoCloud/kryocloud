package eu.kryocloud.api.plugin.scheduler;

public interface IPluginTask extends AutoCloseable {

    boolean cancelled();

    void cancel();

    @Override
    default void close() {
        cancel();
    }

}
