package com.aac.plugin.listeners;

import com.aac.plugin.AAC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public class ChatListener implements Listener {
    
    private final AAC plugin;
    private final Map<UUID, String> pendingReports = new ConcurrentHashMap<>();
    
    public ChatListener(AAC plugin) {
        this.plugin = plugin;
    }
    
    public void addPendingReport(Player reporter, String reportedName) {
        pendingReports.put(reporter.getUniqueId(), reportedName);
        reporter.sendMessage(plugin.getLanguageManager().getMessage("prefix") + 
                           plugin.getLanguageManager().getMessage("enter-report-reason")
                           .replace("{player}", reportedName));
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        if (pendingReports.containsKey(playerId)) {
            event.setCancelled(true);
            
            String reportedName = pendingReports.remove(playerId);
            String reason = event.getMessage();
            
            if (reason.equalsIgnoreCase("iptal") || reason.equalsIgnoreCase("cancel")) {
                player.sendMessage(plugin.getLanguageManager().getMessage("prefix") + 
                                 plugin.getLanguageManager().getMessage("report-cancelled"));
                return;
            }
            
            Runnable task = () -> {
                plugin.getReportManager().createReport(player, reportedName, reason);
                player.sendMessage(plugin.getLanguageManager().getMessage("prefix") + 
                                 plugin.getLanguageManager().getMessage("report-sent"));
            };
            
            try {
                // Try Folia/Paper method first
                player.getScheduler().run(plugin, t -> task.run(), null);
            } catch (NoSuchMethodError | Exception e) {
                // Fallback to Bukkit scheduler
                plugin.getServer().getScheduler().runTask(plugin, task);
            }
        }
    }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingReports.remove(event.getPlayer().getUniqueId());
    }
}
