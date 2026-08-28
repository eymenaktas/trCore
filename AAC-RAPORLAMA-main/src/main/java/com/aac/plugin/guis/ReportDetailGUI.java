package com.aac.plugin.guis;

import com.aac.plugin.AAC;
import com.aac.plugin.models.Report;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReportDetailGUI {
    
    private final AAC plugin;
    private final Player viewer;
    private final Report report;
    private final Inventory inventory;
    private Economy economy;
    
    public ReportDetailGUI(AAC plugin, Player viewer, Report report) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.report = report;
        String title = plugin.getLanguageManager().getMessage("gui.report-detail.title");
        this.inventory = Bukkit.createInventory(null, 45, title);
        setupEconomy();
        fillGlass();
        addHeaderSign();
        addPlayerHead();
        addLeftStatsItem();
        addRightLogsItem();
        addBackButton();
    }
    
    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> registration = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            return;
        }
        economy = registration.getProvider();
    }
    
    private void fillGlass() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, glass);
        }
    }
    
    private void addHeaderSign() {
        ItemStack sign = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = sign.getItemMeta();
        meta.setDisplayName("§fRaporlanan: §6" + report.getReported());
        List<String> lore = new ArrayList<>();
        lore.add("§fSebep: §6" + report.getReason());
        lore.add("§fRapor Eden: §6" + report.getReporter());
        lore.add("§fTarih: §6" + report.getDate());
        lore.add("§fSaat: §6" + report.getTime());
        meta.setLore(lore);
        sign.setItemMeta(meta);
        inventory.setItem(4, sign);
    }
    
    private void addPlayerHead() {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(report.getReported());
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(offlinePlayer);
        meta.setDisplayName("§6" + report.getReported());
        List<String> lore = new ArrayList<>();
        lore.add("§fİsim: §6" + report.getReported());
        lore.add("§fKayıt Tarihi: §6" + getFirstJoinDate(offlinePlayer));
        lore.add("§fOynama Süresi: §6" + getPlayTime(offlinePlayer));
        lore.add("§fOyun Modu: §6" + getGameMode(offlinePlayer));
        lore.add("§fKalan Can: §6" + getHealth(offlinePlayer));
        lore.add("");
        lore.add("§6Son Mesajları:");
        for (String message : report.getMessages()) {
            lore.add("§f- " + message);
        }
        meta.setLore(lore);
        head.setItemMeta(meta);
        inventory.setItem(22, head);
    }
    
    private void addLeftStatsItem() {
        ItemStack table = new ItemStack(Material.FLETCHING_TABLE);
        ItemMeta meta = table.getItemMeta();
        meta.setDisplayName("§6Oyuncu Bilgileri");
        List<String> lore = new ArrayList<>();
        lore.add("§fToplam Rapor Sayısı: §6" + getTotalReportsForPlayer());
        lore.add("§fEkonomi Sistemi: §6" + (economy != null ? "Vault" : "Yok"));
        lore.add("§fParası: §6" + getBalance());
        lore.add("");
        lore.add("§6Ek bilgiler");
        lore.add("§f- Sunucuda kalma süresi §4Çok Yakında Eklenecektir");
        lore.add("§f- Genel oyuncu istatistikleri §4Çok Yakında Eklenecek.");
        meta.setLore(lore);
        table.setItemMeta(meta);
        inventory.setItem(20, table);
    }
    
    private void addRightLogsItem() {
        ItemStack crafting = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta meta = crafting.getItemMeta();
        meta.setDisplayName("§6GrimAC Logları §4YAKINDA");
        List<String> lore = new ArrayList<>();
        lore.add("§fBu bölüm GrimAC logları içindir ");
        lore.add("§fbu sürümde grim entegrasyonu şimdilik yok ");
        lore.add("§fİleride loglar burada listelenecek ");
        meta.setLore(lore);
        crafting.setItemMeta(meta);
        inventory.setItem(24, crafting);
    }
    
    private void addBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.setDisplayName(plugin.getLanguageManager().getMessage("gui.report-detail.back"));
        List<String> lore = new ArrayList<>();
        lore.add(plugin.getLanguageManager().getMessage("gui.report-detail.back-lore"));
        meta.setLore(lore);
        back.setItemMeta(meta);
        inventory.setItem(40, back);
    }
    
    private String getFirstJoinDate(OfflinePlayer player) {
        long firstPlayed = player.getFirstPlayed();
        if (firstPlayed <= 0) {
            return "Bilinmiyor";
        }
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(firstPlayed), ZoneId.systemDefault());
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
    
    private String getPlayTime(OfflinePlayer offlinePlayer) {
        Player player = Bukkit.getPlayerExact(offlinePlayer.getName());
        if (player == null) {
            return "Çevrimdışı";
        }
        int ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        int seconds = ticks / 20;
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        return hours + "s " + minutes + "d";
    }
    
    private String getGameMode(OfflinePlayer offlinePlayer) {
        Player player = Bukkit.getPlayerExact(offlinePlayer.getName());
        if (player == null) {
            return "Çevrimdışı";
        }
        GameMode mode = player.getGameMode();
        if (mode == null) {
            return "Bilinmiyor";
        }
        return mode.name();
    }
    
    private String getHealth(OfflinePlayer offlinePlayer) {
        Player player = Bukkit.getPlayerExact(offlinePlayer.getName());
        if (player == null) {
            return "Çevrimdışı";
        }
        double health = player.getHealth();
        double max = player.getMaxHealth();
        return (int) health + "/" + (int) max;
    }
    
    private int getTotalReportsForPlayer() {
        List<Report> reports = plugin.getReportManager().getReports();
        int count = 0;
        for (Report r : reports) {
            if (r.getReported().equalsIgnoreCase(report.getReported())) {
                count++;
            }
        }
        return count;
    }
    
    private String getBalance() {
        if (economy == null) {
            return "Bilinmiyor";
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(report.getReported());
        double balance = economy.getBalance(offlinePlayer);
        return economy.format(balance);
    }
    
    public void open() {
        viewer.openInventory(inventory);
    }
}
