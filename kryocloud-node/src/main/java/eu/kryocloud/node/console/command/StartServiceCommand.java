package eu.kryocloud.node.console.command;

import eu.kryocloud.network.protocol.CloudServiceType;
import eu.kryocloud.node.console.ConsoleCategory;
import eu.kryocloud.node.console.ConsoleCommand;
import eu.kryocloud.node.console.ConsoleContext;
import eu.kryocloud.node.service.schedule.ServiceStartPlan;
import eu.kryocloud.node.service.schedule.ServiceStartResult;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class StartServiceCommand implements ConsoleCommand {

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
        return "Starts services from a cloud group or manually";
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
        int count = 1;

        if (arguments.size() >= 2) {
            count = parsePositiveInt(arguments.get(1), "count");
        }

        List<ServiceStartResult> results = context.node().serviceScheduler().startGroup(groupName, count);
        context.print("Started " + results.size() + " service request(s) from group " + groupName + ".");

        for (ServiceStartResult result : results) {
            context.print("  " + result.serviceId() + " -> " + result.wrapperId() + " (" + result.requestId() + ")");
        }
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
        ServiceStartResult result = context.node().serviceScheduler().start(new ServiceStartPlan(serviceId, groupName, templateName, serviceType, port, memoryMb, staticService));

        context.print("Manual start request sent.");
        context.print("  Request: " + result.requestId());
        context.print("  Service: " + result.serviceId());
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