package eu.kryocloud.node.console.command;

import eu.kryocloud.node.console.ConsoleCategory;
import eu.kryocloud.node.console.ConsoleCommand;
import eu.kryocloud.node.console.ConsoleContext;
import eu.kryocloud.node.console.tui.Box;
import eu.kryocloud.node.console.tui.Column;
import eu.kryocloud.node.console.tui.ProgressBar;
import eu.kryocloud.node.console.tui.Table;
import eu.kryocloud.node.console.tui.Tone;
import eu.kryocloud.node.wrapper.WrapperSnapshot;

import java.util.Collection;
import java.util.List;

public final class WrappersCommand implements ConsoleCommand {

    @Override
    public String name() {
        return "wrapper";
    }

    @Override
    public ConsoleCategory category() {
        return ConsoleCategory.CLUSTER;
    }

    @Override
    public Collection<String> aliases() {
        return List.of("wrappers", "nodes");
    }

    @Override
    public String description() {
        return "Lists or inspects connected wrappers";
    }

    @Override
    public String usage() {
        return "wrapper list | wrapper <id> info | wrapper timedout";
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

        if (arguments.size() >= 2 && "info".equalsIgnoreCase(arguments.get(1))) {
            info(context, List.of("info", arguments.getFirst()));
            return;
        }

        if (arguments.size() == 1) {
            info(context, List.of("info", arguments.getFirst()));
            return;
        }

        throw new IllegalArgumentException("Usage: " + usage());
    }

    private void list(ConsoleContext context) {
        List<WrapperSnapshot> wrappers = context.node().wrapperRegistry().wrappers();

        if (wrappers.isEmpty()) {
            context.warn("No wrappers connected.");
            return;
        }

        context.header("Connected wrappers");

        Table table = Table.builder().column(Column.left("Wrapper").minWidth(14)).column(Column.left("State").minWidth(9)).column(Column.right("Services").minWidth(8)).column(Column.left("Process RAM").minWidth(26)).column(Column.left("CPU").minWidth(16)).column(Column.left("Remote").minWidth(20));

        for (WrapperSnapshot wrapper : wrappers) {
            table.row(Tone.PRIMARY.paint(wrapper.wrapperId()), Tone.INFO.paint(wrapper.state().toString()), Tone.CRYSTAL.paint(String.valueOf(wrapper.runningServices())), memoryCell(wrapper), ProgressBar.renderWithPercent(wrapper.cpuLoadRatio(), 8), Tone.MUTED.paint(wrapper.remoteAddress()));
        }

        table.render(context::print);
        context.print("");
    }

    private String memoryCell(WrapperSnapshot wrapper) {
        double ratio = ratio(wrapper.usedMemoryMb(), wrapper.maxMemoryMb());
        return ProgressBar.render(ratio, 12) + " " + Tone.CRYSTAL.paint(wrapper.usedMemoryMb() + "/" + wrapper.maxMemoryMb() + "MB");
    }

    private double ratio(int used, int max) {
        if (max < 1) {
            return 0.0D;
        }

        return Math.max(0.0D, Math.min(1.0D, (double) used / (double) max));
    }

    private void timedOut(ConsoleContext context) {
        List<WrapperSnapshot> wrappers = context.node().wrapperRegistry().timedOutWrappers();

        if (wrappers.isEmpty()) {
            context.warn("No timed out wrappers.");
            return;
        }

        context.header("Timed out wrappers");

        Table table = Table.builder().column(Column.left("Wrapper").minWidth(14)).column(Column.right("Last heartbeat").minWidth(16));

        for (WrapperSnapshot wrapper : wrappers) {
            table.row(Tone.WARNING.paint(wrapper.wrapperId()), Tone.MUTED.paint(String.valueOf(wrapper.lastHeartbeatAtMillis())));
        }

        table.render(context::print);
        context.print("");
    }

    private void info(ConsoleContext context, List<String> arguments) {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException("Usage: wrappers info <wrapperId>");
        }

        String wrapperId = arguments.get(1);
        WrapperSnapshot wrapper = context.node().wrapperRegistry().wrapper(wrapperId).orElseThrow(() -> new IllegalArgumentException("Unknown wrapper: " + wrapperId));
        double memoryRatio = ratio(wrapper.usedMemoryMb(), wrapper.maxMemoryMb());

        context.print("");
        Box.titled("Wrapper " + wrapper.wrapperId()).minWidth(58).blank().line(label("State          ") + " " + Tone.INFO.paint(wrapper.state().toString())).line(label("Hostname       ") + " " + Tone.CRYSTAL.paint(wrapper.hostname())).line(label("Address        ") + " " + Tone.CRYSTAL.paint(wrapper.address())).line(label("OS             ") + " " + Tone.FROST.paint(wrapper.osName())).line(label("Cores          ") + " " + Tone.CRYSTAL.paint(String.valueOf(wrapper.availableProcessors()))).line(label("Process RAM    ") + " " + Tone.CRYSTAL.paint(wrapper.usedMemoryMb() + "/" + wrapper.maxMemoryMb() + "MB") + "  " + ProgressBar.renderWithPercent(memoryRatio, 18)).line(label("CPU load       ") + " " + ProgressBar.renderWithPercent(wrapper.cpuLoadRatio(), 18)).line(label("Services       ") + " " + Tone.CRYSTAL.paint(String.valueOf(wrapper.runningServices()))).line(label("Remote         ") + " " + Tone.MUTED.paint(wrapper.remoteAddress())).line(label("Registered     ") + " " + Tone.MUTED.paint(String.valueOf(wrapper.registeredAtMillis()))).line(label("Last heartbeat ") + " " + Tone.MUTED.paint(String.valueOf(wrapper.lastHeartbeatAtMillis()))).blank().render(context::print);
        context.print("");
    }

    private String label(String value) {
        return Tone.SECONDARY.paint(value);
    }
}
