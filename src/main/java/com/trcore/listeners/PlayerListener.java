package com.trcore.listeners;

import com.trcore.TRCore;
import com.trcore.managers.BackManager;
import com.trcore.managers.CombatManager;
import com.trcore.utils.CC;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.List;

/**
 * Oyuncu olayları — ölüm, combat, teleport, respawn.
 *
 * Optimizasyon:
 * - Kill-system ve death-command config değerleri cache'lenir.
 * - Her ölümde config.getBoolean/getString yapılmaz.
 * - Combat whitelisted-worlds check HashSet ile O(1).
 */
public class PlayerListener implements Listener {
    private final TRCore plugin;
    private final CombatManager combatManager;
    private final BackManager backManager;

    // Cached config — her ölümde YAML parse yok
    private boolean deathCommandsEnabled;
    private boolean killSystemEnabled;
    private boolean preventSelfKillTitle;
    private boolean killTitleEnabled;
    private String killTitleMain;
    private String killTitleSub;
    private String killSound;
    private String deathFormat;
    private String selfKillFormat;

    public PlayerListener(TRCore plugin, CombatManager combatManager, BackManager backManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
        this.backManager = backManager;
        loadConfig();
    }

    /** Reload sırasında çağrılır. */
    public void loadConfig() {
        this.deathCommandsEnabled = plugin.getConfig().getBoolean("death-commands.enabled", false);
        this.killSystemEnabled = plugin.getConfig().getBoolean("kill-system.enabled", true);
        this.preventSelfKillTitle = plugin.getConfig().getBoolean("kill-system.prevent-self-kill-title", true);
        this.killTitleEnabled = plugin.getConfig().getBoolean("kill-system.title.enabled", true);
        this.killTitleMain = plugin.getConfig().getString("kill-system.title.main", "");
        this.killTitleSub = plugin.getConfig().getString("kill-system.title.sub", "");
        this.killSound = plugin.getConfig().getString("kill-system.sound", "");

        this.deathFormat = plugin.getConfigManager().getMessages().getString("death-messages.format",
                "<gray>[☠] <red>%message%");
        this.selfKillFormat = plugin.getConfigManager().getMessages().getString("death-messages.self-kill",
                "<gray>[☠] <red>%message%");
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND
                || e.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {

            // Optimizasyon: Eğer oyuncu zaten aynı (veya çok yakın) yere ışınlanıyorsa,
            // önceki 'back' konumunu (ölüm konumu gibi) korumak için kaydetme.
            if (e.getTo() != null && e.getFrom().getWorld().equals(e.getTo().getWorld())
                    && e.getFrom().distanceSquared(e.getTo()) < 0.01) {
                return;
            }

            backManager.setLastLocation(e.getPlayer(), e.getFrom());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();

        // --- 1. ÖZELLEŞTİRİLEBİLİR ÖLÜM MESAJI ---
        String originalMsg = e.getDeathMessage();
        e.setDeathMessage(null);

        String victimName = plugin.getDisguiseManager() != null ? plugin.getDisguiseManager().getDisguise(victim) : victim.getName();
        String defaultMsg = originalMsg != null ? originalMsg.replace(victim.getName(), victimName) : victimName + " öldü.";
        boolean isSelfKill = killer == null || killer.equals(victim);
        String killerName = "";
        if (!isSelfKill && killer != null) {
            killerName = plugin.getDisguiseManager() != null ? plugin.getDisguiseManager().getDisguise(killer) : killer.getName();
        }
        Location deathLoc = victim.getLocation();

        // Cache'lenmiş format kullan — config okunmaz
        String format = isSelfKill ? selfKillFormat : deathFormat;

        String finalMessage = format
                .replace("%message%", defaultMsg)
                .replace("%victim%", victimName)
                .replace("%killer%", killerName);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(victim)) {
                p.sendMessage(CC.parse(finalMessage));
                continue;
            }

            if (!p.getWorld().equals(deathLoc.getWorld()))
                continue;

            if (!plugin.getToggleManager().isDeathMessageEnabled(p.getUniqueId()))
                continue;

            int radius = plugin.getToggleManager().getDeathMessageRadius(p.getUniqueId());
            if (p.getLocation().distanceSquared(deathLoc) <= (double) radius * radius) {
                p.sendMessage(CC.parse(finalMessage));
            }
        }

        // --- 2. DİĞER İŞLEMLER (Back, Combat, Kill System) ---
        backManager.setLastLocation(victim, victim.getLocation());

        if (combatManager.isInCombat(victim)) {
            Player attacker = combatManager.getLastAttacker(victim);
            combatManager.removeTag(victim);
            if (attacker != null)
                combatManager.removeTag(attacker);
        }

        if (deathCommandsEnabled) {
            String w = victim.getWorld().getName();
            List<String> cmds = plugin.getConfig().getStringList("death-commands.worlds." + w);
            String victimNameReplaced = plugin.getDisguiseManager() != null ? plugin.getDisguiseManager().getDisguise(victim) : victim.getName();
            for (String cmd : cmds) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", victimNameReplaced));
            }
        }

        if (!killSystemEnabled)
            return;
        if (killer == null)
            return;
        if (killer.equals(victim) && preventSelfKillTitle)
            return;

        if (killTitleEnabled) {
            String victimNameReplaced = plugin.getDisguiseManager() != null ? plugin.getDisguiseManager().getDisguise(victim) : victim.getName();
            String main = killTitleMain.replace("%victim%", victimNameReplaced);
            String sub = killTitleSub.replace("%victim%", victimNameReplaced);
            killer.showTitle(Title.title(CC.parse(main), CC.parse(sub)));
        }

        if (killSound != null && !killSound.isEmpty()) {
            CC.playSound(killer, killSound.toUpperCase());
        }
    }

    // --- Night Vision: Respawn sonrası geri uygula ---
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();

        // Akıllı Respawn: Eğer dünyasına özel spawn (ör. Box) varsa oraya, yoksa ana
        // spawn'a gönder
        org.bukkit.Location respawnLoc = plugin.getSpawnManager().getSpawnLocation(player);
        if (respawnLoc != null) {
            e.setRespawnLocation(respawnLoc);
        }

        if (plugin.getNightVisionManager().isNightVisionEnabled(player.getUniqueId())) {
            player.getScheduler().runDelayed(plugin, t -> plugin.getNightVisionManager().applyEffect(player), null, 1L);
        }
    }

    // --- Night Vision: Totem patlayınca geri uygula ---
    @EventHandler(ignoreCancelled = true)
    public void onTotemUse(EntityResurrectEvent e) {
        if (!(e.getEntity() instanceof Player player))
            return;
        if (!plugin.getNightVisionManager().isNightVisionEnabled(player.getUniqueId()))
            return;
        player.getScheduler().runDelayed(plugin, t -> plugin.getNightVisionManager().applyEffect(player), null, 2L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent e) {
        if (!plugin.getConfigManager().combatLogEnabled)
            return;
        if (e.getEntity() instanceof Player victim) {
            Player attacker = null;
            if (e.getDamager() instanceof Player p)
                attacker = p;
            else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p)
                attacker = p;

            if (attacker != null && !attacker.equals(victim)) {
                combatManager.tag(victim, attacker);
                combatManager.tag(attacker, victim);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (plugin.getConfigManager().combatLogEnabled
                && plugin.getConfigManager().combatLogKillOnQuit && combatManager.isInCombat(p)) {
            Player attacker = combatManager.getLastAttacker(p);
            p.setHealth(0);
            // Bukkit.broadcast(CC.get("combat.tagged", "%player%", p.getName()));
            if (attacker != null && attacker.isOnline()) {
                attacker.sendMessage(CC.get("combat.opponent-quit"));
                combatManager.removeTag(attacker);
            }
            combatManager.removeTag(p);
        }
    }

}
