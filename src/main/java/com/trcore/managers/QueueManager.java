package com.trcore.managers;

import com.trcore.TRCore;
import com.trcore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Folia-uyumlu, tamamen asenkron RTP Queue yöneticisi.
 *
 * Teleport pipeline:
 *  1. AsyncScheduler → konum hesapla (main thread dışı)
 *  2. world.getChunkAtAsync() → chunk yükle (I/O block yok)
 *  3. RegionScheduler → getHighestBlockYAt (chunk loaded, anlık)
 *  4. teleportAsync() → Paper async teleport API
 *
 * Spark profilinde Bukkit / main thread'de görünmez.
 */
public class QueueManager {

    private final TRCore plugin;
    // ConcurrentHashMap: join/leave async context'ten de çağrılabilir
    private final LinkedList<Player> queue = new LinkedList<>();
    private final Map<UUID, ScheduledTask> timeoutTasks = new ConcurrentHashMap<>();

    // Cached config — her tick'te config okuması yok
    private String matchCommand;
    private int minRange;
    private int maxRange;
    private int timeoutSec;
    private List<String> queueWorlds;

    public QueueManager(TRCore plugin) {
        this.plugin = plugin;
        loadSettings();
    }

    public void loadSettings() {
        matchCommand  = plugin.getConfig().getString("rtp-queue.command", "");
        minRange      = plugin.getConfig().getInt("rtp-queue.min-range", 100);
        maxRange      = plugin.getConfig().getInt("rtp-queue.max-range", 6000);
        timeoutSec    = plugin.getConfig().getInt("rtp-queue.timeout-seconds", 96);
        queueWorlds   = plugin.getConfigManager().queueWorlds;
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    public void toggle(Player p) {
        if (queue.contains(p)) leave(p);
        else join(p);
    }

    public boolean isInQueue(Player p) {
        return queue.contains(p);
    }

    public synchronized void join(Player p) {
        if (queue.contains(p)) return;
        if (plugin.getRankedQueueManager() != null && plugin.getRankedQueueManager().isInQueue(p)) {
            p.sendMessage(CC.parse("<red>Önce bulunduğun sıradan çıkmalısın!</red>"));
            return;
        }
        
        queue.add(p);
        p.sendMessage(CC.get("rtp-queue.joined", "%current%", String.valueOf(queue.size())));

        // Timeout task — player scheduler (Folia: player'a bağlı region)
        ScheduledTask task = p.getScheduler().runDelayed(plugin, t -> {
            if (queue.contains(p)) {
                leave(p);
                p.sendMessage(CC.get("rtp-queue.timeout"));
            }
        }, null, timeoutSec * 20L);

        // task null dönebilir (player offline olursa)
        if (task != null) timeoutTasks.put(p.getUniqueId(), task);

        checkMatch();
    }

    public synchronized void leave(Player p) {
        if (queue.remove(p)) {
            p.sendMessage(CC.get("rtp-queue.left"));
            cancelTimeout(p.getUniqueId());
        }
    }

    /** Oyuncu çıkışında çağrılır — mesaj göndermeden temizler. */
    public synchronized void cleanup(Player player) {
        queue.remove(player);
        cancelTimeout(player.getUniqueId());
    }

    // ------------------------------------------------------------------
    // Eşleşme
    // ------------------------------------------------------------------

    private void checkMatch() {
        if (queue.size() < 2) return;

        Player p1 = queue.poll();
        Player p2 = queue.poll();
        if (p1 == null || p2 == null) return;

        cancelTimeout(p1.getUniqueId());
        cancelTimeout(p2.getUniqueId());

        p1.sendMessage(CC.get("rtp-queue.matched"));
        p2.sendMessage(CC.get("rtp-queue.matched"));

        startMatch(p1, p2);
    }

    // ------------------------------------------------------------------
    // Tamamen async teleport pipeline
    // ------------------------------------------------------------------

    private void startMatch(Player p1, Player p2) {
        if (queueWorlds == null || queueWorlds.isEmpty()) {
            p1.sendMessage(CC.parse("<red>Hata: RTP Queue dunyasi yapilandirilmamis!"));
            return;
        }

        String wName = queueWorlds.get(ThreadLocalRandom.current().nextInt(queueWorlds.size()));
        World world = Bukkit.getWorld(wName);
        if (world == null) {
            p1.sendMessage(CC.parse("<red>Hata: " + wName + " dunyasi bulunamadi!"));
            return;
        }

        Location[] locs = plugin.getRTPManager().getBufferedPair(wName);
        if (locs != null) {
            teleportMatch(p1, p2, locs);
        } else {
            // Buffer boşsa bilgilendir
            net.kyori.adventure.text.Component msg = CC.get("rtp-queue.searching-location");
            p1.sendMessage(msg);
            p2.sendMessage(msg);

            // Fallback: On-demand asenkron arama (RTPManager içinde 10 denemeli)
            plugin.getRTPManager().findPair(world, new RTPManager.RTPWorld(minRange, maxRange, false), foundLocs -> {
                if (foundLocs != null) {
                    teleportMatch(p1, p2, foundLocs);
                } else {
                    net.kyori.adventure.text.Component error = CC.get("rtp.no-location");
                    p1.sendMessage(error);
                    p2.sendMessage(error);
                }
            });
        }
    }

    private void teleportMatch(Player p1, Player p2, Location[] locs) {
        p1.teleportAsync(locs[0]).thenAccept(ok -> {
            if (ok && matchCommand != null && !matchCommand.isEmpty()) {
                p1.getScheduler().run(plugin, t -> p1.performCommand(matchCommand.replace("%player%", p1.getName())), null);
            }
        });

        p2.teleportAsync(locs[1]).thenAccept(ok -> {
            if (ok && matchCommand != null && !matchCommand.isEmpty()) {
                p2.getScheduler().run(plugin, t -> p2.performCommand(matchCommand.replace("%player%", p2.getName())), null);
            }
        });
    }

    // ------------------------------------------------------------------
    // Yardımcı
    // ------------------------------------------------------------------

    private void cancelTimeout(UUID uuid) {
        ScheduledTask t = timeoutTasks.remove(uuid);
        if (t != null) t.cancel();
    }
}
