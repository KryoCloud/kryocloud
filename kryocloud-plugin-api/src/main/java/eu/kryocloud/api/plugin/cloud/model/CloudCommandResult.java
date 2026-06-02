package eu.kryocloud.api.plugin.cloud.model;

import java.util.List;

public record CloudCommandResult(boolean success, String message, List<String> lines) {

    public CloudCommandResult {
        message = message == null ? "" : message;
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    public static CloudCommandResult success(String message, List<String> lines) {
        return new CloudCommandResult(true, message, lines);
    }

    public static CloudCommandResult failure(String message) {
        return new CloudCommandResult(false, message, List.of(message));
    }

}
