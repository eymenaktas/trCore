package com.trcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.trcore.TRCore;

/**
 * /banefekt <oyuncu> [kontrol]
 *
 * Ecstacy'nin checks.yml dosyasindaki punish.commands listesinden cagrilir:
 * once olum efekti oynar, ardindan gercek ceza uygulanir. Boylece anticheat'in
 * kendi Java API'sine ihtiyac kalmiyor (o API bu lisansta jar'da bulunmuyor).
 */
public final class BanEfektCommand implements CommandExecutor {

    private final TRCore plugin;

    public BanEfektCommand(TRCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        // Yalnizca konsol ve yetkili: oyuncular kendi kendini banlatamasin.
        if (sender instanceof Player oyuncu && !oyuncu.hasPermission("iocore.banefekt")) {
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("Kullanim: /banefekt <oyuncu> [kontrol]");
            return true;
        }

        Player hedef = Bukkit.getPlayerExact(args[0]);
        if (hedef == null) {
            sender.sendMessage("Oyuncu cevrimici degil: " + args[0]);
            return true;
        }
        String kontrol = args.length > 1 ? args[1] : "bilinmeyen";
        plugin.getBanEfekti().calistir(hedef, kontrol);
        return true;
    }
}
