package t.me.p1azmer.engine.utils.values;

import fr.euphyllia.energie.model.SchedulerType;
import org.jetbrains.annotations.NotNull;
import t.me.p1azmer.engine.NexPlugin;

public class UniTask {

    private final NexPlugin<?> plugin;
    private final Runnable runnable;
    private final long interval;
    private final boolean async;

    private int taskId;

    public UniTask(@NotNull NexPlugin<?> plugin, @NotNull Runnable runnable, long interval, boolean async) {
        this.plugin = plugin;
        this.runnable = runnable;
        this.interval = interval;
        this.async = async;

        this.taskId = -1;
    }

    @NotNull
    public static Builder builder(@NotNull NexPlugin<?> plugin) {
        return new Builder(plugin);
    }

    public final void restart() {
        this.stop();
        this.start();
    }

    public boolean start() {
        if (this.taskId >= 0) return false;
        if (this.interval <= 0L) return false;

        this.taskId = NexPlugin.getScheduler().runAtFixedRate(this.async ? SchedulerType.ASYNC : SchedulerType.SYNC, schedulerTaskInter ->  runnable.run(), 0L, interval).getTaskId();

        return true;
    }

    public boolean stop() {
        if (this.taskId < 0) return false;

        NexPlugin.getScheduler().cancelTask(this.taskId);
        this.taskId = -1;
        return true;
    }

    public static class Builder {

        private final NexPlugin<?> plugin;

        private Runnable runnable;
        private long interval;
        private boolean async;

        public Builder(@NotNull NexPlugin<?> plugin) {
            this.plugin = plugin;
        }

        @NotNull
        public Builder withRunnable(@NotNull Runnable runnable) {
            this.runnable = runnable;
            return this;
        }

        @NotNull
        public Builder withTicks(long interval) {
            this.interval = interval;
            return this;
        }

        @NotNull
        public Builder withSeconds(int interval) {
            this.interval = interval * 20L;
            return this;
        }

        @NotNull
        public Builder async() {
            this.async = true;
            return this;
        }

        @NotNull
        public UniTask build() {
            return new UniTask(plugin, runnable, interval, async);
        }

        @NotNull
        public UniTask buildAndRun() {
            UniTask task = this.build();
            task.start();
            return task;
        }
    }
}