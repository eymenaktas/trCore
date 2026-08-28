package com.trcore.commands;

import com.trcore.TRCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RankedQueueCommand implements CommandExecutor {

    private final TRCore plugin;

    public RankedQueueCommand(TRCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) return true;
        
        if (plugin.getRankedQueueManager() != null) {
            plugin.getRankedQueueManager().toggle(p);
        }
        return true;
    }
}
