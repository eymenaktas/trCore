package com.trcore.managers;

import com.trcore.TRCore;
import com.trcore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class DuelManager {
    private final TRCore plugin;
    private final MenuManager menuManager;

    private final Map<UUID, DuelRequest> invites = new HashMap<>();
    private final Map<UUID, SetupSession> setupSessions = new HashMap<>();

    public DuelManager(TRCore plugin, MenuManager menuManager) {
        this.plugin = plugin;
        this.menuManager = menuManager;
    }

    public void cleanup(Player player) {
        invites.remove(player.getUniqueId());
        setupSessions.remove(player.getUniqueId());
    }

    // --- HAZIRLIK MENÜSÜ AÇMA ---
    public void openSetupMenu(Player sender, Player target) {
        if (target.equals(sender)) {
            sender.sendMessage(CC.parse("<red>Kendine düello atamazsın."));
            return;
        }
        if (!target.isOnline()) {
            sender.sendMessage(CC.parse("<red>Oyuncu çevrimiçi değil."));
            return;
        }
        if (!plugin.getToggleManager().isDuelEnabled(target.getUniqueId()) && !sender.hasPermission("trcore.admin")) {
            sender.sendMessage(CC.get("toggle.target-disabled"));
            return;
        }
        setupSessions.put(sender.getUniqueId(), new SetupSession(target.getUniqueId(), 0));
        sender.getScheduler().run(plugin, t -> updateSetupMenu(sender), null);
    }

    public void updateSetupMenu(Player sender) {
        SetupSession session = setupSessions.get(sender.getUniqueId());
        if (session == null) return;

        Player target = Bukkit.getPlayer(session.targetId);
        if (target == null) {
            sender.closeInventory();
            sender.sendMessage(CC.parse("<red>Oyuncu oyundan çıktı."));
            return;
        }

        List<String> worlds = plugin.getConfigManager().duelWorlds;
        if (worlds == null || worlds.isEmpty()) {
            sender.sendMessage(CC.parse("<red>Config hatası: Hiç dünya yok!"));
            return;
        }

        if (session.worldIndex >= worlds.size()) session.worldIndex = 0;

        String currentWorldID = worlds.get(session.worldIndex);
        String displayName = plugin.getConfig().getString("rtp-duel.world-names." + currentWorldID, currentWorldID);

        Inventory inv = Bukkit.createInventory(new SetupHolder(), 27, CC.parse("<black>Düello Ayarları"));

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        headMeta.setOwningPlayer(target);
        headMeta.displayName(CC.parse("<!italic><yellow>Rakip: <white>" + target.getName()));
        head.setItemMeta(headMeta);
        inv.setItem(13, head);

        String matName = plugin.getConfig().getString("rtp-duel.world-icons." + currentWorldID, "GRASS_BLOCK");
        Material worldMat = Material.matchMaterial(matName);
        if (worldMat == null) worldMat = Material.GRASS_BLOCK;

        ItemStack worldItem = new ItemStack(worldMat);
        ItemMeta worldMeta = worldItem.getItemMeta();
        worldMeta.displayName(CC.parse("<!italic><dark_gray>Harita: <green>" + displayName));
        worldMeta.lore(List.of(CC.parse("<!italic><gray>Değiştirmek için tıkla!")));
        worldItem.setItemMeta(worldMeta);
        inv.setItem(22, worldItem);

        ItemStack sendBtn = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta sendMeta = sendBtn.getItemMeta();
        sendMeta.displayName(CC.parse("<!italic><green><bold>İsteği Gönder"));
        sendBtn.setItemMeta(sendMeta);
        inv.setItem(15, sendBtn);

        ItemStack cancelBtn = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancelBtn.getItemMeta();
        cancelMeta.displayName(CC.parse("<!italic><red><bold>İptal Et"));
        cancelBtn.setItemMeta(cancelMeta);
        inv.setItem(11, cancelBtn);

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(CC.parse(" "));
        filler.setItemMeta(fillerMeta);
        for(int i=0; i<27; i++) { if(inv.getItem(i) == null) inv.setItem(i, filler); }

        sender.openInventory(inv);
    }

    public void handleSetupClick(Player sender, int slot) {
        SetupSession session = setupSessions.get(sender.getUniqueId());
        if (session == null) return;

        if (slot == 22) {
            session.worldIndex++;
            playSound(sender, "UI_BUTTON_CLICK");
            updateSetupMenu(sender);
        }
        else if (slot == 15) {
            List<String> worlds = plugin.getConfigManager().duelWorlds;
            if (session.worldIndex < worlds.size()) {
                String selectedWorld = worlds.get(session.worldIndex);
                Player target = Bukkit.getPlayer(session.targetId);

                if (target != null) {
                    if (!plugin.getToggleManager().isDuelEnabled(target.getUniqueId()) && !sender.hasPermission("trcore.admin")) {
                        sender.closeInventory();
                        sender.sendMessage(CC.get("toggle.target-disabled"));
                        return;
                    }
                    sender.closeInventory();
                    invite(sender, target, selectedWorld);
                } else {
                    sender.closeInventory();
                    sender.sendMessage(CC.parse("<red>Oyuncu oyundan çıktı."));
                }
            }
        }
        else if (slot == 11) {
            sender.closeInventory();
            sender.sendMessage(CC.parse("<red>İşlem iptal edildi."));
            setupSessions.remove(sender.getUniqueId());
        }
    }

    private void invite(Player sender, Player target, String worldName) {
        if (target == null || !target.isOnline()) {
            sender.sendMessage(CC.parse("<red>Oyuncu artık çevrimiçi değil."));
            return;
        }

        setupSessions.remove(sender.getUniqueId());
        invites.put(target.getUniqueId(), new DuelRequest(sender.getUniqueId(), worldName));

        sender.sendMessage(CC.get("rtp-duel.sent", "%target%", target.getName()));
        target.sendMessage(CC.get("rtp-duel.received", "%player%", sender.getName()));
        playSound(target, "tpa.sound-on-request");
    }

    public void openAcceptMenu(Player target) {
        DuelRequest req = invites.get(target.getUniqueId());
        if (req == null) {
            target.sendMessage(CC.get("tpa.no-request"));
            return;
        }
        Player sender = Bukkit.getPlayer(req.senderId);

        target.getScheduler().run(plugin, t -> {
            Inventory inv = menuManager.create("duel-accept-menu", target, sender);
            if (inv == null) {
                target.sendMessage(CC.parse("<red>Hata: Menü dosyası bulunamadı!"));
                return;
            }

            String matName = plugin.getConfig().getString("rtp-duel.world-icons." + req.worldName, "PAPER");
            String displayName = plugin.getConfig().getString("rtp-duel.world-names." + req.worldName, req.worldName);

            Material mat = Material.matchMaterial(matName);
            if (mat != null) {
                ItemStack icon = new ItemStack(mat);
                ItemMeta meta = icon.getItemMeta();
                meta.displayName(CC.parse("<!italic><dark_gray>Harita: <white>" + displayName));
                icon.setItemMeta(meta);
                inv.setItem(22, icon);
            }
            target.openInventory(inv);
        }, null);
    }

    public void accept(Player target) {
        DuelRequest req = invites.remove(target.getUniqueId());
        if (req == null) return;

        Player sender = Bukkit.getPlayer(req.senderId);
        target.closeInventory();

        if (sender == null) {
            target.sendMessage(CC.parse("<red>Rakip oyundan çıkmış."));
            return;
        }
        startDuel(sender, target, req.worldName);
    }

    private void startDuel(Player sender, Player target, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(CC.parse("<red>Hata: " + worldName + " dünyası bulunamadı."));
            target.sendMessage(CC.parse("<red>Hata: " + worldName + " dünyası bulunamadı."));
            return;
        }

        // Buffer'dan çift al
        Location[] locs = plugin.getRTPManager().getBufferedPair(worldName);
        if (locs != null) {
            teleportMatch(sender, target, locs);
        } else {
            // Buffer boşsa (beklenmedik), on-demand fallback ama asenkron ve güvenli
            plugin.getRTPManager().findPair(world, new RTPManager.RTPWorld(
                    plugin.getConfig().getInt("rtp-duel.min-range"),
                    plugin.getConfig().getInt("rtp-duel.max-range"),
                    false
            ), foundLocs -> {
                if (foundLocs != null) {
                    teleportMatch(sender, target, foundLocs);
                } else {
                    sender.sendMessage(CC.parse("<red>Hata: Uygun konum bulunamadı. Lütfen tekrar dene."));
                    target.sendMessage(CC.parse("<red>Hata: Uygun konum bulunamadı. Lütfen tekrar dene."));
                }
            });
        }
    }

    private void teleportMatch(Player sender, Player target, Location[] locs) {
        String cmd = plugin.getConfig().getString("rtp-duel.command");

        sender.teleportAsync(locs[0]).thenAccept(success -> {
            if (success && cmd != null && !cmd.isEmpty()) {
                sender.getScheduler().run(plugin, t -> sender.performCommand(cmd.replace("%player%", sender.getName())), null);
            }
        });

        target.teleportAsync(locs[1]).thenAccept(success -> {
            if (success && cmd != null && !cmd.isEmpty()) {
                target.getScheduler().run(plugin, t -> target.performCommand(cmd.replace("%player%", target.getName())), null);
            }
        });
    }

    public void remove(Player p) { invites.remove(p.getUniqueId()); }

    private void playSound(Player player, String configPath) {
        String soundName = plugin.getConfig().getString(configPath);
        if ("UI_BUTTON_CLICK".equals(configPath)) soundName = "UI_BUTTON_CLICK";
        if (soundName != null && !soundName.isEmpty()) {
            CC.playSound(player, soundName.toUpperCase());
        }
    }

    private static class DuelRequest {
        final UUID senderId;
        final String worldName;
        DuelRequest(UUID senderId, String worldName) { this.senderId = senderId; this.worldName = worldName; }
    }
    private static class SetupSession {
        final UUID targetId;
        int worldIndex;
        SetupSession(UUID targetId, int worldIndex) { this.targetId = targetId; this.worldIndex = worldIndex; }
    }
    public static class SetupHolder implements org.bukkit.inventory.InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
}


