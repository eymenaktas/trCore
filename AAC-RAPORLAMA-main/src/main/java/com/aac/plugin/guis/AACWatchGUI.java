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

public class AACWatchGUI {
    
    private final AAC plugin;
    private final Player watcher;
    private int currentPage;
    private Inventory inventory;
    
    public AACWatchGUI(AAC plugin, Player watcher) {
        this.plugin = plugin;
        this.watcher = watcher;
        this.currentPage = 0;
        createInventory();
    }
    
    private void createInventory() {
        String title = plugin.getLanguageManager().getMessage("gui.aacwatch.title");
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
        
        List<Integer> glassSlots = plugin.getConfigManager().getAACWatchGlassSlots();
        for (int slot : glassSlots) {
            inventory.setItem(slot, glass);
        }
    }
    
    private void fillPlayerHeads() {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        onlinePlayers.remove(watcher);
        
        List<Integer> playerSlots = plugin.getConfigManager().getAACWatchPlayerSlots();
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
            plugin.getLanguageManager().getMessage("gui.aacwatch.player-head-lore")
        );
        meta.setLore(lore);
        
        head.setItemMeta(meta);
        return head;
    }
    
    private void addNavigationItems() {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        onlinePlayers.remove(watcher);
        
        int playersPerPage = plugin.getConfigManager().getAACWatchPlayerSlots().size();
        int totalPages = (int) Math.ceil((double) onlinePlayers.size() / playersPerPage);
        
        ItemStack previousPage = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = previousPage.getItemMeta();
        prevMeta.setDisplayName(plugin.getLanguageManager().getMessage("gui.aacwatch.previous-page"));
        previousPage.setItemMeta(prevMeta);
        inventory.setItem(plugin.getConfigManager().getAACWatchPreviousPageSlot(), previousPage);

        ItemStack nextPage = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = nextPage.getItemMeta();
        nextMeta.setDisplayName(plugin.getLanguageManager().getMessage("gui.aacwatch.next-page"));
        nextPage.setItemMeta(nextMeta);
        inventory.setItem(plugin.getConfigManager().getAACWatchNextPageSlot(), nextPage);
        
        ItemStack closeButton = new ItemStack(Material.OAK_SIGN);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName("§cMenüden Çık");
        closeButton.setItemMeta(closeMeta);
        inventory.setItem(49, closeButton);
    }
    
    public void handlePlayerClick(String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null || !target.isOnline()) {
            watcher.sendMessage(plugin.getLanguageManager().getMessage("prefix") + 
                              plugin.getLanguageManager().getMessage("player-not-found"));
            return;
        }
        
        watcher.teleportAsync(target.getLocation()).thenRun(() -> {
            String message = plugin.getLanguageManager().getMessage("aacwatch.teleported")
                .replace("{player}", target.getName());
            watcher.sendMessage(message);
        }).exceptionally(throwable -> {
            watcher.sendMessage(plugin.getLanguageManager().getMessage("prefix") + 
                              "Teleportation failed" + throwable.getMessage());
            return null;
        });
        
        watcher.closeInventory();
    }
    
    public void nextPage() {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        
        int playersPerPage = plugin.getConfigManager().getAACWatchPlayerSlots().size();
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
        watcher.openInventory(inventory);
    }
    
    public Inventory getInventory() {
        return inventory;
    }
    
    public Player getWatcher() {
        return watcher;
    }

    public int getCurrentPage() {
        return currentPage;
    }
}