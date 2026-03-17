package eu.kryocloud.api.manifest;

import eu.kryocloud.api.manifest.dependency.Dependency;
import eu.kryocloud.api.manifest.software.Software;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class Manifest {

    private final Object2ObjectOpenHashMap<String, Software> softwares;
    private final ObjectArrayList<Dependency> dependencies;

    public Manifest() {
        this.softwares = new Object2ObjectOpenHashMap<>();
        this.dependencies = new ObjectArrayList<>();
    }
}
