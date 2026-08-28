package com.trcore.elo;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

public class RankedMatchListener implements Listener {

    private final EloManager eloManager;
    private final RankedMatchManager matchManager;
    private final RankedQueueManager queueManager;

    public RankedMatchListener(EloManager eloManager, RankedMatchManager matchManager, RankedQueueManager queueManager) {
        this.eloManager = eloManager;
        this.matchManager = matchManager;
        this.queueManager = queueManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        // ConnectionListener zaten async load yapiyor.
        // Burada tekrar senkron load yapmak monitor lock contention olusturuyor.
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();

        // Queue'dan cikar
        queueManager.cleanup(p);

        // Mactaysa yenilgi saydir
        ActiveMatch match = matchManager.getMatch(uuid);
        if (match != null) {
            matchManager.handleDisconnect(uuid, match);
        }

        // Cache'ten sil
        eloManager.unloadPlayer(uuid);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        UUID deadUUID = p.getUniqueId();

        ActiveMatch match = matchManager.getMatch(deadUUID);
        if (match != null) {
            UUID opponentUUID = match.getOpponent(deadUUID);
            Player opponent = Bukkit.getPlayer(opponentUUID);

            // DRAW CHECK: If opponent is also dead OR health <= 0 (same tick deaths)
            if (opponent != null && (opponent.isDead() || opponent.getHealth() <= 0)) {
                matchManager.handleDraw(match);
                return;
            }

            if (opponentUUID != null) {
                matchManager.endMatch(opponentUUID, deadUUID, match);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof EnderCrystal) {
            Player source = resolveDamager(e.getDamager());
            if (source != null && matchManager.getMatch(source.getUniqueId()) != null) {
                matchManager.addCrystalExplosion(source.getUniqueId());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnyPlayerDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player target)) return;
        ActiveMatch match = matchManager.getMatch(target.getUniqueId());
        if (match == null) return;

        UUID opponent = match.getOpponent(target.getUniqueId());
        if (opponent == null) return;

        // Oyuncunun aldığı hasarı rakibin "rakibe toplam hasar" istatistiğine yazar.
        matchManager.addDamage(opponent, e.getFinalDamage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemPop(EntityResurrectEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (matchManager.getMatch(p.getUniqueId()) == null) return;
        matchManager.addTotemPop(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnchorUse(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (e.getClickedBlock().getType() != Material.RESPAWN_ANCHOR) return;

        Player p = e.getPlayer();
        ActiveMatch match = matchManager.getMatch(p.getUniqueId());
        if (match == null) return;

        Block b = e.getClickedBlock();
        if (!(b.getBlockData() instanceof RespawnAnchor anchorData)) return;

        boolean canExplode = !b.getWorld().getEnvironment().name().equals("NETHER") && anchorData.getCharges() > 0;
        if (canExplode) {
            matchManager.addAnchorExplosion(p.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHistoryClick(InventoryClickEvent e) {
        matchManager.handleHistoryClick(e);
    }

    private Player resolveDamager(Entity raw) {
        if (raw instanceof Player p) return p;
        if (raw instanceof org.bukkit.entity.Projectile proj) {
            ProjectileSource shooter = proj.getShooter();
            if (shooter instanceof Player p) return p;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(org.bukkit.event.player.PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        org.bukkit.configuration.file.FileConfiguration config = eloManager.getEloConfig().get();

        // 0. Check Countdown Commands
        if (queueManager.isInCountdown(p)) {
            String msg = e.getMessage();
            int spaceIndex = msg.indexOf(' ');
            String rootCommand = spaceIndex == -1 ? msg.toLowerCase() : msg.substring(0, spaceIndex).toLowerCase();

            // Sadece /rq ve türevlerine izin ver
            if (!rootCommand.equalsIgnoreCase("/rq") && !rootCommand.equalsIgnoreCase("/rankedqueue") && !rootCommand.equalsIgnoreCase("/rtpranked")) {
                e.setCancelled(true);
                p.sendMessage(com.trcore.utils.CC.parse("<red>Işınlanma aşamasında komut kullanamazsın! İptal için: <white>/rq</white></red>"));
                return;
            }
        }

        // 1. Check Match Commands
        String raw = e.getMessage();
        String[] split = raw.split(" ");
        String base = split[0].toLowerCase();
        if (base.equals("/rankedhistory")) {
            e.setCancelled(true);
            if (split.length == 1) {
                matchManager.openLatestHistory(p);
            } else {
                try {
                    java.util.UUID matchId = java.util.UUID.fromString(split[1]);
                    matchManager.openHistoryDetail(p, matchId);
                } catch (Exception ex) {
                    p.sendMessage(com.trcore.utils.CC.parse("<red>Gecersiz UUID. Kullanim: /rankedhistory <match-uuid>"));
                }
            }
            return;
        }

        ActiveMatch match = matchManager.getMatch(uuid);
        if (match != null) {
            if (!config.getBoolean("match-commands.enabled", true)) return;
            
            String msg = e.getMessage();
            int spaceIndex = msg.indexOf(' ');
            String rootCommand = spaceIndex == -1 ? msg.toLowerCase() : msg.substring(0, spaceIndex).toLowerCase();
            
            java.util.List<String> allowed = config.getStringList("match-commands.allowed");
            if (!allowed.contains(rootCommand)) {
                e.setCancelled(true);
                String err = config.getString("match-commands.blocked-message", "<red>Ranked mactayken sadece belirli komutlari kullanabilirsin!</red>");
                p.sendMessage(com.trcore.utils.CC.parse(err));
            }
            return;
        }

        // 2. Check Queue Commands
        if (queueManager.isInQueue(p)) {
            if (!config.getBoolean("queue-commands.enabled", true)) return;

            String msg = e.getMessage();
            int spaceIndex = msg.indexOf(' ');
            String rootCommand = spaceIndex == -1 ? msg.toLowerCase() : msg.substring(0, spaceIndex).toLowerCase();

            java.util.List<String> allowed = config.getStringList("queue-commands.allowed");
            if (!allowed.contains(rootCommand)) {
                e.setCancelled(true);
                String err = config.getString("queue-commands.blocked-message", "<red>Sıradayken sadece belirli komutları kullanabilirsin!</red>");
                p.sendMessage(com.trcore.utils.CC.parse(err));
            }
        }
    }
}
