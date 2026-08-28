package com.trcore.commands;

import com.trcore.TRCore;
import com.trcore.utils.CC;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TPAutoCommand implements CommandExecutor {
    private final TRCore plugin;

    public TPAutoCommand(TRCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        // 1. Ayarı tersine çevir (Aç/Kapat)
        plugin.getToggleManager().toggleAutoTPA(player.getUniqueId());

        // 2. Yeni durumu kontrol et
        boolean isEnabled = plugin.getToggleManager().isAutoTPA(player.getUniqueId());

        if (isEnabled) {
            // AÇILDIYSA: BossBar göster, Ses çal, Mesaj gönder
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
            plugin.getSettingsManager().showAutoTpaBossBar(player);
            player.sendMessage(CC.get("toggle.tpauto-enabled"));
        } else {
            // KAPATILDIYSA: BossBar'ı sil, Mesaj gönder
            plugin.getSettingsManager().hideAutoTpaBossBar(player);
            player.sendMessage(CC.get("toggle.tpauto-disabled"));
        }

        return true;
    }
}

