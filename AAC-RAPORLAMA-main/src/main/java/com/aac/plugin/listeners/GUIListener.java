package com.aac.plugin.listeners;

import com.aac.plugin.AAC;
import com.aac.plugin.guis.AACWatchGUI;
import com.aac.plugin.guis.ReportDetailGUI;
import com.aac.plugin.guis.ReportDeleteConfirmGUI;
import com.aac.plugin.guis.ReportPlayerGUI;
import com.aac.plugin.guis.ReportReasonsGUI;
import com.aac.plugin.guis.ReportsGUI;
import com.aac.plugin.models.Report;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIListener implements Listener {
    
    private final AAC plugin;
    private final Map<UUID, ReportPlayerGUI> reportPlayerGUIs = new HashMap<>();
    private final Map<UUID, ReportReasonsGUI> reportReasonsGUIs = new HashMap<>();
    private final Map<UUID, ReportsGUI> reportsGUIs = new HashMap<>();
    private final Map<UUID, ReportDetailGUI> reportDetailGUIs = new HashMap<>();
    private final Map<UUID, ReportDeleteConfirmGUI> reportDeleteConfirmGUIs = new HashMap<>();
    private final Map<UUID, AACWatchGUI> aacWatchGUIs = new HashMap<>();
    
    public GUIListener(AAC plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (title.equals(plugin.getLanguageManager().getMessage("gui.report-player.title"))) {
            handleReportPlayerGUI(event, player);
        } else if (title.equals(plugin.getLanguageManager().getMessage("gui.report-reasons.title"))) {
            handleReportReasonsGUI(event, player);
        } else if (title.equals(plugin.getLanguageManager().getMessage("gui.reports.title"))) {
            handleReportsGUI(event, player);
        } else if (title.equals(plugin.getLanguageManager().getMessage("gui.report-detail.title"))) {
            handleReportDetailGUI(event, player);
        } else if (title.equals("§cRapor Silme Onayı")) {
            handleReportDeleteConfirmGUI(event, player);
        } else if (title.equals(plugin.getLanguageManager().getMessage("gui.aacwatch.title"))) {
            handleAACWatchGUI(event, player);
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean defaultEnabled = plugin.getConfigManager().isAlertsAutoEnableOnJoin();
        plugin.getReportManager().initializeAlertsForJoin(player, defaultEnabled);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();
        String title = event.getView().getTitle();
        
        if (title.equals(plugin.getLanguageManager().getMessage("gui.report-player.title"))) {
            reportPlayerGUIs.remove(playerId);
        } else if (title.equals(plugin.getLanguageManager().getMessage("gui.report-reasons.title"))) {
            reportReasonsGUIs.remove(playerId);
        } else if (title.equals(plugin.getLanguageManager().getMessage("gui.reports.title"))) {
            reportsGUIs.remove(playerId);
        } else if (title.equals(plugin.getLanguageManager().getMessage("gui.report-detail.title"))) {
            reportDetailGUIs.remove(playerId);
        } else if (title.equals("§cRapor Silme Onayı")) {
            reportDeleteConfirmGUIs.remove(playerId);
        } else if (title.equals(plugin.getLanguageManager().getMessage("gui.aacwatch.title"))) {
            aacWatchGUIs.remove(playerId);
        } else {
            reportPlayerGUIs.remove(playerId);
            reportReasonsGUIs.remove(playerId);
            reportsGUIs.remove(playerId);
            reportDetailGUIs.remove(playerId);
            reportDeleteConfirmGUIs.remove(playerId);
            aacWatchGUIs.remove(playerId);
        }
    }
    
    private void handleReportPlayerGUI(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;
        
        int slot = event.getSlot();
        ReportPlayerGUI gui = reportPlayerGUIs.get(player.getUniqueId());
        
        if (gui == null) {
            gui = new ReportPlayerGUI(plugin, player);
            reportPlayerGUIs.put(player.getUniqueId(), gui);
        } else {
            
        }
        
        if (slot == plugin.getConfigManager().getNextPageSlot()) {
            gui.nextPage();
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        } else if (slot == plugin.getConfigManager().getPreviousPageSlot()) {
            gui.previousPage();
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        } else if (slot == 49) {
            player.closeInventory();
        } else if (clickedItem.getItemMeta() instanceof SkullMeta) {
            SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
            if (meta.getOwningPlayer() != null) {
                String targetName = meta.getOwningPlayer().getName();
                
                if (targetName.equals(player.getName())) {
                    player.sendMessage(plugin.getLanguageManager().getMessage("prefix") + 
                                     plugin.getLanguageManager().getMessage("cannot-report-self"));
                    return;
                }
                
                ReportReasonsGUI reasonsGUI = new ReportReasonsGUI(plugin, player, targetName);
                reportReasonsGUIs.put(player.getUniqueId(), reasonsGUI);
                reasonsGUI.open();
            }
        }
    }
    
    private void handleReportReasonsGUI(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;
        
        int slot = event.getSlot();
        ReportReasonsGUI gui = reportReasonsGUIs.get(player.getUniqueId());
        
        if (gui == null) {
            plugin.getLogger().warning("GUIListener ReportReasonsGUI not found for player....." + player.getName());
            return;
        }
        
        if (slot == 31) {
            ReportPlayerGUI reportPlayerGUI = reportPlayerGUIs.get(player.getUniqueId());
            if (reportPlayerGUI == null) {
                reportPlayerGUI = new ReportPlayerGUI(plugin, player);
                reportPlayerGUIs.put(player.getUniqueId(), reportPlayerGUI);
            }
            reportPlayerGUI.open();
        } else {
            gui.handleReasonClick(slot);
        }
    }
    
    private void handleReportsGUI(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;
        if (event.getClickedInventory() == null || !event.getView().getTopInventory().equals(event.getClickedInventory())) return;
        
        int slot = event.getSlot();
        ReportsGUI gui = reportsGUIs.get(player.getUniqueId());
        
        if (gui == null) {
            gui = new ReportsGUI(plugin, player);
            reportsGUIs.put(player.getUniqueId(), gui);
        }
        
        if (slot == plugin.getConfigManager().getReportsNextPageSlot()) {
            gui.nextPage();
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        } else if (slot == plugin.getConfigManager().getReportsPreviousPageSlot()) {
            gui.previousPage();
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        } else if (slot == 49) {
            player.closeInventory();
        } else {
            Report report = gui.getReportAtSlot(slot);
            
            if (report != null) {
                ClickType click = event.getClick();
                if (click == ClickType.SHIFT_RIGHT) {
                    if (player.hasPermission("aac.reports.manage")) {
                        if (plugin.getConfigManager().isDeleteReportConfirmGuiEnabled()) {
                            ReportDeleteConfirmGUI confirmGUI = new ReportDeleteConfirmGUI(plugin, player, report);
                            reportDeleteConfirmGUIs.put(player.getUniqueId(), confirmGUI);
                            confirmGUI.open();
                        } else {
                            plugin.getReportManager().removeReport(report.getId());
                            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                            player.sendMessage(plugin.getLanguageManager().getMessage("prefix") + "§aRapor başarıyla silindi.");
                            
                            gui.refresh();
                        }
                    }
                } else {
                    ReportDetailGUI detailGUI = new ReportDetailGUI(plugin, player, report);
                    reportDetailGUIs.put(player.getUniqueId(), detailGUI);
                    detailGUI.open();
                }
            }
        }
    }
    
    private void handleReportDetailGUI(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;
        if (event.getClickedInventory() == null || !event.getView().getTopInventory().equals(event.getClickedInventory())) return;
        
        int slot = event.getSlot();
        
        if (slot == 40) {
            ReportsGUI reportsGUI = reportsGUIs.get(player.getUniqueId());
            if (reportsGUI == null) {
                reportsGUI = new ReportsGUI(plugin, player);
            }
            reportsGUIs.put(player.getUniqueId(), reportsGUI);
            reportsGUI.open();
        }
    }
    
    private void handleReportDeleteConfirmGUI(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;
        if (event.getClickedInventory() == null || !event.getView().getTopInventory().equals(event.getClickedInventory())) return;
        
        int slot = event.getSlot();
        ReportDeleteConfirmGUI gui = reportDeleteConfirmGUIs.get(player.getUniqueId());
        if (gui == null) {
            ReportsGUI reportsGUI = new ReportsGUI(plugin, player);
            reportsGUIs.put(player.getUniqueId(), reportsGUI);
            reportsGUI.open();
            return;
        }
        
        if (slot == 11) {
            ReportsGUI reportsGUI = reportsGUIs.get(player.getUniqueId());
            if (reportsGUI == null) {
                reportsGUI = new ReportsGUI(plugin, player);
            }
            reportsGUIs.put(player.getUniqueId(), reportsGUI);
            reportsGUI.open();
        } else if (slot == 15) {
            Report report = gui.getReport();
            if (report != null) {
                plugin.getReportManager().removeReport(report.getId());
            }
            ReportsGUI reportsGUI = reportsGUIs.get(player.getUniqueId());
            if (reportsGUI == null) {
                reportsGUI = new ReportsGUI(plugin, player);
            } else {
                reportsGUI.refresh();
            }
            reportsGUIs.put(player.getUniqueId(), reportsGUI);
            reportsGUI.open();
        }
    }
    
    private void handleAACWatchGUI(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;
        
        int slot = event.getSlot();
        AACWatchGUI gui = aacWatchGUIs.get(player.getUniqueId());
        
        if (gui == null) {
            gui = new AACWatchGUI(plugin, player);
            aacWatchGUIs.put(player.getUniqueId(), gui);
        } else {
            
        }
        
        if (slot == plugin.getConfigManager().getAACWatchNextPageSlot()) {
            gui.nextPage();
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        } else if (slot == plugin.getConfigManager().getAACWatchPreviousPageSlot()) {
            gui.previousPage();
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
        } else if (slot == 49) {
            player.closeInventory();
        } else if (clickedItem.getItemMeta() instanceof SkullMeta) {
            SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
            if (meta.getOwningPlayer() != null) {
                String targetName = meta.getOwningPlayer().getName();
                gui.handlePlayerClick(targetName);
            }
        }
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        plugin.getReportManager().addPlayerMessage(player, message);
    }
    
    public void registerReportPlayerGUI(Player player, ReportPlayerGUI gui) {
        reportPlayerGUIs.put(player.getUniqueId(), gui);
    }
    
    public void registerReportReasonsGUI(Player player, ReportReasonsGUI gui) {
        reportReasonsGUIs.put(player.getUniqueId(), gui);
    }
    
    public void registerReportsGUI(Player player, ReportsGUI gui) {
        reportsGUIs.put(player.getUniqueId(), gui);
    }
    
    public void registerAACWatchGUI(Player player, AACWatchGUI gui) {
        aacWatchGUIs.put(player.getUniqueId(), gui);
    }
}
