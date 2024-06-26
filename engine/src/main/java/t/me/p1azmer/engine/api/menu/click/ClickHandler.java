package t.me.p1azmer.engine.api.menu.click;

import fr.euphyllia.energie.model.SchedulerType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import t.me.p1azmer.engine.NexPlugin;
import t.me.p1azmer.engine.api.menu.impl.Menu;

import java.util.HashMap;
import java.util.Map;

public class ClickHandler<E extends Enum<E>> {

    private final Map<Enum<E>, ItemClick> clicks;

    public ClickHandler() {
        this.clicks = new HashMap<>();
    }

    @NotNull
    public static ItemClick forNextPage(@NotNull Menu<?> menu) {
        return ((viewer, event) -> {
            if (viewer.getPage() < viewer.getPages()) {
                menu.open(viewer.getPlayer(), viewer.getPage() + 1);
            }
        });
    }

    @NotNull
    public static ItemClick forPreviousPage(@NotNull Menu<?> menu) {
        return ((viewer, event) -> {
            if (viewer.getPage() > 1) {
                menu.open(viewer.getPlayer(), viewer.getPage() - 1);
            }
        });
    }

    @NotNull
    public static ItemClick forClose(@NotNull Menu<?> menu) {
        return ((viewer, event) -> NexPlugin.getScheduler().runTask(SchedulerType.SYNC, viewer.getPlayer(), task -> viewer.getPlayer().closeInventory(), null));
    }

    @NotNull
    public ClickHandler<E> addClick(@NotNull E type, @NotNull ItemClick click) {
        this.clicks.put(type, click);
        return this;
    }

    @Nullable
    public ItemClick getClick(@NotNull Enum<?> type) {
        return this.clicks.get(type);
    }

    @NotNull
    public Map<Enum<E>, ItemClick> getClicks() {
        return clicks;
    }
}