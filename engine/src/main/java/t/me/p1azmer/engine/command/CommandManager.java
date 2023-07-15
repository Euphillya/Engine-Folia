package t.me.p1azmer.engine.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import t.me.p1azmer.engine.NexPlugin;
import t.me.p1azmer.engine.api.command.GeneralCommand;
import t.me.p1azmer.engine.api.manager.AbstractManager;
import t.me.p1azmer.engine.command.list.AboutSubCommand;
import t.me.p1azmer.engine.command.list.HelpSubCommand;
import t.me.p1azmer.engine.utils.ArrayUtil;

import java.util.HashSet;
import java.util.Set;

public class CommandManager<P extends NexPlugin<P>> extends AbstractManager<P> {

    private Set<GeneralCommand<P>> commands;
    private PluginMainCommand<P>   mainCommand;

    public CommandManager(@NotNull P plugin) {
        super(plugin);
    }

    @Override
    public void onLoad() {
        this.commands = new HashSet<>();
        if (this.plugin.getConfigManager().commandAliases == null || this.plugin.getConfigManager().commandAliases.length == 0) {
            this.plugin.error("Could not register plugin commands!");
            return;
        }

        // Create main plugin command and attach help sub-command as a default executor.
        this.mainCommand = new PluginMainCommand<>(this.plugin);
        this.mainCommand.addDefaultCommand(new HelpSubCommand<>(this.plugin));

        // Register child plugin sub-commands to the main plugin command.
        this.plugin.registerCommands(this.mainCommand);

        if (!this.plugin.isEngine()) {
            this.mainCommand.addChildren(new AboutSubCommand<>(this.plugin));
        }

        // Register main command as a bukkit command.
        this.registerCommand(this.mainCommand);
    }

    @Override
    public void onShutdown() {
        for (GeneralCommand<P> command : new HashSet<>(this.commands)) {
            this.unregisterCommand(command);
            command.getChildrens().clear();
        }
        this.commands.clear();
    }

    @NotNull
    public Set<GeneralCommand<P>> getCommands() {
        return this.commands;
    }

    @NotNull
    public PluginMainCommand<P> getMainCommand() {
        return this.mainCommand;
    }

    @Nullable
    public GeneralCommand<P> getCommand(@NotNull String alias) {
        return this.getCommands().stream()
                .filter(command -> ArrayUtil.contains(command.getAliases(), alias))
                .findFirst().orElse(null);
    }

    public void registerCommand(@NotNull GeneralCommand<P> command) {
        if (this.commands.add(command)) {
            CommandRegister.register(this.plugin, command);
        }
    }

    public boolean unregisterCommand(@NotNull String alias) {
        GeneralCommand<P> command = this.getCommand(alias);
        if (command != null) {
            return this.unregisterCommand(command);
        }
        return false;
    }

    public boolean unregisterCommand(@NotNull GeneralCommand<P> command) {
        if (this.commands.remove(command)) {
            return CommandRegister.unregister(command.getAliases()[0]);
        }
        return false;
    }
}