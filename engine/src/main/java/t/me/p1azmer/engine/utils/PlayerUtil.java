package t.me.p1azmer.engine.utils;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.euphyllia.energie.model.SchedulerType;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import t.me.p1azmer.engine.NexPlugin;
import t.me.p1azmer.engine.config.EngineConfig;
import t.me.p1azmer.engine.integration.external.VaultHook;
import t.me.p1azmer.engine.lang.EngineLang;
import t.me.p1azmer.engine.utils.collections.AutoRemovalCollection;
import t.me.p1azmer.engine.utils.message.NexParser;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlayerUtil {

    private static final Collection<Player> messageCache = AutoRemovalCollection.newArrayList(2, TimeUnit.SECONDS);

    public static void dispatchCommands(@NotNull Player player, @NotNull String... commands) {
        for (String command : commands) {
            dispatchCommand(player, command);
        }
    }

    public static void dispatchCommand(@NotNull Player player, @NotNull String command) {
        command = command.replace("[CONSOLE]", "");
        command = command.trim().replace("%player%", player.getName());
        command = Placeholders.forPlayer(player).apply(command);
        if (EngineUtils.hasPlaceholderAPI()) {
            command = PlaceholderAPI.setPlaceholders(player, command);
        }
        if (command.startsWith("[BUNGEE]")) {
            command = command.replace("[BUNGEE]", "");
            connectToServer(player, command);
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    public static void connectToServer(Player player, String server) {
        NexPlugin.getScheduler().runTask(SchedulerType.ASYNC, task -> {
            try {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Connect");
                out.writeUTF(server);
                player.sendPluginMessage(EngineUtils.ENGINE, "BungeeCord", out.toByteArray());
            } catch (Exception ignored) {
            }
        });
    }

    @NotNull
    public static Optional<Player> find(@NotNull String nameOrNick) {
        return Optional.ofNullable(getPlayer(nameOrNick));
    }

    @Nullable
    public static Player getPlayer(@NotNull String nameOrNick) {
        if (!EngineConfig.RESPECT_PLAYER_DISPLAYNAME.get()) {
            return Bukkit.getServer().getPlayer(nameOrNick);
        }

        Player found = Bukkit.getServer().getPlayerExact(nameOrNick);
        if (found != null) {
            return found;
        }

        String lowerName = nameOrNick.toLowerCase();
        int lowerLength = lowerName.length();
        int delta = Integer.MAX_VALUE;

        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            String nameReal = player.getName().toLowerCase(Locale.ENGLISH);
            String nameCustom = player.getDisplayName().toLowerCase();

            int length;
            if (nameReal.startsWith(lowerName)) {
                length = player.getName().length();
            }
            else if (nameCustom.startsWith(lowerName)) {
                length = player.getDisplayName().length();
            }
            else continue;

            int curDelta = Math.abs(length - lowerLength);
            if (curDelta < delta) {
                found = player;
                delta = curDelta;
            }

            if (curDelta == 0) break;
        }

        return found;
    }

    public static boolean isBedrockPlayer(@NotNull Player player) {
        return EngineUtils.hasFloodgate() && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
    }

    public static boolean isReal(@NotNull Player player) {
        return Bukkit.getServer().getPlayer(player.getUniqueId()) != null;
    }

    @NotNull
    public static String getPermissionGroup(@NotNull Player player) {
        return EngineUtils.hasVault() ? VaultHook.getPermissionGroup(player).toLowerCase() : Placeholders.DEFAULT;
    }

    @NotNull
    public static Set<String> getPermissionGroups(@NotNull Player player) {
        return EngineUtils.hasVault() ? VaultHook.getPermissionGroups(player) : Collections.emptySet();
    }

    @Deprecated
    public static long getGroupValueLong(@NotNull Player player, @NotNull Map<String, Long> rankMap, boolean isNegaBetter) {
        Map<String, Double> map2 = rankMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> (double) v.getValue()));
        return (long) getGroupValueDouble(player, map2, isNegaBetter);
    }

    @Deprecated
    public static int getGroupValueInt(@NotNull Player player, @NotNull Map<String, Integer> map, boolean isNegaBetter) {
        Map<String, Double> map2 = map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> (double) v.getValue()));
        return (int) getGroupValueDouble(player, map2, isNegaBetter);
    }

    @Deprecated
    public static double getGroupValueDouble(@NotNull Player player, @NotNull Map<String, Double> map, boolean isNegaBetter) {
        Set<String> groups = getPermissionGroups(player);
        // System.out.println("[0] groups of '" + player.getName() + "': " + groups);
        // System.out.println("[1] map to compare: " + map);

        Optional<Map.Entry<String, Double>> opt = map.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(Placeholders.DEFAULT) || groups.contains(entry.getKey())).min((entry1, entry2) -> {
            double val1 = entry1.getValue();
            double val2 = entry2.getValue();
            if (isNegaBetter && val2 < 0) return 1;
            if (isNegaBetter && val1 < 0) return -1;
            return (int) (val2 - val1);
        });

        // System.out.println("[2] max value for '" + player.getName() + "': " +
        // (opt.isPresent() ? opt.get() : "-1x"));

        return opt.isPresent() ? opt.get().getValue() : -1D;
    }

    @NotNull
    public static String getPrefix(@NotNull Player player) {
        return EngineUtils.hasVault() ? VaultHook.getPrefix(player) : "";
    }

    @NotNull
    public static String getSuffix(@NotNull Player player) {
        return EngineUtils.hasVault() ? VaultHook.getSuffix(player) : "";
    }

    public static void sendRichMessage(@NotNull CommandSender sender, @NotNull String message) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Colorizer.apply(NexParser.toPlainText(message)));
            return;
        }
        NexParser.toMessage(message).send(sender);
    }

    public static void sendActionBar(@NotNull Player player, @NotNull String msg) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, NexParser.toMessage(msg).build());
    }

    public static boolean hasEmptyInventory(@NotNull Player player) {
        return Stream.of(player.getInventory().getContents()).allMatch(item -> item == null || item.getType().isAir());
    }

    public static boolean hasEmptyContents(@NotNull Player player) {
        return Stream.of(player.getInventory().getContents()).allMatch(item -> item == null || item.getType().isAir());
    }

    public static int countItemSpace(@NotNull Player player, @NotNull ItemStack item) {
        int stackSize = item.getType().getMaxStackSize();
        return Stream.of(player.getInventory().getContents()).mapToInt(itemHas -> {
            if (itemHas == null || itemHas.getType().isAir()) {
                return stackSize;
            }
            if (itemHas.isSimilar(item)) {
                return (stackSize - itemHas.getAmount());
            }
            return 0;
        }).sum();
    }

    public static int countItem(@NotNull Player player, @NotNull Predicate<ItemStack> predicate) {
        return Stream.of(player.getInventory().getContents())
                .filter(item -> item != null && !item.getType().isAir() && predicate.test(item))
                .mapToInt(ItemStack::getAmount).sum();
    }

    public static int countItem(@NotNull Player player, @NotNull ItemStack item) {
        return countItem(player, item::isSimilar);
    }

    public static int countItem(@NotNull Player player, @NotNull Material material) {
        return countItem(player, itemHas -> itemHas.getType() == material);
    }

    public static boolean takeItem(@NotNull Player player, @NotNull ItemStack item) {
        return takeItem(player, itemHas -> itemHas.isSimilar(item), countItem(player, item));
    }

    public static boolean takeItem(@NotNull Player player, @NotNull ItemStack item, int amount) {
        return takeItem(player, itemHas -> itemHas.isSimilar(item), amount);
    }

    public static boolean takeItem(@NotNull Player player, @NotNull Material material) {
        return takeItem(player, itemHas -> itemHas.getType() == material, countItem(player, material));
    }

    public static boolean takeItem(@NotNull Player player, @NotNull Material material, int amount) {
        return takeItem(player, itemHas -> itemHas.getType() == material, amount);
    }

    public static boolean takeItem(@NotNull Player player, @NotNull Predicate<ItemStack> predicate) {
        return takeItem(player, predicate, countItem(player, predicate));
    }

    public static boolean takeItem(@NotNull Player player, @NotNull Predicate<ItemStack> predicate, int amount) {
        if (countItem(player, predicate) < amount) return false;

        int takenAmount = 0;

        Inventory inventory = player.getInventory();
        for (ItemStack itemHas : inventory.getContents()) {
            if (itemHas == null || !predicate.test(itemHas)) continue;

            int hasAmount = itemHas.getAmount();
            if (takenAmount + hasAmount > amount) {
                int diff = (takenAmount + hasAmount) - amount;
                itemHas.setAmount(diff);
                break;
            }

            itemHas.setAmount(0);
            if ((takenAmount += hasAmount) == amount) {
                break;
            }
        }
        return true;
    }

    public static void addItem(@NotNull Player player, @NotNull ItemStack... items) {
        Arrays.asList(items).forEach(item -> addItem(player, item, item.getAmount()));
    }

    public static void addItem(@NotNull Player player, @NotNull ItemStack item2, int amount) {
        if (amount <= 0 || item2.getType().isAir()) return;

        World world = player.getWorld();

        ItemStack item = new ItemStack(item2);
        item.setAmount(Math.min(item.getMaxStackSize(), amount));
        player.getInventory().addItem(item).values().forEach(left -> {
            world.dropItem(player.getLocation(), left);
            if (messageCache.add(player)) {
                EngineUtils.ENGINE.getMessage(EngineLang.ERROR_INVENTORY_IS_FULL).send(player);
                EngineUtils.ENGINE.getMessage(EngineLang.ERROR_ITEMS_DROP_UNDER_YOU).send(player);
            }

        });

        amount -= item.getAmount();
        if (amount > 0) addItem(player, item2, amount);
    }
}