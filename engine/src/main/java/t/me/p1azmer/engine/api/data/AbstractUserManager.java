package t.me.p1azmer.engine.api.data;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import t.me.p1azmer.engine.NexPlugin;
import t.me.p1azmer.engine.api.manager.AbstractListener;
import t.me.p1azmer.engine.api.manager.AbstractManager;
import t.me.p1azmer.engine.utils.EntityUtil;
import t.me.p1azmer.engine.utils.PlayerUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public abstract class AbstractUserManager<P extends NexPlugin<P>, U extends AbstractUser<P>> extends AbstractManager<P> {

    private final UserDataHolder<P, U> dataHolder;
    private final Map<UUID, U> usersLoaded;

    public AbstractUserManager(@NotNull P plugin, @NotNull UserDataHolder<P, U> dataHolder) {
        super(plugin);
        this.dataHolder = dataHolder;
        this.usersLoaded = new ConcurrentHashMap<>();
    }

    @Override
    protected void onLoad() {
        this.addListener(new PlayerListener(this.plugin));
    }

    @Override
    protected void onShutdown() {
        this.getUsersLoaded().forEach(user -> this.dataHolder.getData().saveUser(user));
        this.getUsersLoadedMap().clear();
    }

    @NotNull
    protected abstract U createData(@NotNull UUID uuid, @NotNull String name);

    public void loadOnlineUsers() {
        this.plugin.getServer().getOnlinePlayers().stream().map(Player::getUniqueId).forEach(this::getUserData);
    }

    /**
     * Gets the preloaded user data for specified player.
     * Throws an exception if user data is not loaded for the player, because it has to be loaded on player login.
     *
     * @param player A player instance to get user data for.
     * @return User data for the specified player.
     */
    @NotNull
    public final U getUserData(@NotNull Player player) {
        if (EntityUtil.isNPC(player)) {
            throw new IllegalStateException("Could not load user data from an NPC!");
        }

        U user = this.getUserLoaded(player.getUniqueId());
        if (user == null) {
            user = this.getUserData(player.getUniqueId());
            new Throwable().printStackTrace();
            this.plugin.warn("Main thread user data load for '" + player.getUniqueId() + "' aka '" + player.getName() + "'.");
        }
        if (user == null) {
            throw new IllegalStateException("User data for '" + player.getName() + "' is not loaded or created!");
        }
        return user;
    }

    /**
     * Attempts to load user data from online or cached player with that name.
     * If no such player data found, attempts to load it from the database.
     *
     * @param name A user name to load data for.
     * @return User data for the specified user name.
     */
    @Nullable
    public final U getUserData(@NotNull String name) {
        Player player = PlayerUtil.getPlayer(name);
        if (player != null) return this.getUserData(player);

        U user = this.getUserLoaded(name);
        if (user != null) return user;

        user = this.dataHolder.getData().getUser(name);
        if (user != null) {
            user.onLoad();
            this.cache(user);
        }

        return user;
    }

    /**
     * Attempts to load user data from online player with that UUID (if there is any).
     * In case if no such player is online, attempts to load data from the database.
     *
     * @param uuid A user unique id to load data for.
     * @return User data for the specified uuid.
     */
    @Nullable
    public final U getUserData(@NotNull UUID uuid) {
        U user = this.getUserLoaded(uuid);
        if (user != null) return user;

        user = this.dataHolder.getData().getUser(uuid);
        if (user != null) {
            user.onLoad();
            this.cache(user);
        }

        return user;
    }

    public final CompletableFuture<U> getUserDataAsync(@NotNull String name) {
        return CompletableFuture.supplyAsync(() -> this.getUserData(name));
    }

    public final CompletableFuture<U> getUserDataAsync(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> this.getUserData(uuid));
    }

    public void getUserDataAndPerform(@NotNull String name, Consumer<U> consumer) {
        U user = this.getUserLoaded(name);
        if (user != null) {
            consumer.accept(user);
        }
        else this.getUserDataAsync(name).thenAccept(user2 -> this.plugin.runTask(task -> consumer.accept(user2)));
    }

    public void getUserDataAndPerform(@NotNull UUID uuid, Consumer<U> consumer) {
        U user = this.getUserLoaded(uuid);
        if (user != null) {
            consumer.accept(user);
        }
        else this.getUserDataAsync(uuid).thenAccept(user2 -> this.plugin.runTask(task -> consumer.accept(user2)));
    }

    public void getUserDataAndPerformAsync(@NotNull String name, Consumer<U> consumer) {
        U user = this.getUserLoaded(name);
        if (user != null) {
            consumer.accept(user);
        }
        else this.getUserDataAsync(name).thenAccept(consumer);
    }

    public void getUserDataAndPerformAsync(@NotNull UUID uuid, Consumer<U> consumer) {
        U user = this.getUserLoaded(uuid);
        if (user != null) {
            consumer.accept(user);
        }
        else this.getUserDataAsync(uuid).thenAccept(consumer);
    }

    public final void unloadUser(@NotNull Player player) {
        this.unloadUser(player.getUniqueId());
    }

    public final void unloadUser(@NotNull UUID uuid) {
        U user = this.getUsersLoadedMap().get(uuid);
        if (user == null) return;

        this.unloadUser(user);
    }

    public void unloadUser(@NotNull U user) {
        Player player = user.getPlayer();
        if (player != null) {
            user.setName(player.getName());
            user.setLastOnline(System.currentTimeMillis());
        }
        this.saveUser(user);

        this.plugin.runTaskLaterAsync(task -> {
            if (!user.isOnline()) {
                this.getUsersLoadedMap().remove(user.getId());
                user.onUnload();
            }
        }, 40L);
    }

    public void saveUser(@NotNull U user) {
        this.plugin.runTaskAsync(task -> this.dataHolder.getData().saveUser(user));
    }

    @NotNull
    public Set<U> getAllUsers() {
        Map<UUID, U> users = new HashMap<>();
        this.getUsersLoaded().forEach(user -> users.put(user.getId(), user));
        this.dataHolder.getData().getUsers().forEach(user -> {
            if (!users.containsKey(user.getId())) {
                users.put(user.getId(), user);
            }
        });
        return new HashSet<>(users.values());
    }

    @NotNull
    public Map<UUID, @NotNull U> getUsersLoadedMap() {
        return this.usersLoaded;
    }

    @NotNull
    public Collection<@NotNull U> getUsersLoaded() {

        return new HashSet<>(this.getUsersLoadedMap().values());
    }

    @Nullable
    public U getUserLoaded(@NotNull UUID uuid) {
        return this.getUsersLoadedMap().get(uuid);
    }

    @Nullable
    public U getUserLoaded(@NotNull String name) {
        return this.getUsersLoaded().stream().filter(user -> user.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public boolean isUserLoaded(@NotNull Player player) {
        return this.getUsersLoadedMap().containsKey(player.getUniqueId());
    }

    public void cache(@NotNull U user) {
        this.getUsersLoadedMap().put(user.getId(), user);
    }

    class PlayerListener extends AbstractListener<P> {

        public PlayerListener(@NotNull P plugin) {
            super(plugin);
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onUserLogin(AsyncPlayerPreLoginEvent event) {
            if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

            UUID uuid = event.getUniqueId();
            U user;
            if (!dataHolder.getData().isUserExists(uuid)) {
                user = createData(uuid, event.getName());
                user.setRecentlyCreated(true);
                cache(user);
                dataHolder.getData().addUser(user);
                plugin.info("Created new user data for: '" + uuid + "'");
                return;
            } else {
                user = getUserData(uuid);
            }

            if (user == null) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Unable to load your user data.");
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onUserQuit(PlayerQuitEvent event) {
            // slow down the process without loading the main thread so that the data is saved without loss
            // for Proxy switching
            CompletableFuture.runAsync(() -> {
                plugin.runTask(sync -> unloadUser(event.getPlayer()));
            }).join();
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onUserQuit(PlayerKickEvent event) {
            CompletableFuture.runAsync(() -> {
                plugin.runTask(sync -> unloadUser(event.getPlayer()));
            }).join();
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onDisable(PluginDisableEvent event) {
            if (event.getPlugin().equals(this.plugin)) {
                CompletableFuture.runAsync(() -> getUsersLoaded().forEach(AbstractUserManager.this::saveUser));
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onDisable(WorldUnloadEvent event) {
            if (event.getWorld().getEnvironment().equals(World.Environment.NORMAL)) {
                CompletableFuture.runAsync(() -> getUsersLoaded().forEach(AbstractUserManager.this::saveUser));
            }
        }
    }
}