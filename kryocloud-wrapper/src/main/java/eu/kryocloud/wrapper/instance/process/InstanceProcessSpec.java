package eu.kryocloud.wrapper.instance.process;

import java.nio.file.Path;
import java.util.List;

public record InstanceProcessSpec(String name, String javaExecutable, Path workingDirectory, int minMemoryMb, int maxMemoryMb, List<String> jvmArgs, String jarName) {

    public InstanceProcessSpec {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        if (javaExecutable == null || javaExecutable.isBlank()) {
            throw new IllegalArgumentException("javaExecutable must not be blank");
        }

        if (workingDirectory == null) {
            throw new IllegalArgumentException("workingDirectory must not be null");
        }

        if (minMemoryMb < 1) {
            throw new IllegalArgumentException("minMemoryMb must be greater than 0");
        }

        if (maxMemoryMb < minMemoryMb) {
            throw new IllegalArgumentException("maxMemoryMb must be greater than or equal to minMemoryMb");
        }

        if (jvmArgs == null) {
            throw new IllegalArgumentException("jvmArgs must not be null");
        }

        if (jarName == null || jarName.isBlank()) {
            throw new IllegalArgumentException("jarName must not be blank");
        }

        jvmArgs = List.copyOf(jvmArgs);
    }
}
