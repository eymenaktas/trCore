//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.mcore.managers;

import com.mcore.mCore;
import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigManager {
    private final mCore plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public ConfigManager(mCore plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        this.plugin.saveDefaultConfig();
        this.plugin.reloadConfig();
    }

    public void loadMessages() {
        this.messagesFile = new File(this.plugin.getDataFolder(), "messages.yml");
        if (!this.messagesFile.exists()) {
            this.plugin.saveResource("messages.yml", false);
        }

        this.messagesConfig = YamlConfiguration.loadConfiguration(this.messagesFile);
    }

    public FileConfiguration getMessages() {
        if (this.messagesConfig == null) {
            this.loadMessages();
        }

        return this.messagesConfig;
    }
}
