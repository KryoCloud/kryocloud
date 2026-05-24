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
import eu.kryocloud.node.console.tui.Tone;
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
        return "service";
    }

    @Override
    public ConsoleCategory category() {
        return ConsoleCategory.SERVICE;
    }

    @Override
    public Collection<String> aliases() {
        return List.of("services", "instance", "instances", "server", "servers");
    }

    @Override
    public String description() {
        return "Controls Minecraft services";
    }

    @Override
    public String usage() {
        return "service <name> <info|stop|kill|logs|cmd> | service list | service cleanup";
    }

    @Override
    public void execute(ConsoleContext context, List<String> arguments) {
        if (arguments.isEmpty()) {
            list(context);
            return;
        }

        String first = arguments.getFirst();

        if ("list".equalsIgnoreCase(first)) {
            list(context);
            return;
        }

        if ("running".equalsIgnoreCase(first)) {
            running(context);
            return;
        }

        if ("cleanup".equalsIgnoreCase(first)) {
            cleanup(context, arguments);
            return;
        }

        serviceAction(context, arguments);
    }

    private void serviceAction(ConsoleContext context, List<String> arguments) {
        String serviceId = arguments.getFirst();
        String action = arguments.size() >= 2 ? arguments.get(1) : "info";

        if ("info".equalsIgnoreCase(action)) {
            info(context, serviceId);
            return;
        }

        if ("stop".equalsIgnoreCase(action)) {
            stop(context, serviceId, joined(arguments, 2, "Console stop requested"), false);
            return;
        }

        if ("kill".equalsIgnoreCase(action)) {
            stop(context, serviceId, "Force stop requested from console", true);
            return;
        }

        if ("logs".equalsIgnoreCase(action) || "log".equalsIgnoreCase(action)) {
            int lines = arguments.size() >= 3 ? positiveInt(arguments.get(2), "lines") : 80;
            logs(context, serviceId, lines);
            return;
        }

        if ("cmd".equalsIgnoreCase(action) || "command".equalsIgnoreCase(action) || "write".equalsIgnoreCase(action)) {
            command(context, serviceId, joined(arguments, 2, ""));
            return;
        }

        throw new IllegalArgumentException("Usage: service " + serviceId + " <info|stop|kill|logs|cmd>");
    }

    private void list(ConsoleContext context) {
        List<NodeServiceSnapshot> services = context.node().serviceRegistry().services();

        if (services.isEmpty()) {
            context.warn("No Minecraft services known.");
            return;
        }

        context.header("Minecraft services");
        renderServiceTable(context, services);
    }

    private void running(ConsoleContext context) {
        List<NodeServiceSnapshot> services = context.node().serviceRegistry().runningServices();

        if (services.isEmpty()) {
            context.warn("No running Minecraft services.");
            return;
        }

        context.header("Running Minecraft services");
        renderServiceTable(context, services);
    }

    private void renderServiceTable(ConsoleContext context, List<NodeServiceSnapshot> services) {
        for (NodeServiceSnapshot service : services) {
            String memory = service.processMemoryMb() > 0 ? service.processMemoryMb() + "MB" : "collecting";
            String cpu = service.cpuLoadPermille() > 0 ? String.format("%.1f%%", service.cpuLoadPermille() / 10.0D) : "0.0%";
            context.print(" " + ConsoleTheme.bullet() + " " + context.accent(service.serviceId()) + context.muted("  •  ") + ConsoleTheme.state(service.state()) + context.muted("  •  ") + service.groupName() + context.muted("  •  ") + memory + context.muted(" / ") + cpu);
        }
    }

    private void info(ConsoleContext context, String serviceId) {
        NodeServiceSnapshot service = service(context, serviceId);

        context.header("Service " + service.serviceId());
        context.row("Group", service.groupName());
        context.row("Type", service.serviceType().name());
        context.row("State", ConsoleTheme.state(service.state()));
        context.row("Wrapper", service.wrapperId());
        context.row("Address", service.host() + ":" + service.port());
        context.row("Memory", service.processMemoryMb() > 0 ? service.processMemoryMb() + "MB" : "collecting");
        context.row("CPU", String.format("%.1f%%", service.cpuLoadPermille() / 10.0D));
        context.row("Uptime", formatDuration(service.uptimeMillis()));
        context.row("Message", service.message());
    }

    private void stop(ConsoleContext context, String serviceId, String reason, boolean force) {
        NodeServiceSnapshot service = service(context, serviceId);
        KryoConnection connection = wrapperConnection(context, service);
        UUID requestId = UUID.randomUUID();

        animation.spin(context, (force ? "shattering " : "freezing ") + service.serviceId(), Duration.ofMillis(500));
        connection.send(new ServiceStopRequestPacket(requestId, service.serviceId(), force, reason));
        animation.success(context, (force ? "Kill" : "Stop") + " request sent to " + service.serviceId());
    }

    private void logs(ConsoleContext context, String serviceId, int lines) {
        NodeServiceSnapshot service = service(context, serviceId);
        KryoConnection connection = wrapperConnection(context, service);

        animation.spin(context, "collecting frost logs from " + service.serviceId(), Duration.ofMillis(450));
        connection.send(new ServiceLogsRequestPacket(UUID.randomUUID(), service.serviceId(), lines));
        animation.success(context, "Requested last " + lines + " log line(s).");
    }

    private void command(ConsoleContext context, String serviceId, String command) {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("command must not be blank");
        }

        NodeServiceSnapshot service = service(context, serviceId);
        KryoConnection connection = wrapperConnection(context, service);

        animation.spin(context, "writing command into " + service.serviceId(), Duration.ofMillis(450));
        connection.send(new ServiceCommandRequestPacket(UUID.randomUUID(), service.serviceId(), command));
        animation.success(context, "Command sent: " + context.code(command));
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

            animation.spin(context, dryRun ? "scanning orphaned temp instances" : "cleaning orphaned temp instances", Duration.ofMillis(600));
            animation.success(context, (dryRun ? "Cleanup preview" : "Cleanup") + " requested on " + sent + " wrapper(s).");
            return;
        }

        if (!sendCleanup(context, target, requestId, dryRun)) {
            throw new IllegalStateException("Wrapper is not connected: " + target);
        }

        animation.spin(context, dryRun ? "scanning " + target : "cleaning " + target, Duration.ofMillis(600));
        animation.success(context, (dryRun ? "Cleanup preview" : "Cleanup") + " requested on " + target + ".");
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

    private String formatDuration(long millis) {
        if (millis < 1) {
            return Tone.MUTED.paint("collecting");
        }

        long seconds = millis / 1000L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;

        if (hours > 0) {
            return hours + "h " + minutes % 60L + "m";
        }

        if (minutes > 0) {
            return minutes + "m " + seconds % 60L + "s";
        }

        return seconds + "s";
    }
}
