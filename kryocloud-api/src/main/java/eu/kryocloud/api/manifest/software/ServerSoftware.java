package eu.kryocloud.api.manifest.software;

import eu.kryocloud.api.manifest.version.Version;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class ServerSoftware extends Software {

    private final Object2ObjectOpenHashMap<String, Version> versions;

    protected ServerSoftware(String latestVersion) {
        super(latestVersion);
        this.versions = new Object2ObjectOpenHashMap<>();
    }

    public Object2ObjectOpenHashMap<String, Version> getVersions() {
        return versions;
    }
}
