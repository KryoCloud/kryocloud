package eu.kryocloud.node.console;

public enum ConsoleCategory {

    CORE("Core Management"),
    SERVICE("Minecraft Services"),
    PLAYER("Minecraft Players"),
    CLUSTER("Groups & Wrappers");

    private final String displayName;

    ConsoleCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
