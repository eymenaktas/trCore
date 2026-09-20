package com.trcore.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.trcore.TRCore;
import com.trcore.utils.CC;

/**
 * /unpunish <oyuncu> — ban ve susturmayi birlikte kaldirir.
 *
 * Yetkiliye LiteBans'in kendi komutlarini acmadan calisir: komutlar
 * konsoldan gidiyor, kaldiran kisi --sender ile duyuruda gorunuyor.
 */
public final class UnpunishCommand implements CommandExecutor, TabCompleter {

    private final TRCore plugin;

    public UnpunishCommand(TRCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("iocore.unpunish")) {
            sender.sendMessage(CC.parse(plugin.getCezaManager().mesaj("izin-yok", "<red>Bunun icin yetkin yok.")));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(CC.parse(plugin.getCezaManager()
                    .mesaj("kaldir-kullanim", "<gray>Kullanım: <white>/unpunish <oyuncu>")));
            return true;
        }

        String ad = args[0];
        boolean sessiz = args.length > 1 && args[1].equalsIgnoreCase("-s");
        plugin.getCezaManager().kaldir(sender, ad, sessiz);
        sender.sendMessage(CC.parse(plugin.getCezaManager()
                .mesaj("kaldirildi", "<#8ccefe>✔ <white>%player% <gray>üzerindeki ban ve susturma kaldırıldı.")
                .replace("%player%", ad)));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) return List.of();
        String on = args[0].toLowerCase(Locale.ROOT);
        List<String> cikti = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase(Locale.ROOT).startsWith(on)) cikti.add(p.getName());
        }
        return cikti;
    }
}
