package eu.kryocloud.common.config;

import eu.kryocloud.api.config.IConfig;
import eu.kryocloud.api.config.IConfigProvider;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.nio.file.Path;

public class ConfigProvider implements IConfigProvider {

    private final Object2ObjectOpenHashMap<Path, IConfig> configurations;

    public ConfigProvider() {
        this.configurations = new Object2ObjectOpenHashMap<>();
    }

    @Override
    public <T> T registerConfig(Path path, Class<T> clazz) {
        if (!Config.class.isAssignableFrom(clazz)) {
            return null;
        }
        return null;
    }

    @Override
    public void unregisterConfig(Path path) {

    }
}
