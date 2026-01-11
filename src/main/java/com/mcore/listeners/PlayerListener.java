package com.mcore.listeners;
import com.mcore.mCore;
import com.mcore.managers.CombatManager;
import com.mcore.utils.CC;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class PlayerListener implements Listener {
    private final mCore plugin;
    private final CombatManager combatManager;
    public static final HashMap<UUID, Location> lastLocations = new HashMap<>();

    public PlayerListener(mCore plugin, CombatManager combatManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
    }

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

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        lastLocations.put(victim.getUniqueId(), victim.getLocation());

        if (combatManager.isInCombat(victim)) {
            Player attacker = combatManager.getLastAttacker(victim);
            combatManager.removeTag(victim);
            if (attacker != null) combatManager.removeTag(attacker);
        }

        // Ölüm Komutları
        if (plugin.getConfig().getBoolean("death-commands.enabled")) {
            String w = victim.getWorld().getName();
            List<String> cmds = plugin.getConfig().getStringList("death-commands.worlds." + w);
            for (String cmd : cmds) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", victim.getName()));
            }
        }

        if (!plugin.getConfig().getBoolean("kill-system.enabled")) return;
        Player killer = victim.getKiller();
        if (killer == null) return;
        if (killer.equals(victim) && plugin.getConfig().getBoolean("kill-system.prevent-self-kill-title")) return;

        if (plugin.getConfig().getBoolean("kill-system.title.enabled")) {
            String main = plugin.getConfig().getString("kill-system.title.main").replace("%victim%", victim.getName());
            String sub = plugin.getConfig().getString("kill-system.title.sub").replace("%victim%", victim.getName());
            killer.showTitle(Title.title(CC.parse(main), CC.parse(sub)));
        }

        // GÜNCELLENDİ: Güvenli Ses Çalma
        try {
            String soundName = plugin.getConfig().getString("kill-system.sound");
            if (soundName != null && !soundName.isEmpty()) {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                killer.playSound(killer.getLocation(), sound, 1f, 1f);
            }
        } catch (IllegalArgumentException ignored) {}
    }
}