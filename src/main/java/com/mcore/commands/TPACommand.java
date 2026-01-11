package com.mcore.commands;

import com.mcore.mCore;
import com.mcore.managers.TPAManager;
import com.mcore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TPACommand implements CommandExecutor {
    private final mCore plugin;
    private final TPAManager tpaManager;

    public TPACommand(mCore plugin, TPAManager tpaManager) {
        this.plugin = plugin;
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;
        String commandName = cmd.getName().toLowerCase();

        // TPA EVENT
        if (commandName.equals("tpaevent")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("join")) {
                tpaManager.joinEvent(player); // Artık hata vermez
                return true;
            }
            if (player.hasPermission("mcore.tpaevent")) {
                if (args.length > 0 && args[0].equalsIgnoreCase("stop")) {
                    tpaManager.stopEvent(false); // Artık hata vermez
                    player.sendMessage(CC.parse("<red>Etkinlik durduruldu."));
                } else {
                    tpaManager.startEvent(player);
                }
            } else {
                player.sendMessage(CC.get("no-perm"));
            }
            return true;
        }

        // TPA CANCEL
        if (commandName.equals("tpacancel")) {
            tpaManager.cancel(player);
            return true;
        }

        // TPA & TPAHERE
        if (args.length == 0) {
            player.sendMessage(CC.get("tpa.usage"));
            return true;
        }

        // Internal Accept (Menüden gelen komut)
        if (args[0].equals("internal_accept") && args.length > 1) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null && target.isOnline()) {
                tpaManager.openAcceptMenu(player);
            }
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(CC.parse("<red>Oyuncu bulunamadı."));
            return true;
        }

        if (player.equals(target)) {
            player.sendMessage(CC.parse("<red>Kendine istek atamazsın."));
            return true;
        }

        tpaManager.send(player, target, commandName.equals("tpa") ? "tpa" : "tpahere");
        return true;
    }
}