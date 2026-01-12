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

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(CC.parse("<red>Kullanım: /rtpduel <oyuncu>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            duelManager.accept(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("deny")) {
            duelManager.remove(player);
            player.sendMessage(CC.parse("<red>Düello reddedildi."));
            return true;
        }

        if (args[0].equals("internal_accept")) {
            duelManager.openAcceptMenu(player);
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(CC.parse("<red>Oyuncu bulunamadı."));
            return true;
        }

        if (player.equals(target)) {
            player.sendMessage(CC.parse("<red>Kendine düello atamazsın."));
            return true;
        }

        duelManager.invite(player, target);
        return true;
    }
}