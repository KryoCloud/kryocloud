package eu.kryocloud.node.console;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class KryoCompleter implements Completer {

    private final Completer commandCompleter;
    private final AtomicReference<Completer> promptCompleter = new AtomicReference<>();

    public KryoCompleter(Completer commandCompleter) {
        if (commandCompleter == null) {
            throw new IllegalArgumentException("commandCompleter must not be null");
        }

        this.commandCompleter = commandCompleter;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        if (candidates == null) {
            return;
        }

        Completer activePromptCompleter = promptCompleter.get();

        if (activePromptCompleter != null) {
            activePromptCompleter.complete(reader, line, candidates);
            return;
        }

        commandCompleter.complete(reader, line, candidates);
    }

    public <T> T withPromptCandidates(Collection<String> values, Supplier<T> action) {
        if (values == null) {
            throw new IllegalArgumentException("values must not be null");
        }

        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }

        Completer previousCompleter = promptCompleter.getAndSet(new StaticPromptCompleter(values));

        try {
            return action.get();
        } finally {
            promptCompleter.set(previousCompleter);
        }
    }

    private record StaticPromptCompleter(Collection<String> values) implements Completer {

        private StaticPromptCompleter {
            if (values == null) {
                throw new IllegalArgumentException("values must not be null");
            }
        }

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            if (candidates == null) {
                return;
            }

            String prefix = line == null ? "" : line.word();

            for (String value : values) {
                if (value == null || value.isBlank()) {
                    continue;
                }

                if (prefix != null && !prefix.isBlank() && !value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
                    continue;
                }

                candidates.add(new Candidate(value));
            }
        }
    }
}
