package com.mcore.commands;
import com.mcore.mCore;
import com.mcore.managers.DuelManager;
import com.mcore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RTPDuelCommand implements CommandExecutor {
    private final mCore plugin;
    private final DuelManager duelManager;

    public RTPDuelCommand(mCore p, DuelManager d) { plugin = p; duelManager = d; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length > 0 && args[0].equals("internal_accept")) {
            duelManager.openAcceptMenu(player);
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(CC.parse("<red>Kullanım: /rtpduel <oyuncu>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target != null) {
            if (target.equals(player)) {
                player.sendMessage(CC.parse("<red>Kendinle düello atamazsın."));
                return true;
            }
            duelManager.invite(player, target);
        } else {
            player.sendMessage(CC.parse("<red>Oyuncu bulunamadı."));
        }
        return true;
    }
}