package eu.kryocloud.launcher.argument;

import java.util.Arrays;
import java.util.List;

public record LauncherArguments(LauncherMode mode, boolean help) {

    public static LauncherArguments parse(String[] args) {
        List<String> values = args == null ? List.of() : Arrays.asList(args);

        if (values.isEmpty()) {
            return new LauncherArguments(LauncherMode.NODE, false);
        }

        if (values.stream().anyMatch(LauncherArguments::isHelpArgument)) {
            return new LauncherArguments(LauncherMode.NODE, true);
        }

        String mode = values.getFirst();

        if ("--mode".equalsIgnoreCase(mode) || "-mode".equalsIgnoreCase(mode)) {
            return parseModeFlag(values);
        }

        if (mode.startsWith("--mode=")) {
            return new LauncherArguments(LauncherMode.parse(mode.substring("--mode=".length())), false);
        }

        return new LauncherArguments(LauncherMode.parse(mode), false);
    }

    private static LauncherArguments parseModeFlag(List<String> values) {
        if (values.size() < 2) {
            throw new IllegalArgumentException("Missing value for --mode");
        }

        return new LauncherArguments(LauncherMode.parse(values.get(1)), false);
    }

    private static boolean isHelpArgument(String value) {
        if (value == null) {
            return false;
        }

        return switch (value.trim().toLowerCase()) {
            case "help", "--help", "-help", "-h", "?" -> true;
            default -> false;
        };
    }

}
