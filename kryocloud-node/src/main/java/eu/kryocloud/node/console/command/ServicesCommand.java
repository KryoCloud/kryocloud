package eu.kryocloud.node.console.command;

import eu.kryocloud.network.connection.KryoConnection;
import eu.kryocloud.network.packet.type.service.ServiceCleanupRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceCommandRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceLogsRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceStopRequestPacket;
import eu.kryocloud.node.console.ConsoleCategory;
import eu.kryocloud.node.console.ConsoleCommand;
import eu.kryocloud.node.console.ConsoleContext;
import eu.kryocloud.node.console.tui.ConsoleAnimation;
import eu.kryocloud.node.console.tui.ConsoleTheme;
import eu.kryocloud.node.service.runtime.NodeServiceSnapshot;
import eu.kryocloud.node.wrapper.WrapperSnapshot;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class ServicesCommand implements ConsoleCommand {

    private final ConsoleAnimation animation = new ConsoleAnimation();

    @Override
    public String name() {
        return "services";
    }

    @Override
    public ConsoleCategory category() {
        return ConsoleCategory.SERVICE;
    }

    @Override
    public Collection<String> aliases() {
        return List.of("service", "instances", "instance", "servers");
    }

    @Override
    public String description() {
        return "Lists, inspects and controls Minecraft services";
    }

    @Override
    public String usage() {
        return "services [list|running|info <serviceId>|stop <serviceId> [reason]|kill <serviceId>|logs <serviceId> [lines]|command <serviceId> <command...>|cleanup [all|wrapperId] [--dry-run]]";
    }

    @Override
    public void execute(ConsoleContext context, List<String> arguments) {
        if (arguments.isEmpty()) {
            list(context);
            return;
        }

        String action = arguments.getFirst();

        if ("list".equalsIgnoreCase(action)) {
            list(context);
            return;
        }

        if ("running".equalsIgnoreCase(action)) {
            running(context);
            return;
        }

        if ("info".equalsIgnoreCase(action)) {
            info(context, arguments);
            return;
        }

        if ("stop".equalsIgnoreCase(action)) {
            stop(context, arguments, false);
            return;
        }

        if ("kill".equalsIgnoreCase(action)) {
            stop(context, arguments, true);
            return;
        }

        if ("logs".equalsIgnoreCase(action)) {
            logs(context, arguments);
            return;
        }

        if ("command".equalsIgnoreCase(action)) {
            command(context, arguments);
            return;
        }

        if ("cleanup".equalsIgnoreCase(action)) {
            cleanup(context, arguments);
            return;
        }

        throw new IllegalArgumentException("Usage: " + usage());
    }

    private void list(ConsoleContext context) {
        List<NodeServiceSnapshot> services = context.node().serviceRegistry().services();

        if (services.isEmpty()) {
            context.warn("No Minecraft services known.");
            return;
        }

        context.header("Minecraft services");

        for (NodeServiceSnapshot service : services) {
            context.print(" • " + context.accent(service.serviceId()) + " | " + service.groupName() + " | " + service.serviceType() + " | " + ConsoleTheme.state(service.state()) + " | " + service.wrapperId() + " | " + service.host() + ":" + service.port());
        }
    }

    private void running(ConsoleContext context) {
        List<NodeServiceSnapshot> services = context.node().serviceRegistry().runningServices();

        if (services.isEmpty()) {
            context.warn("No running Minecraft services.");
            return;
        }

        context.header("Running Minecraft services");

        for (NodeServiceSnapshot service : services) {
            context.print(" • " + context.accent(service.serviceId()) + " | " + service.groupName() + " | " + service.wrapperId() + " | " + service.host() + ":" + service.port());
        }
    }

    private void info(ConsoleContext context, List<String> arguments) {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException("Usage: services info <serviceId>");
        }

        String serviceId = arguments.get(1);
        NodeServiceSnapshot service = service(context, serviceId);

        context.header("Service " + service.serviceId());
        context.print("  Group: " + service.groupName());
        context.print("  Type: " + service.serviceType());
        context.print("  State: " + ConsoleTheme.state(service.state()));
        context.print("  Wrapper: " + service.wrapperId());
        context.print("  Address: " + service.host() + ":" + service.port());
        context.print("  Message: " + service.message());
        context.print("  Updated: " + service.timestamp());
    }

    private void stop(ConsoleContext context, List<String> arguments, boolean force) {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException(force ? "Usage: services kill <serviceId>" : "Usage: services stop <serviceId> [reason]");
        }

        NodeServiceSnapshot service = service(context, arguments.get(1));
        KryoConnection connection = wrapperConnection(context, service);
        String reason = force ? "Force stop requested from console" : joined(arguments, 2, "Console stop requested");
        UUID requestId = UUID.randomUUID();

        connection.send(new ServiceStopRequestPacket(requestId, service.serviceId(), force, reason));
        animation.spin(context, (force ? "Sending kill request to " : "Sending stop request to ") + service.serviceId(), Duration.ofMillis(450));
        context.success((force ? "Kill" : "Stop") + " request sent to " + service.serviceId() + " on " + service.wrapperId() + ".");
    }

    private void logs(ConsoleContext context, List<String> arguments) {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException("Usage: services logs <serviceId> [lines]");
        }

        NodeServiceSnapshot service = service(context, arguments.get(1));
        KryoConnection connection = wrapperConnection(context, service);
        int lines = arguments.size() >= 3 ? positiveInt(arguments.get(2), "lines") : 80;
        UUID requestId = UUID.randomUUID();

        connection.send(new ServiceLogsRequestPacket(requestId, service.serviceId(), lines));
        animation.spin(context, "Requesting logs from " + service.serviceId(), Duration.ofMillis(450));
        context.success("Requested last " + lines + " log line(s) from " + service.serviceId() + ".");
    }

    private void command(ConsoleContext context, List<String> arguments) {
        if (arguments.size() < 3) {
            throw new IllegalArgumentException("Usage: services command <serviceId> <command...>");
        }

        NodeServiceSnapshot service = service(context, arguments.get(1));
        KryoConnection connection = wrapperConnection(context, service);
        String command = joined(arguments, 2, "");

        if (command.isBlank()) {
            throw new IllegalArgumentException("command must not be blank");
        }

        UUID requestId = UUID.randomUUID();
        connection.send(new ServiceCommandRequestPacket(requestId, service.serviceId(), command));
        animation.spin(context, "Sending Minecraft command to " + service.serviceId(), Duration.ofMillis(450));
        context.success("Command sent to " + service.serviceId() + ": " + context.code(command));
    }

    private void cleanup(ConsoleContext context, List<String> arguments) {
        boolean dryRun = arguments.stream().anyMatch(argument -> "--dry-run".equalsIgnoreCase(argument) || "dry".equalsIgnoreCase(argument) || "preview".equalsIgnoreCase(argument));
        String target = cleanupTarget(arguments);
        UUID requestId = UUID.randomUUID();
        int sent = 0;

        if ("all".equalsIgnoreCase(target)) {
            for (WrapperSnapshot wrapper : context.node().wrapperRegistry().wrappers()) {
                if (sendCleanup(context, wrapper.wrapperId(), requestId, dryRun)) {
                    sent++;
                }
            }

            if (sent < 1) {
                context.warn("No connected wrappers available for cleanup.");
                return;
            }

            animation.spin(context, (dryRun ? "Requesting cleanup preview" : "Requesting cleanup") + " on wrappers", Duration.ofMillis(600));
            context.success((dryRun ? "Cleanup preview" : "Cleanup") + " requested on " + sent + " wrapper(s).");
            return;
        }

        if (!sendCleanup(context, target, requestId, dryRun)) {
            throw new IllegalStateException("Wrapper is not connected: " + target);
        }

        animation.spin(context, (dryRun ? "Requesting cleanup preview" : "Requesting cleanup") + " on " + target, Duration.ofMillis(600));
        context.success((dryRun ? "Cleanup preview" : "Cleanup") + " requested on " + target + ".");
    }

    private boolean sendCleanup(ConsoleContext context, String wrapperId, UUID requestId, boolean dryRun) {
        return context.node().wrapperRegistry().connection(wrapperId).map(connection -> {
            connection.send(new ServiceCleanupRequestPacket(requestId, dryRun));
            return true;
        }).orElse(false);
    }

    private String cleanupTarget(List<String> arguments) {
        for (String argument : arguments.subList(1, arguments.size())) {
            if ("--dry-run".equalsIgnoreCase(argument) || "dry".equalsIgnoreCase(argument) || "preview".equalsIgnoreCase(argument)) {
                continue;
            }

            return argument;
        }

        return "all";
    }

    private NodeServiceSnapshot service(ConsoleContext context, String serviceId) {
        return context.node().serviceRegistry().service(serviceId).orElseThrow(() -> new IllegalArgumentException("Unknown Minecraft service: " + serviceId));
    }

    private KryoConnection wrapperConnection(ConsoleContext context, NodeServiceSnapshot service) {
        return context.node().wrapperRegistry().connection(service.wrapperId()).orElseThrow(() -> new IllegalStateException("Wrapper is not connected: " + service.wrapperId()));
    }

    private String joined(List<String> arguments, int fromIndex, String fallback) {
        if (arguments.size() <= fromIndex) {
            return fallback;
        }

        return String.join(" ", arguments.subList(fromIndex, arguments.size()));
    }

    private int positiveInt(String input, String name) {
        try {
            int value = Integer.parseInt(input);

            if (value < 1) {
                throw new IllegalArgumentException(name + " must be greater than 0");
            }

            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a valid number", exception);
        }
    }
}