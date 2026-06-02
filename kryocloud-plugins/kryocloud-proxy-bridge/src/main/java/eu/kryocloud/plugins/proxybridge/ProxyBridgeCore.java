package eu.kryocloud.plugins.proxybridge;

import eu.kryocloud.api.plugin.CloudAPI;
import eu.kryocloud.api.plugin.bootstrap.CloudPluginSession;
import eu.kryocloud.api.plugin.cloud.model.CloudServiceSnapshot;
import eu.kryocloud.api.plugin.context.PluginContext;
import eu.kryocloud.api.plugin.event.IEventSubscription;
import eu.kryocloud.api.plugin.event.service.ServiceFailedEvent;
import eu.kryocloud.api.plugin.event.service.ServiceStartedEvent;
import eu.kryocloud.api.plugin.event.service.ServiceStateChangedEvent;
import eu.kryocloud.api.plugin.event.service.ServiceStoppedEvent;
import eu.kryocloud.api.plugin.event.service.ServiceStoppingEvent;
import eu.kryocloud.api.plugin.scheduler.IPluginTask;
import eu.kryocloud.plugins.proxybridge.command.CloudCommandBridge;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ProxyBridgeCore {

    private final ProxyPlatform platform;
    private final ConcurrentMap<String, ProxyRegistration> registered = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ProxyRegistration> aliases = new ConcurrentHashMap<>();
    private final Set<String> managed = ConcurrentHashMap.newKeySet();
    private final Collection<IEventSubscription> subscriptions = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean active = new AtomicBoolean();
    private final CloudCommandBridge commandBridge = new CloudCommandBridge();

    private volatile CloudPluginSession session;
    private volatile PluginContext context;
    private volatile IPluginTask syncTask;

    public ProxyBridgeCore(ProxyPlatform platform) {
        if (platform == null) {
            throw new IllegalArgumentException("platform must not be null");
        }

        this.platform = platform;
    }

    public CloudCommandBridge commandBridge() {
        return commandBridge;
    }

    public void start() {
        if (!active.compareAndSet(false, true)) {
            return;
        }

        CloudAPI.bootstrap(platform.plugin())
                .plugin(new CloudProxyBridgeExtension(this))
                .connectAsync()
                .whenComplete(this::handleSessionResult);
    }

    public void stop() {
        if (!active.compareAndSet(true, false)) {
            return;
        }

        disable();

        CloudPluginSession current = session;
        session = null;

        if (current == null) {
            return;
        }

        current.disconnect().whenComplete((value, throwable) -> {
            if (throwable == null) {
                return;
            }

            platform.warn("Could not disconnect KryoCloud session: " + throwable.getMessage());
        });
    }

    public void enable(PluginContext context) {
        this.context = context;

        subscribe();
        synchronize();

        syncTask = context.scheduler().repeat(Duration.ofSeconds(15), Duration.ofSeconds(15), this::synchronize);
        platform.info("Connected to KryoCloud through local plugin API as " + context.identity().serviceName());
    }

    public void disable() {
        if (syncTask != null) {
            syncTask.cancel();
            syncTask = null;
        }

        for (IEventSubscription subscription : new ArrayList<>(subscriptions)) {
            subscription.unsubscribe();
        }

        subscriptions.clear();
        unregisterAll();
        context = null;
    }

    private void subscribe() {
        PluginContext current = context;

        if (current == null) {
            return;
        }

        subscriptions.add(current.events().listen(ServiceStartedEvent.class, event -> register(event.service())));
        subscriptions.add(current.events().listen(ServiceStateChangedEvent.class, event -> update(event.service())));
        subscriptions.add(current.events().listen(ServiceStoppingEvent.class, event -> unregister(event.service().name())));
        subscriptions.add(current.events().listen(ServiceStoppedEvent.class, event -> unregister(event.service().name())));
        subscriptions.add(current.events().listen(ServiceFailedEvent.class, event -> unregister(event.service().name())));
    }

    private void synchronize() {
        PluginContext current = context;

        if (current == null) {
            return;
        }

        current.cloud().services().services()
                .thenAccept(this::synchronize)
                .exceptionally(throwable -> {
                    platform.warn("Could not synchronize services: " + throwable.getMessage());
                    return null;
                });
    }

    private void synchronize(Collection<CloudServiceSnapshot> services) {
        Set<String> activeServices = ConcurrentHashMap.newKeySet();

        for (CloudServiceSnapshot service : services) {
            ProxyServiceMapper.registration(service).ifPresent(registration -> {
                activeServices.add(registration.name());
                register(registration);
            });
        }

        for (String serviceName : new ArrayList<>(managed)) {
            if (activeServices.contains(serviceName)) {
                continue;
            }

            unregister(serviceName, false);
        }

        refreshAliases();
    }

    private void update(CloudServiceSnapshot service) {
        if (ProxyServiceMapper.registerable(service)) {
            register(service);
            return;
        }

        unregister(service.name());
    }

    private void register(CloudServiceSnapshot service) {
        ProxyServiceMapper.registration(service).ifPresent(this::register);
    }

    private void register(ProxyRegistration registration) {
        ProxyRegistration existing = registered.get(registration.name());

        if (registration.sameAddress(existing)) {
            return;
        }

        platform.register(registration);
        registered.put(registration.name(), registration);
        managed.add(registration.name());
        platform.info("Registered cloud service " + registration.name() + " -> " + registration.host() + ":" + registration.port());

        refreshAliases();
    }

    private void registerAlias(String alias, ProxyRegistration target) {
        ProxyRegistration registration = new ProxyRegistration(alias, target.groupName(), target.host(), target.port());
        ProxyRegistration existing = aliases.get(alias);

        if (registration.sameAddress(existing)) {
            return;
        }

        platform.register(registration);
        aliases.put(alias, registration);
        platform.info("Registered cloud group alias " + alias + " -> " + target.name() + " (" + target.host() + ":" + target.port() + ")");
    }

    private void unregister(String serviceName) {
        unregister(serviceName, true);
    }

    private void unregister(String serviceName, boolean refreshAliases) {
        if (serviceName == null || serviceName.isBlank()) {
            return;
        }

        ProxyRegistration removed = registered.remove(serviceName);
        boolean managedService = managed.remove(serviceName);

        if (removed == null && !managedService) {
            return;
        }

        platform.unregister(serviceName);
        platform.info("Unregistered cloud service " + serviceName);

        if (refreshAliases) {
            refreshAliases();
        }
    }

    private void refreshAliases() {
        Map<String, ProxyRegistration> activeAliases = new HashMap<>();

        for (ProxyRegistration registration : registered.values()) {
            String alias = ProxyServiceMapper.groupAlias(registration.groupName());

            if (alias.isBlank()) {
                continue;
            }

            Optional<ProxyRegistration> existing = Optional.ofNullable(activeAliases.get(alias));

            if (existing.isPresent() && serviceComparator().compare(existing.get(), registration) <= 0) {
                continue;
            }

            activeAliases.put(alias, registration);
        }

        for (Map.Entry<String, ProxyRegistration> entry : activeAliases.entrySet()) {
            registerAlias(entry.getKey(), entry.getValue());
        }

        for (String alias : new ArrayList<>(aliases.keySet())) {
            if (activeAliases.containsKey(alias)) {
                continue;
            }

            aliases.remove(alias);
            platform.unregister(alias);
            platform.info("Unregistered cloud group alias " + alias);
        }
    }

    private Comparator<ProxyRegistration> serviceComparator() {
        return Comparator.comparing(ProxyRegistration::name, String.CASE_INSENSITIVE_ORDER);
    }

    private void unregisterAll() {
        for (String alias : new ArrayList<>(aliases.keySet())) {
            aliases.remove(alias);
            platform.unregister(alias);
            platform.info("Unregistered cloud group alias " + alias);
        }

        for (String serviceName : new ArrayList<>(managed)) {
            unregister(serviceName, false);
        }
    }

    private void handleSessionResult(CloudPluginSession session, Throwable throwable) {
        if (throwable != null) {
            platform.error("Could not connect KryoCloud proxy bridge", throwable);
            return;
        }

        this.session = session;
    }

}
