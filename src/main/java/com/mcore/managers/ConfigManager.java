package com.mcore.managers;

import com.mcore.mCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ConfigManager {
    private final mCore plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private FileConfiguration playerDataConfig;
    private File playerDataFile;

    public ConfigManager(mCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        loadConfig();
        loadMessages();
        loadPlayerData();
        saveMenus();
    }

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

    public void loadPlayerData() {
        playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            try { playerDataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    public void saveMenus() {
        File menusFolder = new File(plugin.getDataFolder(), "menus");
        if (!menusFolder.exists()) {
            menusFolder.mkdirs();
        }

        String[] menuFiles = {
                "tpa-accept-menu.yml",
                "tpa-here-accept-menu.yml",
                "duel-accept-menu.yml"
        };

        for (String fileName : menuFiles) {
            File file = new File(menusFolder, fileName);
            if (!file.exists()) {
                try {
                    plugin.saveResource("menus/" + fileName, false);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("UYARI: Menü dosyası JAR içinde bulunamadı: menus/" + fileName);
                }
            }
        }
    }

    public void saveSpeed(String playerName, float walkSpeed, float flySpeed) {
        if (playerDataConfig == null) loadPlayerData();
        playerDataConfig.set("speeds." + playerName + ".walk", walkSpeed);
        playerDataConfig.set("speeds." + playerName + ".fly", flySpeed);
        savePlayerDataFile();
    }

    public void savePlayerIP(String playerName, String ip) {
        if (playerDataConfig == null) loadPlayerData();
        playerDataConfig.set("players." + playerName.toLowerCase(), ip);
        String safeIP = ip.replace(".", "_");
        List<String> accounts = playerDataConfig.getStringList("ips." + safeIP);
        if (!accounts.contains(playerName)) {
            accounts.add(playerName);
            playerDataConfig.set("ips." + safeIP, accounts);
        }
        savePlayerDataFile();
    }

    private void savePlayerDataFile() {
        try { playerDataConfig.save(playerDataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public FileConfiguration getMessages() {
        if (messagesConfig == null) loadMessages();
        return messagesConfig;
    }

    public FileConfiguration getPlayerData() {
        if (playerDataConfig == null) loadPlayerData();
        return playerDataConfig;
    }
}