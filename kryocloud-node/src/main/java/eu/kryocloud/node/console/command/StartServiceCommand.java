package eu.kryocloud.node.console.command;

import eu.kryocloud.api.group.IGroup;
import eu.kryocloud.network.protocol.CloudServiceType;
import eu.kryocloud.node.console.ConsoleCategory;
import eu.kryocloud.node.console.ConsoleCommand;
import eu.kryocloud.node.console.ConsoleContext;
import eu.kryocloud.node.console.tui.ConsoleAnimation;
import eu.kryocloud.node.service.schedule.ServiceStartPlan;
import eu.kryocloud.node.service.schedule.ServiceStartResult;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class StartServiceCommand implements ConsoleCommand {

    private final ConsoleAnimation animation = new ConsoleAnimation();

    @Override
    public String name() {
        return "start";
    }

    @Override
    public ConsoleCategory category() {
        return ConsoleCategory.SERVICE;
    }

    @Override
    public Collection<String> aliases() {
        return List.of("startservice");
    }

    @Override
    public String description() {
        return "Starts Minecraft services from a group or manually";
    }

    @Override
    public String usage() {
        return "start <groupName> [count] | start manual <serviceId> <group> <template> <server|proxy> <memoryMb> <port> [static]";
    }

    @Override
    public void execute(ConsoleContext context, List<String> arguments) {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("Usage: " + usage());
        }

        if ("manual".equalsIgnoreCase(arguments.getFirst())) {
            startManual(context, arguments);
            return;
        }

        startGroup(context, arguments);
    }

    private void startGroup(ConsoleContext context, List<String> arguments) {
        String groupName = arguments.getFirst();
        int count = defaultCount(context, groupName);

        if (arguments.size() >= 2) {
            count = parsePositiveInt(arguments.get(1), "count");
        }

        animation.spin(context, "crystallizing " + count + " service slot(s) for " + groupName, Duration.ofMillis(700));
        List<ServiceStartResult> results = context.node().serviceScheduler().startGroup(groupName, count);
        animation.success(context, "Start request accepted for group " + groupName);

        for (ServiceStartResult result : results) {
            context.print("  " + context.accent(result.serviceId()) + context.muted(" -> ") + result.wrapperId() + context.muted(" / ") + result.requestId());
        }
    }

    private int defaultCount(ConsoleContext context, String groupName) {
        IGroup group = context.node().groupManager().groupByName(groupName);

        if (group != null) {
            return Math.max(1, group.serviceCount());
        }

        return 1;
    }

    private void startManual(ConsoleContext context, List<String> arguments) {
        if (arguments.size() < 7) {
            throw new IllegalArgumentException("Usage: " + usage());
        }

        String serviceId = arguments.get(1);
        String groupName = arguments.get(2);
        String templateName = arguments.get(3);
        CloudServiceType serviceType = parseServiceType(arguments.get(4));
        int memoryMb = parsePositiveInt(arguments.get(5), "memoryMb");
        int port = parsePort(arguments.get(6));
        boolean staticService = arguments.size() >= 8 && Boolean.parseBoolean(arguments.get(7));

        animation.spin(context, "preparing manual Minecraft service " + serviceId, Duration.ofMillis(650));
        ServiceStartResult result = context.node().serviceScheduler().start(new ServiceStartPlan(serviceId, groupName, templateName, "java", "127.0.0.1", serviceType, port, memoryMb, staticService, serviceType == CloudServiceType.PROXY, "NONE", ""));
        animation.success(context, "Manual start request sent for " + result.serviceId());
        context.print("  Request: " + result.requestId());
        context.print("  Wrapper: " + result.wrapperId());
    }

    private CloudServiceType parseServiceType(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("serviceType must not be blank");
        }

        String normalized = input.toUpperCase(Locale.ROOT);

        if ("SERVER".equals(normalized)) {
            return CloudServiceType.SERVER;
        }

        if ("PROXY".equals(normalized)) {
            return CloudServiceType.PROXY;
        }

        throw new IllegalArgumentException("serviceType must be server or proxy");
    }

    private int parsePositiveInt(String input, String name) {
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

    private int parsePort(String input) {
        int port = parsePositiveInt(input, "port");

        if (port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }

        return port;
    }
}
