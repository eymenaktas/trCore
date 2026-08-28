package com.trcore.commands;

import com.trcore.TRCore;
import com.trcore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;

public class TPACommand implements CommandExecutor {
    private final TRCore plugin;

    public TPACommand(TRCore plugin) {
        this.plugin = plugin;
    }

    private boolean isVanished(Player player) {
        if (player.hasMetadata("vanished")) {
            for (MetadataValue meta : player.getMetadata("vanished")) {
                if (meta.asBoolean()) return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        String cmdName = command.getName().toLowerCase();
        if (cmdName.equals("tpaccept")) {
            String targetName = args.length > 0 ? args[0] : null;
            plugin.getTpaManager().openAcceptMenu(player, targetName);
            return true;
        }
        if (cmdName.equals("tpadeny")) {
            String targetName = args.length > 0 ? args[0] : null;
            plugin.getTpaManager().deny(player, targetName);
            return true;
        }

        if (args.length > 0) {
            String subCmd = args[0].toLowerCase();
            switch (subCmd) {
                case "confirm_send":
                    plugin.getTpaManager().confirmSend(player);
                    return true;
                case "cancel_send":
                    plugin.getTpaManager().cancelSend(player);
                    return true;
                case "accept":
                    plugin.getTpaManager().accept(player);
                    return true;
                case "deny":
                    plugin.getTpaManager().deny(player, null);
                    return true;
            }

            // TPA Event Yönetimi (Eğer komut tpaevent ise)
            if (command.getName().equalsIgnoreCase("tpaevent")) {
                String noPermMsg = plugin.getConfigManager().getMessages().getString("no-perm", "<red>Buna Yetkin Bulunmamaktadır.");

                switch (subCmd) {
                    case "start":
                        if (!player.hasPermission("trcore.tpaevent.host")) { player.sendMessage(CC.parse(noPermMsg)); return true; }
                        plugin.getTpaManager().startEvent(player);
                        return true;
                    case "stop":
                        if (!player.hasPermission("trcore.tpaevent.host")) { player.sendMessage(CC.parse(noPermMsg)); return true; }
                        plugin.getTpaManager().stopEvent(player, false);
                        return true;
                    case "join":
                        if (!player.hasPermission("trcore.tpaevent.join")) { player.sendMessage(CC.parse(noPermMsg)); return true; }
                        String hostName = args.length > 1 ? args[1] : null;
                        plugin.getTpaManager().joinEvent(player, hostName);
                        return true;
                }
            }
        }

        // --- EÄER TPAEVENT YAZIP BOÅ BIRAKTIYSA ---
        if (command.getName().equalsIgnoreCase("tpaevent")) {
            if (player.hasPermission("trcore.tpaevent.host")) {
                player.sendMessage(CC.parse("<yellow>Kullanım: /tpaevent <start|stop|join>"));
            } else if (player.hasPermission("trcore.tpaevent.join")) {
                player.sendMessage(CC.parse("<yellow>Kullanım: /tpaevent join [oyuncu]"));
            } else {
                player.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("no-perm", "<red>Buna Yetkin Bulunmamaktadır.")));
            }
            return true;
        }

        // --- İPTAL ETME ---
        if (command.getName().equalsIgnoreCase("tpacancel")) {
            plugin.getTpaManager().cancel(player);
            return true;
        }

        // --- NORMAL TPA ve HEDEF KONTROLÜ ---
        if (args.length == 0) {
            player.sendMessage(CC.parse("<red>Kullanım: /" + label + " <oyuncu>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null || !player.canSee(target) || isVanished(target)) {
            player.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("tpa.player-not-found", "<red>Oyuncu bulunamadı veya aktif değil.")));
            return true;
        }

        if (player.equals(target)) {
            player.sendMessage(CC.parse("<red>Kendine istek gönderemezsin!"));
            return true;
        }

        String type = command.getName().equalsIgnoreCase("tpahere") ? "tpahere" : "tpa";
        plugin.getTpaManager().openSendMenu(player, target, type);

        return true;
    }
}

