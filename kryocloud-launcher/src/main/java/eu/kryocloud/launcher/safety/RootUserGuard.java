package eu.kryocloud.launcher.safety;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class RootUserGuard {

    private RootUserGuard() {
    }

    public static boolean isRootUser() {
        if (isRootName(System.getProperty("user.name", ""))) {
            return true;
        }

        if (ProcessHandle.current().info().user().map(RootUserGuard::isRootName).orElse(false)) {
            return true;
        }

        if ("0".equals(System.getenv("EUID")) || "0".equals(System.getenv("UID"))) {
            return true;
        }

        return unixUserId().map("0"::equals).orElse(false);
    }

    public static void abortIfRootUser() {
        if (!isRootUser()) {
            return;
        }

        System.err.println("⚠ KryoCloud sollte nicht als root gestartet werden.");
        System.err.println("⚠ Starte KryoCloud als normaler User, damit Minecraft-Instanzen und Cloud-Dateien keine root-Rechte bekommen.");
        System.exit(1);
    }

    private static boolean isRootName(String userName) {
        return userName != null && "root".equals(userName.trim());
    }

    private static Optional<String> unixUserId() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        if (osName.contains("win")) {
            return Optional.empty();
        }

        Process process = null;

        try {
            process = new ProcessBuilder("id", "-u").redirectErrorStream(true).start();

            if (!process.waitFor(Duration.ofSeconds(1).toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return Optional.empty();
            }

            if (process.exitValue() != 0) {
                return Optional.empty();
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                return line == null || line.isBlank() ? Optional.empty() : Optional.of(line.trim());
            }
        } catch (Exception ignored) {
            if (process != null) {
                process.destroyForcibly();
            }

            return Optional.empty();
        }
    }
}
