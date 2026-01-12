//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.mcore.commands;

import com.mcore.mCore;
import com.mcore.listeners.PlayerListener;
import com.mcore.utils.CC;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BackCommand implements CommandExecutor {
    private final mCore plugin;

    public BackCommand(mCore p) {
        this.plugin = p;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (!player.hasPermission("mcore.back")) {
                player.sendMessage(CC.get("no-perm"));
                return true;
            } else {
                Location loc = (Location)PlayerListener.lastLocations.remove(player.getUniqueId());
                if (loc == null) {
                    player.sendMessage(CC.get("back.no-location"));
                    return true;
                } else {
                    player.teleport(loc);
                    player.sendMessage(CC.get("back.teleporting"));
                    return true;
                }
            }
        } else {
            return true;
        }
    }
}
