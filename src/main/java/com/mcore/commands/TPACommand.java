//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

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

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return false;
        } else {
            String commandName = cmd.getName().toLowerCase();
            if (commandName.equals("tpaevent")) {
                if (args.length > 0 && args[0].equalsIgnoreCase("join")) {
                    if (args.length > 1) {
                        this.tpaManager.joinEvent(player, args[1]);
                    } else {
                        player.sendMessage(CC.parse("<red>Hata: Etkinlik sahibi belirtilmedi."));
                    }

                    return true;
                } else {
                    if (player.hasPermission("mcore.tpaevent")) {
                        if (args.length > 0 && args[0].equalsIgnoreCase("stop")) {
                            this.tpaManager.stopEvent(player, false);
                            player.sendMessage(CC.parse("<red>Kendi etkinliğin durduruldu."));
                        } else {
                            this.tpaManager.startEvent(player);
                        }
                    } else {
                        player.sendMessage(CC.get("no-perm", new String[0]));
                    }

                    return true;
                }
            } else if (commandName.equals("tpacancel")) {
                this.tpaManager.cancel(player);
                return true;
            } else if (args.length == 0) {
                player.sendMessage(CC.get("tpa.usage", new String[0]));
                return true;
            } else if (args[0].equals("internal_accept") && args.length > 1) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null && target.isOnline()) {
                    this.tpaManager.openAcceptMenu(player);
                }

                return true;
            } else if (args[0].equals("interact_accept")) {
                this.tpaManager.accept(player);
                return true;
            } else if (args[0].equals("interact_deny")) {
                this.tpaManager.deny(player);
                return true;
            } else {
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    player.sendMessage(CC.parse("<red>Oyuncu bulunamadı."));
                    return true;
                } else if (player.equals(target)) {
                    player.sendMessage(CC.parse("<red>Kendine istek atamazsın."));
                    return true;
                } else {
                    this.tpaManager.send(player, target, commandName.equals("tpa") ? "tpa" : "tpahere");
                    return true;
                }
            }
        }
    }
}
