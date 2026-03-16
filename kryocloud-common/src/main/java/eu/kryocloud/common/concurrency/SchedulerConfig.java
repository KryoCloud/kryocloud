package eu.kryocloud.common.concurrency;

import java.time.Duration;

public record SchedulerConfig(int cpuPoolSize, int backgroundPoolSize, int maxQueuedTasksPerPool, Duration defaultTimeout, Duration shutdownGracePeriod, RejectionPolicy rejectionPolicy, boolean metricsEnabled, boolean callerRunsAllowedOnEventLoop) {

    public static SchedulerConfig defaults() {
        return new SchedulerConfig(Runtime.getRuntime().availableProcessors(), 2, 1024, Duration.ofSeconds(30), Duration.ofSeconds(10), RejectionPolicy.ABORT, true, false);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int cpuPoolSize = Runtime.getRuntime().availableProcessors();
        private int backgroundPoolSize = 2;
        private int maxQueuedTasksPerPool = 1024;
        private Duration defaultTimeout = Duration.ofSeconds(30);
        private Duration shutdownGracePeriod = Duration.ofSeconds(10);
        private RejectionPolicy rejectionPolicy = RejectionPolicy.ABORT;
        private boolean metricsEnabled = true;
        private boolean callerRunsAllowedOnEventLoop = false;

        public Builder cpuPoolSize(int size) {
            this.cpuPoolSize = size;
            return this;
        }

        public Builder backgroundPoolSize(int size) {
            this.backgroundPoolSize = size;
            return this;
        }

        public Builder maxQueuedTasksPerPool(int max) {
            this.maxQueuedTasksPerPool = max;
            return this;
        }

        public Builder defaultTimeout(Duration timeout) {
            this.defaultTimeout = timeout;
            return this;
        }

        public Builder shutdownGracePeriod(Duration period) {
            this.shutdownGracePeriod = period;
            return this;
        }

        public Builder rejectionPolicy(RejectionPolicy policy) {
            this.rejectionPolicy = policy;
            return this;
        }

        public Builder metricsEnabled(boolean enabled) {
            this.metricsEnabled = enabled;
            return this;
        }

        public Builder callerRunsAllowedOnEventLoop(boolean allowed) {
            this.callerRunsAllowedOnEventLoop = allowed;
            return this;
        }

        public SchedulerConfig build() {
            return new SchedulerConfig(cpuPoolSize, backgroundPoolSize, maxQueuedTasksPerPool, defaultTimeout, shutdownGracePeriod, rejectionPolicy, metricsEnabled, callerRunsAllowedOnEventLoop);
        }
    }
}
