//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.mcore.managers;

import com.mcore.mCore;
import com.mcore.utils.CC;
import com.mcore.utils.TeleportMath;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class QueueManager {
    private final mCore plugin;
    private final LinkedList<Player> queue = new LinkedList();
    private final Map<UUID, Integer> timeoutTasks = new HashMap();

    public QueueManager(mCore plugin) {
        this.plugin = plugin;
    }

    public void cleanup(Player player) {
        this.leave(player);
    }

    public void toggle(Player p) {
        if (this.queue.contains(p)) {
            this.leave(p);
        } else {
            this.join(p);
        }

    }

    public void join(Player p) {
        this.queue.add(p);
        p.sendMessage(CC.get("rtp-queue.joined", "%current%", String.valueOf(this.queue.size())));
        this.playMusic(p);
        int timeoutSec = this.plugin.getConfig().getInt("rtp-queue.timeout-seconds", 96);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (this.queue.contains(p)) {
                this.leave(p);
                p.sendMessage(CC.get("rtp-queue.timeout"));
                this.playSound(p, "rtp-queue.timeout-sound");
            }

        }, (long)timeoutSec * 20L);
        this.timeoutTasks.put(p.getUniqueId(), task.getTaskId());
        this.checkMatch();
    }

    public void leave(Player p) {
        if (this.queue.remove(p)) {
            p.sendMessage(CC.get("rtp-queue.left"));
            this.stopMusic(p);
            if (this.timeoutTasks.containsKey(p.getUniqueId())) {
                Bukkit.getScheduler().cancelTask((Integer)this.timeoutTasks.remove(p.getUniqueId()));
            }
        }

    }

    private void playMusic(Player p) {
        String soundName = this.plugin.getConfig().getString("rtp-queue.waiting-sound");
        if (soundName != null && !soundName.isEmpty()) {
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                p.playSound(p.getLocation(), sound, 10000.0F, 1.0F);
            } catch (IllegalArgumentException var4) {
            }

        }
    }

    private void stopMusic(Player p) {
        String soundName = this.plugin.getConfig().getString("rtp-queue.waiting-sound");
        if (soundName != null && !soundName.isEmpty()) {
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                p.stopSound(sound);
            } catch (IllegalArgumentException var4) {
            }

        }
    }

    private void playSound(Player p, String path) {
        String soundName = this.plugin.getConfig().getString(path);
        if (soundName != null && !soundName.isEmpty()) {
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                p.playSound(p.getLocation(), sound, 10000.0F, 1.0F);
            } catch (IllegalArgumentException var5) {
            }

        }
    }

    private void checkMatch() {
        if (this.queue.size() >= 2) {
            Player p1 = (Player)this.queue.poll();
            Player p2 = (Player)this.queue.poll();
            this.cleanupMatchFound(p1);
            this.cleanupMatchFound(p2);
            this.startMatch(p1, p2);
        }

    }

    private void cleanupMatchFound(Player p) {
        this.stopMusic(p);
        if (this.timeoutTasks.containsKey(p.getUniqueId())) {
            Bukkit.getScheduler().cancelTask((Integer)this.timeoutTasks.remove(p.getUniqueId()));
        }

        this.playSound(p, "rtp-queue.match-sound");
    }

    public int getQueueSize() {
        return this.queue.size();
    }

    private void startMatch(Player p1, Player p2) {
        p1.sendMessage(CC.get("rtp-queue.matched"));
        p2.sendMessage(CC.get("rtp-queue.matched"));
        String wName = this.plugin.getConfig().getString("rtp-queue.world");
        World world = Bukkit.getWorld(wName);
        if (world != null) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                int min = this.plugin.getConfig().getInt("rtp-queue.min-range");
                int max = this.plugin.getConfig().getInt("rtp-queue.max-range");
                int x = ThreadLocalRandom.current().nextInt(min, max) * (ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
                int z = ThreadLocalRandom.current().nextInt(min, max) * (ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
                int y = world.getHighestBlockYAt(x, z) + 1;
                Location center = new Location(world, (double)x, (double)y, (double)z);
                double dist = this.plugin.getConfig().getDouble("rtp-queue.distance-between-players");
                Location[] locs = TeleportMath.getFacingLocs(center, dist);
                Bukkit.getScheduler().runTask(this.plugin, () -> {
                    p1.teleport(locs[0]);
                    p2.teleport(locs[1]);
                    String cmd = this.plugin.getConfig().getString("rtp-queue.command");
                    if (cmd != null && !cmd.isEmpty()) {
                        try {
                            p1.performCommand(cmd.replace("%player%", p1.getName()));
                            p2.performCommand(cmd.replace("%player%", p2.getName()));
                        } catch (Exception var6) {
                        }
                    }

                });
            });
        }
    }
}
