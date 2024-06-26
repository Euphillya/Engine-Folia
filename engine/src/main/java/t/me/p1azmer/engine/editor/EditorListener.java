package t.me.p1azmer.engine.editor;

import fr.euphyllia.energie.model.SchedulerType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import t.me.p1azmer.engine.NexEngine;
import t.me.p1azmer.engine.NexPlugin;
import t.me.p1azmer.engine.api.editor.InputHandler;
import t.me.p1azmer.engine.api.editor.InputWrapper;
import t.me.p1azmer.engine.api.manager.AbstractListener;
import t.me.p1azmer.engine.utils.Colorizer;
import t.me.p1azmer.engine.utils.StringUtil;

import java.util.HashSet;

public class EditorListener extends AbstractListener<NexEngine> {

    public EditorListener(@NotNull NexEngine plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        EditorManager.endEdit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChatText(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        InputHandler handler = EditorManager.getInputHandler(player);
        if (handler == null) return;

        event.getRecipients().clear();
        event.setCancelled(true);

        InputWrapper wrapper = new InputWrapper(event);

        NexPlugin.getScheduler().runTask(SchedulerType.SYNC, player, task -> {
            if (wrapper.getTextRaw().equalsIgnoreCase(EditorManager.EXIT) || handler.handle(wrapper)) {
                EditorManager.endEdit(player);
            }
        }, null);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChatCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        InputHandler handler = EditorManager.getInputHandler(player);
        if (handler == null) return;

        event.setCancelled(true);

        String raw = event.getMessage();
        String text = Colorizer.apply(raw.substring(1));
        if (text.startsWith(EditorManager.VALUES)) {
            String[] split = text.split(" ");
            int page = split.length >= 2 ? StringUtil.getInteger(split[1], 0) : 0;
            boolean auto = split.length >= 3 && Boolean.parseBoolean(split[2]);
            EditorManager.displayValues(player, auto, page);
            return;
        }

        AsyncPlayerChatEvent chatEvent = new AsyncPlayerChatEvent(true, player, text, new HashSet<>());
        InputWrapper wrapper = new InputWrapper(chatEvent);

        NexPlugin.getScheduler().runTask(SchedulerType.SYNC, player, task -> {
            if (wrapper.getTextRaw().equalsIgnoreCase(EditorManager.EXIT) || handler.handle(wrapper)) {
                EditorManager.endEdit(player);
            }
        }, null);
    }
}