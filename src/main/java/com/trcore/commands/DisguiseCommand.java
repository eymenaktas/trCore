package com.trcore.commands;

import com.trcore.TRCore;
import com.trcore.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DisguiseCommand implements CommandExecutor {

    private final TRCore plugin;

    public DisguiseCommand(TRCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Sadece oyuncular kullanabilir.");
            return true;
        }

        if (!p.hasPermission("trcore.admin")) {
            p.sendMessage(CC.parse("<red>Bunun için yetkiniz yok."));
            return true;
        }

        if (args.length == 0) {
            if (plugin.getDisguiseManager().isDisguised(p)) {
                plugin.getDisguiseManager().setDisguise(p, null);
                p.sendMessage(CC.parse("<green>Disguise modundan çıktınız."));
            } else {
                p.sendMessage(CC.parse("<red>Kullanım: /disguise <isim>"));
            }
            return true;
        }

        String disguiseName = args[0];
        plugin.getDisguiseManager().setDisguise(p, disguiseName);
        p.sendMessage(CC.parse("<green>Artık <white>" + disguiseName + " <green>olarak görüneceksiniz."));

        return true;
    }
}
