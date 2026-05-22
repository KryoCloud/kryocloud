package eu.kryocloud.node.console;

import java.util.Collection;
import java.util.List;

public interface ConsoleCommand {

    String name();

    ConsoleCategory category();

    String description();

    String usage();

    void execute(ConsoleContext context, List<String> arguments) throws Exception;

    default Collection<String> aliases() {
        return List.of();
    }
}