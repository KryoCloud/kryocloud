package eu.kryocloud.api.manifest.version;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public record Version(
        String link,
        int javaVersion,
        ObjectArrayList<String> javaFlags
) {
}
