package eu.kryocloud.wrapper.instance.metrics;

import eu.kryocloud.common.logging.KryoLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class InstanceMetricsCollector {

    private static final KryoLogger LOGGER = KryoLogger.logger("InstanceMetrics");

    public InstanceMetrics collect(String serviceId) {
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }

        Optional<ProcessHandle> process = findMinecraftProcess(serviceId);

        if (process.isEmpty()) {
            return InstanceMetrics.unavailable(serviceId);
        }

        ProcessHandle handle = process.get();
        long pid = handle.pid();
        int memoryMb = memoryMb(pid);
        int cpuLoadPermille = cpuLoadPermille(handle);
        long uptimeMillis = uptimeMillis(handle);

        return new InstanceMetrics(serviceId, pid, memoryMb, cpuLoadPermille, uptimeMillis);
    }

    private Optional<ProcessHandle> findMinecraftProcess(String serviceId) {
        String marker = "-Dkryocloud.service.id=" + serviceId;
        List<ProcessHandle> markedProcesses = ProcessHandle.allProcesses().filter(ProcessHandle::isAlive).filter(handle -> commandLine(handle).contains(marker)).toList();

        if (markedProcesses.isEmpty()) {
            return Optional.empty();
        }

        Optional<ProcessHandle> markedJavaProcess = heaviest(markedProcesses.stream().filter(this::isJavaProcess).toList());

        if (markedJavaProcess.isPresent()) {
            return markedJavaProcess;
        }

        List<ProcessHandle> descendantJavaProcesses = markedProcesses.stream().flatMap(ProcessHandle::descendants).filter(ProcessHandle::isAlive).filter(this::isJavaProcess).toList();
        Optional<ProcessHandle> descendantJavaProcess = heaviest(descendantJavaProcesses);

        if (descendantJavaProcess.isPresent()) {
            return descendantJavaProcess;
        }

        return Optional.empty();
    }

    private Optional<ProcessHandle> heaviest(List<ProcessHandle> handles) {
        return handles.stream().max(Comparator.comparingInt(handle -> memoryMb(handle.pid())));
    }

    private boolean isJavaProcess(ProcessHandle handle) {
        String command = command(handle).toLowerCase(Locale.ROOT);
        String executable = executableName(command);

        if ("java".equals(executable) || "java.exe".equals(executable)) {
            return true;
        }

        if (executable.startsWith("java-") || executable.startsWith("java_")) {
            return true;
        }

        return false;
    }

    private String executableName(String command) {
        if (command == null || command.isBlank()) {
            return "";
        }

        Path path = Path.of(command);
        Path fileName = path.getFileName();

        if (fileName == null) {
            return command;
        }

        return fileName.toString();
    }

    private String command(ProcessHandle handle) {
        return handle.info().command().orElse("");
    }

    private String commandLine(ProcessHandle handle) {
        return handle.info().commandLine().orElse("");
    }

    private int memoryMb(long pid) {
        if (linux()) {
            int linuxMemory = linuxMemoryMb(pid);

            if (linuxMemory > 0) {
                return linuxMemory;
            }
        }

        if (windows()) {
            int windowsMemory = windowsMemoryMb(pid);

            if (windowsMemory > 0) {
                return windowsMemory;
            }
        }

        return psMemoryMb(pid);
    }

    private int linuxMemoryMb(long pid) {
        Path status = Path.of("/proc", String.valueOf(pid), "status");

        if (!Files.exists(status)) {
            return 0;
        }

        try {
            for (String line : Files.readAllLines(status)) {
                if (!line.startsWith("VmRSS:")) {
                    continue;
                }

                String digits = line.replaceAll("[^0-9]", "");

                if (digits.isBlank()) {
                    return 0;
                }

                long kb = Long.parseLong(digits);
                return (int) Math.max(1L, Math.round(kb / 1024.0D));
            }
        } catch (Exception exception) {
            LOGGER.debug("Failed to read memory from " + status + ": " + exception.getMessage());
        }

        return 0;
    }

    private int psMemoryMb(long pid) {
        try {
            Process process = new ProcessBuilder("ps", "-o", "rss=", "-p", String.valueOf(pid)).redirectErrorStream(true).start();

            if (!process.waitFor(800L, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return 0;
            }

            String output = new String(process.getInputStream().readAllBytes()).trim();

            if (output.isBlank()) {
                return 0;
            }

            long kb = Long.parseLong(output.split("\\R")[0].trim());
            return (int) Math.max(1L, Math.round(kb / 1024.0D));
        } catch (Exception exception) {
            return 0;
        }
    }

    private int windowsMemoryMb(long pid) {
        try {
            String command = "(Get-Process -Id " + pid + ").WorkingSet64";
            Process process = new ProcessBuilder("powershell", "-NoProfile", "-Command", command).redirectErrorStream(true).start();

            if (!process.waitFor(1200L, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return 0;
            }

            String output = new String(process.getInputStream().readAllBytes()).trim();

            if (output.isBlank()) {
                return 0;
            }

            long bytes = Long.parseLong(output.split("\\R")[0].trim());
            return (int) Math.max(1L, Math.round(bytes / 1024.0D / 1024.0D));
        } catch (Exception exception) {
            return 0;
        }
    }

    private int cpuLoadPermille(ProcessHandle handle) {
        Optional<Duration> cpu = handle.info().totalCpuDuration();
        Optional<Instant> started = handle.info().startInstant();

        if (cpu.isEmpty() || started.isEmpty()) {
            return 0;
        }

        long uptimeMillis = Math.max(1L, Duration.between(started.get(), Instant.now()).toMillis());
        double cpuMillis = cpu.get().toMillis();
        double cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        double ratio = cpuMillis / (uptimeMillis * cores);

        return (int) Math.max(0, Math.round(Math.min(1.0D, ratio) * 1000.0D));
    }

    private long uptimeMillis(ProcessHandle handle) {
        Optional<Instant> started = handle.info().startInstant();

        if (started.isEmpty()) {
            return 0L;
        }

        return Math.max(0L, Duration.between(started.get(), Instant.now()).toMillis());
    }

    private boolean linux() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
    }

    private boolean windows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
