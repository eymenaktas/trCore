package com.aac.plugin.guis;

import com.aac.plugin.AAC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class ReportReasonsGUI {
    
    private final AAC plugin;
    private final Player reporter;
    private final String reportedPlayer;
    private Inventory inventory;
    
    private String selectedReason = null;
    private int selectedSlot = -1;
    private final int[] reasonSlots = {16, 12, 13, 14, 10};
    private final String[] reasonKeys = {"hacking", "swearing", "inappropriate-name", "spam", "griefing"};
    
    public ReportReasonsGUI(AAC plugin, Player reporter, String reportedPlayer) {
        this.plugin = plugin;
        this.reporter = reporter;
        this.reportedPlayer = reportedPlayer;
        createInventory();
    }
    
    private void createInventory() {
        String title = plugin.getLanguageManager().getMessage("gui.report-reasons.title");
        inventory = Bukkit.createInventory(null, 36, title);
        
        fillGlassSlots();
        addReasonItems();
    }
    
    private void fillGlassSlots() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(plugin.getLanguageManager().getMessage("gui.glass-name"));
        glass.setItemMeta(glassMeta);
        
        List<Integer> glassSlots = plugin.getConfigManager().getReportReasonsGlassSlots();
        for (int slot : glassSlots) {
            if (slot < 36) {
                inventory.setItem(slot, glass);
            }
        }
    }
    
    private void addReasonItems() {
        for (int i = 0; i < reasonSlots.length; i++) {
            String reasonKey = reasonKeys[i];
            inventory.setItem(reasonSlots[i], createReasonItem(Material.PAPER, 
                plugin.getLanguageManager().getMessage("gui.report-reasons." + reasonKey),
                plugin.getLanguageManager().getMessage("gui.report-reasons." + reasonKey + "-lore"),
                reasonKey, false));
        }
        
        inventory.setItem(22, createConfirmButton(false));
        
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(plugin.getLanguageManager().getMessage("gui.back-button"));
        backButton.setItemMeta(backMeta);
        inventory.setItem(31, backButton);
    }
    
    private ItemStack createReasonItem(Material material, String name, String lore, String reasonKey, boolean selected) {
        ItemStack item = new ItemStack(selected ? Material.LIME_CONCRETE : material);
        ItemMeta meta = item.getItemMeta();
        
        if (selected) {
            meta.setDisplayName("§a✔ " + name);
            meta.setLore(Arrays.asList(lore, "", "§a» " + plugin.getLanguageManager().getMessage("gui.selected")));
        } else {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore, "", "§7» " + plugin.getLanguageManager().getMessage("gui.click-to-select")));
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createConfirmButton(boolean enabled) {
        ItemStack item = new ItemStack(enabled ? Material.GREEN_CONCRETE : Material.GRAY_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        
        if (enabled) {
            meta.setDisplayName("§a" + plugin.getLanguageManager().getMessage("gui.send-report"));
            meta.setLore(Arrays.asList("§7" + plugin.getLanguageManager().getMessage("gui.send-report-desc"), "", "§a» " + plugin.getLanguageManager().getMessage("gui.click")));
        } else {
            meta.setDisplayName("§7" + plugin.getLanguageManager().getMessage("gui.send-report"));
            meta.setLore(Arrays.asList("§7" + plugin.getLanguageManager().getMessage("gui.select-reason-first"), "", "§c» " + plugin.getLanguageManager().getMessage("gui.disabled")));
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    public void handleReasonClick(int slot) {
        if (slot == 22) {
            if (selectedReason != null) {
                if (!plugin.getReportManager().canReport(reporter)) {
                    reporter.closeInventory();
                    reporter.sendMessage(plugin.getLanguageManager().getMessage("prefix") + 
                                       plugin.getLanguageManager().getMessage("report-cooldown"));
                    return;
                }
                
                plugin.getReportManager().createReport(reporter, reportedPlayer, selectedReason);
                reporter.closeInventory();
                reporter.sendMessage(plugin.getLanguageManager().getMessage("prefix") + 
                                   plugin.getLanguageManager().getMessage("report-sent"));
            }
            return;
        }
        
        for (int i = 0; i < reasonSlots.length; i++) {
            if (reasonSlots[i] == slot) {
                selectReason(i);
                return;
            }
        }
    }
    
    private void selectReason(int reasonIndex) {
        selectedSlot = reasonSlots[reasonIndex];
        selectedReason = plugin.getLanguageManager().getMessage("gui.report-reasons." + reasonKeys[reasonIndex]);
        
        updateReasonItems();
        
        inventory.setItem(22, createConfirmButton(true));
        
        reporter.playSound(reporter.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }
    
    private void updateReasonItems() {
        for (int i = 0; i < reasonSlots.length; i++) {
            String reasonKey = reasonKeys[i];
            boolean isSelected = (reasonSlots[i] == selectedSlot);
            
            inventory.setItem(reasonSlots[i], createReasonItem(Material.PAPER, 
                plugin.getLanguageManager().getMessage("gui.report-reasons." + reasonKey),
                plugin.getLanguageManager().getMessage("gui.report-reasons." + reasonKey + "-lore"),
                reasonKey, isSelected));
        }
    }
    
    public void open() {
        reporter.openInventory(inventory);
    }
    
    public Inventory getInventory() {
        return inventory;
    }
    
    public Player getReporter() {
        return reporter;
    }
    
    public String getReportedPlayer() {
        return reportedPlayer;
    }
}