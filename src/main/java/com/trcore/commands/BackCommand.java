package com.trcore.commands;

import com.trcore.TRCore;
import com.trcore.utils.CC;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BackCommand implements CommandExecutor {
    private final TRCore plugin;

    public BackCommand(TRCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (!player.hasPermission("trcore.back")) {
            player.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("no-perm", "<red>Buna Yetkin Bulunmamaktadır.</red>")));
            return true;
        }

        // BackManager'dan oyuncunun son konumunu al
        Location lastLoc = plugin.getBackManager().popLastLocation(player);

        // Eğer kayıtlı bir konumu yoksa
        if (lastLoc == null) {
            player.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("back.no-location", "<red>Dönecek bir konumun yok.</red>")));
            return true;
        }

        // Paper API Optimizasyonu: Asenkron Işınlanma
        player.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("back.teleporting", "<gray>Önceki konumuna dönülüyor...</gray>")));
        player.teleportAsync(lastLoc).thenAccept(success -> {
            if (!success) {
                player.sendMessage(CC.parse("<red>Işınlanma sırasında bir hata oluştu."));
            }
        });

        return true;
    }
}
