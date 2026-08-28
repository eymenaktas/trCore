package com.trcore.managers;

import com.trcore.TRCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DisguiseManager {

    private final TRCore plugin;
    private File file;
    private FileConfiguration config;

    private final Map<UUID, String> disguises = new ConcurrentHashMap<>();

    public DisguiseManager(TRCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "disguise_data.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);

        disguises.clear();
        if (config.contains("disguises")) {
            for (String uuidStr : config.getConfigurationSection("disguises").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String name = config.getString("disguises." + uuidStr);
                    if (name != null) {
                        disguises.put(uuid, name);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void save() {
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
            config.set("disguises", null);
            for (Map.Entry<UUID, String> entry : disguises.entrySet()) {
                config.set("disguises." + entry.getKey().toString(), entry.getValue());
            }
            try {
                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void setDisguise(Player player, String disguiseName) {
        if (disguiseName == null || disguiseName.isEmpty()) {
            disguises.remove(player.getUniqueId());
        } else {
            disguises.put(player.getUniqueId(), disguiseName);
        }
        save();
    }

    public boolean isDisguised(Player player) {
        return disguises.containsKey(player.getUniqueId());
    }

    public String getDisguise(Player player) {
        return disguises.getOrDefault(player.getUniqueId(), player.getName());
    }
}
