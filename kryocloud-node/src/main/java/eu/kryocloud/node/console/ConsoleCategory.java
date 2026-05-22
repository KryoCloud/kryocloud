package eu.kryocloud.node.console;

public enum ConsoleCategory {

    CORE("Core Management Commands"),
    SERVICE("Service Management"),
    PLAYER("Player Management"),
    CLUSTER("Cluster & Tasks");

    private final String displayName;

    ConsoleCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}