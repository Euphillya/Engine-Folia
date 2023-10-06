package t.me.p1azmer.engine.api.data;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import t.me.p1azmer.engine.NexPlugin;

import java.util.UUID;

public abstract class AbstractUser<P extends NexPlugin<P>> {

    protected transient final P plugin;
    private transient boolean isRecent = false;

    protected final UUID uuid;
    protected String name;
    protected long dateCreated;
    protected long lastOnline;

    public AbstractUser(@NotNull P plugin, @NotNull UUID uuid, @NotNull String name, long dateCreated, long lastOnline) {
        this.plugin = plugin;
        this.uuid = uuid;
        this.name = name;
        this.setDateCreated(dateCreated);
        this.setLastOnline(lastOnline);
    }

    public void onLoad() {

    }

    public void onUnload() {
        Player player = this.getPlayer();
        if (player != null) {
            this.name = player.getName();
            this.setLastOnline(System.currentTimeMillis());
        }
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    public <U extends AbstractUser<P>> void saveData(@NotNull UserDataHolder<P, U> dataHolder) {
        this.plugin.runTaskAsync(task -> dataHolder.getData().saveUser((U) this));
    }

    public boolean isRecentlyCreated() {
        return isRecent;
    }

    public void setRecentlyCreated(boolean recent) {
        isRecent = recent;
    }

    @NotNull
    public final UUID getId() {
        return this.uuid;
    }

    @NotNull
    public final String getName() {
        return this.name;
    }

    public final long getDateCreated() {
        return dateCreated;
    }

    public final void setDateCreated(long dateCreated) {
        this.dateCreated = dateCreated;
    }

    public final long getLastOnline() {
        return this.lastOnline;
    }

    public final void setLastOnline(long lastOnline) {
        this.lastOnline = lastOnline;
    }

    public final boolean isOnline() {
        return this.getPlayer() != null;
    }

    @NotNull
    public final OfflinePlayer getOfflinePlayer() {
        return this.plugin.getServer().getOfflinePlayer(this.getId());
    }

    @Nullable
    public final Player getPlayer() {
        return this.plugin.getServer().getPlayer(this.getId());
    }

    @Override
    public String toString() {
        return "AbstractUser [uuid=" + this.uuid + ", name=" + this.name + ", lastOnline=" + this.lastOnline + "]";
    }
}