//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.mcore.managers;

import com.mcore.mCore;
import com.mcore.utils.CC;
import com.mcore.utils.TeleportMath;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class DuelManager {
    private final mCore plugin;
    private final MenuManager menuManager;
    private final Map<UUID, UUID> invites = new HashMap();

    public DuelManager(mCore plugin, MenuManager menuManager) {
        this.plugin = plugin;
        this.menuManager = menuManager;
    }

    public void cleanup(Player player) {
        this.invites.remove(player.getUniqueId());
    }

    public void invite(Player sender, Player target) {
        this.invites.put(target.getUniqueId(), sender.getUniqueId());
        sender.sendMessage(CC.get("rtp-duel.sent", "%target%", target.getName()));
        target.sendMessage(CC.get("rtp-duel.received", "%player%", sender.getName()));
    }

    public void openAcceptMenu(Player target) {
        UUID senderId = (UUID)this.invites.get(target.getUniqueId());
        if (senderId == null) {
            target.sendMessage(CC.get("tpa.no-request"));
        } else {
            Player sender = Bukkit.getPlayer(senderId);
            Inventory inv = this.menuManager.create("duel-accept-menu", sender);
            target.openInventory(inv);
        }
    }

    public void accept(Player target) {
        UUID senderId = (UUID)this.invites.remove(target.getUniqueId());
        if (senderId != null) {
            Player sender = Bukkit.getPlayer(senderId);
            target.closeInventory();
            if (sender == null) {
                target.sendMessage(CC.parse("<red>Rakip oyundan çıkmış."));
            } else {
                this.startDuel(sender, target);
            }
        }
    }

    private void startDuel(Player sender, Player target) {
        String worldName = this.plugin.getConfig().getString("rtp-duel.world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(CC.parse("<red>Hata: Düello dünyası bulunamadı."));
            target.sendMessage(CC.parse("<red>Hata: Düello dünyası bulunamadı."));
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                int min = this.plugin.getConfig().getInt("rtp-duel.min-range");
                int max = this.plugin.getConfig().getInt("rtp-duel.max-range");
                int x = ThreadLocalRandom.current().nextInt(min, max) * (ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
                int z = ThreadLocalRandom.current().nextInt(min, max) * (ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
                int y = world.getHighestBlockYAt(x, z) + 1;
                double dist = this.plugin.getConfig().getDouble("rtp-duel.distance-between-players");
                Location[] locs = TeleportMath.getFacingLocs(new Location(world, (double)x, (double)y, (double)z), dist);
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    sender.teleport(locs[0]);
                    target.teleport(locs[1]);
                    String cmd = this.plugin.getConfig().getString("rtp-duel.command");
                    if (cmd != null && !cmd.isEmpty()) {
                        sender.performCommand(cmd.replace("%player%", sender.getName()));
                        target.performCommand(cmd.replace("%player%", target.getName()));
                    }

                });
            });
        }
    }

    public void remove(Player p) {
        this.invites.remove(p.getUniqueId());
    }
}
