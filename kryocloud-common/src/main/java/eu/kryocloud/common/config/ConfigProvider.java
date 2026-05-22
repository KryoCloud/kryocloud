package eu.kryocloud.common.config;

import eu.kryocloud.api.config.IConfig;
import eu.kryocloud.api.config.IConfigProvider;
import eu.kryocloud.api.config.type.IConfigTypeProvider;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigProvider implements IConfigProvider {

    private static final Map<Class<?>, IConfig> configurations = new ConcurrentHashMap<>();
    private static final Map<Class<?>, IConfigTypeProvider> typeProviderOverrides = new ConcurrentHashMap<>();

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
        if(clazz == null) return;
        IConfig config = configurations.remove(clazz);
        typeProviderOverrides.remove(config.getProvider());
    }

    @Override
    public <T> T getConfig(Class<T> clazz) {
        if (!Config.class.isAssignableFrom(clazz)) {
            return null;
        }
        return (T) configurations.get(clazz);
    }
}
