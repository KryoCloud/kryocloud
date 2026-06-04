package eu.kryocloud.sphere;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class KryoSphereExecutableLookup {

    private final ConcurrentMap<String, Optional<Path>> cache = new ConcurrentHashMap<>();

    public Optional<Path> find(String executable) {
        if (executable == null || executable.isBlank()) {
            return Optional.empty();
        }

        return cache.computeIfAbsent(executable, this::findUncached);
    }

    private Optional<Path> findUncached(String executable) {
        Path direct = Path.of(executable);

        if (direct.isAbsolute() && Files.isExecutable(direct)) {
            return Optional.of(direct.toAbsolutePath().normalize());
        }

        String path = System.getenv("PATH");

        if (path == null || path.isBlank()) {
            return Optional.empty();
        }

        return Arrays.stream(path.split(java.io.File.pathSeparator))
                .filter(value -> value != null && !value.isBlank())
                .map(value -> Path.of(value).resolve(executable))
                .filter(Files::isExecutable)
                .map(value -> value.toAbsolutePath().normalize())
                .findFirst();
    }

}
