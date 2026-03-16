package eu.kryocloud.common.concurrency.netty;

import eu.kryocloud.common.concurrency.CloudScheduler;
import eu.kryocloud.common.concurrency.ScheduledTask;
import eu.kryocloud.common.concurrency.TaskKind;
import eu.kryocloud.common.concurrency.TaskPriority;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class NettyOffloader {

    private final CloudScheduler scheduler;

    public NettyOffloader(CloudScheduler scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler);
    }

    public <T> ScheduledTask<T> offload(TaskKind kind, ChannelHandlerContext ctx, Supplier<T> work, Consumer<T> onResult) {
        return offload(kind, null, TaskPriority.NORMAL, null, ctx, work, onResult);
    }

    public <T> ScheduledTask<T> offload(TaskKind kind, String name, TaskPriority priority, Duration timeout, ChannelHandlerContext ctx, Supplier<T> work, Consumer<T> onResult) {
        EventLoop eventLoop = ctx.channel().eventLoop();

        ScheduledTask<T> task = scheduler.submit(kind, name, priority, timeout, work);

        task.future().whenComplete((result, error) -> {
            if (error != null) return;
            eventLoop.execute(() -> onResult.accept(result));
        });

        return task;
    }

    public <T> ScheduledTask<T> offloadIO(ChannelHandlerContext ctx, Supplier<T> work, Consumer<T> onResult) {
        return offload(TaskKind.BLOCKING_IO, ctx, work, onResult);
    }

    public <T> ScheduledTask<T> offloadCpu(ChannelHandlerContext ctx, Supplier<T> work, Consumer<T> onResult) {
        return offload(TaskKind.CPU_BOUND, ctx, work, onResult);
    }

    public ScheduledTask<Void> offloadFire(TaskKind kind, ChannelHandlerContext ctx, Runnable work) {
        return offload(kind, ctx, () -> {
            work.run();
            return null;
        }, v -> {});
    }
}
