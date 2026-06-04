package eu.kryocloud.sphere;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

public final class KryoSphereLauncher {

    private static final String SANDBOX_WORKSPACE = "/srv/kryocloud/service";
    private static final KryoSphereLauncher GLOBAL = new KryoSphereLauncher(KryoSpherePlatform.current(), new KryoSphereExecutableLookup());

    private final KryoSpherePlatform platform;
    private final KryoSphereExecutableLookup executableLookup;

    public KryoSphereLauncher(KryoSpherePlatform platform) {
        this(platform, new KryoSphereExecutableLookup());
    }

    private KryoSphereLauncher(KryoSpherePlatform platform, KryoSphereExecutableLookup executableLookup) {
        if (platform == null) {
            throw new IllegalArgumentException("platform must not be null");
        }

        if (executableLookup == null) {
            throw new IllegalArgumentException("executableLookup must not be null");
        }

        this.platform = platform;
        this.executableLookup = executableLookup;
    }

    public static KryoSphereLauncher global() {
        return GLOBAL;
    }

    public KryoSphereLaunchPlan plan(KryoSphereServiceSpec spec, KryoSphereSettings settings) {
        if (spec == null) {
            throw new IllegalArgumentException("spec must not be null");
        }

        KryoSphereSettings safeSettings = settings == null ? KryoSphereSettings.basic() : settings;

        if (platform != KryoSpherePlatform.LINUX) {
            return platformPlainPlan(spec, safeSettings);
        }

        if (safeSettings.mode() == KryoSphereMode.NONE) {
            return unixBasicPlan(spec, safeSettings, KryoSphereMode.NONE, List.of());
        }

        if (safeSettings.mode() == KryoSphereMode.STRICT) {
            return unixStrictPlan(spec, safeSettings);
        }

        return unixBasicPlan(spec, safeSettings, KryoSphereMode.BASIC, List.of());
    }

    private KryoSphereLaunchPlan platformPlainPlan(KryoSphereServiceSpec spec, KryoSphereSettings settings) {
        List<String> warnings = List.of("KryoSphere isolation is only supported on Linux. Isolation disabled on " + platform + ".");

        if (platform == KryoSpherePlatform.WINDOWS) {
            return windowsPlainPlan(spec, settings, warnings);
        }

        return unixBasicPlan(spec, KryoSphereSettings.disabled(), KryoSphereMode.NONE, warnings);
    }

    private KryoSphereLaunchPlan windowsPlainPlan(KryoSphereServiceSpec spec, KryoSphereSettings settings, List<String> warnings) {
        List<String> commands = new ArrayList<>();

        commands.add("if not exist " + windowsQuote(spec.workingDirectory().resolve("logs")) + " mkdir " + windowsQuote(spec.workingDirectory().resolve("logs")));
        commands.add("if not exist " + windowsQuote(spec.workingDirectory().resolve(".kryocloud")) + " mkdir " + windowsQuote(spec.workingDirectory().resolve(".kryocloud")));

        for (String warning : warnings) {
            if (warning == null || warning.isBlank()) {
                continue;
            }

            commands.add("echo " + windowsSafeEcho("[KryoSphere] " + warning) + " >> " + windowsQuote(spec.logFile()));
        }

        commands.add("cd /d " + windowsQuote(spec.workingDirectory()));
        commands.add(windowsJavaCommand(spec) + " >> " + windowsQuote(spec.logFile()) + " 2>&1");

        return new KryoSphereLaunchPlan(settings.mode(), KryoSphereMode.NONE, platform, "plain", String.join(" & ", commands), warnings);
    }

    private KryoSphereLaunchPlan unixStrictPlan(KryoSphereServiceSpec spec, KryoSphereSettings settings) {
        Optional<Path> bwrap = executableLookup.find("bwrap");

        if (bwrap.isEmpty()) {
            throw new IllegalStateException("KryoSphere STRICT requires bubblewrap. Install bwrap or use kryoSphereMode BASIC.");
        }

        return unixBubblewrapPlan(spec, settings, bwrap.get());
    }

    private KryoSphereLaunchPlan unixBasicPlan(KryoSphereServiceSpec spec, KryoSphereSettings settings, KryoSphereMode effectiveMode, List<String> incomingWarnings) {
        List<String> commands = new ArrayList<>();
        List<String> warnings = new ArrayList<>(incomingWarnings == null ? List.of() : incomingWarnings);

        if (!settings.allowNetwork()) {
            warnings.add("Network isolation requires KryoSphere STRICT with bubblewrap. BASIC keeps host networking.");
        }

        if (settings.cpuLimitPercent() > 0) {
            warnings.add("CPUQuota needs a cgroup backend and is not enforced by basic shell mode yet.");
        }

        if (settings.clearEnvironment()) {
            warnings.add("Environment clearing is only enforced by KryoSphere STRICT/bubblewrap.");
        }

        commands.add("umask 077");
        commands.add("mkdir -p " + shellQuote(spec.workingDirectory().resolve("logs")) + " " + shellQuote(spec.workingDirectory().resolve(".kryocloud")));
        commands.add("chmod 700 " + shellQuote(spec.workingDirectory()) + " " + shellQuote(spec.workingDirectory().resolve("logs")) + " " + shellQuote(spec.workingDirectory().resolve(".kryocloud")) + " || true");
        commands.add("cd " + shellQuote(spec.workingDirectory()));
        commands.addAll(resourceLimitCommands(settings));
        commands.addAll(warningCommands(warnings, spec.logFile()));
        commands.add("echo $$ > " + shellQuote(spec.pidFile()));
        commands.add("exec " + noNewPrivilegesPrefix(settings, false) + unixJavaCommand(spec, null, settings) + " >> " + shellQuote(spec.logFile()) + " 2>&1");

        return new KryoSphereLaunchPlan(settings.mode(), effectiveMode, platform, "basic", String.join("; ", commands), warnings);
    }

    private KryoSphereLaunchPlan unixBubblewrapPlan(KryoSphereServiceSpec spec, KryoSphereSettings settings, Path bwrap) {
        List<String> warnings = new ArrayList<>();
        List<String> arguments = new ArrayList<>();

        if (settings.cpuLimitPercent() > 0) {
            warnings.add("CPUQuota needs a cgroup backend and is not enforced by bubblewrap/basic shell mode yet.");
        }

        if (!settings.serviceUser().isBlank()) {
            warnings.add("serviceUser is configured but Linux user switching is intentionally not applied inside KryoSphere yet: " + settings.serviceUser());
        }

        arguments.add(shellQuote(bwrap));
        arguments.add("--die-with-parent");
        arguments.add("--new-session");
        arguments.add("--unshare-user");
        arguments.add("--unshare-pid");
        arguments.add("--unshare-uts");
        arguments.add("--unshare-ipc");
        arguments.add("--hostname " + shellQuote(safeHostname(spec.serviceId())));

        if (!settings.allowNetwork()) {
            arguments.add("--unshare-net");
        }

        if (settings.clearEnvironment()) {
            arguments.add("--clearenv");
            arguments.add("--setenv HOME " + shellQuote(SANDBOX_WORKSPACE));
            arguments.add("--setenv USER kryocloud");
            arguments.add("--setenv LOGNAME kryocloud");
            arguments.add("--setenv TMPDIR /tmp");
            arguments.add("--setenv LANG C.UTF-8");
            arguments.add("--setenv PATH /usr/local/bin:/usr/bin:/bin");
        }

        if (settings.restrictProc()) {
            arguments.add("--proc /proc");
        }

        arguments.add("--dev /dev");

        if (settings.privateTmp()) {
            arguments.add("--tmpfs /tmp");
        } else {
            addWritableBind(arguments, Path.of("/tmp"), Path.of("/tmp"));
        }

        arguments.add("--dir /srv");
        arguments.add("--dir /srv/kryocloud");
        arguments.add("--dir /runtime");

        addBaseReadOnlyBinds(arguments);
        addJavaRuntimeBind(arguments, spec);
        addConfiguredReadOnlyBinds(arguments, settings.readOnlyPaths());
        addConfiguredWritableBinds(arguments, settings.writablePaths());
        addWritableBind(arguments, spec.workingDirectory(), Path.of(SANDBOX_WORKSPACE));

        arguments.add("--chdir " + shellQuote(SANDBOX_WORKSPACE));
        arguments.add(unixJavaCommand(spec, SANDBOX_WORKSPACE, settings));

        List<String> commands = new ArrayList<>();
        commands.add("umask 077");
        commands.add("mkdir -p " + shellQuote(spec.workingDirectory().resolve("logs")) + " " + shellQuote(spec.workingDirectory().resolve(".kryocloud")));
        commands.add("chmod 700 " + shellQuote(spec.workingDirectory()) + " " + shellQuote(spec.workingDirectory().resolve("logs")) + " " + shellQuote(spec.workingDirectory().resolve(".kryocloud")) + " || true");
        commands.add("cd " + shellQuote(spec.workingDirectory()));
        commands.addAll(resourceLimitCommands(settings));
        commands.addAll(warningCommands(warnings, spec.logFile()));
        commands.add("echo $$ > " + shellQuote(spec.pidFile()));
        commands.add("exec " + String.join(" ", arguments) + " >> " + shellQuote(spec.logFile()) + " 2>&1");

        return new KryoSphereLaunchPlan(settings.mode(), KryoSphereMode.STRICT, platform, "bubblewrap", String.join("; ", commands), warnings);
    }

    private List<String> resourceLimitCommands(KryoSphereSettings settings) {
        List<String> commands = new ArrayList<>();

        if (settings.openFileLimit() > 0) {
            commands.add("ulimit -n " + settings.openFileLimit() + " || true");
        }

        if (settings.processLimit() > 0) {
            commands.add("ulimit -u " + settings.processLimit() + " || true");
        }

        if (settings.memoryLimitMb() > 0) {
            commands.add("ulimit -v " + (settings.memoryLimitMb() * 1024L) + " || true");
        }

        return commands;
    }

    private List<String> warningCommands(List<String> warnings, Path logFile) {
        if (warnings == null || warnings.isEmpty()) {
            return List.of();
        }

        List<String> commands = new ArrayList<>();

        for (String warning : warnings) {
            if (warning == null || warning.isBlank()) {
                continue;
            }

            commands.add("echo " + shellQuote("[KryoSphere] " + warning) + " >> " + shellQuote(logFile));
        }

        return commands;
    }

    private String windowsJavaCommand(KryoSphereServiceSpec spec) {
        StringJoiner joiner = new StringJoiner(" ");
        joiner.add(windowsQuote(spec.javaExecutable()));
        joiner.add("-Xms" + spec.minMemoryMb() + "M");
        joiner.add("-Xmx" + spec.maxMemoryMb() + "M");

        for (String argument : spec.jvmArgs()) {
            if (argument == null || argument.isBlank()) {
                continue;
            }

            joiner.add(windowsQuote(argument));
        }

        joiner.add("-jar");
        joiner.add(windowsQuote(spec.jarName()));
        joiner.add("nogui");
        return joiner.toString();
    }

    private String unixJavaCommand(KryoSphereServiceSpec spec, String sandboxWorkspace, KryoSphereSettings settings) {
        StringJoiner joiner = new StringJoiner(" ");
        joiner.add(javaExecutable(spec, sandboxWorkspace));
        joiner.add("-Xms" + spec.minMemoryMb() + "M");
        joiner.add("-Xmx" + spec.maxMemoryMb() + "M");

        for (String argument : spec.jvmArgs()) {
            if (argument == null || argument.isBlank()) {
                continue;
            }

            joiner.add(shellQuote(argument));
        }

        joiner.add("-jar");
        joiner.add(shellQuote(spec.jarName()));
        joiner.add("nogui");
        return joiner.toString();
    }

    private String javaExecutable(KryoSphereServiceSpec spec, String sandboxWorkspace) {
        if (sandboxWorkspace == null || sandboxWorkspace.isBlank()) {
            return shellQuote(spec.javaExecutable());
        }

        Path javaHome = javaHome(spec.javaExecutable());

        if (javaHome == null || systemPath(javaHome)) {
            return shellQuote(spec.javaExecutable());
        }

        Path executable = Path.of(spec.javaExecutable()).getFileName();

        if (executable == null) {
            return shellQuote(spec.javaExecutable());
        }

        return shellQuote("/runtime/java/bin/" + executable);
    }

    private String noNewPrivilegesPrefix(KryoSphereSettings settings, boolean beforeBubblewrap) {
        if (!settings.noNewPrivileges()) {
            return "";
        }

        if (beforeBubblewrap) {
            return "";
        }

        Optional<Path> setpriv = executableLookup.find("setpriv");

        if (setpriv.isEmpty()) {
            return "";
        }

        return shellQuote(setpriv.get()) + " --no-new-privs -- ";
    }

    private void addBaseReadOnlyBinds(List<String> arguments) {
        addReadOnlyBind(arguments, Path.of("/usr"), Path.of("/usr"));
        addReadOnlyBind(arguments, Path.of("/bin"), Path.of("/bin"));
        addReadOnlyBind(arguments, Path.of("/sbin"), Path.of("/sbin"));
        addReadOnlyBind(arguments, Path.of("/lib"), Path.of("/lib"));
        addReadOnlyBind(arguments, Path.of("/lib64"), Path.of("/lib64"));
        addReadOnlyBind(arguments, Path.of("/etc/ssl"), Path.of("/etc/ssl"));
        addReadOnlyBind(arguments, Path.of("/etc/pki"), Path.of("/etc/pki"));
        addReadOnlyBind(arguments, Path.of("/etc/ca-certificates"), Path.of("/etc/ca-certificates"));
        addReadOnlyBind(arguments, Path.of("/etc/resolv.conf"), Path.of("/etc/resolv.conf"));
        addReadOnlyBind(arguments, Path.of("/etc/hosts"), Path.of("/etc/hosts"));
        addReadOnlyBind(arguments, Path.of("/etc/nsswitch.conf"), Path.of("/etc/nsswitch.conf"));
        addReadOnlyBind(arguments, Path.of("/etc/passwd"), Path.of("/etc/passwd"));
        addReadOnlyBind(arguments, Path.of("/etc/group"), Path.of("/etc/group"));
        addReadOnlyBind(arguments, Path.of("/etc/protocols"), Path.of("/etc/protocols"));
        addReadOnlyBind(arguments, Path.of("/etc/services"), Path.of("/etc/services"));
    }

    private void addJavaRuntimeBind(List<String> arguments, KryoSphereServiceSpec spec) {
        Path javaHome = javaHome(spec.javaExecutable());

        if (javaHome == null || systemPath(javaHome)) {
            return;
        }

        addReadOnlyBind(arguments, javaHome, Path.of("/runtime/java"));
    }

    private void addConfiguredReadOnlyBinds(List<String> arguments, List<String> paths) {
        for (String path : paths) {
            if (path == null || path.isBlank()) {
                continue;
            }

            Path value = Path.of(path).toAbsolutePath().normalize();
            addReadOnlyBind(arguments, value, value);
        }
    }

    private void addConfiguredWritableBinds(List<String> arguments, List<String> paths) {
        for (String path : paths) {
            if (path == null || path.isBlank()) {
                continue;
            }

            Path value = Path.of(path).toAbsolutePath().normalize();
            addWritableBind(arguments, value, value);
        }
    }

    private void addReadOnlyBind(List<String> arguments, Path source, Path target) {
        if (source == null || target == null || !Files.exists(source)) {
            return;
        }

        arguments.add("--ro-bind " + shellQuote(source) + " " + shellQuote(target));
    }

    private void addWritableBind(List<String> arguments, Path source, Path target) {
        if (source == null || target == null || !Files.exists(source)) {
            return;
        }

        arguments.add("--bind " + shellQuote(source) + " " + shellQuote(target));
    }

    private boolean systemPath(Path path) {
        if (path == null) {
            return false;
        }

        Path normalized = path.toAbsolutePath().normalize();
        return normalized.startsWith("/usr")
                || normalized.startsWith("/bin")
                || normalized.startsWith("/sbin")
                || normalized.startsWith("/lib")
                || normalized.startsWith("/lib64");
    }

    private Path javaHome(String javaExecutable) {
        Path raw = Path.of(javaExecutable);

        if (!raw.isAbsolute()) {
            return null;
        }

        Path path = raw.toAbsolutePath().normalize();
        Path parent = path.getParent();

        if (parent == null) {
            return null;
        }

        if ("bin".equals(parent.getFileName().toString())) {
            return parent.getParent();
        }

        return parent;
    }

    private String safeHostname(String value) {
        if (value == null || value.isBlank()) {
            return "kryocloud-service";
        }

        String normalized = value.toLowerCase().replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");

        if (normalized.isBlank()) {
            return "kryocloud-service";
        }

        if (normalized.length() <= 63) {
            return normalized;
        }

        return normalized.substring(0, 63).replaceAll("-$", "");
    }

    private String windowsQuote(Path path) {
        return windowsQuote(path.toAbsolutePath().normalize().toString());
    }

    private String windowsQuote(String value) {
        if (value == null || value.isBlank()) {
            return "\"\"";
        }

        return "\"" + value.replace("\"", "\\\"") + "\"";
    }

    private String windowsSafeEcho(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value
                .replace("^", "^^")
                .replace("&", "^&")
                .replace("<", "^<")
                .replace(">", "^>")
                .replace("|", "^|");
    }

    private String shellQuote(Path path) {
        return shellQuote(path.toAbsolutePath().normalize().toString());
    }

    private String shellQuote(String value) {
        if (value == null || value.isBlank()) {
            return "''";
        }

        return "'" + value.replace("'", "'\\''") + "'";
    }

}
