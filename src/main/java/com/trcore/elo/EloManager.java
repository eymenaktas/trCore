package com.trcore.elo;

import com.trcore.TRCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Elo data cache and database manager
 */
public class EloManager {
    private final TRCore plugin;
    private final EloConfig eloConfig;
    
    // Memory Cache
    private final Map<UUID, EloPlayerData> cache = new ConcurrentHashMap<>();
    
    // Data File Base
    private File dataFile;
    private FileConfiguration dataConfig;

    private final Object lock = new Object();

    public EloManager(TRCore plugin) {
        this.plugin = plugin;
        this.eloConfig = new EloConfig(plugin);
        this.eloConfig.load();
        
        loadDataFile();
        startAutoSave();
    }

    private void loadDataFile() {
        synchronized (lock) {
            dataFile = new File(plugin.getDataFolder(), "elo_data.yml");
            if (!dataFile.exists()) {
                try {
                    dataFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        }
    }

    private void startAutoSave() {
        // Folia Async Scheduler - Her 10 dakikada bir otomatik kayit
        plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, task -> {
            saveAllData();
        }, 10L, 10L, java.util.concurrent.TimeUnit.MINUTES);
    }

    /**
     * Verileri YAML'a asenkron ve güvenli bir şekilde yazar.
     */
    public void saveAllDataAsync() {
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            saveAllData();
        });
    }

    /**
     * Verileri YAML'a senkron (ancak thread-safe) yazar.
     */
    public void saveAllData() {
        // Cache kopyasını alarak iterasyon süresini kısaltıyoruz
        java.util.List<EloPlayerData> copy = new java.util.ArrayList<>(cache.values());
        if (copy.isEmpty()) return;

        synchronized (lock) {
            for (EloPlayerData data : copy) {
                String path = "players." + data.getUuid().toString();
                dataConfig.set(path + ".elo", data.getElo());
                dataConfig.set(path + ".kills", data.getKills());
                dataConfig.set(path + ".deaths", data.getDeaths());
                dataConfig.set(path + ".played-ranked", data.hasPlayedRanked());
                dataConfig.set(path + ".setting", data.getDisplaySetting().name());
                if (data.getLastName() != null) {
                    dataConfig.set(path + ".lastName", data.getLastName());
                }
                
                dataConfig.set(path + ".win-streak", data.getWinStreak());
                dataConfig.set(path + ".loss-protection", data.hasLossProtection());
            }
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save elo_data.yml: " + e.getMessage());
            }
        }
    }

    /**
     * Belirli oyuncunun verisini YAML'a yazar. I/O islemi asenkron threadde gerceklesir.
     */
    public void savePlayerDataAsync(UUID uuid) {
        EloPlayerData data = cache.get(uuid);
        if (data == null) return;
        
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            synchronized (lock) {
                String path = "players." + uuid.toString();
                dataConfig.set(path + ".elo", data.getElo());
                dataConfig.set(path + ".kills", data.getKills());
                dataConfig.set(path + ".deaths", data.getDeaths());
                dataConfig.set(path + ".played-ranked", data.hasPlayedRanked());
                dataConfig.set(path + ".setting", data.getDisplaySetting().name());
                if (data.getLastName() != null) {
                    dataConfig.set(path + ".lastName", data.getLastName());
                }
                
                dataConfig.set(path + ".win-streak", data.getWinStreak());
                dataConfig.set(path + ".loss-protection", data.hasLossProtection());

                try {
                    dataConfig.save(dataFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Online olan tüm oyuncuları yükler. (Reload/Startup için)
     */
    public void loadOnlinePlayers() {
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            for (org.bukkit.entity.Player p : plugin.getServer().getOnlinePlayers()) {
                loadPlayer(p.getUniqueId());
                EloPlayerData data = getPlayerData(p.getUniqueId());
                if (data != null) {
                    data.setLastName(p.getName());
                }
            }
        });
    }

    /**
     * Oyuncu verisini yükler.
     */
    public void loadPlayer(UUID uuid) {
        if (cache.containsKey(uuid)) return;
        
        int defaultElo = eloConfig.get().getInt("elo.default", 1000);
        String path = "players." + uuid.toString();
        
        synchronized (lock) {
            if (dataConfig.contains(path)) {
                int elo = dataConfig.getInt(path + ".elo", defaultElo);
                int kills = dataConfig.getInt(path + ".kills", 0);
                int deaths = dataConfig.getInt(path + ".deaths", 0);
                boolean playedRanked = dataConfig.getBoolean(path + ".played-ranked", false);
                String settingStr = dataConfig.getString(path + ".setting", "RANK");
                String lastName = dataConfig.getString(path + ".lastName", null);
                
                EloPlayerData.DisplaySetting setting;
                try {
                    setting = EloPlayerData.DisplaySetting.valueOf(settingStr);
                } catch (Exception e) {
                    setting = EloPlayerData.DisplaySetting.RANK;
                }
                
                EloPlayerData data = new EloPlayerData(uuid, elo, kills, deaths, playedRanked, setting, lastName);
                data.setWinStreak(dataConfig.getInt(path + ".win-streak", 0));
                data.setLossProtection(dataConfig.getBoolean(path + ".loss-protection", false));
                
                cache.put(uuid, data);
            } else {
                cache.put(uuid, new EloPlayerData(uuid, defaultElo, 0, 0, false, EloPlayerData.DisplaySetting.RANK, null));
            }
        }
    }

    public void unloadPlayer(UUID uuid) {
        savePlayerDataAsync(uuid);
        cache.remove(uuid);
    }

    public EloPlayerData getPlayerData(UUID uuid) {
        return cache.get(uuid);
    }

    public EloConfig getEloConfig() {
        return eloConfig;
    }

    public UUID getUUIDByName(String name) {
        if (dataConfig == null || !dataConfig.contains("players")) return null;
        for (String uuidStr : dataConfig.getConfigurationSection("players").getKeys(false)) {
            String lastName = dataConfig.getString("players." + uuidStr + ".lastName");
            if (lastName != null && lastName.equalsIgnoreCase(name)) {
                try {
                    return UUID.fromString(uuidStr);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }
}
