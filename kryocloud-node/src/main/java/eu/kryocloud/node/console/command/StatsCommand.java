package eu.kryocloud.node.console.command;

import eu.kryocloud.node.console.ConsoleCategory;
import eu.kryocloud.node.console.ConsoleCommand;
import eu.kryocloud.node.console.ConsoleContext;
import eu.kryocloud.node.console.stats.ClusterStatsCollector;
import eu.kryocloud.node.console.stats.ClusterStatsSnapshot;
import eu.kryocloud.node.console.stats.GroupStatsSnapshot;
import eu.kryocloud.node.console.tui.Box;
import eu.kryocloud.node.console.tui.Column;
import eu.kryocloud.node.console.tui.ConsoleAnimation;
import eu.kryocloud.node.console.tui.ConsoleTheme;
import eu.kryocloud.node.console.tui.Glyph;
import eu.kryocloud.node.console.tui.ProgressBar;
import eu.kryocloud.node.console.tui.Table;
import eu.kryocloud.node.console.tui.Tone;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

public final class StatsCommand implements ConsoleCommand {

    private final ConsoleAnimation animation = new ConsoleAnimation();

    @Override
    public String name() {
        return "stats";
    }

    @Override
    public ConsoleCategory category() {
        return ConsoleCategory.CLUSTER;
    }

    @Override
    public Collection<String> aliases() {
        return List.of("usage", "metrics");
    }

    @Override
    public String description() {
        return "Shows animated Minecraft cloud usage stats";
    }

    @Override
    public String usage() {
        return "stats [groups|group <name>|live [seconds] [intervalMs]]";
    }

    @Override
    public void execute(ConsoleContext context, List<String> arguments) {
        if (arguments.isEmpty()) {
            snapshot(context);
            return;
        }

        String action = arguments.getFirst();

        if ("groups".equalsIgnoreCase(action)) {
            groups(context);
            return;
        }

        if ("group".equalsIgnoreCase(action)) {
            group(context, arguments);
            return;
        }

        if ("live".equalsIgnoreCase(action)) {
            live(context, arguments);
            return;
        }

        throw new IllegalArgumentException("Usage: " + usage());
    }

    private void snapshot(ConsoleContext context) {
        ClusterStatsSnapshot snapshot = collector(context).snapshot();

        context.print("");
        Box.titled("Minecraft cloud stats").minWidth(58).blank().line(label("Wrappers") + " " + Tone.INFO.paint(snapshot.wrappersOnline() + " online") + Tone.MUTED.paint("  " + Glyph.SEPARATOR + "  ") + Tone.WARNING.paint(snapshot.wrappersTimedOut() + " timed out")).line(label("Services") + " " + Tone.SUCCESS.paint(snapshot.runningServices() + " running") + Tone.MUTED.paint("  " + Glyph.SEPARATOR + "  ") + Tone.INFO.paint(snapshot.startingServices() + " starting") + Tone.MUTED.paint("  " + Glyph.SEPARATOR + "  ") + Tone.DANGER.paint(snapshot.failedServices() + " failed")).line(label("Process RAM") + " " + memoryRow(snapshot)).line(label("CPU        ") + " " + ProgressBar.renderWithPercent(snapshot.cpuLoadRatio(), 18)).line(label("Groups ") + " " + Tone.CRYSTAL.paint(String.valueOf(snapshot.groupCount()))).blank().render(context::print);
        context.print("");
    }

    private String memoryRow(ClusterStatsSnapshot snapshot) {
        String numbers = memoryValue(snapshot.processMemoryMb(), snapshot.runningServices()) + Tone.MUTED.paint(" / ") + Tone.CRYSTAL.paint(snapshot.wrapperMemoryMaxMb() + "MB");
        return numbers + "  " + ProgressBar.renderWithPercent(snapshot.wrapperMemoryRatio(), 18);
    }

    private String label(String value) {
        return Tone.SECONDARY.paint(value);
    }

    private String memoryValue(int memoryMb, int runningServices) {
        if (runningServices > 0 && memoryMb < 16) {
            return Tone.MUTED.paint("collecting");
        }

        return Tone.CRYSTAL.paint(memoryMb + "MB");
    }

    private void groups(ConsoleContext context) {
        ClusterStatsSnapshot snapshot = collector(context).snapshot();

        if (snapshot.groups().isEmpty()) {
            context.warn("No Minecraft groups configured.");
            return;
        }

        context.header("Minecraft group usage");

        Table table = Table.builder().column(Column.left("Group").minWidth(12)).column(Column.left("Type").minWidth(7)).column(Column.left("Software").minWidth(14)).column(Column.right("Services").minWidth(8)).column(Column.right("RAM").minWidth(8)).column(Column.left("CPU").minWidth(18));

        for (GroupStatsSnapshot group : snapshot.groups()) {
            table.row(Tone.PRIMARY.paint(group.groupName()), Tone.INFO.paint(group.serviceType().name()), Tone.CRYSTAL.paint(group.software() + " " + group.softwareVersion()), Tone.CRYSTAL.paint(group.runningServices() + "/" + group.maxCount()), memoryValue(group.processMemoryMb(), group.runningServices()), ProgressBar.renderWithPercent(group.cpuLoadRatio(), 10));
        }

        table.render(context::print);
        context.print("");
    }

    private void group(ConsoleContext context, List<String> arguments) {
        if (arguments.size() < 2) {
            throw new IllegalArgumentException("Usage: stats group <name>");
        }

        GroupStatsSnapshot group = collector(context).groupSnapshot(arguments.get(1));

        context.print("");
        Box.titled("Group " + group.groupName()).minWidth(58).blank().line(label("Type     ") + " " + Tone.INFO.paint(group.serviceType().name())).line(label("Software ") + " " + Tone.CRYSTAL.paint(group.software() + " " + group.softwareVersion())).line(label("Services ") + " " + Tone.CRYSTAL.paint(group.runningServices() + "/" + group.maxCount()) + "  " + ProgressBar.renderWithPercent(group.serviceUsageRatio(), 18)).line(label("Process  ") + " " + memoryValue(group.processMemoryMb(), group.runningServices()) + Tone.MUTED.paint(" / ") + Tone.CRYSTAL.paint(group.maxMemoryCapacityMb() + "MB") + "  " + ProgressBar.renderWithPercent(group.memoryUsageRatio(), 18)).line(label("CPU      ") + " " + ProgressBar.renderWithPercent(group.cpuLoadRatio(), 18)).line(label("Reserved ") + " " + Tone.MUTED.paint(group.reservedMemoryMb() + "MB configured")).line(label("Static   ") + " " + Tone.CRYSTAL.paint(String.valueOf(group.staticServices()))).line(label("Known    ") + " " + Tone.CRYSTAL.paint(group.knownServices() + " known") + Tone.MUTED.paint("  " + Glyph.SEPARATOR + "  ") + Tone.DANGER.paint(group.failedServices() + " failed")).blank().render(context::print);
        context.print("");
    }

    private void live(ConsoleContext context, List<String> arguments) {
        int seconds = arguments.size() >= 2 ? positiveInt(arguments.get(1), "seconds") : 12;
        int intervalMillis = arguments.size() >= 3 ? positiveInt(arguments.get(2), "intervalMs") : 500;
        ClusterStatsCollector collector = collector(context);

        animation.live(context, () -> liveLine(collector.snapshot()), Duration.ofSeconds(seconds), Duration.ofMillis(intervalMillis));
        animation.success(context, "Stats capture finished.");
        snapshot(context);
    }

    private String liveLine(ClusterStatsSnapshot snapshot) {
        return Tone.MUTED.paint("wrappers ") + Tone.INFO.paint(String.valueOf(snapshot.wrappersOnline())) + Tone.MUTED.paint("  " + Glyph.SEPARATOR + "  ") + Tone.MUTED.paint("services ") + Tone.SUCCESS.paint(String.valueOf(snapshot.runningServices())) + Tone.MUTED.paint("/") + Tone.CRYSTAL.paint(String.valueOf(snapshot.knownServices())) + Tone.MUTED.paint("  " + Glyph.SEPARATOR + "  ") + Tone.MUTED.paint("ram ") + ProgressBar.renderWithPercent(snapshot.wrapperMemoryRatio(), 14);
    }

    private ClusterStatsCollector collector(ConsoleContext context) {
        return new ClusterStatsCollector(context.node());
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
