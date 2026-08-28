package com.trcore.listeners;

import com.trcore.TRCore;
import com.trcore.managers.CooldownManager;
import com.trcore.utils.CC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CooldownListener implements Listener {
    private final TRCore plugin;

    public CooldownListener(TRCore plugin) {
        this.plugin = plugin;
    }

    // LOWEST: Sunucudaki diğer pluginlerden önce biz kontrol ediyoruz.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player player = e.getPlayer();

        // Bypass kontrolü
        if (player.hasPermission("trcore.cooldown.bypass")) return;

        String message = e.getMessage();
        CooldownManager manager = plugin.getCooldownManager();
        if (manager == null) return;

        // Komut bir grupta var mı?
        CooldownManager.CooldownGroup group = manager.getGroup(message);
        if (group == null) return; // Yoksa sal gitsin

        // Süre kontrolü
        long remaining = manager.getRemainingTime(player, group.name);

        if (remaining > 0) {
            // VARSA ENGELLE
            e.setCancelled(true);
            player.sendMessage(CC.get("cooldown.active", "%time%", String.valueOf(remaining)));
        } else {
            // YOKSA SÜREYİ EKLE VE DEVAM ET
            // Burası komut yazıldığı AN çalışır.
            manager.addCooldown(player, group);
        }
    }
}

