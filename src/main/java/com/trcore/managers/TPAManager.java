package com.trcore.managers;

import com.trcore.TRCore;
import com.trcore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.*;

public class TPAManager implements Listener {
    private final TRCore plugin;
    private final MenuManager menuManager;

    private final Map<UUID, UUID> pendingTarget = new HashMap<>();
    private final Map<UUID, String> pendingType = new HashMap<>();

    private final Map<UUID, LinkedHashMap<UUID, String>> targetRequests = new HashMap<>();
    private final Map<UUID, UUID> activeSender = new HashMap<>();
    private final Map<UUID, UUID> activeMenu = new HashMap<>();

    private final Map<UUID, Location> activeEvents = new HashMap<>();
    private final Map<UUID, ScheduledTask> eventTimers = new HashMap<>();
    private final Map<UUID, Set<UUID>> eventParticipants = new HashMap<>();
    private final Map<UUID, ScheduledTask> activeTeleportTasks = new HashMap<>();

    public TPAManager(TRCore plugin, MenuManager menuManager) {
        this.plugin = plugin;
        this.menuManager = menuManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }



    public void openSendMenu(Player sender, Player target, String type) {
        boolean isTpa = type.equals("tpa");
        boolean isEnabled = isTpa ? plugin.getToggleManager().isTPAEnabled(target.getUniqueId())
                : plugin.getToggleManager().isTPAHereEnabled(target.getUniqueId());

            if (!isEnabled && !sender.hasPermission("trcore.tpa.bypass")) {
            sender.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("toggle.target-disabled", "<red>Bu oyuncu istekleri kapatmış.")));
            return;
        }

        if (isInRanked(sender) || isInRanked(target)) {
            sender.sendMessage(CC.parse("<red>Dereceli maçta veya sırasında olan birine/birinden istek gönderilemez."));
            return;
        }

        if (activeSender.containsKey(sender.getUniqueId())) {
            sender.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("tpa.already-sent", "<red>Zaten bekleyen bir isteğin var.")));
            return;
        }

        pendingTarget.put(sender.getUniqueId(), target.getUniqueId());
        pendingType.put(sender.getUniqueId(), type);

        String menuId = isTpa ? "tpa-send-menu" : "tpahere-send-menu";
        Inventory inv = menuManager.create(menuId, sender, target);
        if (inv == null) return;

        if (isTpa) {
            String worldRawName = target.getWorld().getName().toLowerCase();
            String matName = plugin.getConfig().getString("tpa.world-icons." + worldRawName, "MAP");
            String displayName = plugin.getConfig().getString("tpa.world-names." + worldRawName, target.getWorld().getName());

            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.MAP;

            ItemStack icon = new ItemStack(mat);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(CC.parse("<!italic><dark_gray>Harita: <white>" + displayName));
                icon.setItemMeta(meta);
            }
            inv.setItem(22, icon);
        }

        sender.openInventory(inv);
    }

    public void confirmSend(Player sender) {
        UUID targetId = pendingTarget.remove(sender.getUniqueId());
        String type = pendingType.remove(sender.getUniqueId());
        sender.closeInventory();

        if (targetId == null || type == null) return;

        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("tpa.player-not-found", "<red>Oyuncu çevrimdışı.")));
            return;
        }

        executeSend(sender, target, type);
    }

    public void cancelSend(Player sender) {
        pendingTarget.remove(sender.getUniqueId());
        pendingType.remove(sender.getUniqueId());
        sender.closeInventory();
        sender.sendMessage(CC.parse("<red>İstek göndermekten vazgeçtin."));
    }

    private void executeSend(Player sender, Player target, String type) {
        if (!target.isOnline()) {
            sender.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("tpa.player-not-found", "<red>Oyuncu çevrimiçi değil.")));
            return;
        }

        if (isInRanked(sender) || isInRanked(target)) {
            sender.sendMessage(CC.parse("<red>Dereceli maçta veya sırasında olan birine/birinden istek gönderilemez."));
            return;
        }

        boolean isTpa = type.equals("tpa");
        int delay = plugin.getConfig().getInt("tpa.delay", 3);
            if (sender.hasPermission("trcore.tpa.bypass") || target.hasPermission("trcore.tpa.bypass")) delay = 0;

        if (isTpa && plugin.getToggleManager().isAutoTPA(target.getUniqueId())) {
            String senderMsg = plugin.getConfigManager().getMessages().getString("tpa.accepted", "Kabul edildi.").replace("%time%", String.valueOf(delay));
            String targetMsg = plugin.getConfigManager().getMessages().getString("tpa.auto-accepted-target", "<green>%player% isteğini oto-kabul ettin.").replace("%player%", sender.getName());

            sender.sendMessage(CC.parse(senderMsg));
            target.sendMessage(CC.parse(targetMsg));

            CC.playSound(sender, "ENTITY_PLAYER_LEVELUP");
            CC.playSound(target, "ENTITY_PLAYER_LEVELUP");

            startTeleportTask(sender, target, type, delay);
            return;
        }

        targetRequests.computeIfAbsent(target.getUniqueId(), k -> new LinkedHashMap<>()).put(sender.getUniqueId(), type);
        activeSender.put(sender.getUniqueId(), target.getUniqueId());

        sender.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("tpa.sent", "İstek gönderildi.").replace("%target%", target.getName())));
        
        String soundName = plugin.getConfig().getString("tpa.sound-on-request");
        if (soundName != null && !soundName.isEmpty()) {
            CC.playSound(target, soundName);
        } else {
            CC.playSound(target, "ENTITY_EXPERIENCE_ORB_PICKUP");
        }

        if (isTpa) {
            target.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("tpa.received", "Sana istek gönderdi.").replace("%player%", sender.getName())));
        } else {
            target.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("tpa.received-here", "Seni yanına çağırıyor.").replace("%player%", sender.getName())));
        }

        int timeout = plugin.getConfig().getInt("tpa.timeout", 60);
        sender.getScheduler().runDelayed(plugin, task -> {
            if (activeSender.containsKey(sender.getUniqueId()) && activeSender.get(sender.getUniqueId()).equals(target.getUniqueId())) {
                cancel(sender);
                sender.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("tpa.timeout", "<red>Zaman aşımı.")));
            }
        }, null, timeout * 20L);
    }

    public void openAcceptMenu(Player target, String senderName) {
        LinkedHashMap<UUID, String> reqs = targetRequests.get(target.getUniqueId());
        if (reqs == null || reqs.isEmpty()) {
            target.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("tpa.no-request", "<red>İstek bulunamadı.")));
            return;
        }

        if (isInRanked(target)) {
            target.sendMessage(CC.parse("<red>Dereceli maçta veya sırasında istek kabul edemezsin."));
            return;
        }

        UUID senderId = null;
        if (senderName != null) {
            Player s = Bukkit.getPlayer(senderName);
            if (s != null && reqs.containsKey(s.getUniqueId())) senderId = s.getUniqueId();
        } else {
            for (UUID key : reqs.keySet()) senderId = key;
        }

        if (senderId == null) {
            target.sendMessage(CC.parse("<red>Bu isimde birinden gelen istek bulunamadı."));
            return;
        }

        Player sender = Bukkit.getPlayer(senderId);
        if (sender == null || !sender.isOnline()) {
            target.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("tpa.player-not-found", "<red>Oyuncu çevrimdışı.")));
            reqs.remove(senderId);
            return;
        }

        activeMenu.put(target.getUniqueId(), senderId);
        String type = reqs.get(senderId);
        String menuId = type.equals("tpa") ? "tpa-accept-menu" : "tpa-here-accept-menu";

        Inventory inv = menuManager.create(menuId, target, sender);
        if (inv == null) return;

        if (type.equals("tpahere")) {
            String worldRawName = sender.getWorld().getName().toLowerCase();
            String matName = plugin.getConfig().getString("tpa.world-icons." + worldRawName, "MAP");
            String displayName = plugin.getConfig().getString("tpa.world-names." + worldRawName, sender.getWorld().getName());

            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.MAP;

            ItemStack icon = new ItemStack(mat);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(CC.parse("<!italic><dark_gray>Harita: <white>" + displayName));
                icon.setItemMeta(meta);
            }
            inv.setItem(22, icon);
        } else {
            ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = filler.getItemMeta();
            if (meta != null) {
                meta.displayName(CC.parse(" "));
                filler.setItemMeta(meta);
            }
            inv.setItem(22, filler);
        }

        target.openInventory(inv);
    }

    public void accept(Player target) {
        UUID senderId = activeMenu.remove(target.getUniqueId());
        LinkedHashMap<UUID, String> reqs = targetRequests.get(target.getUniqueId());

        if (reqs == null || reqs.isEmpty() || senderId == null || !reqs.containsKey(senderId)) {
            target.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("tpa.no-request", "<red>İstek bulunamadı.")));
            return;
        }

        String type = reqs.remove(senderId);
        activeSender.remove(senderId);
        target.closeInventory();

        Player sender = Bukkit.getPlayer(senderId);
        if (sender == null || !sender.isOnline()) {
            target.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("tpa.player-not-found", "<red>Oyuncu çevrimdışı.")));
            return;
        }

        int delay = plugin.getConfig().getInt("tpa.delay", 3);
            if (sender.hasPermission("trcore.tpa.bypass") || target.hasPermission("trcore.tpa.bypass")) delay = 0;

        String acceptMsg = plugin.getConfigManager().getMessages().getString("tpa.accepted", "Kabul edildi.").replace("%time%", String.valueOf(delay));
        target.sendMessage(CC.parse(acceptMsg));
        sender.sendMessage(CC.parse(acceptMsg));

        startTeleportTask(sender, target, type, delay);
    }

    public void deny(Player target, String senderName) {
        LinkedHashMap<UUID, String> reqs = targetRequests.get(target.getUniqueId());
        if (reqs == null || reqs.isEmpty()) {
            target.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("tpa.no-request", "<red>İstek bulunamadı.")));
            return;
        }

        UUID senderId = activeMenu.remove(target.getUniqueId());
        if (senderId == null) {
            if (senderName != null) {
                Player s = Bukkit.getPlayer(senderName);
                if (s != null && reqs.containsKey(s.getUniqueId())) senderId = s.getUniqueId();
            } else {
                for (UUID key : reqs.keySet()) senderId = key;
            }
        }

        if (senderId == null || !reqs.containsKey(senderId)) {
            target.sendMessage(CC.parse("<red>Bu isimde bir istek yok."));
            return;
        }

        reqs.remove(senderId);
        activeSender.remove(senderId);
        target.closeInventory();

        Player sender = Bukkit.getPlayer(senderId);
        String denyMsg = plugin.getConfigManager().getMessages().getString("tpa.denied", "<red>İstek reddedildi.");
        if (sender != null) sender.sendMessage(CC.parse(denyMsg));
        target.sendMessage(CC.parse(denyMsg));
    }

    private void startTeleportTask(Player sender, Player target, String type, int delay) {
        UUID senderId = sender.getUniqueId();
        UUID targetId = target.getUniqueId();

        ScheduledTask task = target.getScheduler().runAtFixedRate(plugin, new java.util.function.Consumer<ScheduledTask>() {
            int count = delay;
            @Override
            public void accept(ScheduledTask task) {
                if (!sender.isOnline() || !target.isOnline()) {
                    activeTeleportTasks.remove(senderId);
                    activeTeleportTasks.remove(targetId);
                    task.cancel();
                    return;
                }
                if (count <= 0) {
                    Location loc = type.equals("tpa") ? target.getLocation() : sender.getLocation();
                    Player pToTP = type.equals("tpa") ? sender : target;

                    pToTP.teleportAsync(loc).thenAccept(success -> {
                        if (success) CC.playSound(pToTP, "ENTITY_ENDERMAN_TELEPORT");
                    });

                    activeTeleportTasks.remove(senderId);
                    activeTeleportTasks.remove(targetId);
                    task.cancel();
                    return;
                }
                CC.playSound(sender, "UI_BUTTON_CLICK");
                CC.playSound(target, "UI_BUTTON_CLICK");
                count--;
            }
        }, null, 1L, 20L);

        activeTeleportTasks.put(senderId, task);
        activeTeleportTasks.put(targetId, task);
    }

    public void cancel(Player sender) {
        UUID targetId = activeSender.remove(sender.getUniqueId());
        if (targetId != null) {
            LinkedHashMap<UUID, String> reqs = targetRequests.get(targetId);
            if (reqs != null) reqs.remove(sender.getUniqueId());
            sender.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("tpa.cancelled", "<red>İptal edildi.")));
        } else {
            sender.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("tpa.no-request", "<red>İstek yok.")));
        }
    }

    public void startEvent(Player admin) {
        if (activeEvents.containsKey(admin.getUniqueId())) {
            admin.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("tpa-event.already-active", "<red>Zaten aktif!")));
            return;
        }
        activeEvents.put(admin.getUniqueId(), admin.getLocation());
        eventParticipants.put(admin.getUniqueId(), new HashSet<>());

        admin.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("tpa-event.started", "<light_purple>Etkinlik başladı!")));

        String rawBroadcast = plugin.getConfigManager().getMessages().getString("tpa-event.broadcast", "<light_purple>%player% TPA eventi başlattı!</light_purple>");
        plugin.getServer().sendMessage(CC.parse(rawBroadcast.replace("%player%", admin.getName())));

        ScheduledTask task = admin.getScheduler().runDelayed(plugin, t -> {
            stopEvent(admin, false);
        }, null, 120 * 20L);
        eventTimers.put(admin.getUniqueId(), task);
    }

    public void joinEvent(Player player, String hostName) {
        UUID targetHostUUID = null;
        if (hostName == null) {
            if (activeEvents.size() == 1) targetHostUUID = activeEvents.keySet().iterator().next();
        } else {
            Player host = Bukkit.getPlayer(hostName);
            if (host != null) targetHostUUID = host.getUniqueId();
        }

        if (targetHostUUID == null || !activeEvents.containsKey(targetHostUUID)) {
            player.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("tpa-event.expired", "<red>Süresi doldu.")));
            return;
        }

        Set<UUID> participants = eventParticipants.get(targetHostUUID);
        if (participants != null && participants.contains(player.getUniqueId())) {
            player.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("tpa-event.already-joined", "<red>Zaten katıldın.")));
            return;
        }

        Location targetLoc = activeEvents.get(targetHostUUID);

        player.teleportAsync(targetLoc).thenAccept(success -> {
            if (success) {
                if (participants != null) participants.add(player.getUniqueId());
                player.sendMessage(CC.parse(plugin.getConfigManager().getMessages().getString("tpa-event.joined", "<light_purple>Katıldın!")));
                CC.playSound(player, "ENTITY_ENDERMAN_TELEPORT");
            }
        });
    }

    public void stopEvent(Player admin, boolean force) {
        if (!activeEvents.containsKey(admin.getUniqueId()) && !force) return;

        Set<UUID> participants = eventParticipants.get(admin.getUniqueId());
        activeEvents.remove(admin.getUniqueId());
        eventParticipants.remove(admin.getUniqueId());

        if (eventTimers.containsKey(admin.getUniqueId())) {
            eventTimers.get(admin.getUniqueId()).cancel();
            eventTimers.remove(admin.getUniqueId());
        }

        String endedMsg = plugin.getConfigManager().getMessages().getString("tpa-event.ended", "<red>Etkinlik bitti.");
        if (!force) {
            if (admin.isOnline()) admin.sendMessage(CC.parse(endedMsg));
            if (participants != null) {
                for (UUID pUuid : participants) {
                    Player p = Bukkit.getPlayer(pUuid);
                    if (p != null && p.isOnline()) p.sendMessage(CC.parse(endedMsg));
                }
            }
        }
    }

    public void stopAllEvents() {
        for (ScheduledTask task : eventTimers.values()) if (task != null) task.cancel();
        eventTimers.clear();
        activeEvents.clear();
        eventParticipants.clear();
    }

    public void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        if (activeSender.containsKey(uuid)) cancel(player);
        
        ScheduledTask task = activeTeleportTasks.remove(uuid);
        if (task != null) task.cancel();

        targetRequests.remove(uuid);
        activeMenu.remove(uuid);
        pendingTarget.remove(uuid);
        pendingType.remove(uuid);
        if (activeEvents.containsKey(uuid)) stopEvent(player, true);
    }

    private boolean isInRanked(Player player) {
        if (plugin.getRankedQueueManager() != null && plugin.getRankedQueueManager().isInQueue(player)) return true;
        if (plugin.getRankedMatchManager() != null && plugin.getRankedMatchManager().getMatch(player.getUniqueId()) != null) return true;
        return false;
    }
}


