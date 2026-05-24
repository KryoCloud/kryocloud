package eu.kryocloud.wrapper.instance.workspace;

public record InstanceCleanupResult(int scanned, int deleted, int skipped, int failed, String details) {

    public InstanceCleanupResult {
        if (scanned < 0) {
            throw new IllegalArgumentException("scanned must not be negative");
        }

        if (deleted < 0) {
            throw new IllegalArgumentException("deleted must not be negative");
        }

        if (skipped < 0) {
            throw new IllegalArgumentException("skipped must not be negative");
        }

        if (failed < 0) {
            throw new IllegalArgumentException("failed must not be negative");
        }

        if (details == null) {
            throw new IllegalArgumentException("details must not be null");
        }
    }
}
