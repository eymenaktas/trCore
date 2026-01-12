package com.mcore.commands;

import com.mcore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ChatClearCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("mcore.clearchat")) {
            sender.sendMessage(CC.get("no-perm"));
            return true;
        }

        for (int i = 0; i < 100; i++) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.hasPermission("mcore.clearchat.bypass")) {
                    p.sendMessage("");
                }
            }
        }

        String cleaner = sender.getName();
        Bukkit.broadcast(CC.get("clearchat.broadcast", "%player%", cleaner));
        return true;
    }
}