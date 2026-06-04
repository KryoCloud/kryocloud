package eu.kryocloud.sphere;

import java.util.List;

public record KryoSphereLaunchPlan(KryoSphereMode requestedMode, KryoSphereMode effectiveMode, KryoSpherePlatform platform, String backend, String command, List<String> warnings) {

    public KryoSphereLaunchPlan(KryoSphereMode requestedMode, KryoSphereMode effectiveMode, KryoSpherePlatform platform, String command, List<String> warnings) {
        this(requestedMode, effectiveMode, platform, effectiveMode == KryoSphereMode.STRICT ? "bubblewrap" : "basic", command, warnings);
    }

    public KryoSphereLaunchPlan {
        if (requestedMode == null) {
            requestedMode = KryoSphereMode.BASIC;
        }

        if (effectiveMode == null) {
            effectiveMode = requestedMode;
        }

        if (platform == null) {
            platform = KryoSpherePlatform.current();
        }

        if (backend == null || backend.isBlank()) {
            backend = effectiveMode == KryoSphereMode.STRICT ? "bubblewrap" : "basic";
        }

        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("command must not be blank");
        }

        if (warnings == null) {
            warnings = List.of();
        }

        warnings = List.copyOf(warnings);
    }

    public boolean downgraded() {
        return effectiveMode.ordinal() < requestedMode.ordinal();
    }

}
