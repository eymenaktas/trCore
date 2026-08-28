package com.trcore.managers;

import com.trcore.TRCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private final TRCore plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private FileConfiguration playerDataConfig;
    private File playerDataFile;
    private FileConfiguration trimsConfig;
    private File trimsFile;

    public List<String> duelWorlds = new ArrayList<>();
    public List<String> queueWorlds = new ArrayList<>();

    public boolean combatLogEnabled;
    public boolean combatLogKillOnQuit;

    public ConfigManager(TRCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        loadConfig();
        loadMessages();
        loadPlayerData();
        loadTrims();
        saveMenus();
        cacheWorlds();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
    }

    private void cacheWorlds() {
        duelWorlds = plugin.getConfig().getStringList("rtp-duel.world");
        queueWorlds = plugin.getConfig().getStringList("rtp-queue.world");

        if (duelWorlds.isEmpty()) duelWorlds.add("world");
        if (queueWorlds.isEmpty()) queueWorlds.add("world");

        combatLogEnabled = plugin.getConfig().getBoolean("combat-log.enabled", true);
        combatLogKillOnQuit = plugin.getConfig().getBoolean("combat-log.kill-on-quit", true);
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        // Explicit UTF-8 - IBM857 sistem charset'in etkisini engeller
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(messagesFile), StandardCharsets.UTF_8)) {
            messagesConfig = YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        }
    }

    public void loadTrims() {
        trimsFile = new File(plugin.getDataFolder(), "trims.yml");
        if (!trimsFile.exists()) {
            plugin.saveResource("trims.yml", false);
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(trimsFile), StandardCharsets.UTF_8)) {
            trimsConfig = YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            trimsConfig = YamlConfiguration.loadConfiguration(trimsFile);
        }
    }

    public void loadPlayerData() {
        playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            try { playerDataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(playerDataFile), StandardCharsets.UTF_8)) {
            playerDataConfig = YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
        }
    }

    public void saveMenus() {
        File menusFolder = new File(plugin.getDataFolder(), "menus");
        if (!menusFolder.exists()) menusFolder.mkdirs();

        String[] menuFiles = {"tpa-accept-menu.yml", "tpa-here-accept-menu.yml", "duel-accept-menu.yml"};
        for (String fileName : menuFiles) {
            File file = new File(menusFolder, fileName);
            if (!file.exists()) {
                try {
                    plugin.saveResource("menus/" + fileName, false);
                } catch (IllegalArgumentException e) {}
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
        savePlayerIdentity(playerName, null, ip);
    }

    public void savePlayerIdentity(String playerName, java.util.UUID uuid, String ip) {
        if (playerDataConfig == null) loadPlayerData();
        playerDataConfig.set("players." + playerName.toLowerCase(), ip);
        String safeIP = ip.replace(".", "_");
        List<String> accounts = playerDataConfig.getStringList("ips." + safeIP);
        if (!accounts.contains(playerName)) {
            accounts.add(playerName);
            playerDataConfig.set("ips." + safeIP, accounts);
        }

        if (uuid != null) {
            String uuidStr = uuid.toString();
            playerDataConfig.set("uuids-by-name." + playerName.toLowerCase(), uuidStr);
            playerDataConfig.set("names-by-uuid." + uuidStr, playerName);
            playerDataConfig.set("ips-by-uuid." + uuidStr, ip);
        }

        savePlayerDataFile();
    }

    // --- DÜZELTME BURADA: private -> public YAPILDI ---
    public void savePlayerDataFile() {
        try { playerDataConfig.save(playerDataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public FileConfiguration getMessages() { if (messagesConfig == null) loadMessages(); return messagesConfig; }
    public FileConfiguration getPlayerData() { if (playerDataConfig == null) loadPlayerData(); return playerDataConfig; }
    public FileConfiguration getTrimsConfig() { if (trimsConfig == null) loadTrims(); return trimsConfig; }
}

