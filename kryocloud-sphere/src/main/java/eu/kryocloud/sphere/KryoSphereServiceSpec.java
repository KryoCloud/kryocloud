package eu.kryocloud.sphere;

import java.nio.file.Path;
import java.util.List;

public record KryoSphereServiceSpec(
        String serviceId,
        String javaExecutable,
        Path workingDirectory,
        int minMemoryMb,
        int maxMemoryMb,
        List<String> jvmArgs,
        String jarName,
        Path logFile,
        Path pidFile
) {

    public KryoSphereServiceSpec {
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
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

        if (logFile == null) {
            throw new IllegalArgumentException("logFile must not be null");
        }

        if (pidFile == null) {
            throw new IllegalArgumentException("pidFile must not be null");
        }

        jvmArgs = List.copyOf(jvmArgs);
    }

}
