package eu.kryocloud.api.plugin.messaging;

@FunctionalInterface
public interface IPluginMessageListener {

    void handle(PluginMessage message) throws Exception;

}
