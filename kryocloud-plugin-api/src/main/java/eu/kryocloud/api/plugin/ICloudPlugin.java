package eu.kryocloud.api.plugin;

import eu.kryocloud.api.plugin.context.PluginContext;
import eu.kryocloud.api.plugin.description.PluginDescription;

public interface ICloudPlugin {

    default PluginDescription description() {
        return PluginDescription.from(getClass());
    }

    default void load(PluginContext context) throws Exception {

    }

    default void enable(PluginContext context) throws Exception {

    }

    default void disable(PluginContext context) throws Exception {

    }

}
