package com.aac.plugin.guis;

import com.aac.plugin.AAC;
import com.aac.plugin.models.Report;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class ReportDeleteConfirmGUI {
    
    private final AAC plugin;
    private final Player viewer;
    private final Report report;
    private final Inventory inventory;
    
    public ReportDeleteConfirmGUI(AAC plugin, Player viewer, Report report) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.report = report;
        this.inventory = Bukkit.createInventory(null, 27, "§cRapor Silme Onayı");
        addPlayerHead();
        addConfirmButton();
        addCancelButton();
    }
    
    private void addPlayerHead() {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(report.getReported());
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(offlinePlayer);
        meta.setDisplayName("§6" + report.getReported());
        List<String> lore = new ArrayList<>();
        lore.add("§fBu oyuncunun raporunu silmek üzeresin");
        lore.add("§fRaporu silmek için tıklayın");
        meta.setLore(lore);
        head.setItemMeta(meta);
        inventory.setItem(13, head);
    }
    
    private void addConfirmButton() {
        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = confirm.getItemMeta();
        meta.setDisplayName("§aOnaylıyorum");
        List<String> lore = new ArrayList<>();
        lore.add("§fBu raporu kalıcı olarak sil");
        meta.setLore(lore);
        confirm.setItemMeta(meta);
        inventory.setItem(15, confirm);
    }
    
    private void addCancelButton() {
        ItemStack cancel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = cancel.getItemMeta();
        meta.setDisplayName("§cİptal Et");
        List<String> lore = new ArrayList<>();
        lore.add("§fİşlemi iptal et ve geri dön");
        meta.setLore(lore);
        cancel.setItemMeta(meta);
        inventory.setItem(11, cancel);
    }
    
    public void open() {
        viewer.openInventory(inventory);
    }
    
    public Report getReport() {
        return report;
    }
    
    public Inventory getInventory() {
        return inventory;
    }
    
    public Player getViewer() {
        return viewer;
    }
}
