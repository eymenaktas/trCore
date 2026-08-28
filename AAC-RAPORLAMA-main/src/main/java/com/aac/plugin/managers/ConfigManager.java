package com.aac.plugin.managers;

import com.aac.plugin.AAC;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Arrays;
import java.util.List;

public class ConfigManager {
    
    private final AAC plugin;
    private FileConfiguration config;
    
    public ConfigManager(AAC plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    private void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        setDefaults();
    }
    
    private void setDefaults() {
        if (!config.contains("language")) {
            config.set("language", "tr");
        }
        
        if (!config.contains("report-cooldown")) {
            config.set("report-cooldown", 60);
        }
        
        if (!config.contains("alerts-auto-enable-on-join")) {
            config.set("alerts-auto-enable-on-join", true);
        }
        
        if (!config.contains("gui.report-player.glass-slots")) {
            config.set("gui.report-player.glass-slots", Arrays.asList(
                0, 1, 2, 3, 4, 5, 6, 7, 8,
                9, 17, 18, 26, 27, 35, 36, 44,
                45, 46, 47, 48, 50, 51, 52, 53
            ));
        }
        
        if (!config.contains("gui.report-player.player-slots")) {
            config.set("gui.report-player.player-slots", Arrays.asList(
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
            ));
        }
        
        if (!config.contains("gui.report-player.next-page-slot")) {
            config.set("gui.report-player.next-page-slot", 50);
        }
        
        if (!config.contains("gui.report-player.previous-page-slot")) {
            config.set("gui.report-player.previous-page-slot", 48);
        }
        
        if (!config.contains("gui.report-reasons.glass-slots")) {
            config.set("gui.report-reasons.glass-slots", Arrays.asList(
                0, 1, 2, 3, 4, 5, 6, 7, 8,
                9, 17, 18, 26, 27, 35, 36
            ));
        }
        
        if (!config.contains("gui.report-reasons.reason-slots")) {
            config.set("gui.report-reasons.reason-slots", Arrays.asList(
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
            ));
        }
        
        if (!config.contains("gui.reports.glass-slots")) {
            config.set("gui.reports.glass-slots", Arrays.asList(
                0, 1, 2, 3, 4, 5, 6, 7, 8,
                9, 17, 18, 26, 27, 35, 36, 44,
                45, 46, 47, 48, 50, 51, 52, 53
            ));
        }
        
        if (!config.contains("gui.reports.report-slots")) {
            config.set("gui.reports.report-slots", Arrays.asList(
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
            ));
        }
        
        if (!config.contains("gui.aacwatch.glass-slots")) {
            config.set("gui.aacwatch.glass-slots", Arrays.asList(
                0, 1, 2, 3, 4, 5, 6, 7, 8,
                9, 17, 18, 26, 27, 35, 36, 44,
                45, 46, 47, 48, 50, 51, 52, 53
            ));
        }
        
        if (!config.contains("gui.aacwatch.player-slots")) {
            config.set("gui.aacwatch.player-slots", Arrays.asList(
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
            ));
        }
        
        if (!config.contains("delete-raporlama-ekranı")) {
            config.set("delete-raporlama-ekranı", true);
        }

        plugin.saveConfig();
    }
    
    public boolean isDeleteReportConfirmGuiEnabled() {
        return config.getBoolean("delete-raporlama-ekranı", true);
    }

    public String getLanguage() {
        return config.getString("language", "tr");
    }
    
    public int getReportCooldown() {
        return config.getInt("report-cooldown", 60);
    }
    
    public boolean isAlertsAutoEnableOnJoin() {
        return config.getBoolean("alerts-auto-enable-on-join", true);
    }
    
    public List<Integer> getReportPlayerGlassSlots() {
        return config.getIntegerList("gui.report-player.glass-slots");
    }
    
    public List<Integer> getReportPlayerSlots() {
        return config.getIntegerList("gui.report-player.player-slots");
    }
    
    public int getNextPageSlot() {
        return config.getInt("gui.report-player.next-page-slot", 50);
    }
    
    public int getPreviousPageSlot() {
        return config.getInt("gui.report-player.previous-page-slot", 48);
    }
    
    public List<Integer> getReportReasonsGlassSlots() {
        return config.getIntegerList("gui.report-reasons.glass-slots");
    }
    
    public List<Integer> getReportReasonsSlots() {
        return config.getIntegerList("gui.report-reasons.reason-slots");
    }
    
    public List<Integer> getReportsGlassSlots() {
        return config.getIntegerList("gui.reports.glass-slots");
    }
    
    public List<Integer> getReportsSlots() {
        return config.getIntegerList("gui.reports.report-slots");
    }
    
    public List<Integer> getAACWatchGlassSlots() {
        return config.getIntegerList("gui.aacwatch.glass-slots");
    }
    
    public List<Integer> getAACWatchPlayerSlots() {
        return config.getIntegerList("gui.aacwatch.player-slots");
    }
    
    public int getReportsNextPageSlot() {
        return config.getInt("gui.reports.next-page-slot", 50);
    }
    
    public int getReportsPreviousPageSlot() {
        return config.getInt("gui.reports.previous-page-slot", 48);
    }
    
    public int getAACWatchNextPageSlot() {
        return config.getInt("gui.aacwatch.next-page-slot", 50);
    }
    
    public int getAACWatchPreviousPageSlot() {
        return config.getInt("gui.aacwatch.previous-page-slot", 48);
    }
    
    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
}
