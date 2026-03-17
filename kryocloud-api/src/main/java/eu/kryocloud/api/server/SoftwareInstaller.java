package eu.kryocloud.api.server;

import eu.kryocloud.api.manifest.software.ServerSoftware;
import eu.kryocloud.api.manifest.version.Version;

import java.io.File;

public final class SoftwareInstaller {

    public void install(ServerSoftware software, String version, File target) {
        Version v = software.getVersions().get(version);
        // TODO: Download Jar
    }
}
