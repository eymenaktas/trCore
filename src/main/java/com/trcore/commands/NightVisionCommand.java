package com.trcore.commands;

import com.trcore.TRCore;
import com.trcore.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NightVisionCommand implements CommandExecutor {

    private final TRCore plugin;

    public NightVisionCommand(TRCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(CC.parse("<red>Bu komut sadece oyuncular tarafÄ±ndan kullanÄ±labilir."));
            return true;
        }

        boolean enabled = plugin.getNightVisionManager().toggleNightVision(player);
        player.sendMessage(CC.get(enabled ? "night-vision.enabled" : "night-vision.disabled"));
        return true;
    }
}

