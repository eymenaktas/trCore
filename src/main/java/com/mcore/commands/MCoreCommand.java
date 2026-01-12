package com.mcore.commands;

import com.mcore.mCore;
import com.mcore.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class MCoreCommand implements CommandExecutor {
    private final mCore plugin;

    public MCoreCommand(mCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("mcore.admin")) {
            sender.sendMessage(CC.get("no-perm"));
            return true;
        }

        plugin.getConfigManager().load(); // Config, Lang ve Menüleri yeniler
        sender.sendMessage(CC.get("reload"));
        return true;
    }
}