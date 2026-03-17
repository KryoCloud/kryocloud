package eu.kryocloud.common.config;

import eu.kryocloud.api.config.IConfig;
import eu.kryocloud.api.config.IConfigProvider;
import eu.kryocloud.api.config.type.IConfigTypeProvider;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.nio.file.Path;
import java.util.HashMap;

public class ConfigProvider implements IConfigProvider {

    private static final HashMap<Class<?>, IConfig> configurations = new HashMap<>();;
    private static final HashMap<Class<?>, IConfigTypeProvider> typeProviderOverrides = new HashMap<>();;

    @Override
    public <T> T registerConfig(Path path, Class<T> clazz) throws Exception {
        if (!Config.class.isAssignableFrom(clazz)) {
            return null;
        }
        T config = clazz.getConstructor(Path.class).newInstance(path);
        configurations.put(clazz, (IConfig) config);
        ((IConfig) config).load();
        ((IConfig) config).save();
        return config;
    }

    @Override
    public void unregisterConfig(Class<?> clazz) {
        if (configurations.containsKey(clazz)) {
            configurations.remove(clazz);
        }
        if (typeProviderOverrides.containsKey(clazz)) {
            configurations.remove(clazz);
        }
    }

    @Override
    public <T> T getConfig(Class<T> clazz) {
        if (!Config.class.isAssignableFrom(clazz)) {
            return null;
        }
        return (T) configurations.get(clazz);
    }
}
