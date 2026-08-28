package com.trcore.elo;

import com.trcore.TRCore;
import com.trcore.utils.CC;
import com.trcore.managers.RTPManager;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import net.kyori.adventure.title.Title;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class RankedQueueManager {

    private final TRCore plugin;
    private final EloManager eloManager;
    private final RankedMatchManager matchManager;
    
    private final LinkedList<Player> lowestQueue = new LinkedList<>();
    private final LinkedList<Player> middleQueue = new LinkedList<>();
    private final LinkedList<Player> upperQueue = new LinkedList<>();
    private final LinkedList<Player> masterQueue = new LinkedList<>();
    private final Map<UUID, ScheduledTask> timeoutTasks = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> countdownPartners = new ConcurrentHashMap<>();

    public RankedQueueManager(TRCore plugin, EloManager eloManager, RankedMatchManager matchManager) {
        this.plugin = plugin;
        this.eloManager = eloManager;
        this.matchManager = matchManager;
    }

    public synchronized void toggle(Player p) {
        if (isInQueue(p) || isInCountdown(p)) leave(p);
        else join(p);
    }

    public synchronized void join(Player p) {
        if (isInQueue(p) || isInCountdown(p)) return;

        if (plugin.getQueueManager() != null && plugin.getQueueManager().isInQueue(p)) {
            p.sendMessage(CC.parse("<red>Önce bulunduğun sıradan çıkmalısın!</red>"));
            return;
        }

        // Requirement Check
        if (!checkRequirements(p)) {
            p.sendMessage(CC.parse(eloManager.getEloConfig().get().getString("messages.queue-requirements-failed")));
            String errSound = eloManager.getEloConfig().get().getString("sounds.queue-error");
            if (errSound != null && !errSound.isEmpty()) CC.playSound(p, errSound);
            return;
        }

        // TPA Cleanup
        if (plugin.getTpaManager() != null) {
            plugin.getTpaManager().cleanup(p);
        }

        // --- Ensure data is loaded (Failsafe) ---
        eloManager.loadPlayer(p.getUniqueId());
        EloPlayerData data = eloManager.getPlayerData(p.getUniqueId());
        int elo = data != null ? data.getElo() : 1000;
        
        // Determine league-based queue
        LinkedList<Player> targetQueue;
        String messageKey;
        String defaultMsg;

        if (elo < 1000) {
            targetQueue = lowestQueue;
            messageKey = "messages.lowest-queue-joined";
            defaultMsg = "<gray>Düşük Lig sırasına katıldın! [%current% kişi bekliyor]";
        } else if (elo < 1800) {
            targetQueue = middleQueue;
            messageKey = "messages.lower-queue-joined";
            defaultMsg = "<green>Orta Lig sırasına katıldın! <gray>[%current% kişi bekliyor]";
        } else if (elo < 2300) {
            targetQueue = upperQueue;
            messageKey = "messages.upper-queue-joined";
            defaultMsg = "<light_purple>Yüksek Lig sırasına katıldın! <gray>[%current% kişi bekliyor]";
        } else {
            targetQueue = masterQueue;
            messageKey = "messages.upper-queue-joined"; // Master uses upper message if not defined
            defaultMsg = "<light_purple>Master Lig sırasına katıldın! <gray>[%current% kişi bekliyor]";
        }
        
        targetQueue.add(p);

        p.setGameMode(org.bukkit.GameMode.SURVIVAL); 
        
        // --- PREP (Heal & Rekit) ---
        p.setHealth(20.0);
        p.setFoodLevel(20);
        p.getScheduler().run(plugin, t -> {
            p.performCommand("rekit");
            matchManager.applyRestrictions(p);
        }, null);
        
        String joinMsg = eloManager.getEloConfig().get().getString(messageKey, defaultMsg);
        joinMsg = joinMsg.replace("%current%", String.valueOf(targetQueue.size()));
        p.sendMessage(CC.parse(joinMsg));

        String joinSound = eloManager.getEloConfig().get().getString("sounds.queue-join");
        if (joinSound != null && !joinSound.isEmpty()) CC.playSound(p, joinSound);

        int timeoutSec = eloManager.getEloConfig().get().getInt("ranked-queue.timeout-seconds", 96);
        ScheduledTask task = p.getScheduler().runDelayed(plugin, t -> {
            if (isInQueue(p)) {
                leave(p);
                p.sendMessage(CC.parse(eloManager.getEloConfig().get().getString("messages.queue-timeout")));
            }
        }, null, timeoutSec * 20L);

        if (task != null) timeoutTasks.put(p.getUniqueId(), task);

        checkMatch();
    }

    private boolean checkRequirements(Player p) {
        ConfigurationSection reqSection = eloManager.getEloConfig().get().getConfigurationSection("ranked-queue.requirements");
        if (reqSection == null || !reqSection.getBoolean("enabled", true)) return true;

        List<Map<?, ?>> conditions = reqSection.getMapList("conditions");
        if (conditions.isEmpty()) return true;

        for (Map<?, ?> condition : conditions) {
            String placeholder = (String) condition.get("placeholder");
            Object minObj = condition.get("min");
            int min = minObj instanceof Number ? ((Number) minObj).intValue() : 0;

            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                String parsed = PlaceholderAPI.setPlaceholders(p, placeholder);
                try {
                    int val = Integer.parseInt(parsed);
                    if (val < min) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false; // NaN
                }
            } else {
                return false; 
            }
        }
        return true;
    }

    public synchronized void leave(Player p) {
        if (lowestQueue.remove(p) || middleQueue.remove(p) || upperQueue.remove(p) || masterQueue.remove(p)) {
            p.sendMessage(CC.parse(eloManager.getEloConfig().get().getString("messages.queue-left")));
            cancelTimeout(p.getUniqueId());
            return;
        }

        // Check if in countdown
        UUID partnerUUID = countdownPartners.remove(p.getUniqueId());
        if (partnerUUID != null) {
            countdownPartners.remove(partnerUUID);
            p.sendMessage(CC.parse(eloManager.getEloConfig().get().getString("messages.queue-left", "&8&l➜&r <gray>Sıradan çıktın.</gray>")));
            
            Player partner = Bukkit.getPlayer(partnerUUID);
            if (partner != null) {
                partner.sendMessage(CC.parse("<red>Rakibin eşleşmeyi iptal ettiği için sıradan çıkarıldın.</red>"));
                // Clear titles
                partner.hideTitle();
            }
        }
    }

    public synchronized void cleanup(Player player) {
        lowestQueue.remove(player);
        middleQueue.remove(player);
        upperQueue.remove(player);
        masterQueue.remove(player);
        cancelTimeout(player.getUniqueId());
        
        UUID partnerUUID = countdownPartners.remove(player.getUniqueId());
        if (partnerUUID != null) {
            countdownPartners.remove(partnerUUID);
            Player partner = Bukkit.getPlayer(partnerUUID);
            if (partner != null) {
                partner.sendMessage(CC.parse("<red>Rakibin oyundan ayrıldığı için eşleşme iptal edildi.</red>"));
                partner.hideTitle();
            }
        }
    }

    private synchronized void checkMatch() {
        processLeague(lowestQueue);
        processLeague(middleQueue);
        processLeague(upperQueue);
        processLeague(masterQueue);
    }

    private void processLeague(LinkedList<Player> leagueQueue) {
        if (leagueQueue.size() < 2) return;

        Player bestP1 = null;
        Player bestP2 = null;
        int minDiff = Integer.MAX_VALUE;

        for (int i = 0; i < leagueQueue.size(); i++) {
            Player p1 = leagueQueue.get(i);
            EloPlayerData d1 = eloManager.getPlayerData(p1.getUniqueId());
            if (d1 == null) continue;

            for (int j = i + 1; j < leagueQueue.size(); j++) {
                Player p2 = leagueQueue.get(j);
                EloPlayerData d2 = eloManager.getPlayerData(p2.getUniqueId());
                if (d2 == null) continue;



                int diff = Math.abs(d1.getElo() - d2.getElo());
                if (diff < minDiff) {
                    minDiff = diff;
                    bestP1 = p1;
                    bestP2 = p2;
                }
            }
        }

        if (bestP1 != null && bestP2 != null) {
            leagueQueue.remove(bestP1);
            leagueQueue.remove(bestP2);
            executeMatch(bestP1, bestP2);
        }
    }

    private void executeMatch(Player p1, Player p2) {
        cancelTimeout(p1.getUniqueId());
        cancelTimeout(p2.getUniqueId());

        // 1. Determine League and Configs
        EloPlayerData d1 = eloManager.getPlayerData(p1.getUniqueId());
        int elo = d1 != null ? d1.getElo() : 1000;
        String leagueKey = getLeagueKey(elo);

        ConfigurationSection notifyConfig = eloManager.getEloConfig().get().getConfigurationSection("matchmaking-notifications");
        if (notifyConfig == null) {
            startMatch(p1, p2);
            return;
        }

        // 2. Play Found Sound
        String foundSound = notifyConfig.getString("sounds.match-found");
        if (foundSound != null && !foundSound.isEmpty()) {
            CC.playSound(p1, foundSound);
            CC.playSound(p2, foundSound);
        }

        // 3. Prepare World and Region
        List<String> worlds = eloManager.getEloConfig().get().getStringList("ranked-queue.worlds");
        if (worlds == null || worlds.isEmpty()) {
            p1.sendMessage(CC.parse("<red>Hata: Ranked Queue dünyaları yapılandırılmamış!"));
            return;
        }
        String wName = worlds.get(ThreadLocalRandom.current().nextInt(worlds.size()));
        String regionName = plugin.getConfig().getString("tpa.world-names." + wName.toLowerCase(), wName);

        // 4. Start Countdown
        int countdown = notifyConfig.getInt("countdown-seconds", 3);
        String titleTemplate = notifyConfig.getString("leagues." + leagueKey + ".title", "<green>Eşleşme Bulundu!");
        String subTemplate = notifyConfig.getString("leagues." + leagueKey + ".subtitle", "<gray>Bölge: %world% | Başlıyor: <white>%time%");
        String cdSound = notifyConfig.getString("sounds.countdown");

        countdownPartners.put(p1.getUniqueId(), p2.getUniqueId());
        countdownPartners.put(p2.getUniqueId(), p1.getUniqueId());

        startCountdown(p1, p2, countdown, titleTemplate, subTemplate, regionName, cdSound, wName);
    }

    private String getLeagueKey(int elo) {
        if (elo < 1000) return "lowest";
        if (elo < 1800) return "middle";
        if (elo < 2300) return "upper";
        return "master";
    }

    private void startCountdown(Player p1, Player p2, int seconds, String titleTpt, String subTpt, String region, String sound, String worldName) {
        new Object() {
            int remaining = seconds;
            {
                Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> {
                    if (!p1.isOnline() || !p2.isOnline() || !countdownPartners.containsKey(p1.getUniqueId())) {
                        t.cancel();
                        countdownPartners.remove(p1.getUniqueId());
                        countdownPartners.remove(p2.getUniqueId());
                        return;
                    }

                if (remaining <= 0) {
                    t.cancel();
                    Bukkit.getGlobalRegionScheduler().run(plugin, t2 -> startMatchAfterCountdown(p1, p2, worldName));
                    return;
                }

                String sub = subTpt.replace("%world%", region).replace("%time%", String.valueOf(remaining));
                Title titleObj = Title.title(CC.parse(titleTpt), CC.parse(sub));
                
                p1.showTitle(titleObj);
                p2.showTitle(titleObj);

                if (sound != null && !sound.isEmpty()) {
                    CC.playSound(p1, sound);
                    CC.playSound(p2, sound);
                }

                remaining--;
                }, 1L, 20L);
            }
        };
    }

    private void startMatchAfterCountdown(Player p1, Player p2, String wName) {
        World world = Bukkit.getWorld(wName);
        if (world == null) {
            countdownPartners.remove(p1.getUniqueId());
            countdownPartners.remove(p2.getUniqueId());
            p1.sendMessage(CC.parse("<red>Hata: " + wName + " dünyası bulunamadı!"));
            return;
        }

        int minRange = eloManager.getEloConfig().get().getInt("ranked-queue.min-range", 100);
        int maxRange = eloManager.getEloConfig().get().getInt("ranked-queue.max-range", 6000);

        Location[] locs = plugin.getRTPManager().getBufferedPair(wName);
        if (locs != null) {
            teleportMatch(p1, p2, locs, wName);
        } else {
            p1.sendMessage(CC.get("rtp-queue.searching-location"));
            p2.sendMessage(CC.get("rtp-queue.searching-location"));

            plugin.getRTPManager().findPair(world, new RTPManager.RTPWorld(minRange, maxRange, false), foundLocs -> {
                if (foundLocs != null) {
                    teleportMatch(p1, p2, foundLocs, wName);
                } else {
                    countdownPartners.remove(p1.getUniqueId());
                    countdownPartners.remove(p2.getUniqueId());
                    p1.sendMessage(CC.get("rtp.no-location"));
                    p2.sendMessage(CC.get("rtp.no-location"));
                }
            });
        }
    }

    private void startMatch(Player p1, Player p2) {
        List<String> worlds = eloManager.getEloConfig().get().getStringList("ranked-queue.worlds");
        if (worlds == null || worlds.isEmpty()) {
            p1.sendMessage(CC.parse("<red>Hata: Ranked Queue dünyaları yapılandırılmamış!"));
            return;
        }

        String wName = worlds.get(ThreadLocalRandom.current().nextInt(worlds.size()));
        startMatchAfterCountdown(p1, p2, wName);
    }

    private void teleportMatch(Player p1, Player p2, Location[] locs, String worldName) {
        countdownPartners.remove(p1.getUniqueId());
        countdownPartners.remove(p2.getUniqueId());
        
        String matchCmd = eloManager.getEloConfig().get().getString("ranked-queue.command", "");

        p1.teleportAsync(locs[0]).thenAccept(ok -> {
            if (ok) {
                p1.getScheduler().run(plugin, t -> {
                    if (matchCmd != null && !matchCmd.isEmpty()) {
                        p1.performCommand(matchCmd.replace("%player%", p1.getName()));
                    }
                    matchManager.applyRestrictions(p1);
                    sendMatchStartNotification(p1);
                }, null);
            }
        });

        p2.teleportAsync(locs[1]).thenAccept(ok -> {
            if (ok) {
                p2.getScheduler().run(plugin, t -> {
                    if (matchCmd != null && !matchCmd.isEmpty()) {
                        p2.performCommand(matchCmd.replace("%player%", p2.getName()));
                    }
                    matchManager.applyRestrictions(p2);
                    sendMatchStartNotification(p2);
                }, null);
            }
        });

        matchManager.startMatch(p1, p2, locs[0], locs[1], worldName);
    }

    private void sendMatchStartNotification(Player player) {
        ConfigurationSection config = eloManager.getEloConfig().get().getConfigurationSection("match-start-notification");
        if (config == null) return;

        String title = config.getString("title");
        String sub = config.getString("subtitle");
        String sound = config.getString("sound");

        if (title != null || sub != null) {
            player.showTitle(Title.title(CC.parse(title), CC.parse(sub)));
        }
        if (sound != null && !sound.isEmpty()) {
            CC.playSound(player, sound);
        }
    }

    public boolean isInQueue(Player p) {
        return lowestQueue.contains(p) || middleQueue.contains(p) || upperQueue.contains(p) || masterQueue.contains(p);
    }

    private void cancelTimeout(UUID uuid) {
        ScheduledTask t = timeoutTasks.remove(uuid);
        if (t != null) t.cancel();
    }

    public boolean isInCountdown(Player p) {
        return countdownPartners.containsKey(p.getUniqueId());
    }
}
