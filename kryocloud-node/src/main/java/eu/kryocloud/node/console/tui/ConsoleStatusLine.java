package eu.kryocloud.node.console.tui;

import eu.kryocloud.node.console.stats.ClusterStatsCollector;
import org.jline.terminal.Terminal;

public final class ConsoleStatusLine implements AutoCloseable {

    public ConsoleStatusLine(Terminal terminal, ClusterStatsCollector collector) {
        if (terminal == null) {
            throw new IllegalArgumentException("terminal must not be null");
        }

        if (collector == null) {
            throw new IllegalArgumentException("collector must not be null");
        }
    }

    public void start() {
    }

    @Override
    public void close() {
    }
}
