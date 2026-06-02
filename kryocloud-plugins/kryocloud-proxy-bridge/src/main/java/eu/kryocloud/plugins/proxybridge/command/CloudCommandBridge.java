package eu.kryocloud.plugins.proxybridge.command;

import eu.kryocloud.api.plugin.CloudAPI;
import eu.kryocloud.api.plugin.cloud.model.CloudCommandResult;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class CloudCommandBridge {

    public static final String PERMISSION = "kryocloud.admin";

    private static final Duration SUGGEST_TIMEOUT = Duration.ofMillis(650);

    public void execute(CloudCommandAudience audience, List<String> arguments) {
        if (audience == null) {
            return;
        }

        if (!audience.hasPermission(PERMISSION)) {
            audience.sendMessage(CloudMessageFormatter.message("Dafür brauchst du kryocloud.admin."));
            return;
        }

        if (!CloudAPI.connected()) {
            audience.sendMessage(CloudMessageFormatter.message("Die KryoCloud API ist gerade nicht verbunden."));
            return;
        }

        CloudAPI.console().execute(arguments == null ? List.of() : arguments).whenComplete((result, throwable) -> {
            if (throwable != null) {
                audience.sendMessage(CloudMessageFormatter.message("Cloud Command fehlgeschlagen: " + throwable.getMessage()));
                return;
            }

            send(audience, result);
        });
    }

    public List<String> suggest(CloudCommandAudience audience, List<String> arguments) {
        if (audience == null) {
            return List.of();
        }

        if (!audience.hasPermission(PERMISSION)) {
            return List.of();
        }

        if (!CloudAPI.connected()) {
            return List.of();
        }

        try {
            return CloudAPI.console().suggest(arguments == null ? List.of() : arguments).get(SUGGEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private void send(CloudCommandAudience audience, CloudCommandResult result) {
        List<String> lines = result.lines();

        if (lines.isEmpty()) {
            audience.sendMessage(CloudMessageFormatter.message(result.message()));
            return;
        }

        for (String line : lines) {
            audience.sendMessage(CloudMessageFormatter.message(line));
        }
    }

}
