package com.mcore.managers;

import com.mcore.mCore;
import com.mcore.utils.CC;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TPAManager {
    private final mCore plugin;
    private final MenuManager menuManager;

    private final Map<UUID, UUID> requests = new HashMap<>();
    private final Map<UUID, UUID> activeSender = new HashMap<>();
    private final Map<UUID, String> types = new HashMap<>();

    // TPA Event Değişkenleri
    private boolean isEventActive = false;
    private long eventStartTime = 0;
    private Player eventHost;

    public TPAManager(mCore plugin, MenuManager menuManager) {
        this.plugin = plugin;
        this.menuManager = menuManager;
    }

    // --- TPA (Standart) ---
    public void send(Player sender, Player target, String type) {
        if (activeSender.containsKey(sender.getUniqueId())) {
            sender.sendMessage(CC.get("tpa.already-sent"));
            return;
        }
        requests.put(target.getUniqueId(), sender.getUniqueId());
        activeSender.put(sender.getUniqueId(), target.getUniqueId());
        types.put(target.getUniqueId(), type);

        sender.sendMessage(CC.get("tpa.sent", "%target%", target.getName()));
        playSound(target, "tpa.sound-on-request");

        if (type.equals("tpa")) target.sendMessage(CC.get("tpa.received", "%player%", sender.getName()));
        else target.sendMessage(CC.get("tpa.received-here", "%player%", sender.getName()));

        int timeout = plugin.getConfig().getInt("tpa.timeout");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeSender.containsKey(sender.getUniqueId()) && activeSender.get(sender.getUniqueId()).equals(target.getUniqueId())) {
                cancel(sender);
                sender.sendMessage(CC.get("tpa.timeout"));
            }
        }, timeout * 20L);
    }

    public void cancel(Player sender) {
        if (!activeSender.containsKey(sender.getUniqueId())) {
            sender.sendMessage(CC.get("tpa.no-request"));
            return;
        }
        UUID targetId = activeSender.remove(sender.getUniqueId());
        requests.remove(targetId);
        types.remove(targetId);
        sender.sendMessage(CC.get("tpa.cancelled"));
    }

    public void openAcceptMenu(Player target) {
        if (!requests.containsKey(target.getUniqueId())) {
            target.sendMessage(CC.get("tpa.no-request"));
            return;
        }
        UUID senderId = requests.get(target.getUniqueId());
        Player sender = Bukkit.getPlayer(senderId);
        String type = types.get(target.getUniqueId());
        String menuId = type.equals("tpa") ? "tpa-accept-menu" : "tpa-here-accept-menu";
        target.openInventory(menuManager.create(menuId, sender));
    }

    public void accept(Player target) {
        UUID senderId = requests.remove(target.getUniqueId());
        activeSender.remove(senderId);
        String type = types.remove(target.getUniqueId());
        target.closeInventory();
        Player sender = Bukkit.getPlayer(senderId);
        if (sender == null) return;

        int delay = plugin.getConfig().getInt("tpa.delay");
        String timeStr = String.valueOf(delay);
        target.sendMessage(CC.get("tpa.accepted", "%time%", timeStr));
        sender.sendMessage(CC.get("tpa.accepted", "%time%", timeStr));

        new BukkitRunnable() {
            int count = delay;
            @Override
            public void run() {
                if (count <= 0) {
                    playSound(sender, "tpa.sound-on-teleport");
                    playSound(target, "tpa.sound-on-teleport");
                    if (type.equals("tpa")) {
                        if (sender.isOnline() && target.isOnline()) sender.teleport(target);
                    } else {
                        if (sender.isOnline() && target.isOnline()) target.teleport(sender);
                    }
                    this.cancel();
                    return;
                }
                playSound(sender, "tpa.countdown-sound");
                playSound(target, "tpa.countdown-sound");
                count--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void deny(Player target) {
        UUID senderId = requests.remove(target.getUniqueId());
        activeSender.remove(senderId);
        types.remove(target.getUniqueId());
        target.closeInventory();
        target.sendMessage(CC.get("tpa.denied"));
        Player sender = Bukkit.getPlayer(senderId);
        if (sender != null) sender.sendMessage(CC.get("tpa.denied"));
    }

    // --- TPA EVENT (Eksik Metodlar Burada) ---

    public void startEvent(Player admin) {
        if (isEventActive) {
            admin.sendMessage(CC.get("tpa.event-already-active"));
            return;
        }
        this.eventHost = admin;
        this.eventStartTime = System.currentTimeMillis();
        this.isEventActive = true;

        admin.sendMessage(CC.get("tpa.event-location-set"));

        Component broadcastMsg = CC.get("tpa.event-broadcast", "%player%", admin.getName());
        // HATA ÇÖZÜMÜ: Bukkit.broadcast yerine getServer().sendMessage kullanıldı (Component desteği için)
        plugin.getServer().sendMessage(broadcastMsg);
    }

    public void joinEvent(Player player) {
        if (!isEventActive || eventHost == null || !eventHost.isOnline()) {
            player.sendMessage(CC.get("tpa.no-active-event"));
            return;
        }

        long timeElapsed = System.currentTimeMillis() - eventStartTime;
        if (timeElapsed > 120000) { // 120 Saniye
            player.sendMessage(CC.get("tpa.event-expired"));
            return;
        }

        player.teleport(eventHost.getLocation());
        player.sendMessage(CC.get("tpa.event-joined"));
        playSound(player, "tpa.sound-on-teleport");
    }

    public void stopEvent(boolean force) {
        if (!isEventActive && !force) return;

        isEventActive = false;
        eventHost = null;
        eventStartTime = 0;

        if (!force) {
            plugin.getServer().sendMessage(CC.get("tpa.event-ended"));
        }
    }

    // --- UTILS & CLEANUP (Listener için gerekli) ---

    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();

        if (activeSender.containsKey(uuid)) {
            UUID targetId = activeSender.remove(uuid);
            requests.remove(targetId);
            types.remove(targetId);
        }

        if (requests.containsKey(uuid)) {
            UUID senderId = requests.remove(uuid);
            activeSender.remove(senderId);
        }

        if (isEventActive && eventHost != null && eventHost.getUniqueId().equals(uuid)) {
            stopEvent(true);
        }
    }

    private void playSound(Player player, String configPath) {
        if (player == null || !player.isOnline()) return;
        String soundName = plugin.getConfig().getString(configPath);
        if (soundName != null && !soundName.isEmpty()) {
            try {
                player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase()), 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                // Ignore invalid sound
            }
        }
    }
}