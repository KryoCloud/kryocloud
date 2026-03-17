package eu.kryocloud.api.manifest.software;

public abstract class Software {

    private final String latestVersion;

    protected Software(String latestVersion) {
        this.latestVersion = latestVersion;
    }
}
