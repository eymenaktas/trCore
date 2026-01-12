package com.mcore.listeners;

import com.mcore.mCore;
import com.mcore.managers.BackManager;
import com.mcore.managers.CombatManager;
import com.mcore.utils.CC;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.List;

public class PlayerListener implements Listener {
    private final mCore plugin;
    private final CombatManager combatManager;
    private final BackManager backManager; // EKLENDİ

    public PlayerListener(mCore plugin, CombatManager combatManager, BackManager backManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
        this.backManager = backManager;
    }

    // --- Back Sistemi (Işınlanma Kaydı) ---
    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        // Sadece komut veya plugin ile yapılan ışınlanmaları kaydet
        if (e.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND || e.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            backManager.setLastLocation(e.getPlayer(), e.getFrom());
        }
    }

    // --- Ölüm İşlemleri (Back + Kill Sistemi + Ölüm Komutları) ---
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();

        // Back için son konumu kaydet
        backManager.setLastLocation(victim, victim.getLocation());

        // Combat Tag temizle
        if (combatManager.isInCombat(victim)) {
            Player attacker = combatManager.getLastAttacker(victim);
            combatManager.removeTag(victim);
            if (attacker != null) combatManager.removeTag(attacker);
        }

        // Ölüm Komutları (Config'den)
        if (plugin.getConfig().getBoolean("death-commands.enabled")) {
            String w = victim.getWorld().getName();
            List<String> cmds = plugin.getConfig().getStringList("death-commands.worlds." + w);
            for (String cmd : cmds) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", victim.getName()));
            }
        }

        // Kill Sistemi (Title ve Ses)
        if (!plugin.getConfig().getBoolean("kill-system.enabled")) return;
        Player killer = victim.getKiller();
        if (killer == null) return;
        if (killer.equals(victim) && plugin.getConfig().getBoolean("kill-system.prevent-self-kill-title")) return;

        if (plugin.getConfig().getBoolean("kill-system.title.enabled")) {
            String main = plugin.getConfig().getString("kill-system.title.main").replace("%victim%", victim.getName());
            String sub = plugin.getConfig().getString("kill-system.title.sub").replace("%victim%", victim.getName());
            killer.showTitle(Title.title(CC.parse(main), CC.parse(sub)));
        }

        try {
            String soundName = plugin.getConfig().getString("kill-system.sound");
            if (soundName != null && !soundName.isEmpty()) {
                killer.playSound(killer.getLocation(), Sound.valueOf(soundName.toUpperCase()), 1f, 1f);
            }
        } catch (IllegalArgumentException ignored) {}
    }

    // --- Combat Log ---
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent e) {
        if (!plugin.getConfig().getBoolean("combat-log.enabled")) return;
        if (e.getEntity() instanceof Player victim) {
            Player attacker = null;
            if (e.getDamager() instanceof Player p) attacker = p;
            else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) attacker = p;

            if (attacker != null && !attacker.equals(victim)) {
                combatManager.tag(victim, attacker);
                combatManager.tag(attacker, victim);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (plugin.getConfig().getBoolean("combat-log.enabled") && plugin.getConfig().getBoolean("combat-log.kill-on-quit") && combatManager.isInCombat(p)) {
            Player attacker = combatManager.getLastAttacker(p);
            p.setHealth(0);
            Bukkit.broadcast(CC.get("combat.tagged", "%player%", p.getName()));
            if (attacker != null && attacker.isOnline()) {
                attacker.sendMessage(CC.get("combat.opponent-quit"));
                combatManager.removeTag(attacker);
            }
            combatManager.removeTag(p);
        }
    }
}