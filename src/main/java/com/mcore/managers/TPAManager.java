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

    // --- TPA EVENT SİSTEMİ (GÜNCELLENDİ) ---
    // Etkinlik sahibi (Host) -> Etkinlik Konumu
    private final Map<UUID, Location> activeEvents = new HashMap<>();
    // Etkinlik sahibi (Host) -> Zamanlayıcı
    private final Map<UUID, BukkitTask> eventTimers = new HashMap<>();
    // Etkinlik sahibi (Host) -> Katılan Oyuncuların Listesi (Set)
    // Bu sayede her etkinliğin katılımcı listesi kendine özel olur.
    private final Map<UUID, Set<UUID>> eventParticipants = new HashMap<>();

    public TPAManager(mCore plugin, MenuManager menuManager) {
        this.plugin = plugin;
        this.menuManager = menuManager;
    }

    // --- TPA GÖNDERME ---
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

    // --- TPA İPTAL ---
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

    // --- MENÜ AÇMA ---
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

    // --- KABUL ET ---
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

    // --- REDDET ---
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

    // --- TPA EVENT (DÜZELTİLDİ) ---
    public void startEvent(Player admin) {
        if (activeEvents.containsKey(admin.getUniqueId())) {
            admin.sendMessage(CC.get("tpa-event.already-active"));
            return;
        }

        // Eventi başlat
        activeEvents.put(admin.getUniqueId(), admin.getLocation());
        // Bu host için boş bir katılımcı listesi oluştur
        eventParticipants.put(admin.getUniqueId(), new HashSet<>());

        admin.sendMessage(CC.get("tpa-event.started"));

        Component broadcastMsg = CC.get("tpa-event.broadcast", "%player%", admin.getName())
                .replaceText(b -> b.match("/tpaevent join").replacement("/tpaevent join " + admin.getName()));

        plugin.getServer().sendMessage(broadcastMsg);

        // Otomatik bitirme zamanlayıcısı
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            stopEvent(admin, false);
        }, 120 * 20L);
        eventTimers.put(admin.getUniqueId(), task);
    }

    public void joinEvent(Player player, String hostName) {
        UUID targetHostUUID = null;

        // Eğer isim girilmediyse ve sadece 1 tane aktif event varsa ona sok
        if (hostName == null) {
            if (activeEvents.size() == 1) {
                targetHostUUID = activeEvents.keySet().iterator().next();
            }
        } else {
            Player host = Bukkit.getPlayer(hostName);
            if (host != null) targetHostUUID = host.getUniqueId();
        }

        // Etkinlik yoksa veya bittiyse
        if (targetHostUUID == null || !activeEvents.containsKey(targetHostUUID)) {
            player.sendMessage(CC.get("tpa-event.expired"));
            return;
        }

        // OYUNCU BU ÖZEL ETKİNLİĞE ZATEN KATILMIŞ MI?
        Set<UUID> participants = eventParticipants.get(targetHostUUID);
        if (participants != null && participants.contains(player.getUniqueId())) {
            player.sendMessage(CC.get("tpa-event.already-joined"));
            return; // Sadece bu evente katılamaz, başkasına katılabilir.
        }

        // Katılım işlemi
        Location targetLoc = activeEvents.get(targetHostUUID);
        player.teleport(targetLoc);

        // Listeye ekle
        if (participants != null) participants.add(player.getUniqueId());

        player.sendMessage(CC.get("tpa-event.joined"));
        playSound(player, "tpa.sound-on-teleport");
    }

    public void stopEvent(Player admin, boolean force) {
        if (!activeEvents.containsKey(admin.getUniqueId()) && !force) return;

        // Haritalardan sil (Bu sayede oyuncular tekrar katılabilir veya "zaten katıldın" hatası almaz)
        activeEvents.remove(admin.getUniqueId());
        eventParticipants.remove(admin.getUniqueId()); // LİSTE SİLİNDİĞİ İÇİN SIFIRLANIR

        if (eventTimers.containsKey(admin.getUniqueId())) {
            eventTimers.get(admin.getUniqueId()).cancel();
            eventTimers.remove(admin.getUniqueId());
        }

        if (!force) {
            plugin.getServer().sendMessage(CC.get("tpa-event.ended"));
        }
    }

    public void stopAllEvents() {
        for (BukkitTask task : eventTimers.values()) {
            if (task != null) task.cancel();
        }
        eventTimers.clear();
        activeEvents.clear();
        eventParticipants.clear(); // Herkesi özgür bırak
    }

    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        // TPA İsteklerini temizle
        if (activeSender.containsKey(uuid)) {
            UUID targetId = activeSender.remove(uuid);
            requests.remove(targetId);
            types.remove(targetId);
        }
        if (requests.containsKey(uuid)) {
            UUID senderId = requests.remove(uuid);
            activeSender.remove(senderId);
        }
        // Eğer event sahibi çıkarsa etkinliği bitir
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