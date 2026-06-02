package eu.kryocloud.plugins.proxybridge.bungee;

import eu.kryocloud.plugins.proxybridge.ProxyPlatform;
import eu.kryocloud.plugins.proxybridge.ProxyRegistration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;

public final class BungeeProxyPlatform implements ProxyPlatform {

    private final Plugin plugin;
    private final ProxyServer proxy;

    public BungeeProxyPlatform(Plugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin must not be null");
        }

        this.plugin = plugin;
        this.proxy = plugin.getProxy();
    }

    @Override
    public Object plugin() {
        return plugin;
    }

    @Override
    public String name() {
        return "BungeeCord";
    }

    @Override
    public Set<String> registeredServices() {
        return new HashSet<>(proxy.getServers().keySet());
    }

    @Override
    public void register(ProxyRegistration registration) {
        unregister(registration.name());

        ServerInfo server = proxy.constructServerInfo(registration.name(), registration.address(), "KryoCloud service " + registration.name(), false);
        proxy.getServers().put(registration.name(), server);
    }

    @Override
    public void unregister(String serviceName) {
        proxy.getServers().remove(serviceName);
    }

    @Override
    public void info(String message) {
        plugin.getLogger().info(message);
    }

    @Override
    public void warn(String message) {
        plugin.getLogger().warning(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        plugin.getLogger().log(Level.SEVERE, message, throwable);
    }

}
