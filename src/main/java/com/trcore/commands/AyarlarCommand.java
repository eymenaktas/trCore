package com.trcore.commands;

import com.trcore.TRCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AyarlarCommand implements CommandExecutor {
    private final TRCore plugin;

    public AyarlarCommand(TRCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            plugin.getSettingsManager().openMenu((Player) sender);
        }
        return true;
    }
}
