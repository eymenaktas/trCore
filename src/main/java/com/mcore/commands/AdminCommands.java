package com.mcore.commands;

import com.mcore.mCore;
import com.mcore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AdminCommands implements CommandExecutor {
    private final mCore plugin;

    public AdminCommands(mCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        String name = cmd.getName().toLowerCase();

        // GAMEMODE
        if (name.equals("gmc") || name.equals("gms") || name.equals("gmsp")) {
            if (!checkPerm(sender, "mcore.gamemode")) return true;
            GameMode mode = name.equals("gmc") ? GameMode.CREATIVE : (name.equals("gms") ? GameMode.SURVIVAL : GameMode.SPECTATOR);
            Player target = (sender instanceof Player p) ? p : null;
            if (args.length > 0) target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                sender.sendMessage(CC.parse("<red>Oyuncu yok."));
                return true;
            }
            target.setGameMode(mode);

            // HATA DÜZELTİLDİ: Önce String alıp replace yapıyoruz, sonra parse ediyoruz.
            String msg = getRaw("admin.gamemode-other")
                    .replace("%target%", target.getName())
                    .replace("%mode%", mode.name());
            sender.sendMessage(CC.parse(msg));
            return true;
        }

        // FLY
        if (name.equals("fly")) {
            if (!checkPerm(sender, "mcore.fly")) return true;
            Player target = (sender instanceof Player p) ? p : null;
            if (args.length > 0) target = Bukkit.getPlayer(args[0]);
            if (target == null) return true;

            target.setAllowFlight(!target.getAllowFlight());

            // HATA DÜZELTİLDİ
            String msg = getRaw("admin.fly-other")
                    .replace("%target%", target.getName())
                    .replace("%status%", target.getAllowFlight() ? "Açık" : "Kapalı");
            sender.sendMessage(CC.parse(msg));
            return true;
        }

        // SPEED
        if (name.equals("walkspeed") || name.equals("flyspeed")) {
            if (!checkPerm(sender, "mcore.speed")) return true;
            if (args.length == 0 || !(sender instanceof Player)) return false;
            Player p = (Player) sender;
            try {
                float speed = Float.parseFloat(args[0]) / 10f;
                if (speed > 1f) speed = 1f;
                if (name.equals("walkspeed")) p.setWalkSpeed(speed);
                else p.setFlySpeed(speed);

                // HATA DÜZELTİLDİ
                String msg = getRaw("admin.speed").replace("%speed%", args[0]);
                p.sendMessage(CC.parse(msg));
            } catch (Exception e) { p.sendMessage(CC.parse("<red>Sayı giriniz.")); }
            return true;
        }

        // LIGHTNING
        if (name.equals("lightning")) {
            if (!checkPerm(sender, "mcore.lightning")) return true;
            if (sender instanceof Player p) {
                p.getWorld().strikeLightning(p.getTargetBlock(null, 100).getLocation());
                p.sendMessage(CC.get("admin.lightning")); // Burada tek parametre olduğu için CC.get kullanılabilir
            }
            return true;
        }

        // SUDO
        if (name.equals("sudo")) {
            if (!checkPerm(sender, "mcore.sudo")) return true;
            if (args.length < 2) return false;
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) { sender.sendMessage(CC.parse("<red>Oyuncu yok.")); return true; }
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) sb.append(args[i]).append(" ");
            String msgCommand = sb.toString().trim();

            if (msgCommand.startsWith("/")) target.performCommand(msgCommand.substring(1));
            else target.chat(msgCommand);

            // HATA DÜZELTİLDİ
            String msg = getRaw("admin.sudo")
                    .replace("%target%", target.getName())
                    .replace("%command%", msgCommand);
            sender.sendMessage(CC.parse(msg));
            return true;
        }

        // PLAYER INFO
        if (name.equals("playerinfo")) {
            if (!checkPerm(sender, "mcore.playerinfo")) return true;
            if (args.length == 0) return false;
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) return true;
            int ping = 0;
            try { ping = target.getPing(); } catch (Exception ignored) {}
            String ip = sender.hasPermission("mcore.admin") ? target.getAddress().getHostString() : "***";

            // HATA DÜZELTİLDİ: Liste dönüşümleri
            for (String line : plugin.getConfigManager().getMessages().getStringList("admin.playerinfo")) {
                String parsedLine = line
                        .replace("%player%", target.getName())
                        .replace("%uuid%", target.getUniqueId().toString())
                        .replace("%ip%", ip)
                        .replace("%health%", String.format("%.1f", target.getHealth()))
                        .replace("%maxhealth%", String.format("%.1f", target.getMaxHealth()))
                        .replace("%loc%", locStr(target.getLocation()))
                        .replace("%ping%", String.valueOf(ping));
                sender.sendMessage(CC.parse(parsedLine));
            }
            return true;
        }

        // ALTS
        if (name.equals("alts")) {
            if (!checkPerm(sender, "mcore.alts")) return true;
            if (args.length == 0) return false;
            sender.sendMessage(CC.get("admin.alts.searching"));
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) { sender.sendMessage(CC.parse("<red>Oyuncu aktif değil.")); return; }
                String ip = target.getAddress().getHostString();
                List<String> found = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getAddress().getHostString().equals(ip)) found.add(p.getName());
                }
                if (found.size() <= 1) {
                    sender.sendMessage(CC.get("admin.alts.none"));
                } else {
                    // HATA DÜZELTİLDİ
                    String msg = getRaw("admin.alts.found")
                            .replace("%count%", String.valueOf(found.size()))
                            .replace("%players%", String.join(", ", found));
                    sender.sendMessage(CC.parse(msg));
                }
            });
            return true;
        }
        return true;
    }

    private boolean checkPerm(CommandSender s, String p) {
        if (!s.hasPermission(p)) { s.sendMessage(CC.get("no-perm")); return false; }
        return true;
    }

    private String locStr(org.bukkit.Location l) {
        return l.getWorld().getName() + " " + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }

    // YARDIMCI METOD: Config'den ham string çeker
    private String getRaw(String path) {
        return plugin.getConfigManager().getMessages().getString(path);
    }
}