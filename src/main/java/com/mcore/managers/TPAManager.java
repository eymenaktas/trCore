package com.mcore.managers;

import com.mcore.mCore;
import com.mcore.utils.CC;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class TPAManager {
    private final mCore plugin;
    private final MenuManager menuManager;

    private final Map<UUID, UUID> requests = new HashMap<>();
    private final Map<UUID, UUID> activeSender = new HashMap<>();
    private final Map<UUID, String> types = new HashMap<>();

    // Multi-Event
    private final Map<UUID, Location> activeEvents = new HashMap<>();
    private final Map<UUID, BukkitTask> eventTimers = new HashMap<>();
    // Host -> Katılımcılar Listesi
    private final Map<UUID, Set<UUID>> eventParticipants = new HashMap<>();

    public TPAManager(mCore plugin, MenuManager menuManager) {
        this.plugin = plugin;
        this.menuManager = menuManager;
    }

    // --- SEND ---
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

    // --- CANCEL ---
    public void cancel(Player sender) {
        if (!activeSender.containsKey(sender.getUniqueId())) {
            sender.sendMessage(CC.get("tpa.no-request"));
            return;
        }
        UUID targetId = activeSender.remove(sender.getUniqueId());
        if (targetId != null) {
            requests.remove(targetId);
            types.remove(targetId);
        }
        sender.sendMessage(CC.get("tpa.cancelled"));
    }

    // --- MENU ---
    public void openAcceptMenu(Player target) {
        if (!requests.containsKey(target.getUniqueId())) {
            target.sendMessage(CC.get("tpa.no-request"));
            return;
        }
        UUID senderId = requests.get(target.getUniqueId());
        if (senderId == null) return;

        Player sender = Bukkit.getPlayer(senderId);
        String type = types.get(target.getUniqueId());
        String menuId = type.equals("tpa") ? "tpa-accept-menu" : "tpa-here-accept-menu";

        org.bukkit.inventory.Inventory inv = menuManager.create(menuId, sender);
        if (inv == null) {
            target.sendMessage(CC.parse("<red>Hata: Menü dosyası bulunamadı."));
            return;
        }
        target.openInventory(inv);
    }

    // --- ACCEPT ---
    public void accept(Player target) {
        if (!requests.containsKey(target.getUniqueId())) {
            target.sendMessage(CC.get("tpa.no-request"));
            return;
        }
        UUID senderId = requests.remove(target.getUniqueId());
        if (senderId == null) return;

        activeSender.remove(senderId);
        String type = types.remove(target.getUniqueId());
        target.closeInventory();

        Player sender = Bukkit.getPlayer(senderId);
        if (sender == null || !sender.isOnline()) {
            target.sendMessage(CC.parse("<red>İstek gönderen oyuncu oyundan çıkmış."));
            return;
        }

        int delay = plugin.getConfig().getInt("tpa.delay");
        String timeStr = String.valueOf(delay);
        target.sendMessage(CC.get("tpa.accepted", "%time%", timeStr));
        sender.sendMessage(CC.get("tpa.accepted", "%time%", timeStr));

        new BukkitRunnable() {
            int count = delay;
            @Override
            public void run() {
                if (!sender.isOnline() || !target.isOnline()) {
                    this.cancel();
                    return;
                }
                if (count <= 0) {
                    playSound(sender, "tpa.sound-on-teleport");
                    playSound(target, "tpa.sound-on-teleport");
                    if (type != null && type.equals("tpa")) sender.teleport(target);
                    else target.teleport(sender);
                    this.cancel();
                    return;
                }
                playSound(sender, "tpa.countdown-sound");
                playSound(target, "tpa.countdown-sound");
                count--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // --- DENY ---
    public void deny(Player target) {
        if (!requests.containsKey(target.getUniqueId())) {
            target.sendMessage(CC.get("tpa.no-request"));
            return;
        }
        UUID senderId = requests.remove(target.getUniqueId());
        if (senderId != null) {
            activeSender.remove(senderId);
            Player sender = Bukkit.getPlayer(senderId);
            if (sender != null) sender.sendMessage(CC.get("tpa.denied"));
        }
        types.remove(target.getUniqueId());
        target.closeInventory();
        target.sendMessage(CC.get("tpa.denied"));
    }

    // --- TPA EVENT ---
    public void startEvent(Player admin) {
        if (activeEvents.containsKey(admin.getUniqueId())) {
            admin.sendMessage(CC.get("tpa-event.already-active"));
            return;
        }
        activeEvents.put(admin.getUniqueId(), admin.getLocation());
        eventParticipants.put(admin.getUniqueId(), new HashSet<>()); // Katılımcı listesini başlat

        // "Etkinlik Başladı (2dk)" mesajı SADECE admine gider.
        admin.sendMessage(CC.get("tpa-event.started"));

        // Bu mesaj HERKESE gider (Davet Linki)
        Component broadcastMsg = CC.get("tpa-event.broadcast", "%player%", admin.getName())
                .replaceText(b -> b.match("/tpaevent join").replacement("/tpaevent join " + admin.getName()));

        plugin.getServer().sendMessage(broadcastMsg);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            stopEvent(admin, false);
        }, 120 * 20L);
        eventTimers.put(admin.getUniqueId(), task);
    }

    public void joinEvent(Player player, String hostName) {
        UUID targetHostUUID = null;
        if (hostName == null) {
            if (activeEvents.size() == 1) {
                targetHostUUID = activeEvents.keySet().iterator().next();
            }
        } else {
            Player host = Bukkit.getPlayer(hostName);
            if (host != null) targetHostUUID = host.getUniqueId();
        }

        if (targetHostUUID == null || !activeEvents.containsKey(targetHostUUID)) {
            player.sendMessage(CC.get("tpa-event.expired"));
            return;
        }

        // Katılımcı kontrolü (Sadece bu evente özel)
        Set<UUID> participants = eventParticipants.get(targetHostUUID);
        if (participants != null && participants.contains(player.getUniqueId())) {
            player.sendMessage(CC.get("tpa-event.already-joined"));
            return;
        }

        Location targetLoc = activeEvents.get(targetHostUUID);
        player.teleport(targetLoc);

        // Listeye ekle
        if (participants != null) participants.add(player.getUniqueId());

        player.sendMessage(CC.get("tpa-event.joined"));
        playSound(player, "tpa.sound-on-teleport");
    }

    public void stopEvent(Player admin, boolean force) {
        if (!activeEvents.containsKey(admin.getUniqueId()) && !force) return;

        // Katılımcı listesini al (Silmeden önce!)
        Set<UUID> participants = eventParticipants.get(admin.getUniqueId());

        // Verileri temizle
        activeEvents.remove(admin.getUniqueId());
        eventParticipants.remove(admin.getUniqueId());

        if (eventTimers.containsKey(admin.getUniqueId())) {
            eventTimers.get(admin.getUniqueId()).cancel();
            eventTimers.remove(admin.getUniqueId());
        }

        if (!force) {
            // 1. Etkinliği başlatana (Host) mesaj at
            if (admin.isOnline()) {
                admin.sendMessage(CC.get("tpa-event.ended"));
            }

            // 2. Sadece o etkinliğe katılanlara mesaj at (Sunucu geneline gitmez)
            if (participants != null) {
                for (UUID pUuid : participants) {
                    Player p = Bukkit.getPlayer(pUuid);
                    if (p != null && p.isOnline()) {
                        p.sendMessage(CC.get("tpa-event.ended"));
                    }
                }
            }
        }
    }

    public void stopAllEvents() {
        for (BukkitTask task : eventTimers.values()) {
            if (task != null) task.cancel();
        }
        eventTimers.clear();
        activeEvents.clear();
        eventParticipants.clear();
    }

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
        if (activeEvents.containsKey(uuid)) {
            stopEvent(player, true);
        }
    }

    @SuppressWarnings("deprecation")
    private void playSound(Player player, String configPath) {
        if (player == null || !player.isOnline()) return;
        String soundName = plugin.getConfig().getString(configPath);
        if (soundName != null && !soundName.isEmpty()) {
            try {
                player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase()), 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {}
        }
    }
}