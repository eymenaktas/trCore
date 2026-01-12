package com.mcore.managers;

import com.mcore.mCore;
import com.mcore.utils.CC;
import com.mcore.utils.TeleportMath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class DuelManager {
    private final mCore plugin;
    private final MenuManager menuManager;
    private final Map<UUID, UUID> invites = new HashMap<>();

    public DuelManager(mCore plugin, MenuManager menuManager) {
        this.plugin = plugin;
        this.menuManager = menuManager;
    }

    public void cleanup(Player player) {
        invites.remove(player.getUniqueId());
    }

    public void invite(Player sender, Player target) {
        invites.put(target.getUniqueId(), sender.getUniqueId());
        sender.sendMessage(CC.get("rtp-duel.sent", "%target%", target.getName()));
        target.sendMessage(CC.get("rtp-duel.received", "%player%", sender.getName()));
        playSound(target, "tpa.sound-on-request");
    }

    public void openAcceptMenu(Player target) {
        UUID senderId = invites.get(target.getUniqueId());
        if (senderId == null) {
            target.sendMessage(CC.get("tpa.no-request"));
            return;
        }
        Player sender = Bukkit.getPlayer(senderId);

        Inventory inv = menuManager.create("duel-accept-menu", sender);
        if (inv == null) {
            target.sendMessage(CC.parse("<red>Hata: Menü dosyası (duel-accept-menu.yml) bulunamadı!"));
            return;
        }
        target.openInventory(inv);
    }

    public void accept(Player target) {
        UUID senderId = invites.remove(target.getUniqueId());
        if (senderId == null) return;

        Player sender = Bukkit.getPlayer(senderId);
        target.closeInventory();

        if (sender == null) {
            target.sendMessage(CC.parse("<red>Rakip oyundan çıkmış."));
            return;
        }
        startDuel(sender, target);
    }

    private void startDuel(Player sender, Player target) {
        String worldName = plugin.getConfig().getString("rtp-duel.world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            sender.sendMessage(CC.parse("<red>Hata: Düello dünyası bulunamadı."));
            target.sendMessage(CC.parse("<red>Hata: Düello dünyası bulunamadı."));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int min = plugin.getConfig().getInt("rtp-duel.min-range");
            int max = plugin.getConfig().getInt("rtp-duel.max-range");

            int x = ThreadLocalRandom.current().nextInt(min, max) * (ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
            int z = ThreadLocalRandom.current().nextInt(min, max) * (ThreadLocalRandom.current().nextBoolean() ? 1 : -1);

            // Merkez noktanın Y'si
            int y = world.getHighestBlockYAt(x, z) + 1;

            double dist = plugin.getConfig().getDouble("rtp-duel.distance-between-players");
            Location[] locs = TeleportMath.getFacingLocs(new Location(world, x, y, z), dist);

            Bukkit.getScheduler().runTask(plugin, () -> {
                // --- DÜZELTME BURADA ---
                // Tıpkı QueueManager'daki gibi, ışınlanacakları noktaların
                // Yüksekliğini tekrar hesaplıyoruz.
                locs[0].setY(world.getHighestBlockYAt(locs[0]) + 1);
                locs[1].setY(world.getHighestBlockYAt(locs[1]) + 1);
                // -----------------------

                sender.teleport(locs[0]);
                target.teleport(locs[1]);

                String cmd = plugin.getConfig().getString("rtp-duel.command");
                if (cmd != null && !cmd.isEmpty()) {
                    sender.performCommand(cmd.replace("%player%", sender.getName()));
                    target.performCommand(cmd.replace("%player%", target.getName()));
                }
            });
        });
    }

    public void remove(Player p) { invites.remove(p.getUniqueId()); }

    @SuppressWarnings("deprecation")
    private void playSound(Player player, String configPath) {
        String soundName = plugin.getConfig().getString(configPath);
        if (soundName != null && !soundName.isEmpty()) {
            try {
                player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase()), 1.0f, 1.0f);
            } catch (Exception e) {}
        }
    }
}