package com.trcore.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.trcore.TRCore;
import com.trcore.managers.CezaManager;
import com.trcore.utils.CC;

/**
 * /iopunish <oyuncu> <sebep> [kademe]
 *
 * Sebep cezalar.yml'den gelir; kademeyi oyuncunun gecmisi belirler.
 */
public final class IoPunishCommand implements CommandExecutor, TabCompleter {

    private final TRCore plugin;

    public IoPunishCommand(TRCore plugin) {
        this.plugin = plugin;
    }

    private void yolla(CommandSender kime, String anahtar, String varsayilan, String... degisimler) {
        String metin = plugin.getCezaManager().mesaj(anahtar, varsayilan);
        for (int i = 0; i + 1 < degisimler.length; i += 2) {
            metin = metin.replace(degisimler[i], degisimler[i + 1]);
        }
        kime.sendMessage(CC.parse(metin));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        CezaManager ceza = plugin.getCezaManager();

        if (!sender.hasPermission("iocore.punish") && !sender.hasPermission("iocore.punish.reload")) {
            yolla(sender, "izin-yok", "<red>Bunun icin yetkin yok.");
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("iocore.punish.reload")) {
                yolla(sender, "izin-yok", "<red>Bunun icin yetkin yok.");
                return true;
            }
            Bukkit.getAsyncScheduler().runNow(plugin, t -> {
                ceza.gecmisiKaydet();
                ceza.load();
                yolla(sender, "yenilendi", "<#8ccefe>Ceza ayarlari yenilendi. <gray>(%sayi% sebep)",
                        "%sayi%", String.valueOf(ceza.sebepAdlari().size()));
            });
            return true;
        }

        if (args.length >= 1 && (args[0].equalsIgnoreCase("sebepler") || args[0].equalsIgnoreCase("liste"))) {
            yolla(sender, "liste-basi", "<#8ccefe>Ceza sebepleri:");
            for (String anahtar : ceza.sebepAdlari()) {
                CezaManager.Sebep s = ceza.sebep(anahtar);
                if (s == null || !sender.hasPermission(s.izin())) continue;
                yolla(sender, "liste-satir", "<dark_gray> » <gray>%anahtar% <dark_gray>- <white>%ad% <dark_gray>(%kademe% kademe)",
                        "%anahtar%", s.anahtar(), "%ad%", s.ad(),
                        "%kademe%", String.valueOf(s.kademeler().size()));
            }
            return true;
        }

        if (args.length < 2) {
            yolla(sender, "kullanim", "<gray>Kullanim: <white>/iopunish <oyuncu> <sebep> [kademe]");
            return true;
        }

        CezaManager.Sebep sebep = ceza.sebep(args[1]);
        if (sebep == null) {
            yolla(sender, "sebep-yok", "<red>Boyle bir sebep yok. <gray>/iopunish sebepler");
            return true;
        }
        if (!sender.hasPermission(sebep.izin())) {
            yolla(sender, "izin-yok-sebep", "<red>Bu sebeple ceza veremezsin.");
            return true;
        }

        OfflinePlayer hedef = Bukkit.getPlayerExact(args[0]);
        if (hedef == null) hedef = Bukkit.getOfflinePlayer(args[0]);
        if (hedef.getName() == null || (!hedef.hasPlayedBefore() && !hedef.isOnline())) {
            yolla(sender, "oyuncu-yok", "<red>Bu oyuncu bulunamadi.");
            return true;
        }
        if (hedef.isOnline() && hedef.getPlayer() != null
                && hedef.getPlayer().hasPermission("iocore.punish.muaf")
                && !sender.hasPermission("iocore.punish.muafbypass")) {
            yolla(sender, "muaf", "<red>Bu oyuncu cezadan muaf.");
            return true;
        }
        if (sender instanceof Player p && p.getUniqueId().equals(hedef.getUniqueId())) {
            yolla(sender, "kendine", "<red>Kendine ceza veremezsin.");
            return true;
        }

        int kademe = -1;
        if (args.length >= 3) {
            if (!sender.hasPermission("iocore.punish.kademe")) {
                yolla(sender, "izin-yok-kademe", "<red>Kademe secemezsin.");
                return true;
            }
            try {
                kademe = Math.max(0, Integer.parseInt(args[2]) - 1);
            } catch (NumberFormatException e) {
                yolla(sender, "kademe-sayi", "<red>Kademe bir sayi olmali.");
                return true;
            }
        }

        String komut = ceza.uygula(sender, hedef, sebep, kademe);
        yolla(sender, "uygulandi", "<#8ccefe>%player% <gray>cezalandirildi: <white>%sebep%",
                "%player%", hedef.getName(), "%sebep%", sebep.ad(), "%komut%", komut);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String on = args[0].toLowerCase(Locale.ROOT);
            List<String> cikti = new ArrayList<>();
            if ("reload".startsWith(on) && sender.hasPermission("iocore.punish.reload")) cikti.add("reload");
            if ("sebepler".startsWith(on)) cikti.add("sebepler");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(on)) cikti.add(p.getName());
            }
            return cikti;
        }
        if (args.length == 2) {
            List<String> cikti = new ArrayList<>();
            for (String anahtar : plugin.getCezaManager().tamamlama(args[1])) {
                CezaManager.Sebep s = plugin.getCezaManager().sebep(anahtar);
                if (s != null && sender.hasPermission(s.izin())) cikti.add(anahtar);
            }
            return cikti;
        }
        return List.of();
    }
}
