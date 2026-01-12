package com.mcore.commands;

import com.mcore.mCore;
import com.mcore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
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
            sender.sendMessage(CC.get("admin.gamemode-other", "%target%", target.getName(), "%mode%", mode.name()));
            return true;
        }

        // FLY
        if (name.equals("fly")) {
            if (!checkPerm(sender, "mcore.fly")) return true;
            Player target = (sender instanceof Player p) ? p : null;
            if (args.length > 0) target = Bukkit.getPlayer(args[0]);
            if (target == null) return true;

            target.setAllowFlight(!target.getAllowFlight());
            sender.sendMessage(CC.get("admin.fly-other",
                    "%target%", target.getName(),
                    "%status%", target.getAllowFlight() ? "Açık" : "Kapalı"));
            return true;
        }

        // SPEED (KAYDETME EKLENDİ)
        if (name.equals("walkspeed") || name.equals("flyspeed")) {
            if (!checkPerm(sender, "mcore.speed")) return true;
            if (args.length == 0 || !(sender instanceof Player)) return false;
            Player p = (Player) sender;
            try {
                float val = Float.parseFloat(args[0]);
                if (val > 10) val = 10;
                if (val < 0) val = 0.1f;

                // 1 girilirse varsayılan hıza (0.2 veya 0.1) denk gelir
                float speed = val / 5.0f; // Walk default 0.2
                if (name.equals("flyspeed")) speed = val / 10.0f; // Fly default 0.1

                if (name.equals("walkspeed")) {
                    p.setWalkSpeed(speed);
                    // Hızı kaydet
                    plugin.getConfigManager().saveSpeed(p.getName(), speed, p.getFlySpeed());
                } else {
                    p.setFlySpeed(speed);
                    // Hızı kaydet
                    plugin.getConfigManager().saveSpeed(p.getName(), p.getWalkSpeed(), speed);
                }

                sender.sendMessage(CC.get("admin.speed", "%speed%", args[0]));
            } catch (Exception e) {
                p.sendMessage(CC.parse("<red>Sayı giriniz (1-10)."));
            }
            return true;
        }

        // LIGHTNING
        if (name.equals("lightning")) {
            if (!checkPerm(sender, "mcore.lightning")) return true;
            if (args.length > 0) {
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage(CC.parse("<red>Oyuncu bulunamadı."));
                    return true;
                }
                target.getWorld().strikeLightning(target.getLocation());
                sender.sendMessage(CC.get("admin.lightning-target", "%target%", target.getName()));
            } else {
                if (!(sender instanceof Player)) return true;
                Player p = (Player) sender;
                p.getWorld().strikeLightning(p.getTargetBlock(null, 100).getLocation());
                sender.sendMessage(CC.get("admin.lightning"));
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

            sender.sendMessage(CC.get("admin.sudo", "%target%", target.getName(), "%command%", msgCommand));
            return true;
        }

        // PLAYER INFO
        if (name.equals("playerinfo")) {
            if (!checkPerm(sender, "mcore.playerinfo")) return true;
            if (args.length == 0) return false;

            String targetName = args[0];
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);

            if (!offlineTarget.hasPlayedBefore() && !offlineTarget.isOnline()) {
                sender.sendMessage(CC.get("admin.playerinfo.not-found"));
                return true;
            }

            boolean isOnline = offlineTarget.isOnline();
            String status = isOnline ? "<green>Aktif</green>" : "<red>Çevrimdışı</red>";
            String uuid = offlineTarget.getUniqueId().toString();
            String ip = "Bilinmiyor";
            String health = "N/A";
            String loc = "N/A";

            if (isOnline) {
                Player p = offlineTarget.getPlayer();
                if (p != null) {
                    ip = sender.hasPermission("mcore.admin") ? p.getAddress().getHostString() : "***";
                    health = String.format("%.1f", p.getHealth()) + "/" + String.format("%.1f", p.getMaxHealth());
                    loc = locStr(p.getLocation());
                }
            } else {
                if (sender.hasPermission("mcore.admin")) {
                    String savedIP = plugin.getConfigManager().getPlayerData().getString("players." + targetName.toLowerCase());
                    if (savedIP != null) ip = savedIP + " <gray>(Son)</gray>";
                }
            }

            for (String line : plugin.getConfigManager().getMessages().getStringList("admin.playerinfo.lines")) {
                String formatted = line
                        .replace("%player%", offlineTarget.getName() != null ? offlineTarget.getName() : targetName)
                        .replace("%status%", status)
                        .replace("%uuid%", uuid)
                        .replace("%ip%", ip)
                        .replace("%health%", health)
                        .replace("%loc%", loc);
                sender.sendMessage(CC.parse(formatted));
            }
            return true;
        }

        // ALTS
        if (name.equals("alts")) {
            if (!checkPerm(sender, "mcore.alts")) return true;
            if (args.length == 0) return false;

            String targetName = args[0].toLowerCase();
            sender.sendMessage(CC.get("admin.alts.searching"));

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                String ip = null;
                Player onlineTarget = Bukkit.getPlayer(targetName);
                if (onlineTarget != null) {
                    ip = onlineTarget.getAddress().getHostString();
                } else {
                    ip = plugin.getConfigManager().getPlayerData().getString("players." + targetName);
                }

                if (ip == null) {
                    sender.sendMessage(CC.get("admin.alts.not-found"));
                    return;
                }

                String safeIP = ip.replace(".", "_");
                List<String> accounts = plugin.getConfigManager().getPlayerData().getStringList("ips." + safeIP);

                if (accounts == null || accounts.isEmpty()) {
                    accounts = new ArrayList<>();
                    accounts.add(args[0]);
                } else if (!accounts.contains(args[0])) {
                    accounts.add(args[0]);
                }

                StringBuilder formattedList = new StringBuilder();
                String selfSuffix = plugin.getConfigManager().getMessages().getString("admin.alts.self-suffix");
                String separator = plugin.getConfigManager().getMessages().getString("admin.alts.separator");

                for (int i = 0; i < accounts.size(); i++) {
                    String acc = accounts.get(i);
                    formattedList.append("<yellow>").append(acc).append("</yellow>");
                    if (acc.equalsIgnoreCase(targetName)) formattedList.append(selfSuffix);
                    if (i < accounts.size() - 1) formattedList.append(separator);
                }

                sender.sendMessage(CC.get("admin.alts.format",
                        "%player%", args[0],
                        "%list%", formattedList.toString()));
            });
            return true;
        }
        return true;
    }

    private boolean checkPerm(CommandSender s, String p) {
        if (!s.hasPermission(p)) { s.sendMessage(CC.get("no-perm")); return false; }
        return true;
    }

    private String locStr(Location l) {
        return l.getWorld().getName() + " " + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }
}