package com.aac.plugin.managers;

import com.aac.plugin.AAC;
import com.aac.plugin.models.Report;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.CopyOnWriteArrayList;

public class ReportManager {
    
    private final AAC plugin;
    private final Map<UUID, Long> reportCooldowns;
    private final List<Report> reports;
    private final Map<UUID, List<String>> playerMessages;
    private final Map<UUID, Boolean> alertsSettings;
    private File reportsFile;
    private FileConfiguration reportsConfig;
    
    public ReportManager(AAC plugin) {
        this.plugin = plugin;
        this.reportCooldowns = new ConcurrentHashMap<>();
        this.reports = new CopyOnWriteArrayList<>();
        this.playerMessages = new ConcurrentHashMap<>();
        this.alertsSettings = new ConcurrentHashMap<>();
        
        setupReportsFile();
        loadReports();
    }
    
    private void setupReportsFile() {
        reportsFile = new File(plugin.getDataFolder(), "reports.yml");
        if (!reportsFile.exists()) {
            try {
                reportsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe(".Could not create reports.yml file");
            }
        }
        reportsConfig = YamlConfiguration.loadConfiguration(reportsFile);
    }
    
    private void loadReports() {
        if (reportsConfig.contains("reports")) {
            for (String key : reportsConfig.getConfigurationSection("reports").getKeys(false)) {
                String reporterName = reportsConfig.getString("reports." + key + ".reporter");
                String reportedName = reportsConfig.getString("reports." + key + ".reported");
                String reason = reportsConfig.getString("reports." + key + ".reason");
                String dateTime = reportsConfig.getString("reports." + key + ".datetime");
                List<String> messages = reportsConfig.getStringList("reports." + key + ".messages");
                
                Report report = new Report(
                    UUID.fromString(key),
                    reporterName,
                    reportedName,
                    reason,
                    dateTime,
                    messages
                );
                
                reports.add(report);
            }
        }
    }
    
    public void saveReports() {
        reportsConfig.set("reports", null);
        
        for (Report report : reports) {
            String key = report.getId().toString();
            reportsConfig.set("reports." + key + ".reporter", report.getReporter());
            reportsConfig.set("reports." + key + ".reported", report.getReported());
            reportsConfig.set("reports." + key + ".reason", report.getReason());
            reportsConfig.set("reports." + key + ".datetime", report.getDateTime());
            reportsConfig.set("reports." + key + ".messages", report.getMessages());
        }
        
        try {
            reportsConfig.save(reportsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save reports.yml file");
        }
    }
    
    public boolean canReport(Player player) {
        UUID playerId = player.getUniqueId();
        if (!reportCooldowns.containsKey(playerId)) {
            return true;
        }
        
        long lastReport = reportCooldowns.get(playerId);
        long cooldown = plugin.getConfigManager().getReportCooldown() * 1000L;
        
        return System.currentTimeMillis() - lastReport >= cooldown;
    }
    
    public long getRemainingCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        if (!reportCooldowns.containsKey(playerId)) {
            return 0;
        }
        
        long lastReport = reportCooldowns.get(playerId);
        long cooldown = plugin.getConfigManager().getReportCooldown() * 1000L;
        long remaining = cooldown - (System.currentTimeMillis() - lastReport);
        
        return Math.max(0, remaining / 1000);
    }
    
    public void createReport(Player reporter, String reportedName, String reason) {
        UUID reportId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        String dateTime = now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        
        List<String> messages = playerMessages.getOrDefault(
            getPlayerUUIDByName(reportedName), 
            Arrays.asList(plugin.getLanguageManager().getMessage("gui.reports.no-messages-found"))
        );
        
        Report report = new Report(
            reportId,
            reporter.getName(),
            reportedName,
            reason,
            dateTime,
            new ArrayList<>(messages)
        );
        
        reports.add(report);
        reportCooldowns.put(reporter.getUniqueId(), System.currentTimeMillis());
        
        saveReports();
        sendReportAlert(report);
    }
    
    public void removeReport(UUID reportId) {
        reports.removeIf(report -> report.getId().equals(reportId));
        saveReports();
    }
    
    public List<Report> getReports() {
        return new ArrayList<>(reports);
    }
    
    public Report getReportById(UUID reportId) {
        return reports.stream()
            .filter(report -> report.getId().equals(reportId))
            .findFirst()
            .orElse(null);
    }
    
    public void addPlayerMessage(Player player, String message) {
        UUID playerId = player.getUniqueId();
        List<String> messages = playerMessages.computeIfAbsent(playerId, k -> new ArrayList<>());
        
        messages.add(message);
        
        if (messages.size() > 4) {
            messages.remove(0);
        }
        
        playerMessages.put(playerId, messages);
    }
    
    public boolean isAlertsEnabled(Player player) {
        return alertsSettings.getOrDefault(player.getUniqueId(), false);
    }
    
    public void setAlertsEnabled(Player player, boolean enabled) {
        alertsSettings.put(player.getUniqueId(), enabled);
    }
    
    public void initializeAlertsForJoin(Player player, boolean defaultEnabled) {
        alertsSettings.putIfAbsent(player.getUniqueId(), defaultEnabled);
    }
    
    private UUID getPlayerUUIDByName(String playerName) {
        Player player = plugin.getServer().getPlayer(playerName);
        if (player != null) {
            return player.getUniqueId();
        }
        
        return UUID.randomUUID();
    }
    
    private void sendReportAlert(Report report) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("reporter", report.getReporter());
        placeholders.put("reported", report.getReported());
        placeholders.put("reason", report.getReason());
        
        String prefix = plugin.getLanguageManager().getMessage("prefix");
        String message = plugin.getLanguageManager().getMessage("report-alert", placeholders);
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission("aac.reports.view") && isAlertsEnabled(player)) {
                player.sendMessage(prefix + message);
            }
        }
    }
}
