package eu.kryocloud.node.console.command;

import eu.kryocloud.node.console.ConsoleCategory;
import eu.kryocloud.node.console.ConsoleCommand;
import eu.kryocloud.node.console.ConsoleContext;
import eu.kryocloud.node.wrapper.WrapperSnapshot;

import java.util.Collection;
import java.util.List;

public final class WrappersCommand implements ConsoleCommand {

    @Override
    public String name() {
        return "wrappers";
    }

    @Override
    public ConsoleCategory category() {
        return ConsoleCategory.CLUSTER;
    }

    @Override
    public Collection<String> aliases() {
        return List.of("wrapper", "nodes");
    }

    @Override
    public String description() {
        return "Lists or inspects connected wrappers";
    }

    @Override
    public String usage() {
        return "wrappers [list|info <wrapperId>|timedout]";
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

        if ("timedout".equalsIgnoreCase(action)) {
            timedOut(context);
            return;
        }

        if ("info".equalsIgnoreCase(action)) {
            info(context, arguments);
            return;
        }

        throw new IllegalArgumentException("Usage: " + usage());
    }

    private void list(ConsoleContext context) {
        List<WrapperSnapshot> wrappers = context.node().wrapperRegistry().wrappers();

        if (wrappers.isEmpty()) {
            context.print("No wrappers connected.");
            return;
        }

        context.print("Connected wrappers:");

        for (WrapperSnapshot wrapper : wrappers) {
            context.print("  " + wrapper.wrapperId() + " | " + wrapper.state() + " | " + wrapper.availableMemoryMb() + "MB free | services=" + wrapper.runningServices() + " | " + wrapper.remoteAddress());
        }
    }

    private void timedOut(ConsoleContext context) {
        List<WrapperSnapshot> wrappers = context.node().wrapperRegistry().timedOutWrappers();

        if (wrappers.isEmpty()) {
            context.print("No timed out wrappers.");
            return;
        }

        context.print("Timed out wrappers:");

        for (WrapperSnapshot wrapper : wrappers) {
            context.print("  " + wrapper.wrapperId() + " | lastHeartbeat=" + wrapper.lastHeartbeatAtMillis());
        }
    }

    private void info(ConsoleContext context, List<String> arguments) {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException("Usage: wrappers info <wrapperId>");
        }

        String wrapperId = arguments.get(1);
        WrapperSnapshot wrapper = context.node().wrapperRegistry().wrapper(wrapperId).orElseThrow(() -> new IllegalArgumentException("Unknown wrapper: " + wrapperId));

        context.print("Wrapper " + wrapper.wrapperId());
        context.print("  State: " + wrapper.state());
        context.print("  Hostname: " + wrapper.hostname());
        context.print("  Address: " + wrapper.address());
        context.print("  OS: " + wrapper.osName());
        context.print("  CPU: " + wrapper.availableProcessors());
        context.print("  Memory: " + wrapper.usedMemoryMb() + "/" + wrapper.maxMemoryMb() + "MB");
        context.print("  Services: " + wrapper.runningServices());
        context.print("  Remote: " + wrapper.remoteAddress());
        context.print("  Registered: " + wrapper.registeredAtMillis());
        context.print("  Last heartbeat: " + wrapper.lastHeartbeatAtMillis());
    }
}