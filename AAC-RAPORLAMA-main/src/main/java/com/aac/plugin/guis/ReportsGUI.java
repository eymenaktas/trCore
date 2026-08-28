package com.aac.plugin.guis;

import com.aac.plugin.AAC;
import com.aac.plugin.models.Report;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ReportsGUI {
    
    private final AAC plugin;
    private final Player viewer;
    private int currentPage;
    private Inventory inventory;
    
    public ReportsGUI(AAC plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.currentPage = 0;
        createInventory();
    }
    
    private void createInventory() {
        String title = plugin.getLanguageManager().getMessage("gui.reports.title");
        inventory = Bukkit.createInventory(null, 54, title);
        
        fillGlassSlots();
        fillReports();
        addNavigationItems();
    }
    
    private void fillGlassSlots() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(plugin.getLanguageManager().getMessage("gui.glass-name"));
        glass.setItemMeta(glassMeta);
        
        List<Integer> glassSlots = plugin.getConfigManager().getReportsGlassSlots();
        for (int slot : glassSlots) {
            inventory.setItem(slot, glass);
        }
    }
    
    private void fillReports() {
        List<Report> reports = plugin.getReportManager().getReports();
        List<Integer> reportSlots = plugin.getConfigManager().getReportsSlots();
        
        int reportsPerPage = reportSlots.size();
        int startIndex = currentPage * reportsPerPage;
        int endIndex = Math.min(startIndex + reportsPerPage, reports.size());
        
        for (int i = 0; i < reportSlots.size(); i++) {
            int slot = reportSlots.get(i);
            int reportIndex = startIndex + i;
            
            if (reportIndex < endIndex) {
                Report report = reports.get(reportIndex);
                ItemStack reportItem = createReportItem(report);
                inventory.setItem(slot, reportItem);
            } else {
                inventory.setItem(slot, null);
            }
        }
    }
    
    private ItemStack createReportItem(Report report) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName("§6" + report.getReported() + " - " + report.getReason());
        
        List<String> lore = new ArrayList<>();
        lore.add(plugin.getLanguageManager().getMessage("gui.reports.reporter") + ": §6" + report.getReporter());
        lore.add(plugin.getLanguageManager().getMessage("gui.reports.reason") + ": §6" + report.getReason());
        lore.add(plugin.getLanguageManager().getMessage("gui.reports.date") + ": §6" + report.getDate());
        lore.add(plugin.getLanguageManager().getMessage("gui.reports.time") + ": §6" + report.getTime());
        lore.add(plugin.getLanguageManager().getMessage("gui.reports.last-messages") + ":");
        
        for (String message : report.getMessages()) {
            lore.add("§f- " + message);
        }
        
        lore.add("");
        lore.add(plugin.getLanguageManager().getMessage("gui.reports.click-to-view"));
        lore.add(plugin.getLanguageManager().getMessage("gui.reports.shift-right-click-to-delete"));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private void addNavigationItems() {
        List<Report> reports = plugin.getReportManager().getReports();
        int reportsPerPage = plugin.getConfigManager().getReportsSlots().size();
        int totalPages = (int) Math.ceil((double) reports.size() / reportsPerPage);
        
        ItemStack previousPage = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = previousPage.getItemMeta();
        prevMeta.setDisplayName(plugin.getLanguageManager().getMessage("gui.report-player.previous-page"));
        previousPage.setItemMeta(prevMeta);
        inventory.setItem(plugin.getConfigManager().getReportsPreviousPageSlot(), previousPage);

        ItemStack nextPage = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = nextPage.getItemMeta();
        nextMeta.setDisplayName(plugin.getLanguageManager().getMessage("gui.report-player.next-page"));
        nextPage.setItemMeta(nextMeta);
        inventory.setItem(plugin.getConfigManager().getReportsNextPageSlot(), nextPage);
        
        ItemStack closeButton = new ItemStack(Material.OAK_SIGN);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName("§cMenüden Çık");
        closeButton.setItemMeta(closeMeta);
        inventory.setItem(49, closeButton);
    }
    
    public void nextPage() {
        List<Report> reports = plugin.getReportManager().getReports();
        int reportsPerPage = plugin.getConfigManager().getReportsSlots().size();
        int totalPages = (int) Math.ceil((double) reports.size() / reportsPerPage);
        
        if (currentPage < totalPages - 1) {
            currentPage++;
            fillReports();
            addNavigationItems();
        }
    }
    
    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            fillReports();
            addNavigationItems();
        }
    }

    public void refresh() {
        fillReports();
        addNavigationItems();
    }
    
    public Report getReportAtSlot(int slot) {
        List<Integer> reportSlots = plugin.getConfigManager().getReportsSlots();
        int slotIndex = reportSlots.indexOf(slot);
        
        if (slotIndex == -1) return null;
        
        List<Report> reports = plugin.getReportManager().getReports();
        int reportsPerPage = reportSlots.size();
        int reportIndex = currentPage * reportsPerPage + slotIndex;
        
        if (reportIndex >= 0 && reportIndex < reports.size()) {
            return reports.get(reportIndex);
        }
        
        return null;
    }
    
    public void open() {
        viewer.openInventory(inventory);
    }
    
    public Inventory getInventory() {
        return inventory;
    }

    public Player getViewer() {
        return viewer;
    }

    public int getCurrentPage() {
        return currentPage;
    }
}