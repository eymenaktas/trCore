//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.mcore.commands;

import com.mcore.mCore;
import com.mcore.utils.CC;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AdminCommands implements CommandExecutor {
    private final mCore plugin;

    public AdminCommands(mCore plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        String name = cmd.getName().toLowerCase();
        if (!name.equals("gmc") && !name.equals("gms") && !name.equals("gmsp")) {
            if (name.equals("fly")) {
                if (!this.checkPerm(sender, "mcore.fly")) {
                    return true;
                } else {
                    Player var30;
                    if (sender instanceof Player) {
                        Player p = (Player)sender;
                        var30 = p;
                    } else {
                        var30 = null;
                    }

                    Player target = var30;
                    if (args.length > 0) {
                        target = Bukkit.getPlayer(args[0]);
                    }

                    if (target == null) {
                        return true;
                    } else {
                        target.setAllowFlight(!target.getAllowFlight());
                        String msg = this.getRaw("admin.fly-other").replace("%target%", target.getName()).replace("%status%", target.getAllowFlight() ? "Açık" : "Kapalı");
                        sender.sendMessage(CC.parse(msg));
                        return true;
                    }
                }
            } else if (!name.equals("walkspeed") && !name.equals("flyspeed")) {
                if (name.equals("lightning")) {
                    if (!this.checkPerm(sender, "mcore.lightning")) {
                        return true;
                    } else {
                        if (sender instanceof Player) {
                            Player p = (Player)sender;
                            p.getWorld().strikeLightning(p.getTargetBlock((Set)null, 100).getLocation());
                            p.sendMessage(CC.get("admin.lightning"));
                        }

                        return true;
                    }
                } else if (name.equals("sudo")) {
                    if (!this.checkPerm(sender, "mcore.sudo")) {
                        return true;
                    } else if (args.length < 2) {
                        return false;
                    } else {
                        Player target = Bukkit.getPlayer(args[0]);
                        if (target == null) {
                            sender.sendMessage(CC.parse("<red>Oyuncu yok."));
                            return true;
                        } else {
                            StringBuilder sb = new StringBuilder();

                            for(int i = 1; i < args.length; ++i) {
                                sb.append(args[i]).append(" ");
                            }

                            String msgCommand = sb.toString().trim();
                            if (msgCommand.startsWith("/")) {
                                target.performCommand(msgCommand.substring(1));
                            } else {
                                target.chat(msgCommand);
                            }

                            String msg = this.getRaw("admin.sudo").replace("%target%", target.getName()).replace("%command%", msgCommand);
                            sender.sendMessage(CC.parse(msg));
                            return true;
                        }
                    }
                } else if (!name.equals("playerinfo")) {
                    if (name.equals("alts")) {
                        if (!this.checkPerm(sender, "mcore.alts")) {
                            return true;
                        } else if (args.length == 0) {
                            return false;
                        } else {
                            sender.sendMessage(CC.get("admin.alts.searching"));
                            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                                Player target = Bukkit.getPlayer(args[0]);
                                if (target == null) {
                                    sender.sendMessage(CC.parse("<red>Oyuncu aktif değil."));
                                } else {
                                    String ip = target.getAddress().getHostString();
                                    List<String> found = new ArrayList();

                                    for(Player p : Bukkit.getOnlinePlayers()) {
                                        if (p.getAddress().getHostString().equals(ip)) {
                                            found.add(p.getName());
                                        }
                                    }

                                    if (found.size() <= 1) {
                                        sender.sendMessage(CC.get("admin.alts.none"));
                                    } else {
                                        String msg = this.getRaw("admin.alts.found").replace("%count%", String.valueOf(found.size())).replace("%players%", String.join(", ", found));
                                        sender.sendMessage(CC.parse(msg));
                                    }

                                }
                            });
                            return true;
                        }
                    } else {
                        return true;
                    }
                } else if (!this.checkPerm(sender, "mcore.playerinfo")) {
                    return true;
                } else if (args.length == 0) {
                    return false;
                } else {
                    Player target = Bukkit.getPlayer(args[0]);
                    if (target == null) {
                        return true;
                    } else {
                        int ping = 0;

                        try {
                            ping = target.getPing();
                        } catch (Exception var12) {
                        }

                        String ip = sender.hasPermission("mcore.admin") ? target.getAddress().getHostString() : "***";

                        for(String line : this.plugin.getConfigManager().getMessages().getStringList("admin.playerinfo")) {
                            String parsedLine = line.replace("%player%", target.getName()).replace("%uuid%", target.getUniqueId().toString()).replace("%ip%", ip).replace("%health%", String.format("%.1f", target.getHealth())).replace("%maxhealth%", String.format("%.1f", target.getMaxHealth())).replace("%loc%", this.locStr(target.getLocation())).replace("%ping%", String.valueOf(ping));
                            sender.sendMessage(CC.parse(parsedLine));
                        }

                        return true;
                    }
                }
            } else if (!this.checkPerm(sender, "mcore.speed")) {
                return true;
            } else if (args.length != 0 && sender instanceof Player) {
                Player p = (Player)sender;

                try {
                    float speed = Float.parseFloat(args[0]) / 10.0F;
                    if (speed > 1.0F) {
                        speed = 1.0F;
                    }

                    if (name.equals("walkspeed")) {
                        p.setWalkSpeed(speed);
                    } else {
                        p.setFlySpeed(speed);
                    }

                    String msg = this.getRaw("admin.speed").replace("%speed%", args[0]);
                    p.sendMessage(CC.parse(msg));
                } catch (Exception var13) {
                    p.sendMessage(CC.parse("<red>Sayı giriniz."));
                }

                return true;
            } else {
                return false;
            }
        } else if (!this.checkPerm(sender, "mcore.gamemode")) {
            return true;
        } else {
            GameMode mode = name.equals("gmc") ? GameMode.CREATIVE : (name.equals("gms") ? GameMode.SURVIVAL : GameMode.SPECTATOR);
            Player var10000;
            if (sender instanceof Player) {
                Player p = (Player)sender;
                var10000 = p;
            } else {
                var10000 = null;
            }

            Player target = var10000;
            if (args.length > 0) {
                target = Bukkit.getPlayer(args[0]);
            }

            if (target == null) {
                sender.sendMessage(CC.parse("<red>Oyuncu yok."));
                return true;
            } else {
                target.setGameMode(mode);
                String msg = this.getRaw("admin.gamemode-other").replace("%target%", target.getName()).replace("%mode%", mode.name());
                sender.sendMessage(CC.parse(msg));
                return true;
            }
        }
    }

    private boolean checkPerm(CommandSender s, String p) {
        if (!s.hasPermission(p)) {
            s.sendMessage(CC.get("no-perm"));
            return false;
        } else {
            return true;
        }
    }

    private String locStr(Location l) {
        String var10000 = l.getWorld().getName();
        return var10000 + " " + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }

    private String getRaw(String path) {
        return this.plugin.getConfigManager().getMessages().getString(path);
    }
}
