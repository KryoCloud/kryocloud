package eu.kryocloud.wrapper.instance.runtime;

import java.util.List;

public record JavaRuntime(String executable, int majorVersion, List<String> acceptedFlags, List<String> rejectedFlags) {

    public JavaRuntime {
        if (executable == null || executable.isBlank()) {
            throw new IllegalArgumentException("executable must not be blank");
        }

        if (majorVersion < 1) {
            throw new IllegalArgumentException("majorVersion must be greater than 0");
        }

        if (acceptedFlags == null) {
            throw new IllegalArgumentException("acceptedFlags must not be null");
        }

        if (rejectedFlags == null) {
            throw new IllegalArgumentException("rejectedFlags must not be null");
        }

        acceptedFlags = List.copyOf(acceptedFlags);
        rejectedFlags = List.copyOf(rejectedFlags);
    }
}
