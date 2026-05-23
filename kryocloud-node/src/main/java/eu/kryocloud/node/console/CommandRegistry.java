package eu.kryocloud.node.console;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CommandRegistry {

    private final ConcurrentMap<String, ConsoleCommand> commands = new ConcurrentHashMap<>();

    public void register(ConsoleCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }

        registerName(command.name(), command);

        for (String alias : command.aliases()) {
            registerName(alias, command);
        }
    }

    public Optional<ConsoleCommand> command(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(commands.get(normalize(input)));
    }

    public List<ConsoleCommand> commands() {
        Set<ConsoleCommand> uniqueCommands = new LinkedHashSet<>(commands.values());
        return uniqueCommands.stream().sorted(Comparator.comparing(ConsoleCommand::name)).toList();
    }

    public List<String> commandNames() {
        return new ArrayList<>(commands.keySet());
    }

    private void registerName(String name, ConsoleCommand command) {
        if (name == null) {
            throw new IllegalArgumentException("command name must not be null");
        }

        if (name.isBlank()) {
            throw new IllegalArgumentException("command name must not be blank");
        }

        String normalizedName = normalize(name);
        ConsoleCommand previous = commands.putIfAbsent(normalizedName, command);

        if (previous != null) {
            throw new IllegalStateException("Console command is already registered: " + normalizedName);
        }
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}