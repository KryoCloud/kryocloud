package eu.kryocloud.sphere;

import java.util.List;

public record KryoSphereSettings(KryoSphereMode mode, boolean preferBubblewrap, boolean privateTmp, boolean protectHome, boolean restrictProc, boolean noNewPrivileges, boolean clearEnvironment, boolean allowNetwork, int memoryLimitMb, int cpuLimitPercent, int openFileLimit, int processLimit, int tmpSizeMb, String serviceUser, List<String> readOnlyPaths, List<String> writablePaths) {

    public KryoSphereSettings(KryoSphereMode mode, boolean preferBubblewrap, boolean privateTmp, boolean protectHome, boolean restrictProc, boolean noNewPrivileges, int memoryLimitMb, int cpuLimitPercent, int openFileLimit, int processLimit, String serviceUser) {
        this(mode, preferBubblewrap, privateTmp, protectHome, restrictProc, noNewPrivileges, true, true, memoryLimitMb, cpuLimitPercent, openFileLimit, processLimit, 256, serviceUser, List.of(), List.of());
    }

    public KryoSphereSettings {
        if (mode == null) {
            mode = KryoSphereMode.BASIC;
        }

        if (memoryLimitMb < 0) {
            memoryLimitMb = 0;
        }

        if (cpuLimitPercent < 0) {
            cpuLimitPercent = 0;
        }

        if (openFileLimit < 0) {
            openFileLimit = 0;
        }

        if (processLimit < 0) {
            processLimit = 0;
        }

        if (tmpSizeMb < 0) {
            tmpSizeMb = 0;
        }

        serviceUser = serviceUser == null ? "" : serviceUser.trim();
        readOnlyPaths = readOnlyPaths == null ? List.of() : List.copyOf(readOnlyPaths);
        writablePaths = writablePaths == null ? List.of() : List.copyOf(writablePaths);
    }

    public static KryoSphereSettings disabled() {
        return new KryoSphereSettings(KryoSphereMode.NONE, false, false, false, false, false, false, true, 0, 0, 0, 0, 0, "", List.of(), List.of());
    }

    public static KryoSphereSettings basic() {
        return new KryoSphereSettings(KryoSphereMode.BASIC, false, true, true, true, true, true, true, 0, 0, 65_536, 0, 256, "", List.of(), List.of());
    }

    public static KryoSphereSettings strict() {
        return new KryoSphereSettings(KryoSphereMode.STRICT, true, true, true, true, true, true, true, 0, 0, 65_536, 0, 256, "", List.of(), List.of());
    }

    public boolean enabled() {
        return mode.enabled();
    }

}
