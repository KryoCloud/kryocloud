package eu.kryocloud.plugins.proxybridge;

import java.util.Set;

public interface ProxyPlatform {

    Object plugin();

    String name();

    Set<String> registeredServices();

    void register(ProxyRegistration registration);

    void unregister(String serviceName);

    void info(String message);

    void warn(String message);

    void error(String message, Throwable throwable);

}
