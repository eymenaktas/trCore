package com.mcore.commands;

import com.mcore.managers.BackManager;
import com.mcore.utils.CC;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BackCommand implements CommandExecutor {
    private final BackManager backManager;

    public BackCommand(BackManager backManager) {
        this.backManager = backManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (!player.hasPermission("mcore.back")) {
            player.sendMessage(CC.get("no-perm"));
            return true;
        }

        Location lastLoc = backManager.popLastLocation(player);
        if (lastLoc == null) {
            player.sendMessage(CC.get("back.no-location"));
            return true;
        }

        player.teleport(lastLoc);
        player.sendMessage(CC.get("back.teleporting"));
        return true;
    }
}