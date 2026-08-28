package com.trcore.commands;

import com.trcore.TRCore;
import com.trcore.managers.ToggleManager;
import com.trcore.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ToggleCommand implements CommandExecutor {
    private final TRCore plugin;

    public ToggleCommand(TRCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        ToggleManager tm = plugin.getToggleManager();

        if (args.length == 0) {
            player.sendMessage(CC.parse("<red>Kullanım: /toggle <tparequest | tpahere | duelrequest>"));
            return true;
        }

        String sub = args[0].toLowerCase();

        // TPA REQUEST
        if (sub.equals("tparequest") || sub.equals("tpa")) {
            boolean newState = tm.toggleTPA(player.getUniqueId());
            player.sendMessage(CC.get(newState ? "toggle.tpa-enabled" : "toggle.tpa-disabled"));
            return true;
        }

        // TPA HERE REQUEST
        if (sub.equals("tpahererequest") || sub.equals("tpahere")) {
            boolean newState = tm.toggleTPAHere(player.getUniqueId());
            player.sendMessage(CC.get(newState ? "toggle.tpahere-enabled" : "toggle.tpahere-disabled"));
            return true;
        }

        // DUEL REQUEST
        if (sub.equals("duelrequest") || sub.equals("duel")) {
            boolean newState = tm.toggleDuel(player.getUniqueId());
            player.sendMessage(CC.get(newState ? "toggle.duel-enabled" : "toggle.duel-disabled"));
            return true;
        }

        player.sendMessage(CC.parse("<red>Geçersiz argüman."));
        return true;
    }
}

