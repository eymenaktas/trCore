package com.trcore.commands;

import com.trcore.TRCore;
import com.trcore.elo.EloPlayerData;
import com.trcore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EloCommand implements CommandExecutor {

    private final TRCore plugin;

    public EloCommand(TRCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Sadece oyuncular kullanabilir.");
            return true;
        }

        if (plugin.getEloManager() == null) return true;

        if (label.equalsIgnoreCase("rankedhistory")) {
            if (plugin.getRankedMatchManager() == null) return true;
            if (args.length == 0) {
                plugin.getRankedMatchManager().openLatestHistory(p);
                return true;
            }
            try {
                java.util.UUID matchId = java.util.UUID.fromString(args[0]);
                plugin.getRankedMatchManager().openHistoryDetail(p, matchId);
            } catch (Exception ex) {
                p.sendMessage(CC.parse("<red>Geçersiz UUID. Kullanım: /rankedhistory <match-uuid>"));
            }
            return true;
        }

        if (args.length == 0) {
            EloPlayerData data = plugin.getEloManager().getPlayerData(p.getUniqueId());
            if (data == null || !data.hasPlayedRanked()) {
                p.sendMessage(CC.parse("<gray>Henüz Ranked Queue oynamadın.</gray>"));
            } else {
                p.sendMessage(CC.parse("<yellow>Kendi ELO Puanın: <white>" + data.getElo()));
            }
            return true;
        }
        
        if (args[0].equalsIgnoreCase("toggle-setting")) {
            EloPlayerData data = plugin.getEloManager().getPlayerData(p.getUniqueId());
            if (data != null) {
                if (data.getDisplaySetting() == EloPlayerData.DisplaySetting.NUMBER) {
                    data.setDisplaySetting(EloPlayerData.DisplaySetting.RANK);
                } else {
                    data.setDisplaySetting(EloPlayerData.DisplaySetting.NUMBER);
                }
                plugin.getEloManager().savePlayerDataAsync(p.getUniqueId());
            }
            return true;
        }
        
        if (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("ekle")) {
            if (!p.hasPermission("trcore.elo.admin")) {
                p.sendMessage(CC.parse("<red>Bu komutu kullanmak için yetkiniz yok."));
                return true;
            }
            if (args.length < 3) {
                p.sendMessage(CC.parse("<red>Kullanım: /elom " + args[0] + " <isim> <sayı>"));
                return true;
            }

            String targetName = args[1];
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                p.sendMessage(CC.parse("<red>Lütfen geçerli bir sayı girin."));
                return true;
            }

            java.util.UUID uuid = plugin.getEloManager().getUUIDByName(targetName);
            if (uuid == null) {
                org.bukkit.OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(targetName);
                uuid = targetOffline.getUniqueId();
            }
            
            // Veriyi yükle (eğer cache'de yoksa dosyadan yükler)
            plugin.getEloManager().loadPlayer(uuid);
            EloPlayerData data = plugin.getEloManager().getPlayerData(uuid);
            
            if (data == null) {
                p.sendMessage(CC.parse("<red>Oyuncu verisi bulunamadı."));
                return true;
            }

            if (args[0].equalsIgnoreCase("set")) {
                data.setElo(amount);
                p.sendMessage(CC.parse("<green>" + targetName + " ELO puanı <white>" + amount + " <green>olarak ayarlandı."));
            } else {
                data.setElo(data.getElo() + amount);
                p.sendMessage(CC.parse("<green>" + targetName + " ELO puanına <white>" + amount + " <green>eklendi. Yeni: <white>" + data.getElo()));
            }
            
            data.setHasPlayedRanked(true); // Admin müdahalesi sonrası görünür olsun
            plugin.getEloManager().savePlayerDataAsync(uuid);
            return true;
        }

        if (args[0].equalsIgnoreCase("setting") && args.length > 1) {
            String modeStr = args[1].toUpperCase();
            try {
                EloPlayerData.DisplaySetting setting = EloPlayerData.DisplaySetting.valueOf(modeStr);
                EloPlayerData data = plugin.getEloManager().getPlayerData(p.getUniqueId());
                if (data != null) {
                    data.setDisplaySetting(setting);
                    plugin.getEloManager().savePlayerDataAsync(p.getUniqueId());
                    p.sendMessage(CC.parse("<green>ELO görünüm ayarı şu şekilde güncellendi: <white>" + setting.name()));
                }
            } catch (Exception e) {
                p.sendMessage(CC.parse("<red>Geçersiz ayar! Kullanım: /elo setting <NUMBER | RANK>"));
            }
            return true;
        }

        // Diger oyuncunun Elo'su
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            p.sendMessage(CC.parse("<red>Oyuncu bulunamadı."));
            return true;
        }

        EloPlayerData data = plugin.getEloManager().getPlayerData(target.getUniqueId());
        if (data == null || !data.hasPlayedRanked()) {
            p.sendMessage(CC.parse("<gray>Bu oyuncu henüz Ranked Queue oynamadı."));
        } else {
            p.sendMessage(CC.parse("<yellow>" + target.getName() + " ELO: <white>" + data.getElo()));
        }

        return true;
    }
}
