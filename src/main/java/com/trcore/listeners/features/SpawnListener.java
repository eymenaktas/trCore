package com.trcore.listeners.features;

import com.trcore.TRCore;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Spawn/respawn olaylarını yöneten listener.
 *
 * Optimizasyon:
 *  - Tüm config değerleri yapıcıda cache'lenir, event başına YAML okunmaz.
 *  - disabledWorlds HashSet → O(1) contains.
 *  - Spawn lokasyonları SpawnManager cache'inden alınır (config'den değil).
 */
public class SpawnListener implements Listener {
    private final TRCore plugin;

    // Cached config — her event'te YAML parse yok
    private boolean onJoinEnabled;
    private boolean onRespawnEnabled;
    private final Set<String> disabledWorlds = new HashSet<>();
    private final Set<String> forceMainSpawnWorlds = new HashSet<>();

    public SpawnListener(TRCore plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /** Reload sırasında çağrılır. */
    public void loadConfig() {
        this.onJoinEnabled    = plugin.getConfig().getBoolean("spawn.on-join", true);
        this.onRespawnEnabled = plugin.getConfig().getBoolean("spawn.on-respawn", true);

        disabledWorlds.clear();
        disabledWorlds.addAll(plugin.getConfig().getStringList("spawn.disabled-worlds"));

        forceMainSpawnWorlds.clear();
        forceMainSpawnWorlds.addAll(plugin.getConfig().getStringList("spawn.respawn-at-main-spawn"));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        if (!onJoinEnabled) return;
        if (plugin.getSpawnManager().getSpawnLocation() == null) return;
        if (disabledWorlds.contains(event.getPlayer().getWorld().getName())) return;

        // Folia: join sırasında player bölgesi henüz hazır olmayabilir.
        // 1 tick defer ile ghost/chat-disabled sorunu önlenir.
        event.getPlayer().getScheduler().runDelayed(plugin,
                t -> plugin.getSpawnManager().teleportToSpawn(event.getPlayer()),
                null, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!onRespawnEnabled) return;
        String worldName = event.getPlayer().getWorld().getName();
        if (disabledWorlds.contains(worldName)) return;

        Location spawnLoc;
        if (forceMainSpawnWorlds.contains(worldName)) {
            spawnLoc = plugin.getSpawnManager().getSpawnLocation();
        } else {
            spawnLoc = plugin.getSpawnManager().getSpawnLocation(event.getPlayer());
        }

        if (spawnLoc == null || spawnLoc.getWorld() == null) return;

        event.setRespawnLocation(spawnLoc);
    }

    // Rely on standard respawn and onTeleport distance check.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        // Handled by standard respawn.
    }
}
