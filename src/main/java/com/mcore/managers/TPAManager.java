//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.mcore.managers;

import com.mcore.mCore;
import com.mcore.utils.CC;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TPAManager {
    private final mCore plugin;
    private final MenuManager menuManager;
    private final Map<UUID, UUID> requests = new HashMap();
    private final Map<UUID, UUID> activeSender = new HashMap();
    private final Map<UUID, String> types = new HashMap();
    private boolean isEventActive = false;
    private long eventStartTime = 0L;
    private Player eventHost;

    public TPAManager(mCore plugin, MenuManager menuManager) {
        this.plugin = plugin;
        this.menuManager = menuManager;
    }

    public void send(Player sender, Player target, String type) {
        if (this.activeSender.containsKey(sender.getUniqueId())) {
            sender.sendMessage(CC.get("tpa.already-sent"));
        } else {
            this.requests.put(target.getUniqueId(), sender.getUniqueId());
            this.activeSender.put(sender.getUniqueId(), target.getUniqueId());
            this.types.put(target.getUniqueId(), type);
            sender.sendMessage(CC.get("tpa.sent", "%target%", target.getName()));
            this.playSound(target, "tpa.sound-on-request");
            if (type.equals("tpa")) {
                target.sendMessage(CC.get("tpa.received", "%player%", sender.getName()));
            } else {
                target.sendMessage(CC.get("tpa.received-here", "%player%", sender.getName()));
            }

            int timeout = this.plugin.getConfig().getInt("tpa.timeout");
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                if (this.activeSender.containsKey(sender.getUniqueId()) && ((UUID)this.activeSender.get(sender.getUniqueId())).equals(target.getUniqueId())) {
                    this.cancel(sender);
                    sender.sendMessage(CC.get("tpa.timeout"));
                }

            }, (long)timeout * 20L);
        }
    }

    public void cancel(Player sender) {
        if (!this.activeSender.containsKey(sender.getUniqueId())) {
            sender.sendMessage(CC.get("tpa.no-request"));
        } else {
            UUID targetId = (UUID)this.activeSender.remove(sender.getUniqueId());
            this.requests.remove(targetId);
            this.types.remove(targetId);
            sender.sendMessage(CC.get("tpa.cancelled"));
        }
    }

    public void openAcceptMenu(Player target) {
        if (!this.requests.containsKey(target.getUniqueId())) {
            target.sendMessage(CC.get("tpa.no-request"));
        } else {
            UUID senderId = (UUID)this.requests.get(target.getUniqueId());
            Player sender = Bukkit.getPlayer(senderId);
            String type = (String)this.types.get(target.getUniqueId());
            String menuId = type.equals("tpa") ? "tpa-accept-menu" : "tpa-here-accept-menu";
            target.openInventory(this.menuManager.create(menuId, sender));
        }
    }

    public void accept(final Player target) {
        UUID senderId = (UUID)this.requests.remove(target.getUniqueId());
        this.activeSender.remove(senderId);
        final String type = (String)this.types.remove(target.getUniqueId());
        target.closeInventory();
        final Player sender = Bukkit.getPlayer(senderId);
        if (sender != null) {
            final int delay = this.plugin.getConfig().getInt("tpa.delay");
            String timeStr = String.valueOf(delay);
            target.sendMessage(CC.get("tpa.accepted", "%time%", timeStr));
            sender.sendMessage(CC.get("tpa.accepted", "%time%", timeStr));
            (new BukkitRunnable() {
                int count = delay;

                public void run() {
                    if (this.count <= 0) {
                        TPAManager.this.playSound(sender, "tpa.sound-on-teleport");
                        TPAManager.this.playSound(target, "tpa.sound-on-teleport");
                        if (type.equals("tpa")) {
                            if (sender.isOnline() && target.isOnline()) {
                                sender.teleport(target);
                            }
                        } else if (sender.isOnline() && target.isOnline()) {
                            target.teleport(sender);
                        }

                        this.cancel();
                    } else {
                        TPAManager.this.playSound(sender, "tpa.countdown-sound");
                        TPAManager.this.playSound(target, "tpa.countdown-sound");
                        --this.count;
                    }
                }
            }).runTaskTimer(this.plugin, 0L, 20L);
        }
    }

    public void deny(Player target) {
        UUID senderId = (UUID)this.requests.remove(target.getUniqueId());
        this.activeSender.remove(senderId);
        this.types.remove(target.getUniqueId());
        target.closeInventory();
        target.sendMessage(CC.get("tpa.denied"));
        Player sender = Bukkit.getPlayer(senderId);
        if (sender != null) {
            sender.sendMessage(CC.get("tpa.denied"));
        }

    }

    public void startEvent(Player admin) {
        if (this.isEventActive) {
            admin.sendMessage(CC.get("tpa.event-already-active"));
        } else {
            this.eventHost = admin;
            this.eventStartTime = System.currentTimeMillis();
            this.isEventActive = true;
            admin.sendMessage(CC.get("tpa.event-location-set"));
            Component broadcastMsg = CC.get("tpa.event-broadcast", "%player%", admin.getName());
            this.plugin.getServer().sendMessage(broadcastMsg);
        }
    }

    public void joinEvent(Player player) {
        if (this.isEventActive && this.eventHost != null && this.eventHost.isOnline()) {
            long timeElapsed = System.currentTimeMillis() - this.eventStartTime;
            if (timeElapsed > 120000L) {
                player.sendMessage(CC.get("tpa.event-expired"));
            } else {
                player.teleport(this.eventHost.getLocation());
                player.sendMessage(CC.get("tpa.event-joined"));
                this.playSound(player, "tpa.sound-on-teleport");
            }
        } else {
            player.sendMessage(CC.get("tpa.no-active-event"));
        }
    }

    public void stopEvent(boolean force) {
        if (this.isEventActive || force) {
            this.isEventActive = false;
            this.eventHost = null;
            this.eventStartTime = 0L;
            if (!force) {
                this.plugin.getServer().sendMessage(CC.get("tpa.event-ended"));
            }

        }
    }

    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        if (this.activeSender.containsKey(uuid)) {
            UUID targetId = (UUID)this.activeSender.remove(uuid);
            this.requests.remove(targetId);
            this.types.remove(targetId);
        }

        if (this.requests.containsKey(uuid)) {
            UUID senderId = (UUID)this.requests.remove(uuid);
            this.activeSender.remove(senderId);
        }

        if (this.isEventActive && this.eventHost != null && this.eventHost.getUniqueId().equals(uuid)) {
            this.stopEvent(true);
        }

    }

    private void playSound(Player player, String configPath) {
        if (player != null && player.isOnline()) {
            String soundName = this.plugin.getConfig().getString(configPath);
            if (soundName != null && !soundName.isEmpty()) {
                try {
                    player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase()), 1.0F, 1.0F);
                } catch (IllegalArgumentException var5) {
                }
            }

        }
    }
}
