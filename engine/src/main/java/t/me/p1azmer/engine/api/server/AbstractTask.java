package t.me.p1azmer.engine.api.server;

import fr.euphyllia.energie.model.SchedulerType;
import org.jetbrains.annotations.NotNull;
import t.me.p1azmer.engine.NexPlugin;

public abstract class AbstractTask<P extends NexPlugin<P>> {

    @NotNull
    protected final P plugin;

    protected int taskId;

    protected long interval;
    protected boolean async;

    public AbstractTask(@NotNull P plugin, int interval, boolean async) {
        this(plugin, interval * 20L, async);
    }

    public AbstractTask(@NotNull P plugin, long interval, boolean async) {
        this.plugin = plugin;
        this.interval = interval;
        this.async = async;
        this.taskId = -1;
    }

    public abstract void action();

    public final void restart() {
        this.stop();
        this.start();
    }

    public boolean start() {
        if (this.taskId >= 0) return false;
        if (this.interval <= 0L) return false;

        NexPlugin.getScheduler().runAtFixedRate(this.async ? SchedulerType.ASYNC : SchedulerType.SYNC, schedulerTaskInter -> action(), 1L, interval).getTaskId();
        return true;
    }

    public boolean stop() {
        if (this.taskId < 0) return false;
        NexPlugin.getScheduler().cancelTask(this.taskId);
        this.taskId = -1;
        return true;
    }
}