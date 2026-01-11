package com.mcore.managers;

import com.mcore.mCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {
    private final mCore plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public ConfigManager(mCore plugin) {
        this.plugin = plugin;
    }

    // --- EKSİK OLAN METOD BURASI ---
    // MCoreCommand reload yaparken bu metodu çağırıyor
    public void load() {
        loadConfig();
        loadMessages();
    }
    // --------------------------------

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public FileConfiguration getMessages() {
        if (messagesConfig == null) loadMessages();
        return messagesConfig;
    }
}