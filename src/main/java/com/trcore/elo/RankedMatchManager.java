package com.trcore.elo;

import com.trcore.TRCore;
import com.trcore.utils.CC;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;

public class RankedMatchManager {

    private final TRCore plugin;
    private final EloManager eloManager;
    private final EloCalculator eloCalculator;
    // Map: PlayerUUID -> ActiveMatch (her iki oyuncu da eklenir)
    private final Map<UUID, ActiveMatch> activeMatches = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> escapeTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> escapeStrikes = new ConcurrentHashMap<>();
    private final Map<String, Long> recentMatches = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<RankedHistoryEntry>> historyByPlayer = new ConcurrentHashMap<>();
    private FileConfiguration historyMenuConfig;

    private static final DecimalFormat DECIMAL = new DecimalFormat("0.0");

    public static class EloOutcome {
        private final int winnerGain;
        private final int loserLoss;

        public EloOutcome(int winnerGain, int loserLoss) {
            this.winnerGain = winnerGain;
            this.loserLoss = loserLoss;
        }

        public int getWinnerGain() { return winnerGain; }
        public int getLoserLoss() { return loserLoss; }
    }

    public static class RankedHistoryEntry {
        private final UUID matchId;
        private final long startTime;
        private final long endTime;
        private final UUID playerA;
        private final UUID playerB;
        private final String playerAName;
        private final String playerBName;
        private final double playerAFinalHealth;
        private final double playerBFinalHealth;
        private final int playerATotemPops;
        private final int playerBTotemPops;
        private final int playerACrystalExplosions;
        private final int playerBCrystalExplosions;
        private final int playerAAnchorExplosions;
        private final int playerBAnchorExplosions;
        private final double playerADamageToOpponent;
        private final double playerBDamageToOpponent;
        private final int playerAEloChange;
        private final int playerBEloChange;
        private final ItemStack[] playerAInventory;
        private final ItemStack[] playerBInventory;
        private final ItemStack[] playerAArmor;
        private final ItemStack[] playerBArmor;
        private final ItemStack playerAOffhand;
        private final ItemStack playerBOffhand;
        private final long expireAt;

        public RankedHistoryEntry(UUID matchId, long startTime, long endTime,
                                  UUID playerA, UUID playerB,
                                  String playerAName, String playerBName,
                                  double playerAFinalHealth, double playerBFinalHealth,
                                  int playerATotemPops, int playerBTotemPops,
                                  int playerACrystalExplosions, int playerBCrystalExplosions,
                                  int playerAAnchorExplosions, int playerBAnchorExplosions,
                                  double playerADamageToOpponent, double playerBDamageToOpponent,
                                  int playerAEloChange, int playerBEloChange,
                                  ItemStack[] playerAInventory, ItemStack[] playerBInventory,
                                  ItemStack[] playerAArmor, ItemStack[] playerBArmor,
                                  ItemStack playerAOffhand, ItemStack playerBOffhand,
                                  long expireAt) {
            this.matchId = matchId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.playerA = playerA;
            this.playerB = playerB;
            this.playerAName = playerAName;
            this.playerBName = playerBName;
            this.playerAFinalHealth = playerAFinalHealth;
            this.playerBFinalHealth = playerBFinalHealth;
            this.playerATotemPops = playerATotemPops;
            this.playerBTotemPops = playerBTotemPops;
            this.playerACrystalExplosions = playerACrystalExplosions;
            this.playerBCrystalExplosions = playerBCrystalExplosions;
            this.playerAAnchorExplosions = playerAAnchorExplosions;
            this.playerBAnchorExplosions = playerBAnchorExplosions;
            this.playerADamageToOpponent = playerADamageToOpponent;
            this.playerBDamageToOpponent = playerBDamageToOpponent;
            this.playerAEloChange = playerAEloChange;
            this.playerBEloChange = playerBEloChange;
            this.playerAInventory = playerAInventory;
            this.playerBInventory = playerBInventory;
            this.playerAArmor = playerAArmor;
            this.playerBArmor = playerBArmor;
            this.playerAOffhand = playerAOffhand;
            this.playerBOffhand = playerBOffhand;
            this.expireAt = expireAt;
        }
    }

    private static class HistoryHolder implements InventoryHolder {
        private final UUID viewer;
        private final int page;
        public HistoryHolder(UUID viewer, int page) { this.viewer = viewer; this.page = page; }
        public UUID getViewer() { return viewer; }
        public int getPage() { return page; }
        @Override public Inventory getInventory() { return null; }
    }

    private static class HistoryDetailHolder implements InventoryHolder {
        private final UUID viewer;
        private final UUID matchId;
        public HistoryDetailHolder(UUID viewer, UUID matchId) { this.viewer = viewer; this.matchId = matchId; }
        public UUID getViewer() { return viewer; }
        public UUID getMatchId() { return matchId; }
        @Override public Inventory getInventory() { return null; }
    }

    private static class HistoryInventoryHolder implements InventoryHolder {
        private final UUID viewer;
        private final UUID matchId;
        public HistoryInventoryHolder(UUID viewer, UUID matchId) { this.viewer = viewer; this.matchId = matchId; }
        public UUID getViewer() { return viewer; }
        @Override public Inventory getInventory() { return null; }
    }

    public RankedMatchManager(TRCore plugin, EloManager eloManager) {
        this.plugin = plugin;
        this.eloManager = eloManager;
        this.eloCalculator = new EloCalculator(eloManager);
        loadHistoryMenuConfig();
        startProtectionTask();
        startHistoryCleanupTask();
    }

    private void loadHistoryMenuConfig() {
        File file = new File(plugin.getDataFolder(), "menus/ranked-history.yml");
        if (!file.exists()) {
            plugin.saveResource("menus/ranked-history.yml", false);
        }
        historyMenuConfig = YamlConfiguration.loadConfiguration(file);
    }

    public void reloadConfig() {
        loadHistoryMenuConfig();
    }

    private void startHistoryCleanupTask() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> {
            long now = System.currentTimeMillis();
            if (historyByPlayer.isEmpty()) return;
            for (Map.Entry<UUID, Deque<RankedHistoryEntry>> entry : historyByPlayer.entrySet()) {
                Deque<RankedHistoryEntry> deque = entry.getValue();
                if (deque == null || deque.isEmpty()) continue;
                synchronized (deque) {
                    while (!deque.isEmpty() && deque.peekFirst().expireAt <= now) {
                        deque.pollFirst();
                    }
                }
            }
        }, 1200L, 1200L);
    }

    private void startProtectionTask() {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> {
            if (activeMatches.isEmpty()) return;

            org.bukkit.configuration.ConfigurationSection config = eloManager.getEloConfig().get().getConfigurationSection("protection");
            if (config == null || !config.getBoolean("enabled", true)) return;

            double radius = eloManager.getEloConfig().get().getDouble("ranked-queue.escape.max-distance", 150.0);
            String message = config.getString("message", "<red>Burada dereceli maç oynanıyor!");

            java.util.Set<ActiveMatch> uniqueMatches = new java.util.HashSet<>(activeMatches.values());
            for (ActiveMatch match : uniqueMatches) {
                Location p1 = match.getP1StartLoc();
                Location p2 = match.getP2StartLoc();
                if (p1 == null || p2 == null || p1.getWorld() == null) continue;

                Location center = new Location(p1.getWorld(), (p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2, (p1.getZ() + p2.getZ()) / 2);

                for (Player nearby : center.getNearbyPlayers(radius)) {
                    UUID uuid = nearby.getUniqueId();
                    if (activeMatches.containsKey(uuid)) continue;
                    if (plugin.getRankedQueueManager() != null && plugin.getRankedQueueManager().isInQueue(nearby)) continue;
                    if (nearby.hasPermission("trcore.ranked.protection.bypass")) continue;

                    nearby.getScheduler().run(plugin, t2 -> {
                        nearby.performCommand("spawn");
                        nearby.sendMessage(CC.parse(message));
                    }, null);
                }
            }
        }, 60L, 60L); // Her 3 saniyede bir kontrol et
    }

    public void startMatch(Player p1, Player p2, Location p1Loc, Location p2Loc, String worldName) {
        ActiveMatch match = new ActiveMatch(p1.getUniqueId(), p2.getUniqueId(), p1Loc, p2Loc, worldName);
        activeMatches.put(p1.getUniqueId(), match);
        activeMatches.put(p2.getUniqueId(), match);

        if (plugin.getTpaManager() != null) {
            plugin.getTpaManager().cleanup(p1);
            plugin.getTpaManager().cleanup(p2);
        }

        EloPlayerData d1 = eloManager.getPlayerData(p1.getUniqueId());
        if (d1 != null && !d1.hasPlayedRanked())
            d1.setHasPlayedRanked(true);

        EloPlayerData d2 = eloManager.getPlayerData(p2.getUniqueId());
        if (d2 != null && !d2.hasPlayedRanked())
            d2.setHasPlayedRanked(true);

        p1.getScheduler().run(plugin, t -> {
            // Restrictions are now applied after the setup command (e.g. rekit) in RankedQueueManager
        }, null);
        p2.getScheduler().run(plugin, t -> {
        }, null);

        startEscapeTracker(p1, match);
        startEscapeTracker(p2, match);
    }

    public void addAnchorExplosion(UUID uuid) {
        ActiveMatch match = activeMatches.get(uuid);
        if (match != null) match.addAnchorExplosion(uuid);
    }

    public void addCrystalExplosion(UUID uuid) {
        ActiveMatch match = activeMatches.get(uuid);
        if (match != null) match.addCrystalExplosion(uuid);
    }

    public void addTotemPop(UUID uuid) {
        ActiveMatch match = activeMatches.get(uuid);
        if (match != null) match.addTotemPop(uuid);
    }

    public void addDamage(UUID uuid, double damage) {
        ActiveMatch match = activeMatches.get(uuid);
        if (match != null) match.addDamage(uuid, damage);
    }

    public void applyRestrictions(Player player) {
        org.bukkit.configuration.file.FileConfiguration config = eloManager.getEloConfig().get();
        if (!config.getBoolean("restrictions.enabled", true))
            return;

        // 1. Armor Enchantment Swap (Blast Prot -> Prot) - ALL BOOTS IN INVENTORY
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            org.bukkit.inventory.ItemStack item = inv.getItem(i);
            if (item != null && item.getType().name().endsWith("_BOOTS") && item.containsEnchantment(org.bukkit.enchantments.Enchantment.BLAST_PROTECTION)) {
                int level = item.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.BLAST_PROTECTION);
                item.removeEnchantment(org.bukkit.enchantments.Enchantment.BLAST_PROTECTION);
                int currentProt = item.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.PROTECTION);
                if (currentProt < level) {
                    item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, level);
                }
            }
        }

        // 2. Totem and Ender Chest Limits
        limitInventory(player);

        // 3. Blacklisted Items and Potions
        removeBlacklistedItems(player);

        // 4. Clear Restricted Potion Effects
        clearRestrictedPotions(player);
    }

    private void startEscapeTracker(Player player, ActiveMatch match) {
        boolean escapeEnabled = eloManager.getEloConfig().get().getBoolean("ranked-queue.escape.enabled", true);
        if (!escapeEnabled)
            return;

        double maxDist = eloManager.getEloConfig().get().getDouble("ranked-queue.escape.max-distance", 150.0);
        String warningMsg = eloManager.getEloConfig().get().getString("ranked-queue.escape.warning-message",
                "&8&l➜&r <red>Dereceli maçtan kaçamazsın! Uyarı (%strike%/3)</red>");
        String teleportMsg = eloManager.getEloConfig().get().getString("ranked-queue.escape.teleport-message",
                "&8&l➜&r <red>Çok uzaklaştın, geri ışınlandın!</red>");
        String warningSound = eloManager.getEloConfig().get().getString("ranked-queue.escape.warning-sound",
                "ENTITY_ENDERMAN_TELEPORT");
        Location startLoc = player.getUniqueId().equals(match.getPlayer1()) ? match.getP1StartLoc()
                : match.getP2StartLoc();

        escapeStrikes.put(player.getUniqueId(), 0);

        // Folia Player Scheduler (Player task - moves with region)
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, t -> {
            if (!activeMatches.containsKey(player.getUniqueId())) {
                t.cancel();
                escapeStrikes.remove(player.getUniqueId());
                return;
            }

            if (!player.isOnline()) {
                t.cancel();
                escapeStrikes.remove(player.getUniqueId());
                handleDisconnect(player.getUniqueId(), match);
                return;
            }

            Player opponent = Bukkit.getPlayer(match.getOpponent(player.getUniqueId()));
            Location targetLoc = startLoc; // Fallback to own start
            if (opponent != null && opponent.isOnline()) {
                UUID oppUUID = opponent.getUniqueId();
                Location oppStart = oppUUID.equals(match.getPlayer1()) ? match.getP1StartLoc() : match.getP2StartLoc();

                // Teleport to opponent ONLY if they are in the correct world AND within distance
                if (opponent.getWorld().getName().equals(match.getWorldName()) &&
                        opponent.getLocation().distanceSquared(oppStart) <= (maxDist * maxDist)) {
                    targetLoc = opponent.getLocation();
                }
            }

            // Boundary Check
            boolean outOfBounds = !player.getWorld().getName().equals(match.getWorldName()) || 
                                player.getLocation().distanceSquared(startLoc) > (maxDist * maxDist);

            if (outOfBounds) {
                int strikes = escapeStrikes.getOrDefault(player.getUniqueId(), 0) + 1;
                escapeStrikes.put(player.getUniqueId(), strikes);

                if (strikes >= 3) {
                    teleportBack(player, targetLoc, teleportMsg, warningSound);
                    escapeStrikes.put(player.getUniqueId(), 0); // Reset after teleport
                } else {
                    player.sendMessage(CC.parse(warningMsg.replace("%strike%", String.valueOf(strikes))));
                    if (warningSound != null && !warningSound.isEmpty()) {
                        CC.playSound(player, warningSound.toUpperCase());
                    }
                }
            }

        }, null, 20L, 20L); // check every 1 second

        escapeTasks.put(player.getUniqueId(), task);
    }

    private void teleportBack(Player p, Location loc, String msg, String sound) {
        p.teleportAsync(loc).thenAccept(ok -> {
            if (ok) {
                p.sendMessage(CC.parse(msg));
                if (sound != null && !sound.isEmpty()) {
                    CC.playSound(p, sound.toUpperCase());
                }
            }
        });
    }

    public void handleDisconnect(UUID disconnectedUUID, ActiveMatch match) {
        UUID opponentUUID = match.getOpponent(disconnectedUUID);
        endMatch(opponentUUID, disconnectedUUID, match);
    }

    public ActiveMatch getMatch(UUID playerUUID) {
        return activeMatches.get(playerUUID);
    }

    /**
     * Ends the match and calculates ELO
     * 
     * @param winnerUUID The winner
     * @param loserUUID  The loser (died or disconnected)
     */
    public void endMatch(UUID winnerUUID, UUID loserUUID, ActiveMatch match) {
        if (!activeMatches.containsKey(winnerUUID) || !activeMatches.containsKey(loserUUID))
            return; // already ended

        // Cleanup maps to prevent dual processing
        activeMatches.remove(winnerUUID);
        activeMatches.remove(loserUUID);

        cancelTask(winnerUUID);
        cancelTask(loserUUID);

        // Fetch Elo Data
        eloManager.loadPlayer(winnerUUID);
        eloManager.loadPlayer(loserUUID);

        EloPlayerData winnerData = eloManager.getPlayerData(winnerUUID);
        EloPlayerData loserData = eloManager.getPlayerData(loserUUID);

        if (winnerData != null && loserData != null) {
            int wOld = winnerData.getElo();
            int lOld = loserData.getElo();

            EloOutcome outcome = calculateSmartOutcome(winnerData, loserData, match);
            eloCalculator.applyOutcome(winnerData, loserData, outcome.getWinnerGain(), outcome.getLoserLoss());

            int wGain = winnerData.getElo() - wOld;
            int lLoss = lOld - loserData.getElo();

            // Anti-Abuse IP Check
            String wIP = winnerUUID.equals(match.getPlayer1()) ? match.getP1IP() : match.getP2IP();
            String lIP = loserUUID.equals(match.getPlayer1()) ? match.getP1IP() : match.getP2IP();
            boolean sameIPBlock = eloManager.getEloConfig().get().getBoolean("anti-abuse.block-same-ip", true);
            boolean isSameIP = sameIPBlock && wIP != null && lIP != null && !wIP.isEmpty() && wIP.equals(lIP);

            // Anti-Abuse Cooldown Check
            boolean cooldownEnabled = eloManager.getEloConfig().get().getBoolean("anti-abuse.match-cooldown.enabled",
                    true);
            String matchKey = getMatchKey(winnerUUID, loserUUID);
            boolean isInCooldown = false;

            if (cooldownEnabled) {
                Long lastMatchTime = recentMatches.get(matchKey);
                // Check if they are within 5 minutes of their last match
                if (lastMatchTime != null && (System.currentTimeMillis() - lastMatchTime < (5 * 60 * 1000L))) {
                    // Activate cooldown ONLY if the winner has already beaten this opponent 3+
                    // times
                    if (winnerData.getLastOpponent() != null && winnerData.getLastOpponent().equals(loserUUID)
                            && winnerData.getConsecutiveKillsAgainstOpponent() >= 3) {
                        isInCooldown = true;
                    }
                }
                // Record the time of this match for the next one
                recentMatches.put(matchKey, System.currentTimeMillis());
            }

            if (isSameIP) {
                wGain = 0;
                lLoss = 0;
                winnerData.setElo(wOld);
                loserData.setElo(lOld);
            } else {
                // Win Streak Logic
                winnerData.setWinStreak(winnerData.getWinStreak() + 1);
                loserData.setWinStreak(0); // Reset loser's streak

                if (winnerData.getLastOpponent() != null && winnerData.getLastOpponent().equals(loserUUID)) {
                    winnerData.setConsecutiveKillsAgainstOpponent(winnerData.getConsecutiveKillsAgainstOpponent() + 1);
                } else {
                    winnerData.setConsecutiveKillsAgainstOpponent(1);
                }
                winnerData.setLastOpponent(loserUUID);
                loserData.setLastOpponent(winnerUUID);
                loserData.setConsecutiveKillsAgainstOpponent(0);

                // Loss Protection Check (granted at match end for next time)
                int streakForProt = eloManager.getEloConfig().get().getInt("streak-system.loss-protection-streak", 4);
                if (winnerData.getWinStreak() >= streakForProt) {
                    winnerData.setLossProtection(true);
                }

                // Increment stats
                winnerData.setKills(winnerData.getKills() + 1);
                loserData.setDeaths(loserData.getDeaths() + 1);
            }

            // Notify winner
            Player winner = Bukkit.getPlayer(winnerUUID);
            if (winner != null && winner.isOnline()) {
                if (isSameIP) {
                    winner.sendMessage(CC.parse("<red>Aynı IP'deki bir oyuncuyu yendiğin için ELO kazanamadın."));
                } else {
                    String msg = "<green>Rakibini yendin! <gray>(+" + wGain + " ELO)";
                    if (winnerData.getWinStreak() >= 4) msg += " <dark_aqua>[" + winnerData.getWinStreak() + " Streak]";
                winner.sendMessage(CC.parse(msg));
                sendMatchSummary(winner, match);
            }
            }

            // Notify loser
            Player loser = Bukkit.getPlayer(loserUUID);
            if (loser != null && loser.isOnline()) {
                if (isSameIP) {
                    loser.sendMessage(CC.parse("<red>Aynı IP'deki bir oyuncuya yenildiğin için ELO kaybetmedin."));
                } else {
                    String msg = "<red>Maçı kaybettin! <gray>(-" + lLoss + " ELO)";
                    if (lLoss == 0) msg += " <aqua>[Loss Protection]";
                    loser.sendMessage(CC.parse(msg));
                    sendMatchSummary(loser, match);
                }
            }

            snapshotMatchPlayers(match);
            storeHistory(match, winnerUUID, loserUUID, wGain, lLoss);

            // Async Save directly targeting these UUIDs
            eloManager.savePlayerDataAsync(winnerUUID);
            eloManager.savePlayerDataAsync(loserUUID);
        }
    }

    private void snapshotMatchPlayers(ActiveMatch match) {
        Player p1 = Bukkit.getPlayer(match.getPlayer1());
        Player p2 = Bukkit.getPlayer(match.getPlayer2());
        if (p1 != null) match.snapshotPlayer(p1);
        if (p2 != null) match.snapshotPlayer(p2);
    }

    private void storeHistory(ActiveMatch match, UUID winnerUUID, UUID loserUUID, int winnerGain, int loserLoss) {
        FileConfiguration cfg = eloManager.getEloConfig().get();
        if (!cfg.getBoolean("ranked-history.enabled", true)) return;

        long now = System.currentTimeMillis();
        long expire = now + (cfg.getLong("ranked-history.expire-seconds", 600) * 1000L);
        int maxPerPlayer = Math.max(1, cfg.getInt("ranked-history.max-stored-matches-per-player", 20));

        UUID p1 = match.getPlayer1();
        UUID p2 = match.getPlayer2();
        int p1Delta = p1.equals(winnerUUID) ? winnerGain : -loserLoss;
        int p2Delta = p2.equals(winnerUUID) ? winnerGain : -loserLoss;

        RankedHistoryEntry entry = new RankedHistoryEntry(
                match.getMatchId(),
                match.getStartTime(),
                now,
                p1,
                p2,
                match.getP1Name(),
                match.getP2Name(),
                match.getP1FinalHealth() < 0 ? 0.0 : match.getP1FinalHealth(),
                match.getP2FinalHealth() < 0 ? 0.0 : match.getP2FinalHealth(),
                match.getTotemPops(p1),
                match.getTotemPops(p2),
                match.getCrystalExplosions(p1),
                match.getCrystalExplosions(p2),
                match.getAnchorExplosions(p1),
                match.getAnchorExplosions(p2),
                match.getDamageToOpponent(p1),
                match.getDamageToOpponent(p2),
                p1Delta,
                p2Delta,
                match.getP1InvSnapshot(),
                match.getP2InvSnapshot(),
                match.getP1ArmorSnapshot(),
                match.getP2ArmorSnapshot(),
                match.getP1OffhandSnapshot(),
                match.getP2OffhandSnapshot(),
                expire
        );

        pushHistory(p1, entry, maxPerPlayer);
        pushHistory(p2, entry, maxPerPlayer);
    }

    private void pushHistory(UUID uuid, RankedHistoryEntry entry, int maxPerPlayer) {
        Deque<RankedHistoryEntry> deque = historyByPlayer.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(entry);
            while (deque.size() > maxPerPlayer) {
                deque.pollFirst();
            }
        }
    }

    private EloOutcome calculateSmartOutcome(EloPlayerData winner, EloPlayerData loser, ActiveMatch match) {
        EloOutcome base = eloCalculator.calculateOutcome(winner, loser);
        FileConfiguration cfg = eloManager.getEloConfig().get();
        if (!cfg.getBoolean("smart-elo.enabled", true) || match == null) return base;

        double minMult = cfg.getDouble("smart-elo.multiplier.min", 0.85);
        double maxMult = cfg.getDouble("smart-elo.multiplier.max", 1.25);
        double tolerance = cfg.getDouble("smart-elo.tolerance.small-diff-threshold", 0.06);

        UUID winnerUuid = winner.getUuid();
        UUID loserUuid = loser.getUuid();

        double wDamage = match.getDamageToOpponent(winnerUuid);
        double lDamage = match.getDamageToOpponent(loserUuid);
        int wCrystal = match.getCrystalExplosions(winnerUuid);
        int lCrystal = match.getCrystalExplosions(loserUuid);
        int wAnchor = match.getAnchorExplosions(winnerUuid);
        int lAnchor = match.getAnchorExplosions(loserUuid);
        int wTotem = match.getTotemPops(winnerUuid);
        int lTotem = match.getTotemPops(loserUuid);

        double wd = cfg.getDouble("smart-elo.weights.damage", 0.45);
        double wc = cfg.getDouble("smart-elo.weights.crystal", 0.2);
        double wa = cfg.getDouble("smart-elo.weights.anchor", 0.15);
        double wt = cfg.getDouble("smart-elo.weights.totem", 0.15);
        double ws = cfg.getDouble("smart-elo.weights.duration", 0.05);

        double durationSec = Math.max(1.0, (System.currentTimeMillis() - match.getStartTime()) / 1000.0);
        double durationFactor = Math.min(1.0, durationSec / Math.max(1.0, cfg.getDouble("smart-elo.duration.full-score-seconds", 180.0)));

        double damageShare = ratio(wDamage, lDamage);
        double crystalShare = ratio(wCrystal, lCrystal);
        double anchorShare = ratio(wAnchor, lAnchor);
        double totemShare = ratio(wTotem, lTotem);

        double winnerEffort = (damageShare * wd) + (crystalShare * wc) + (anchorShare * wa) + (totemShare * wt) + (durationFactor * ws);
        double loserEffort = ((1.0 - damageShare) * wd) + ((1.0 - crystalShare) * wc) + ((1.0 - anchorShare) * wa) + ((1.0 - totemShare) * wt) + (durationFactor * ws);

        double delta = winnerEffort - loserEffort;
        if (Math.abs(delta) <= tolerance) {
            return base;
        }

        double normalized = Math.max(-1.0, Math.min(1.0, delta));
        double winnerMult = 1.0 + normalized * cfg.getDouble("smart-elo.multiplier-impact", 0.2);
        winnerMult = Math.max(minMult, Math.min(maxMult, winnerMult));

        int gain = (int) Math.round(base.getWinnerGain() * winnerMult);
        int loss = (int) Math.round(base.getLoserLoss() * winnerMult);

        int maxExtraGain = Math.max(0, cfg.getInt("smart-elo.max-extra-gain", 12));
        int maxExtraLoss = Math.max(0, cfg.getInt("smart-elo.max-extra-loss", 12));

        gain = clamp(gain, Math.max(1, base.getWinnerGain() - maxExtraGain), base.getWinnerGain() + maxExtraGain);
        loss = clamp(loss, Math.max(0, base.getLoserLoss() - maxExtraLoss), base.getLoserLoss() + maxExtraLoss);

        return new EloOutcome(gain, loss);
    }

    private double ratio(double a, double b) {
        double total = a + b;
        if (total <= 0.0001) return 0.5;
        return a / total;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void sendMatchSummary(Player viewer, ActiveMatch match) {
        FileConfiguration cfg = eloManager.getEloConfig().get();
        String line = cfg.getString("ranked-history.match-summary-click", "<yellow>Mac ozetini gormek icin tikla.");
        String hover = cfg.getString("ranked-history.match-summary-hover", "<gray>Detay: <white>%match_uuid%");
        String cmd = "/rankedhistory " + match.getMatchId();

        Component c = CC.parse(line.replace("%match_uuid%", match.getMatchId().toString()))
                .clickEvent(ClickEvent.runCommand(cmd))
                .hoverEvent(HoverEvent.showText(CC.parse(hover.replace("%match_uuid%", match.getMatchId().toString()))));
        viewer.sendMessage(c);
    }

    private String nameOf(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
        return off.getName() != null ? off.getName() : uuid.toString();
    }

    public void openHistoryMenu(Player viewer, int page) {
        List<RankedHistoryEntry> entries = getHistoryEntries(viewer.getUniqueId());
        if (entries.isEmpty()) {
            viewer.sendMessage(CC.parse("<red>Son 10 dakikada ranked geçmişin bulunamadı."));
            return;
        }

        int perPage = 28;
        int maxPage = Math.max(0, (entries.size() - 1) / perPage);
        int currentPage = Math.max(0, Math.min(maxPage, page));

        String titleTpl = historyMenuConfig.getString("list.title", "&6Ranked History &7[%page%/%max_page%]");
        String title = CC.translate(titleTpl.replace("%page%", String.valueOf(currentPage + 1)).replace("%max_page%", String.valueOf(maxPage + 1)));
        Inventory inv = Bukkit.createInventory(new HistoryHolder(viewer.getUniqueId(), currentPage), 54, title);

        int start = currentPage * perPage;
        int end = Math.min(entries.size(), start + perPage);
        int slot = 10;
        for (int i = start; i < end; i++) {
            RankedHistoryEntry e = entries.get(i);
            UUID opp = e.playerA.equals(viewer.getUniqueId()) ? e.playerB : e.playerA;
            String oppName = e.playerA.equals(viewer.getUniqueId()) ? e.playerBName : e.playerAName;
            int selfDelta = e.playerA.equals(viewer.getUniqueId()) ? e.playerAEloChange : e.playerBEloChange;
            long sec = Math.max(1L, (e.endTime - e.startTime) / 1000L);

            Material mat = materialOr(historyMenuConfig.getString(selfDelta >= 0 ? "list.items.win.material" : "list.items.loss.material"), selfDelta >= 0 ? Material.LIME_DYE : Material.RED_DYE);
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            String nameTpl = historyMenuConfig.getString("list.items.name", "&e%opponent% &7| &f%match_short%");
            meta.setDisplayName(CC.translate(replaceHistory(nameTpl, e, viewer.getUniqueId(), oppName, selfDelta, sec, opp)));
            List<String> lore = new ArrayList<>();
            for (String line : historyMenuConfig.getStringList("list.items.lore")) {
                lore.add(CC.translate(replaceHistory(line, e, viewer.getUniqueId(), oppName, selfDelta, sec, opp)));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slot, item);

            if (slot % 9 == 7) slot += 3;
            else slot++;
        }

        if (currentPage > 0) inv.setItem(45, named(materialOr(historyMenuConfig.getString("list.previous.material"), Material.ARROW), historyMenuConfig.getString("list.previous.name", "&aOnceki Sayfa")));
        if (currentPage < maxPage) inv.setItem(53, named(materialOr(historyMenuConfig.getString("list.next.material"), Material.ARROW), historyMenuConfig.getString("list.next.name", "&aSonraki Sayfa")));

        viewer.openInventory(inv);
    }

    private ItemStack named(Material m, String name) {
        ItemStack i = new ItemStack(m);
        ItemMeta im = i.getItemMeta();
        im.setDisplayName(CC.translate(name));
        i.setItemMeta(im);
        return i;
    }

    private Material materialOr(String name, Material fallback) {
        if (name == null || name.isEmpty()) return fallback;
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String replaceHistory(String text, RankedHistoryEntry e, UUID viewer, String opponentName, int selfDelta, long sec, UUID oppUuid) {
        return text.replace("%opponent%", opponentName)
                .replace("%match_uuid%", e.matchId.toString())
                .replace("%match_short%", shortUuid(e.matchId))
                .replace("%duration%", String.valueOf(sec))
                .replace("%elo_change%", (selfDelta > 0 ? "+" : "") + selfDelta)
                .replace("%opponent_uuid%", oppUuid.toString());
    }

    private String shortUuid(UUID uuid) {
        String s = uuid.toString();
        return s.substring(0, Math.min(8, s.length()));
    }

    public void openHistoryDetail(Player viewer, UUID matchId) {
        RankedHistoryEntry entry = getHistoryEntry(viewer.getUniqueId(), matchId);
        if (entry == null) {
            viewer.sendMessage(CC.parse("<red>Bu match gecmisi bulunamadi ya da suresi doldu."));
            return;
        }

        String title = CC.translate(historyMenuConfig.getString("detail.title", "&6Match Detay &7| &f%match_short%")
                .replace("%match_short%", shortUuid(matchId))
                .replace("%match_uuid%", matchId.toString()));
        Inventory inv = Bukkit.createInventory(new HistoryDetailHolder(viewer.getUniqueId(), matchId), 54, title);

        boolean viewerIsA = viewer.getUniqueId().equals(entry.playerA);
        UUID leftUuid = viewerIsA ? entry.playerA : entry.playerB;
        UUID rightUuid = viewerIsA ? entry.playerB : entry.playerA;

        String leftName = viewerIsA ? entry.playerAName : entry.playerBName;
        String rightName = viewerIsA ? entry.playerBName : entry.playerAName;

        double leftHp = viewerIsA ? entry.playerAFinalHealth : entry.playerBFinalHealth;
        double rightHp = viewerIsA ? entry.playerBFinalHealth : entry.playerAFinalHealth;

        int leftTotem = viewerIsA ? entry.playerATotemPops : entry.playerBTotemPops;
        int rightTotem = viewerIsA ? entry.playerBTotemPops : entry.playerATotemPops;

        int leftCrystal = viewerIsA ? entry.playerACrystalExplosions : entry.playerBCrystalExplosions;
        int rightCrystal = viewerIsA ? entry.playerBCrystalExplosions : entry.playerACrystalExplosions;

        int leftAnchor = viewerIsA ? entry.playerAAnchorExplosions : entry.playerBAnchorExplosions;
        int rightAnchor = viewerIsA ? entry.playerBAnchorExplosions : entry.playerAAnchorExplosions;

        double leftDamage = viewerIsA ? entry.playerADamageToOpponent : entry.playerBDamageToOpponent;
        double rightDamage = viewerIsA ? entry.playerBDamageToOpponent : entry.playerADamageToOpponent;

        int leftEloDelta = viewerIsA ? entry.playerAEloChange : entry.playerBEloChange;
        int rightEloDelta = viewerIsA ? entry.playerBEloChange : entry.playerAEloChange;

        renderSide(inv, true, leftUuid, leftName, leftHp, leftTotem, leftCrystal,
                leftAnchor, leftDamage, leftEloDelta, entry.matchId, 45);

        renderSide(inv, false, rightUuid, rightName, rightHp, rightTotem, rightCrystal,
                rightAnchor, rightDamage, rightEloDelta, entry.matchId, 53);

        long sec = Math.max(1L, (entry.endTime - entry.startTime) / 1000L);
        ItemStack center = named(materialOr(historyMenuConfig.getString("detail.center.material"), Material.CLOCK), historyMenuConfig.getString("detail.center.name", "&eMac Ozeti"));
        ItemMeta meta = center.getItemMeta();
        List<String> centerLore = new ArrayList<>();
        for (String line : historyMenuConfig.getStringList("detail.center.lore")) {
            centerLore.add(CC.translate(line.replace("%duration%", String.valueOf(sec)).replace("%match_uuid%", entry.matchId.toString())));
        }
        meta.setLore(centerLore);
        center.setItemMeta(meta);
        inv.setItem(22, center);

        viewer.openInventory(inv);
    }

    private void renderSide(Inventory inv, boolean left, UUID playerUuid, String name, double hp, int totem, int crystal, int anchor, double dmg,
                            int eloDelta, UUID matchId, int inventoryButtonSlot) {
        EloPlayerData data = eloManager.getPlayerData(playerUuid);
        int elo = data != null ? data.getElo() : 0;
        String rank = CC.translate(eloManager.getEloConfig().getRankForElo(elo).getDisplayName());

        int[] slots = left
                ? new int[]{0, 1, 9, 10, 18, 19, 27, 28}
                : new int[]{8, 7, 17, 16, 26, 25, 35, 34};

        inv.setItem(slots[0], statItem("name", historyMenuConfig.getString("detail.items.name.material", "PLAYER_HEAD"),
                historyMenuConfig.getString("detail.items.name.name", "&b%name%"), name, elo, rank, hp, totem, crystal, anchor, dmg, eloDelta, matchId));
        inv.setItem(slots[1], statItem("elo", historyMenuConfig.getString("detail.items.elo.material", "GOLD_INGOT"),
                historyMenuConfig.getString("detail.items.elo.name", "&eELO: &f%elo%"), name, elo, rank, hp, totem, crystal, anchor, dmg, eloDelta, matchId));
        inv.setItem(slots[2], statItem("rank", historyMenuConfig.getString("detail.items.rank.material", "NETHER_STAR"),
                historyMenuConfig.getString("detail.items.rank.name", "&dRutbe"), name, elo, rank, hp, totem, crystal, anchor, dmg, eloDelta, matchId));
        inv.setItem(slots[3], statItem("health", historyMenuConfig.getString("detail.items.health.material", "APPLE"),
                historyMenuConfig.getString("detail.items.health.name", "&cKalan Can"), name, elo, rank, hp, totem, crystal, anchor, dmg, eloDelta, matchId));
        inv.setItem(slots[4], statItem("totem", historyMenuConfig.getString("detail.items.totem.material", "TOTEM_OF_UNDYING"),
                historyMenuConfig.getString("detail.items.totem.name", "&6Totem Pop"), name, elo, rank, hp, totem, crystal, anchor, dmg, eloDelta, matchId));
        inv.setItem(slots[5], statItem("crystal", historyMenuConfig.getString("detail.items.crystal.material", "END_CRYSTAL"),
                historyMenuConfig.getString("detail.items.crystal.name", "&bCrystal Patlatma"), name, elo, rank, hp, totem, crystal, anchor, dmg, eloDelta, matchId));
        inv.setItem(slots[6], statItem("anchor", historyMenuConfig.getString("detail.items.anchor.material", "RESPAWN_ANCHOR"),
                historyMenuConfig.getString("detail.items.anchor.name", "&9Anchor Patlatma"), name, elo, rank, hp, totem, crystal, anchor, dmg, eloDelta, matchId));
        inv.setItem(slots[7], statItem("damage", historyMenuConfig.getString("detail.items.damage.material", "IRON_SWORD"),
                historyMenuConfig.getString("detail.items.damage.name", "&cToplam Hasar"), name, elo, rank, hp, totem, crystal, anchor, dmg, eloDelta, matchId));

        inv.setItem(inventoryButtonSlot, statItem("inventory", historyMenuConfig.getString("detail.items.inventory.material", "CHEST"),
                historyMenuConfig.getString("detail.items.inventory.name", "&a%name% envanterini goruntule"), name, elo, rank, hp, totem, crystal, anchor, dmg, eloDelta, matchId));
    }

    private ItemStack statItem(String statKey, String materialName, String nameTpl, String pName, int elo, String rank, double hp, int totem, int crystal, int anchor, double dmg, int eloDelta, UUID matchId) {
        ItemStack item = new ItemStack(materialOr(materialName, Material.PAPER));
        ItemMeta meta = item.getItemMeta();
        String name = fillDetail(nameTpl, pName, elo, rank, hp, totem, crystal, anchor, dmg, eloDelta, matchId);
        meta.setDisplayName(CC.translate(name));
        List<String> lore = new ArrayList<>();
        List<String> loreTemplate = historyMenuConfig.getStringList("detail.items." + statKey + ".lore");
        if (loreTemplate == null || loreTemplate.isEmpty()) {
            loreTemplate = historyMenuConfig.getStringList("detail.item-lore");
        }
        for (String line : loreTemplate) {
            lore.add(CC.translate(fillDetail(line, pName, elo, rank, hp, totem, crystal, anchor, dmg, eloDelta, matchId)));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String fillDetail(String text, String pName, int elo, String rank, double hp, int totem, int crystal, int anchor, double dmg, int eloDelta, UUID matchId) {
        return text.replace("%name%", pName)
                .replace("%elo%", String.valueOf(elo))
                .replace("%rank%", rank)
                .replace("%health%", DECIMAL.format(hp))
                .replace("%totem%", String.valueOf(totem))
                .replace("%crystal%", String.valueOf(crystal))
                .replace("%anchor%", String.valueOf(anchor))
                .replace("%damage%", DECIMAL.format(dmg))
                .replace("%elo_change%", (eloDelta > 0 ? "+" : "") + eloDelta)
                .replace("%match_uuid%", matchId.toString());
    }

    public void openHistoryInventory(Player viewer, UUID matchId, UUID focusPlayer) {
        RankedHistoryEntry entry = getHistoryEntry(viewer.getUniqueId(), matchId);
        if (entry == null) {
            viewer.sendMessage(CC.parse("<red>Bu match gecmisi bulunamadi ya da suresi doldu."));
            return;
        }
        boolean focusA = entry.playerA.equals(focusPlayer);
        String focusName = focusA ? entry.playerAName : entry.playerBName;
        String title = CC.translate(historyMenuConfig.getString("inventory.title", "&6Envanter &7| &f%player% &8- &7%match_short%")
                .replace("%match_short%", shortUuid(matchId))
                .replace("%player%", focusName)
                .replace("%match_uuid%", matchId.toString()));
        Inventory inv = Bukkit.createInventory(new HistoryInventoryHolder(viewer.getUniqueId(), matchId), 54, title);
        if (focusA) {
            renderInventoryOnly(inv, entry.playerAInventory, entry.playerAArmor, entry.playerAOffhand);
        } else {
            renderInventoryOnly(inv, entry.playerBInventory, entry.playerBArmor, entry.playerBOffhand);
        }
        viewer.openInventory(inv);
    }

    private void renderInventoryOnly(Inventory inv, ItemStack[] invSnapshot, ItemStack[] armor, ItemStack offhand) {
        // Clear visual noise in top inventory so layout is explicit
        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, "&8 ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Player inventory layout (same order as vanilla):
        // - Main inventory: snapshot 9..35 -> chest rows 2..4 (slots 9..35)
        // - Hotbar:        snapshot 0..8  -> chest row 5  (slots 36..44)
        if (invSnapshot != null) {
            // Main inventory (27)
            for (int i = 9; i < 36 && i < invSnapshot.length; i++) {
                ItemStack stack = invSnapshot[i];
                if (stack == null) continue;
                inv.setItem((i - 9) + 9, stack);
            }

            // Hotbar (9)
            for (int i = 0; i < 9 && i < invSnapshot.length; i++) {
                ItemStack stack = invSnapshot[i];
                if (stack == null) continue;
                inv.setItem(36 + i, stack);
            }
        }

        // Bottom row: armor + offhand
        if (armor != null) {
            if (armor.length > 0 && armor[0] != null) inv.setItem(45, armor[0]);  // boots
            if (armor.length > 1 && armor[1] != null) inv.setItem(46, armor[1]);  // leggings
            if (armor.length > 2 && armor[2] != null) inv.setItem(47, armor[2]);  // chestplate
            if (armor.length > 3 && armor[3] != null) inv.setItem(48, armor[3]);  // helmet
        }
        if (offhand != null) inv.setItem(49, offhand);

        // Labels
        inv.setItem(50, named(Material.BOOK, "&eZirh/Offhand"));
        inv.setItem(51, named(Material.CHEST, "&eMac Sonu Envanter"));
    }

    public void handleHistoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getInventory().getHolder() instanceof HistoryHolder holder) {
            e.setCancelled(true);
            if (!holder.getViewer().equals(player.getUniqueId())) return;
            int slot = e.getSlot();
            if (slot == 45) {
                openHistoryMenu(player, holder.getPage() - 1);
                return;
            }
            if (slot == 53) {
                openHistoryMenu(player, holder.getPage() + 1);
                return;
            }
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getItemMeta() == null || clicked.getItemMeta().getDisplayName() == null) return;
            String stripped = org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            if (stripped == null) return;
            int idx = stripped.lastIndexOf("| ");
            if (idx < 0) return;
            String shortId = stripped.substring(idx + 2).trim();
            for (RankedHistoryEntry entry : getHistoryEntries(player.getUniqueId())) {
                if (shortUuid(entry.matchId).equalsIgnoreCase(shortId)) {
                    openHistoryDetail(player, entry.matchId);
                    return;
                }
            }
            return;
        }

        if (e.getInventory().getHolder() instanceof HistoryDetailHolder holder) {
            e.setCancelled(true);
            if (!holder.getViewer().equals(player.getUniqueId())) return;
            if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;
            if (e.getSlot() == 45 || e.getSlot() == 53) {
                RankedHistoryEntry entry = getHistoryEntry(player.getUniqueId(), holder.getMatchId());
                if (entry == null) return;
                boolean viewerIsA = player.getUniqueId().equals(entry.playerA);
                UUID leftUuid = viewerIsA ? entry.playerA : entry.playerB;
                UUID rightUuid = viewerIsA ? entry.playerB : entry.playerA;
                UUID focus = e.getSlot() == 45 ? leftUuid : rightUuid;
                openHistoryInventory(player, holder.getMatchId(), focus);
            }
            return;
        }

        if (e.getInventory().getHolder() instanceof HistoryInventoryHolder holder) {
            e.setCancelled(true);
            if (!holder.getViewer().equals(player.getUniqueId())) return;
        }
    }

    public void openLatestHistory(Player viewer) {
        List<RankedHistoryEntry> entries = getHistoryEntries(viewer.getUniqueId());
        if (entries.isEmpty()) {
            viewer.sendMessage(CC.parse("<red>Son 10 dakikada ranked geçmişin bulunamadı."));
            return;
        }
        openHistoryDetail(viewer, entries.get(0).matchId);
    }

    public List<RankedHistoryEntry> getHistoryEntries(UUID uuid) {
        Deque<RankedHistoryEntry> deque = historyByPlayer.get(uuid);
        if (deque == null || deque.isEmpty()) return Collections.emptyList();
        long now = System.currentTimeMillis();
        List<RankedHistoryEntry> out = new ArrayList<>();
        synchronized (deque) {
            for (RankedHistoryEntry entry : deque) {
                if (entry.expireAt > now) out.add(entry);
            }
        }
        Collections.reverse(out);
        return out;
    }

    public RankedHistoryEntry getHistoryEntry(UUID viewer, UUID matchId) {
        for (RankedHistoryEntry e : getHistoryEntries(viewer)) {
            if (e.matchId.equals(matchId)) return e;
        }
        return null;
    }

    public void handleDraw(ActiveMatch match) {
        UUID u1 = match.getPlayer1();
        UUID u2 = match.getPlayer2();

        if (!activeMatches.containsKey(u1) || !activeMatches.containsKey(u2)) return;

        activeMatches.remove(u1);
        activeMatches.remove(u2);

        cancelTask(u1);
        cancelTask(u2);

        Player p1 = Bukkit.getPlayer(u1);
        Player p2 = Bukkit.getPlayer(u2);

        String msg = "<yellow>Maç berabere bitti! ELO değişimi olmadı.";
        if (p1 != null && p1.isOnline()) p1.sendMessage(CC.parse(msg));
        if (p2 != null && p2.isOnline()) p2.sendMessage(CC.parse(msg));
    }

    private void cancelTask(UUID uuid) {
        ScheduledTask t = escapeTasks.remove(uuid);
        if (t != null)
            t.cancel();
    }

    private String getMatchKey(UUID u1, UUID u2) {
        if (u1.compareTo(u2) > 0) {
            return u1.toString() + "_" + u2.toString();
        }
        return u2.toString() + "_" + u1.toString();
    }

    private void limitInventory(Player player) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        int totemCount = 0;

        EloPlayerData data = eloManager.getPlayerData(player.getUniqueId());
        int elo = data != null ? data.getElo() : 1000;

        int maxTotems = 36; // Default
        org.bukkit.configuration.file.FileConfiguration config = eloManager.getEloConfig().get();

        if (elo >= 2300) {
            maxTotems = config.getInt("leagues.master.totem-limit", 4);
        } else if (elo >= 1800) {
            maxTotems = config.getInt("leagues.upper.totem-limit", 8);
        } else if (elo >= 1000) {
            maxTotems = config.getInt("leagues.middle.totem-limit", 14);
        } else {
            maxTotems = 36; // Lowest (Unranked)
        }

        for (int i = 0; i < inv.getSize(); i++) {
            org.bukkit.inventory.ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == org.bukkit.Material.TOTEM_OF_UNDYING) {
                totemCount += item.getAmount();
            }
        }

        org.bukkit.inventory.ItemStack offHand = inv.getItemInOffHand();
        if (offHand != null && offHand.getType() == org.bukkit.Material.TOTEM_OF_UNDYING) {
            totemCount += offHand.getAmount();
        }

        if (totemCount > maxTotems) {
            int toRemove = totemCount - maxTotems;
            // Prune main inventory -> hotbar -> offhand
            for (int i = 9; i <= 35 && toRemove > 0; i++)
                toRemove = pruneSlot(inv, i, toRemove);
            for (int i = 0; i <= 8 && toRemove > 0; i++)
                toRemove = pruneSlot(inv, i, toRemove);
            if (toRemove > 0 && offHand != null && offHand.getType() == org.bukkit.Material.TOTEM_OF_UNDYING) {
                int amount = offHand.getAmount();
                if (amount <= toRemove)
                    inv.setItemInOffHand(null);
                else
                    offHand.setAmount(amount - toRemove);
            }
        }
    }

    private int pruneSlot(org.bukkit.inventory.PlayerInventory inv, int slot, int toRemove) {
        org.bukkit.inventory.ItemStack item = inv.getItem(slot);
        if (item == null || item.getType() != org.bukkit.Material.TOTEM_OF_UNDYING)
            return toRemove;
        int amount = item.getAmount();
        if (amount <= toRemove) {
            inv.setItem(slot, null);
            return toRemove - amount;
        } else {
            item.setAmount(amount - toRemove);
            return 0;
        }
    }

    public void removeBlacklistedItems(Player player) {
        org.bukkit.configuration.file.FileConfiguration config = eloManager.getEloConfig().get();
        if (!config.getBoolean("restrictions.enabled", true))
            return;

        java.util.List<String> blacklist = config.getStringList("restrictions.blacklisted-items");
        java.util.List<String> potBlacklist = config.getStringList("restrictions.blacklisted-potions");

        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            org.bukkit.inventory.ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != org.bukkit.Material.AIR
                    && shouldRemove(item, blacklist, potBlacklist)) {
                inv.setItem(i, null);
            }
        }

        org.bukkit.inventory.ItemStack offHand = inv.getItemInOffHand();
        if (offHand != null && shouldRemove(offHand, blacklist, potBlacklist))
            inv.setItemInOffHand(null);
    }

    private boolean shouldRemove(org.bukkit.inventory.ItemStack item, java.util.List<String> itemBlacklist,
            java.util.List<String> potBlacklist) {
        if (itemBlacklist.contains(item.getType().name()))
            return true;
        if (item.getType() == org.bukkit.Material.POTION || item.getType() == org.bukkit.Material.SPLASH_POTION ||
                item.getType() == org.bukkit.Material.LINGERING_POTION
                || item.getType() == org.bukkit.Material.TIPPED_ARROW) {
            org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
            if (meta == null)
                return false;
            if (meta.getBasePotionType() != null) {
                for (org.bukkit.potion.PotionEffect effect : meta.getBasePotionType().getPotionEffects()) {
                    if (potBlacklist.contains(effect.getType().getName()))
                        return true;
                }
            }
            for (org.bukkit.potion.PotionEffect effect : meta.getCustomEffects()) {
                if (potBlacklist.contains(effect.getType().getName()))
                    return true;
            }
        }
        return false;
    }

    public void clearRestrictedPotions(Player player) {
        org.bukkit.configuration.file.FileConfiguration config = eloManager.getEloConfig().get();
        if (!config.getBoolean("restrictions.enabled", true))
            return;
        java.util.List<String> blacklist = config.getStringList("restrictions.blacklisted-potions");
        for (String potName : blacklist) {
            org.bukkit.potion.PotionEffectType type = org.bukkit.potion.PotionEffectType.getByName(potName);
            if (type != null && player.hasPotionEffect(type))
                player.removePotionEffect(type);
        }
    }
}
