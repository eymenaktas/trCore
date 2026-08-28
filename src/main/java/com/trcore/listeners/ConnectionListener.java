package com.trcore.listeners;

import com.trcore.TRCore;
import com.trcore.elo.EloPlayerData;
import com.trcore.managers.DuelManager;
import com.trcore.managers.QueueManager;
import com.trcore.managers.TPAManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class ConnectionListener implements Listener {
    private final TRCore plugin;
    private final TPAManager tpaManager;
    private final QueueManager queueManager;
    private final DuelManager duelManager;

    public ConnectionListener(TRCore plugin, TPAManager tpaManager, QueueManager queueManager, DuelManager duelManager) {
        this.plugin = plugin;
        this.tpaManager = tpaManager;
        this.queueManager = queueManager;
        this.duelManager = duelManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        // IP kaydet (asenkron â€” I/O thread-safe)
        String ip = player.getAddress().getHostString();
        org.bukkit.Bukkit.getAsyncScheduler().runNow(plugin,
                task -> plugin.getConfigManager().savePlayerIdentity(name, uuid, ip));

        // Hız ayarını yükle
        FileConfiguration data = plugin.getConfigManager().getPlayerData();
        if (data.contains("speeds." + name)) {
            float walk = (float) data.getDouble("speeds." + name + ".walk", 0.2f);
            float fly = (float) data.getDouble("speeds." + name + ".fly", 0.1f);
            player.setWalkSpeed(walk);
            player.setFlySpeed(fly);
        }

        // --- Per-player toggle & NV verisini yükle (dünya değişikliğinde DEÄİL, sadece
        // gerçek giriş) ---
        plugin.getToggleManager().loadPlayer(uuid);
        plugin.getNightVisionManager().loadPlayer(uuid);
        
        // --- Elo verisini asenkron yükle ---
        org.bukkit.Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            plugin.getEloManager().loadPlayer(uuid);
            EloPlayerData eloData = plugin.getEloManager().getPlayerData(uuid);
            if (eloData != null) eloData.setLastName(player.getName());
        });

        // TPAuto BossBar'ı geri göster
        if (plugin.getToggleManager().isAutoTPA(uuid)) {
            plugin.getSettingsManager().showAutoTpaBossBar(player);
        }

        // Night Vision efektini geri uygula
        if (plugin.getNightVisionManager().isNightVisionEnabled(uuid)) {
            plugin.getNightVisionManager().applyEffect(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        if (tpaManager != null)
            tpaManager.cleanup(player);
        if (queueManager != null)
            queueManager.cleanup(player);
        if (duelManager != null)
            duelManager.cleanup(player);

        if (plugin.getCooldownManager() != null) {
            plugin.getCooldownManager().cleanup(player);
        }

        // TpAuto BossBar temizle
        plugin.getSettingsManager().hideAutoTpaBossBar(player);

        // --- Per-player veriyi asenkron kaydet, ardından RAM'den kaldır ---
        // Not: savePlayer dosyayı yazıyor (I/O), bunu async yapıyoruz, unload global region'da
        org.bukkit.Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            plugin.getToggleManager().savePlayer(uuid);
            plugin.getNightVisionManager().savePlayer(uuid);
            // Kayıt bittikten sonra global region'a dön ve RAM'den kaldır
            org.bukkit.Bukkit.getGlobalRegionScheduler().run(plugin, t -> {
                plugin.getToggleManager().unloadPlayer(uuid);
                plugin.getNightVisionManager().unloadPlayer(uuid);
            });
        });
    }
}

