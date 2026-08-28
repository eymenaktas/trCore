package com.trcore.managers;

import com.trcore.TRCore;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SpawnManager {
    private final TRCore plugin;
    private Location spawnLocation;
    private final java.util.Map<String, Location> worldSpawns = new java.util.HashMap<>();

    public SpawnManager(TRCore plugin) {
        this.plugin = plugin;
        loadSpawn();
        ensureSpawnLoaded();
    }

    public void loadSpawn() {
        // Global spawn
        if (plugin.getConfig().contains("spawn.location.world")) {
            String worldName = plugin.getConfig().getString("spawn.location.world");
            double x = plugin.getConfig().getDouble("spawn.location.x");
            double y = plugin.getConfig().getDouble("spawn.location.y");
            double z = plugin.getConfig().getDouble("spawn.location.z");
            float yaw = (float) plugin.getConfig().getDouble("spawn.location.yaw");
            float pitch = (float) plugin.getConfig().getDouble("spawn.location.pitch");
            if (worldName != null) {
                org.bukkit.World w = org.bukkit.Bukkit.getWorld(worldName);
                spawnLocation = new Location(w, x, y, z, yaw, pitch);
            }
        }

        // World spawns
        worldSpawns.clear();
        if (plugin.getConfig().contains("spawn.world-locations")) {
            org.bukkit.configuration.ConfigurationSection section = plugin.getConfig().getConfigurationSection("spawn.world-locations");
            if (section != null) {
                for (String worldName : section.getKeys(false)) {
                    double x = plugin.getConfig().getDouble("spawn.world-locations." + worldName + ".x");
                    double y = plugin.getConfig().getDouble("spawn.world-locations." + worldName + ".y");
                    double z = plugin.getConfig().getDouble("spawn.world-locations." + worldName + ".z");
                    float yaw = (float) plugin.getConfig().getDouble("spawn.world-locations." + worldName + ".yaw");
                    float pitch = (float) plugin.getConfig().getDouble("spawn.world-locations." + worldName + ".pitch");
                    org.bukkit.World w = org.bukkit.Bukkit.getWorld(worldName);
                    worldSpawns.put(worldName, new Location(w, x, y, z, yaw, pitch));
                }
            }
        }
    }

    public void setSpawn(Location loc) {
        this.spawnLocation = loc;
        plugin.getConfig().set("spawn.location.world", loc.getWorld().getName());
        plugin.getConfig().set("spawn.location.x", loc.getX());
        plugin.getConfig().set("spawn.location.y", loc.getY());
        plugin.getConfig().set("spawn.location.z", loc.getZ());
        plugin.getConfig().set("spawn.location.yaw", loc.getYaw());
        plugin.getConfig().set("spawn.location.pitch", loc.getPitch());
        
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            plugin.saveConfig();
        });
    }

    public void setWorldSpawn(Location loc) {
        String worldName = loc.getWorld().getName();
        worldSpawns.put(worldName, loc.clone());

        plugin.getConfig().set("spawn.world-locations." + worldName + ".world", worldName);
        plugin.getConfig().set("spawn.world-locations." + worldName + ".x", loc.getX());
        plugin.getConfig().set("spawn.world-locations." + worldName + ".y", loc.getY());
        plugin.getConfig().set("spawn.world-locations." + worldName + ".z", loc.getZ());
        plugin.getConfig().set("spawn.world-locations." + worldName + ".yaw", loc.getYaw());
        plugin.getConfig().set("spawn.world-locations." + worldName + ".pitch", loc.getPitch());
        
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            plugin.saveConfig();
        });

        // Update Minecraft's native world spawn
        loc.getWorld().setSpawnLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        
        ensureSpawnLoaded(loc);
    }

    public Location getWorldSpawn(org.bukkit.World world) {
        if (world == null) return null;
        Location loc = worldSpawns.get(world.getName());
        if (loc != null && loc.getWorld() == null) {
            loc.setWorld(world);
        }
        return loc;
    }

    public Location getSpawnLocation() {
        if (spawnLocation != null && spawnLocation.getWorld() == null) {
            String worldName = plugin.getConfig().getString("spawn.location.world");
            if (worldName != null) {
                org.bukkit.World w = org.bukkit.Bukkit.getWorld(worldName);
                if (w != null) spawnLocation.setWorld(w);
            }
        }
        return spawnLocation;
    }

    public Location getSpawnLocation(Player player) {
        if (player != null) {
            Location worldSpawn = getWorldSpawn(player.getWorld());
            if (worldSpawn != null) return worldSpawn;
        }
        return getSpawnLocation();
    }

    public void teleportToSpawn(Player player) {
        Location loc = getSpawnLocation(player);
        if (loc == null || loc.getWorld() == null) return;
        player.teleportAsync(loc);
    }

    public void ensureSpawnLoaded(Location loc) {
        if (loc != null && loc.getWorld() != null) {
            loc.getWorld().getChunkAtAsync(loc).thenAccept(chunk -> {
                chunk.addPluginChunkTicket(plugin);
            });
        }
    }

    private void ensureSpawnLoaded() {
        ensureSpawnLoaded(getSpawnLocation());
        for (Location loc : worldSpawns.values()) {
            ensureSpawnLoaded(loc);
        }
    }
}
