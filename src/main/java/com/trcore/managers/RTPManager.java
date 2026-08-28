package com.trcore.managers;

import com.trcore.TRCore;
import com.trcore.utils.TeleportMath;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Folia/Leaf-uyumlu, sıfır-server-thread RTP yöneticisi (v3).
 *
 * TEmel değişiklik — getHighestBlockYAt sorunu:
 *  Eski: world.getHighestBlockYAt(x, z)
 *        → CraftRegionAccessor üzerinden bölge thread'ine girer (profil: 0.01%)
 *
 *  Yeni: chunk.getChunkSnapshot(includeMaxBlockY=true, ...)
 *        → getHighestBlockYAt(relX, relZ) snapshot üzerinden
 *        → immutable, herhangi bir thread'den okunabilir, sıfır lock
 *
 *  isLocationSafe de artık snapshot ile çalışır — Block.getType() çağrısı yok.
 *
 * Yük patlaması koruması:
 *  - activeTasks AtomicInteger semaphore olarak kullanılır.
 *  - Filler tick'i başında activeTasks >= maxConcurrentFills ise o dünya atlanır.
 *  - 50 oyuncu eş zamanlı RTP yapsa bile filler thread pool'u basmaz.
 */
public class RTPManager {

    private final TRCore plugin;
    private File rtpFile;
    private FileConfiguration rtpConfig;

    private final Map<String, RTPWorld>          worlds      = new HashMap<>();
    private final Map<String, Deque<Location>>   buffers     = new HashMap<>();
    private final Map<String, Deque<Location[]>> pairBuffers = new HashMap<>();
    private ScheduledTask fillerTask;

    // Cached config değerleri
    private int    bufferSize        = 100;
    private int    pairBufferSize    = 50;
    private int    fillInterval      = 20;
    private int    retryCount        = 10;
    private int    maxConcurrentFills= 8;
    private double duelDistance      = 12.0;

    /** Anlık aktif chunk-load + snapshot işlemi sayısı. */
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    // Tehlikeli zemin blokları — EnumSet: O(1) contains, bit-mask
    private static final EnumSet<Material> UNSAFE_FLOOR = EnumSet.of(
            Material.LAVA,
            Material.WATER,
            Material.FIRE,
            Material.MAGMA_BLOCK,
            Material.CACTUS,
            Material.SWEET_BERRY_BUSH,
            Material.WITHER_ROSE,
            Material.COBWEB
    );

    // Geçilebilir (havaya sayılan) bloklar — ayak/baş olabilir
    private static final EnumSet<Material> PASSABLE = EnumSet.of(
            Material.AIR,
            Material.CAVE_AIR,
            Material.VOID_AIR,
            Material.SHORT_GRASS,
            Material.TALL_GRASS,
            Material.FERN,
            Material.LARGE_FERN,
            Material.SNOW,
            Material.SEAGRASS
    );

    public RTPManager(TRCore plugin) {
        this.plugin = plugin;
        load();
        startFiller();
    }

    // ──────────────────────────────────────────────────────────────────
    // Config yükleme
    // ──────────────────────────────────────────────────────────────────

    public void load() {
        worlds.clear();

        rtpFile = new File(plugin.getDataFolder(), "rtp.yml");
        if (!rtpFile.exists()) plugin.saveResource("rtp.yml", false);

        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(rtpFile), StandardCharsets.UTF_8)) {
            rtpConfig = YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            rtpConfig = YamlConfiguration.loadConfiguration(rtpFile);
        }

        ConfigurationSection section = rtpConfig.getConfigurationSection("worlds");
        if (section == null) return;

        for (String worldName : section.getKeys(false)) {
            int min    = section.getInt(worldName + ".min", 100);
            int max    = section.getInt(worldName + ".max", 5000);
            boolean wb = section.getBoolean(worldName + ".use-worldborder", false);
            worlds.put(worldName, new RTPWorld(min, max, wb));
            buffers.computeIfAbsent(worldName, k -> new ConcurrentLinkedDeque<>());
            pairBuffers.computeIfAbsent(worldName, k -> new ConcurrentLinkedDeque<>());
        }

        this.bufferSize         = rtpConfig.getInt("settings.buffer-size", 100);
        this.pairBufferSize     = rtpConfig.getInt("settings.pair-buffer-size", 50);
        this.fillInterval       = rtpConfig.getInt("settings.fill-interval-ticks", 20);
        this.retryCount         = rtpConfig.getInt("settings.retry-count", 10);
        this.maxConcurrentFills = rtpConfig.getInt("settings.max-concurrent-fills", 8);
        this.duelDistance       = plugin.getConfig().getDouble("rtp-duel.distance-between-players", 12.0);
    }

    // ──────────────────────────────────────────────────────────────────
    // Teleport
    // ──────────────────────────────────────────────────────────────────

    public void teleport(Player player, String worldName, Consumer<Location> callback) {
        Deque<Location> buffer = buffers.get(worldName);
        if (buffer != null) {
            Location loc = buffer.pollFirst();
            if (loc != null) {
                player.teleportAsync(loc).thenAccept(ok -> {
                    if (callback != null)
                        player.getScheduler().run(plugin, t -> callback.accept(ok ? loc : null), null);
                });
                return;
            }
        }

        RTPWorld cfg = worlds.get(worldName);
        if (cfg == null)  { if (callback != null) callback.accept(null); return; }
        World world = Bukkit.getWorld(worldName);
        if (world == null){ if (callback != null) callback.accept(null); return; }

        findLocation(world, cfg, loc -> {
            if (loc == null) { if (callback != null) callback.accept(null); return; }
            player.teleportAsync(loc).thenAccept(ok -> {
                if (callback != null)
                    player.getScheduler().run(plugin, t -> callback.accept(ok ? loc : null), null);
            });
        });
    }

    // ──────────────────────────────────────────────────────────────────
    // Buffer Filler — semaphore korumalı
    // ──────────────────────────────────────────────────────────────────

    private void startFiller() {
        if (fillerTask != null) fillerTask.cancel();

        fillerTask = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, t -> {
            for (Map.Entry<String, Deque<Location>> entry : buffers.entrySet()) {
                String worldName = entry.getKey();
                Deque<Location>   buffer = entry.getValue();
                RTPWorld cfg = worlds.get(worldName);
                World    world = Bukkit.getWorld(worldName);
                if (cfg == null || world == null) continue;

                if (buffer.size() < bufferSize && activeTasks.get() < maxConcurrentFills) {
                    findLocation(world, cfg, loc -> { if (loc != null) buffer.addLast(loc); });
                }

                Deque<Location[]> pairBuffer = pairBuffers.get(worldName);
                if (pairBuffer != null && pairBuffer.size() < pairBufferSize
                        && activeTasks.get() < maxConcurrentFills) {
                    findPair(world, cfg, pair -> { if (pair != null) pairBuffer.addLast(pair); });
                }
            }
        }, fillInterval * 50L, fillInterval * 50L, TimeUnit.MILLISECONDS);
    }

    // ──────────────────────────────────────────────────────────────────
    // findPair — ChunkSnapshot tabanlı, sıfır server-thread
    // ──────────────────────────────────────────────────────────────────

    public void findPair(World world, RTPWorld cfg, Consumer<Location[]> result) {
        findPairRecursive(world, cfg, result, 0);
    }

    /**
     * Her iki chunk paralel yüklenir (CompletableFuture.allOf).
     * Snapshot içinden getHighestBlockYAt → server thread'e dokunmaz.
     * Retry'da yeni task açılmaz — mevcut async CB chain'de devam eder.
     */
    private void findPairRecursive(World world, RTPWorld cfg,
                                   Consumer<Location[]> result, int attempt) {
        if (attempt >= retryCount) { result.accept(null); return; }

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int cx = rng.nextInt(cfg.min, cfg.max + 1) * (rng.nextBoolean() ? 1 : -1);
        int cz = rng.nextInt(cfg.min, cfg.max + 1) * (rng.nextBoolean() ? 1 : -1);

        Location center = new Location(world, cx, 0, cz);
        // locs[0] ve locs[1] duelDistance kadar birbirinden uzak, birbirine bakıyor
        Location[] locs = TeleportMath.getFacingLocs(center, duelDistance);

        int chunkX1 = locs[0].getBlockX() >> 4, chunkZ1 = locs[0].getBlockZ() >> 4;
        int chunkX2 = locs[1].getBlockX() >> 4, chunkZ2 = locs[1].getBlockZ() >> 4;
        boolean sameChunk = (chunkX1 == chunkX2 && chunkZ1 == chunkZ2);

        activeTasks.incrementAndGet();
        CompletableFuture<org.bukkit.Chunk> f1 = world.getChunkAtAsync(chunkX1, chunkZ1);
        CompletableFuture<org.bukkit.Chunk> f2 = sameChunk
                ? f1
                : world.getChunkAtAsync(chunkX2, chunkZ2);

        CompletableFuture.allOf(f1, f2).thenAccept(ignored -> {
            activeTasks.decrementAndGet();

            // Snapshot: includeMaxBlockY = true → highestY dahil, thread-safe
            ChunkSnapshot snap1 = f1.join().getChunkSnapshot(true, false, false);
            ChunkSnapshot snap2 = sameChunk ? snap1 : f2.join().getChunkSnapshot(true, false, false);

            int rx1 = locs[0].getBlockX() & 0xF, rz1 = locs[0].getBlockZ() & 0xF;
            int rx2 = locs[1].getBlockX() & 0xF, rz2 = locs[1].getBlockZ() & 0xF;

            int y1 = snap1.getHighestBlockYAt(rx1, rz1);
            int y2 = snap2.getHighestBlockYAt(rx2, rz2);

            locs[0].setY(y1 + 1);
            locs[1].setY(y2 + 1);

            if (isSnapshotSafe(snap1, rx1, rz1, y1, world)
                    && isSnapshotSafe(snap2, rx2, rz2, y2, world)) {
                result.accept(locs);
            } else {
                // Aynı async thread'de devam — yeni task yok
                findPairRecursive(world, cfg, result, attempt + 1);
            }
        }).exceptionally(ex -> {
            activeTasks.decrementAndGet();
            result.accept(null);
            return null;
        });
    }

    // ──────────────────────────────────────────────────────────────────
    // findLocation — ChunkSnapshot tabanlı, sıfır server-thread
    // ──────────────────────────────────────────────────────────────────

    public void findLocation(World world, RTPWorld cfg, Consumer<Location> result) {
        findLocationRecursive(world, cfg, result, 0);
    }

    /**
     * getChunkAtAsync → snapshot → highestY + güvenlik → teleportAsync
     * Server thread'e hiç dokunulmaz.
     * Retry'da yeni async task açılmaz.
     */
    private void findLocationRecursive(World world, RTPWorld cfg,
                                       Consumer<Location> result, int attempt) {
        if (attempt >= retryCount) { result.accept(null); return; }

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int min = cfg.min;
        int max = cfg.max;

        if (cfg.useWorldborder) {
            double wbHalf = world.getWorldBorder().getSize() / 2.0;
            max = (int) Math.min(max, wbHalf - 16);
            if (max < min) max = min;
        }

        int x = rng.nextInt(min, max + 1) * (rng.nextBoolean() ? 1 : -1);
        int z = rng.nextInt(min, max + 1) * (rng.nextBoolean() ? 1 : -1);

        activeTasks.incrementAndGet();
        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            activeTasks.decrementAndGet();

            // includeMaxBlockY=true: snapshot içinde highestY hazır, ekstra I/O yok
            ChunkSnapshot snap = chunk.getChunkSnapshot(true, false, false);
            int relX = x & 0xF;
            int relZ = z & 0xF;
            int y    = snap.getHighestBlockYAt(relX, relZ);

            if (isSnapshotSafe(snap, relX, relZ, y, world)) {
                result.accept(new Location(world, x + 0.5, y + 1, z + 0.5));
            } else {
                // Thread zaten async CB'de — yeni task açmadan rekürsiyon
                findLocationRecursive(world, cfg, result, attempt + 1);
            }
        }).exceptionally(ex -> {
            activeTasks.decrementAndGet();
            result.accept(null);
            return null;
        });
    }

    // ──────────────────────────────────────────────────────────────────
    // Güvenlik kontrolü — tamamen snapshot, sıfır server-thread
    // ──────────────────────────────────────────────────────────────────

    /**
     * @param snap   chunk snapshot (includeMaxBlockY=true ile alınmış)
     * @param relX   chunk-içi X koordinatı (0–15)
     * @param relZ   chunk-içi Z koordinatı (0–15)
     * @param floorY getHighestBlockYAt sonucu (zemin bloğunun Y'si)
     * @param world  highestY sınır kontrolü için
     *
     * Herhangi bir thread'den çağrılabilir — Block, World API kullanılmaz.
     */
    private boolean isSnapshotSafe(ChunkSnapshot snap, int relX, int relZ,
                                   int floorY, World world) {
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        // Dünya sınırlarında değilse geçersiz
        if (floorY <= minY || floorY >= maxY - 2) return false;

        Material floor = snap.getBlockType(relX, floorY,     relZ);
        Material feet  = snap.getBlockType(relX, floorY + 1, relZ);
        Material head  = snap.getBlockType(relX, floorY + 2, relZ);

        if (UNSAFE_FLOOR.contains(floor)) return false;
        return PASSABLE.contains(feet) && PASSABLE.contains(head);
    }

    // ──────────────────────────────────────────────────────────────────
    // Public yardımcılar
    // ──────────────────────────────────────────────────────────────────

    public Location    getBufferedSpawnLocation(String worldName) { return null; }

    public Location[]  getBufferedPair(String worldName) {
        Deque<Location[]> buf = pairBuffers.get(worldName);
        return (buf != null) ? buf.pollFirst() : null;
    }

    public void addBufferedLocation(String worldName, Location loc) {
        Deque<Location> buf = buffers.get(worldName);
        if (buf != null) buf.addLast(loc);
    }

    public boolean hasWorld(String name)     { return worlds.containsKey(name); }
    public Map<String, RTPWorld> getWorlds() { return worlds; }

    // ──────────────────────────────────────────────────────────────────

    public static final class RTPWorld {
        public final int min, max;
        public final boolean useWorldborder;
        public RTPWorld(int min, int max, boolean useWorldborder) {
            this.min = min; this.max = max; this.useWorldborder = useWorldborder;
        }
    }
}
