package com.mcore.managers;

import com.mcore.mCore;
import com.mcore.utils.CC;
import com.mcore.utils.TeleportMath; // EKLENDİ (Eğer utils paketindeyse)
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class QueueManager {
    private final mCore plugin;
    private final LinkedList<Player> queue = new LinkedList<>();
    private final Map<UUID, Integer> timeoutTasks = new HashMap<>();

    public QueueManager(mCore plugin) { this.plugin = plugin; }

    // DÜZELTME: cleanup metodu eklendi (ConnectionListener için gerekli)
    public void cleanup(Player player) {
        leave(player);
    }

    public void toggle(Player p) {
        if (queue.contains(p)) leave(p);
        else join(p);
    }

    public void join(Player p) {
        queue.add(p);
        p.sendMessage(CC.get("rtp-queue.joined", "%current%", String.valueOf(queue.size())));

        playMusic(p);

        int timeoutSec = plugin.getConfig().getInt("rtp-queue.timeout-seconds", 96);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (queue.contains(p)) {
                leave(p);
                p.sendMessage(CC.get("rtp-queue.timeout"));
                playSound(p, "rtp-queue.timeout-sound");
            }
        }, timeoutSec * 20L);
        timeoutTasks.put(p.getUniqueId(), task.getTaskId());

        checkMatch();
    }

    public void leave(Player p) {
        if (queue.remove(p)) {
            p.sendMessage(CC.get("rtp-queue.left"));
            stopMusic(p);
            if (timeoutTasks.containsKey(p.getUniqueId())) {
                Bukkit.getScheduler().cancelTask(timeoutTasks.remove(p.getUniqueId()));
            }
        }
    }

    private void playMusic(Player p) {
        String soundName = plugin.getConfig().getString("rtp-queue.waiting-sound");
        if (soundName == null || soundName.isEmpty()) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            p.playSound(p.getLocation(), sound, 10000f, 1f);
        } catch (IllegalArgumentException ignored) {}
    }

    private void stopMusic(Player p) {
        String soundName = plugin.getConfig().getString("rtp-queue.waiting-sound");
        if (soundName == null || soundName.isEmpty()) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            p.stopSound(sound);
        } catch (IllegalArgumentException ignored) {}
    }

    private void playSound(Player p, String path) {
        String soundName = plugin.getConfig().getString(path);
        if (soundName == null || soundName.isEmpty()) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            p.playSound(p.getLocation(), sound, 10000f, 1f);
        } catch (IllegalArgumentException ignored) {}
    }

    private void checkMatch() {
        if (queue.size() >= 2) {
            Player p1 = queue.poll();
            Player p2 = queue.poll();

            cleanupMatchFound(p1);
            cleanupMatchFound(p2);

            startMatch(p1, p2);
        }
    }

    private void cleanupMatchFound(Player p) {
        stopMusic(p);
        if (timeoutTasks.containsKey(p.getUniqueId())) {
            Bukkit.getScheduler().cancelTask(timeoutTasks.remove(p.getUniqueId()));
        }
        playSound(p, "rtp-queue.match-sound");
    }

    public int getQueueSize() { return queue.size(); }

    private void startMatch(Player p1, Player p2) {
        p1.sendMessage(CC.get("rtp-queue.matched"));
        p2.sendMessage(CC.get("rtp-queue.matched"));

        String wName = plugin.getConfig().getString("rtp-queue.world");
        World world = Bukkit.getWorld(wName);
        if (world == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int min = plugin.getConfig().getInt("rtp-queue.min-range");
            int max = plugin.getConfig().getInt("rtp-queue.max-range");
            int x = ThreadLocalRandom.current().nextInt(min, max) * (ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
            int z = ThreadLocalRandom.current().nextInt(min, max) * (ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
            int y = world.getHighestBlockYAt(x, z) + 1;

            Location center = new Location(world, x, y, z);
            double dist = plugin.getConfig().getDouble("rtp-queue.distance-between-players");
            Location[] locs = TeleportMath.getFacingLocs(center, dist);

            Bukkit.getScheduler().runTask(plugin, () -> {
                p1.teleport(locs[0]);
                p2.teleport(locs[1]);
                String cmd = plugin.getConfig().getString("rtp-queue.command");
                if (cmd != null && !cmd.isEmpty()) {
                    try {
                        p1.performCommand(cmd.replace("%player%", p1.getName()));
                        p2.performCommand(cmd.replace("%player%", p2.getName()));
                    } catch (Exception e) {}
                }
            });
        });
    }
}