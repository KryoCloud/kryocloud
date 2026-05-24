package eu.kryocloud.node.console.command;

import eu.kryocloud.network.connection.KryoConnection;
import eu.kryocloud.network.packet.type.service.ServiceCleanupRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceCommandRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceLogsRequestPacket;
import eu.kryocloud.network.packet.type.service.ServiceStopRequestPacket;
import eu.kryocloud.node.console.ConsoleCategory;
import eu.kryocloud.node.console.ConsoleCommand;
import eu.kryocloud.node.console.ConsoleContext;
import eu.kryocloud.node.console.tui.Box;
import eu.kryocloud.node.console.tui.Column;
import eu.kryocloud.node.console.tui.ConsoleAnimation;
import eu.kryocloud.node.console.tui.ConsoleTheme;
import eu.kryocloud.node.console.tui.Glyph;
import eu.kryocloud.node.console.tui.Table;
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
        renderServiceTable(context, services);
        context.print("");
    }

    private void running(ConsoleContext context) {
        List<NodeServiceSnapshot> services = context.node().serviceRegistry().runningServices();

        if (services.isEmpty()) {
            context.warn("No running Minecraft services.");
            return;
        }

        context.header("Running Minecraft services");
        renderServiceTable(context, services);
        context.print("");
    }

    private void renderServiceTable(ConsoleContext context, List<NodeServiceSnapshot> services) {
        Table table = Table.builder().column(Column.left("Service").minWidth(14)).column(Column.left("Group").minWidth(10)).column(Column.left("Type").minWidth(7)).column(Column.left("State").minWidth(9)).column(Column.left("Wrapper").minWidth(12)).column(Column.left("Address").minWidth(18));

        for (NodeServiceSnapshot service : services) {
            table.row(Tone.PRIMARY.paint(service.serviceId()), Tone.CRYSTAL.paint(service.groupName()), Tone.INFO.paint(service.serviceType().toString()), ConsoleTheme.state(service.state()), Tone.MUTED.paint(service.wrapperId()), Tone.CRYSTAL.paint(service.host() + ":" + service.port()));
        }

        table.render(context::print);
    }

    private void info(ConsoleContext context, List<String> arguments) {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException("Usage: services info <serviceId>");
        }

        String serviceId = arguments.get(1);
        NodeServiceSnapshot service = service(context, serviceId);

        context.print("");
        Box.titled("Service " + service.serviceId()).minWidth(58).blank().line(label("Group   ") + " " + Tone.CRYSTAL.paint(service.groupName())).line(label("Type    ") + " " + Tone.INFO.paint(service.serviceType().toString())).line(label("State   ") + " " + ConsoleTheme.state(service.state())).line(label("RAM     ") + " " + Tone.CRYSTAL.paint(service.processMemoryMb() + "MB")).line(label("CPU     ") + " " + ConsoleTheme.progressBarWithPercent(service.cpuLoadRatio(), 12)).line(label("Uptime  ") + " " + Tone.MUTED.paint(formatDuration(service.uptimeMillis()))).line(label("Wrapper ") + " " + Tone.MUTED.paint(service.wrapperId())).line(label("Address ") + " " + Tone.CRYSTAL.paint(service.host() + ":" + service.port())).line(label("Message ") + " " + Tone.FROST.paint(service.message())).line(label("Updated ") + " " + Tone.MUTED.paint(String.valueOf(service.timestamp()))).blank().render(context::print);
        context.print("");
    }

    private String label(String value) {
        return Tone.SECONDARY.paint(value);
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


    private String formatDuration(long millis) {
        if (millis < 1) {
            return "unknown";
        }

        long seconds = millis / 1000L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;

        if (hours > 0) {
            return hours + "h " + (minutes % 60L) + "m";
        }

        if (minutes > 0) {
            return minutes + "m " + (seconds % 60L) + "s";
        }

        return seconds + "s";
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
