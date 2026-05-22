package eu.kryocloud.node.console.command;

import eu.kryocloud.node.console.ConsoleCategory;
import eu.kryocloud.node.console.ConsoleCommand;
import eu.kryocloud.node.console.ConsoleContext;
import eu.kryocloud.node.service.runtime.NodeServiceSnapshot;

import java.util.Collection;
import java.util.List;

public final class ServicesCommand implements ConsoleCommand {

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
        return List.of("service", "servers");
    }

    @Override
    public String description() {
        return "Lists or inspects cloud services";
    }

    @Override
    public String usage() {
        return "services [list|info <serviceId>|running]";
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

        throw new IllegalArgumentException("Usage: " + usage());
    }

    private void list(ConsoleContext context) {
        List<NodeServiceSnapshot> services = context.node().serviceRegistry().services();

        if (services.isEmpty()) {
            context.print("No services known.");
            return;
        }

        context.print("Cloud services:");

        for (NodeServiceSnapshot service : services) {
            context.print("  " + service.serviceId() + " | " + service.groupName() + " | " + service.serviceType() + " | " + service.state() + " | " + service.wrapperId() + " | " + service.host() + ":" + service.port());
        }
    }

    private void running(ConsoleContext context) {
        List<NodeServiceSnapshot> services = context.node().serviceRegistry().runningServices();

        if (services.isEmpty()) {
            context.print("No running services.");
            return;
        }

        context.print("Running services:");

        for (NodeServiceSnapshot service : services) {
            context.print("  " + service.serviceId() + " | " + service.groupName() + " | " + service.wrapperId() + " | " + service.host() + ":" + service.port());
        }
    }

    private void info(ConsoleContext context, List<String> arguments) {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException("Usage: services info <serviceId>");
        }

        String serviceId = arguments.get(1);
        NodeServiceSnapshot service = context.node().serviceRegistry().service(serviceId).orElseThrow(() -> new IllegalArgumentException("Unknown service: " + serviceId));

        context.print("Service " + service.serviceId());
        context.print("  Group: " + service.groupName());
        context.print("  Type: " + service.serviceType());
        context.print("  State: " + service.state());
        context.print("  Wrapper: " + service.wrapperId());
        context.print("  Address: " + service.host() + ":" + service.port());
        context.print("  Message: " + service.message());
        context.print("  Updated: " + service.timestamp());
    }
}