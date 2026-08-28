package com.aac.plugin;

import com.aac.plugin.commands.AACSystemCommand;
import com.aac.plugin.commands.AACWatchCommand;
import com.aac.plugin.commands.RaporlaCommand;
import com.aac.plugin.commands.RaporlarCommand;
import com.aac.plugin.managers.ConfigManager;
import com.aac.plugin.managers.LanguageManager;
import com.aac.plugin.managers.ReportManager;
import com.aac.plugin.listeners.ChatListener;
import com.aac.plugin.listeners.GUIListener;
import org.bukkit.plugin.java.JavaPlugin;

public class AAC extends JavaPlugin {
    
    private static AAC instance;
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private ReportManager reportManager;
    private GUIListener guiListener;
    private ChatListener chatListener;
    
    @Override
    public void onEnable() {
        instance = this;
        
        initializeManagers();
        registerCommands();
        registerListeners();
        
        getLogger().info("==================================================");
        getLogger().info("");
        getLogger().info("      ######      ######     #######  ");
        getLogger().info("     ##    ##    ##    ##    ##       ");
        getLogger().info("    ##      ##  ##      ##   ##       ");
        getLogger().info("    ##########  ##########   ##       ");
        getLogger().info("    ##      ##  ##      ##   ##       ");
        getLogger().info("    ##      ##  ##      ##   ##       ");
        getLogger().info("    ##      ##  ##      ##   ######## ");
        getLogger().info("");
        getLogger().info("  *** AAC Advanced-Anti-Cheat-raporlama author: CanXD ***");
        getLogger().info("       Version " + getDescription().getVersion());
        getLogger().info("==================================================");
    }


    @Override
    public void onDisable() {
        if (reportManager != null) {
            reportManager.saveReports();
        }
        getLogger().info("Plugin disabled");
    }
    
    private void initializeManagers() {
        configManager = new ConfigManager(this);
        languageManager = new LanguageManager(this);
        reportManager = new ReportManager(this);
    }
    
    private void registerCommands() {
        getCommand("raporla").setExecutor(new RaporlaCommand(this));
        getCommand("raporlar").setExecutor(new RaporlarCommand(this));
        getCommand("aacwatch").setExecutor(new AACWatchCommand(this));
        getCommand("aacsystem").setExecutor(new AACSystemCommand(this));
    }
    
    private void registerListeners() {
        guiListener = new GUIListener(this);
        chatListener = new ChatListener(this);
        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(chatListener, this);
    }
    
    public static AAC getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
    
    public ReportManager getReportManager() {
        return reportManager;
    }
    
    public GUIListener getGUIListener() {
        return guiListener;
    }

    public ChatListener getChatListener() {
        return chatListener;
    }
}