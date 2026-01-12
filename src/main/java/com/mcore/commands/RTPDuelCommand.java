//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.mcore.commands;

import com.mcore.managers.DuelManager;
import com.mcore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RTPDuelCommand implements CommandExecutor {
    private final DuelManager duelManager;

    public RTPDuelCommand(DuelManager duelManager) {
        this.duelManager = duelManager;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        } else if (args.length == 0) {
            player.sendMessage(CC.parse("<red>Kullanım: /rtpduel <oyuncu>"));
            return true;
        } else if (args[0].equalsIgnoreCase("accept")) {
            this.duelManager.accept(player);
            return true;
        } else if (args[0].equalsIgnoreCase("deny")) {
            this.duelManager.remove(player);
            player.sendMessage(CC.get("tpa.denied", new String[0]));
            return true;
        } else if (args[0].equals("internal_accept")) {
            this.duelManager.openAcceptMenu(player);
            return true;
        } else {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(CC.parse("<red>Oyuncu bulunamadı."));
                return true;
            } else if (player.equals(target)) {
                player.sendMessage(CC.parse("<red>Kendine düello atamazsın."));
                return true;
            } else {
                this.duelManager.invite(player, target);
                return true;
            }
        }
    }
}
