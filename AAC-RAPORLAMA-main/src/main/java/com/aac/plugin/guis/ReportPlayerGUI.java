package com.aac.plugin.guis;

import com.aac.plugin.AAC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReportPlayerGUI {
    
    private final AAC plugin;
    private final Player viewer;
    private int currentPage;
    private Inventory inventory;
    
    public ReportPlayerGUI(AAC plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.currentPage = 0;
        createInventory();
    }
    
    private void createInventory() {
        String title = plugin.getLanguageManager().getMessage("gui.report-player.title");
        inventory = Bukkit.createInventory(null, 54, title);
        
        fillGlassSlots();
        fillPlayerHeads();
        addNavigationItems();
    }
    
    private void fillGlassSlots() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(plugin.getLanguageManager().getMessage("gui.glass-name"));
        glass.setItemMeta(glassMeta);
        
        List<Integer> glassSlots = plugin.getConfigManager().getReportPlayerGlassSlots();
        for (int slot : glassSlots) {
            inventory.setItem(slot, glass);
        }
    }
    
    private void fillPlayerHeads() {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        onlinePlayers.remove(viewer);
        
        List<Integer> playerSlots = plugin.getConfigManager().getReportPlayerSlots();
        int playersPerPage = playerSlots.size();
        int startIndex = currentPage * playersPerPage;
        int endIndex = Math.min(startIndex + playersPerPage, onlinePlayers.size());
        
        for (int i = 0; i < playerSlots.size(); i++) {
            int slot = playerSlots.get(i);
            int playerIndex = startIndex + i;
            
            if (playerIndex < endIndex) {
                Player target = onlinePlayers.get(playerIndex);
                ItemStack playerHead = createPlayerHead(target);
                inventory.setItem(slot, playerHead);
            } else {
                inventory.setItem(slot, null);
            }
        }
    }
    
    private ItemStack createPlayerHead(Player target) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        meta.setOwningPlayer(target);
        meta.setDisplayName("§6" + target.getName());
        
        List<String> lore = Arrays.asList(
            plugin.getLanguageManager().getMessage("gui.report-player.player-head-lore")
        );
        meta.setLore(lore);
        
        head.setItemMeta(meta);
        return head;
    }
    
    private void addNavigationItems() {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        onlinePlayers.remove(viewer);
        
        int playersPerPage = plugin.getConfigManager().getReportPlayerSlots().size();
        int totalPages = (int) Math.ceil((double) onlinePlayers.size() / playersPerPage);
        
        ItemStack previousPage = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = previousPage.getItemMeta();
        prevMeta.setDisplayName(plugin.getLanguageManager().getMessage("gui.report-player.previous-page"));
        previousPage.setItemMeta(prevMeta);
        inventory.setItem(plugin.getConfigManager().getPreviousPageSlot(), previousPage);

        ItemStack nextPage = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = nextPage.getItemMeta();
        nextMeta.setDisplayName(plugin.getLanguageManager().getMessage("gui.report-player.next-page"));
        nextPage.setItemMeta(nextMeta);
        inventory.setItem(plugin.getConfigManager().getNextPageSlot(), nextPage);
        
        ItemStack closeButton = new ItemStack(Material.OAK_SIGN);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName("§cMenüden Çık");
        closeButton.setItemMeta(closeMeta);
        inventory.setItem(49, closeButton);
    }
    
    public void nextPage() {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        onlinePlayers.remove(viewer);
        
        int playersPerPage = plugin.getConfigManager().getReportPlayerSlots().size();
        int totalPages = (int) Math.ceil((double) onlinePlayers.size() / playersPerPage);
        
        if (currentPage < totalPages - 1) {
            currentPage++;
            fillPlayerHeads();
            addNavigationItems();
        }
    }
    
    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            fillPlayerHeads();
            addNavigationItems();
        }
    }
    
    public void open() {
        viewer.openInventory(inventory);
    }
    
    public Inventory getInventory() {
        return inventory;
    }

    public int getCurrentPage() {
        return currentPage;
    }
    
    public Player getViewer() {
        return viewer;
    }
}