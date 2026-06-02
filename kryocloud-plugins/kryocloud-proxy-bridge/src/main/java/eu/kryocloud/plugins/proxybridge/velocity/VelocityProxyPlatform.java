package eu.kryocloud.plugins.proxybridge.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import eu.kryocloud.plugins.proxybridge.ProxyPlatform;
import eu.kryocloud.plugins.proxybridge.ProxyRegistration;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;

public final class VelocityProxyPlatform implements ProxyPlatform {

    private final Object plugin;
    private final ProxyServer proxy;
    private final Logger logger;

    public VelocityProxyPlatform(Object plugin, ProxyServer proxy, Logger logger) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin must not be null");
        }

        if (proxy == null) {
            throw new IllegalArgumentException("proxy must not be null");
        }

        if (logger == null) {
            throw new IllegalArgumentException("logger must not be null");
        }

        this.plugin = plugin;
        this.proxy = proxy;
        this.logger = logger;
    }

    @Override
    public Object plugin() {
        return plugin;
    }

    @Override
    public String name() {
        return "Velocity";
    }

    @Override
    public Set<String> registeredServices() {
        return proxy.getAllServers().stream()
                .map(server -> server.getServerInfo().getName())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void register(ProxyRegistration registration) {
        unregister(registration.name());
        proxy.registerServer(new ServerInfo(registration.name(), registration.address()));
    }

    @Override
    public void unregister(String serviceName) {
        proxy.getServer(serviceName).ifPresent(server -> proxy.unregisterServer(server.getServerInfo()));
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warn(String message) {
        logger.warn(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

}
