package com.trcore.commands;

import com.trcore.TRCore;
import com.trcore.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class TRCoreCommand implements CommandExecutor {
    private final TRCore plugin;

    public TRCoreCommand(TRCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {

            if (!sender.hasPermission("trcore.admin")) {
            sender.sendMessage(CC.get("no-perm"));
            return true;
        }

        // Reload asenkron yapılır — I/O işlemleri var (config, yml okuma)
        // Mesajı göndermek main thread'den olduğu için önce gönder, sonra async reload yap
        org.bukkit.Bukkit.getAsyncScheduler().runNow(plugin, task -> {

            // 1. Config + messages + menüler
            plugin.getConfigManager().load();

            // 2. Cooldown
            plugin.getCooldownManager().load();

            // 3. Menüler (templates RAM'den temizlenir, yeniden yüklenir)
            plugin.getMenuManager().load();

            // 4. Settings menüsü
            plugin.getSettingsManager().loadConfig();

            // 5. Custom komutlar
            plugin.getCustomCommandManager().reload();

            // 6. Spawn konumu
            if (plugin.getSpawnManager() != null)
                plugin.getSpawnManager().loadSpawn();

            // 7. ItemClear world config
            if (plugin.getItemClearListener() != null)
                plugin.getItemClearListener().loadConfig();

            // 8. RTP config (rtp.yml)
            if (plugin.getRTPManager() != null)
                plugin.getRTPManager().load();

            // 9. Queue settings (min/max/worlds cached)
            plugin.getQueueManager().loadSettings();

            // 10. Elo settings
            if (plugin.getEloManager() != null) plugin.getEloManager().getEloConfig().load();
            if (plugin.getRankedMatchManager() != null) plugin.getRankedMatchManager().reloadConfig();

            if (plugin.getDisguiseManager() != null) plugin.getDisguiseManager().load();

            // 11. Cached listener/manager config'lerini yeniden yükle
            if (plugin.getSpawnListener() != null)
                plugin.getSpawnListener().loadConfig();
            if (plugin.getActionbarListener() != null)
                plugin.getActionbarListener().reload(plugin);
            if (plugin.getPlayerListener() != null)
                plugin.getPlayerListener().loadConfig();
            if (plugin.getWorldChangeListener() != null)
                plugin.getWorldChangeListener().loadConfig();
            if (plugin.getCombatManager() != null)
                plugin.getCombatManager().loadConfig();

            // 12. Night vision — online oyunculara main thread'de uygula
            org.bukkit.Bukkit.getGlobalRegionScheduler().run(plugin, t -> {
                plugin.getNightVisionManager().applyToOnlinePlayers();
                // Reload tamamlandı mesajı
                sender.sendMessage(CC.get("reload"));
            });
        });

        return true;
    }
}
