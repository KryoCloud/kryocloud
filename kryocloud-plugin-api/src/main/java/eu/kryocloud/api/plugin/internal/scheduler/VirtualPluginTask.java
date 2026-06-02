package eu.kryocloud.api.plugin.internal.scheduler;

import eu.kryocloud.api.plugin.scheduler.IPluginTask;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public final class VirtualPluginTask implements IPluginTask {

    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final Future<?> future;

    public VirtualPluginTask(Future<?> future) {
        this.future = future;
    }

    @Override
    public boolean cancelled() {
        return cancelled.get() || future.isCancelled();
    }

    @Override
    public void cancel() {
        if (!cancelled.compareAndSet(false, true)) {
            return;
        }

        future.cancel(true);
    }

}
