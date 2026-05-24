package eu.kryocloud.node.console;

public enum ConsoleCategory {

    CORE("Core"),
    GROUP("Groups"),
    SERVICE("Services"),
    CLUSTER("Cluster"),
    PLAYER("Players");

    private final String displayName;

    ConsoleCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
