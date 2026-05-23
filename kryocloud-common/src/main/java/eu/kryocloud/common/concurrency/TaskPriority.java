package eu.kryocloud.common.concurrency;

public enum TaskPriority {
    LOW(0), NORMAL(5), HIGH(10), CRITICAL(15);

    private final int weight;

    TaskPriority(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
