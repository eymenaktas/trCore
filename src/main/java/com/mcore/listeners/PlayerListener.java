//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.mcore.listeners;

import com.mcore.mCore;
import com.mcore.managers.CombatManager;
import com.mcore.utils.CC;
import java.util.HashMap;
import java.util.UUID;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

public class PlayerListener implements Listener {
    private final mCore plugin;
    private final CombatManager combatManager;
    public static final HashMap<UUID, Location> lastLocations = new HashMap();

    public PlayerListener(mCore plugin, CombatManager combatManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onCombat(EntityDamageByEntityEvent e) {
        if (this.plugin.getConfig().getBoolean("combat-log.enabled")) {
            Entity var3 = e.getEntity();
            if (var3 instanceof Player) {
                Player victim = (Player)var3;
                Player attacker = null;
                Entity var7 = e.getDamager();
                if (var7 instanceof Player) {
                    Player p = (Player)var7;
                    attacker = p;
                } else {
                    var7 = e.getDamager();
                    if (var7 instanceof Projectile) {
                        Projectile proj = (Projectile)var7;
                        ProjectileSource var10 = proj.getShooter();
                        if (var10 instanceof Player) {
                            Player p = (Player)var10;
                            attacker = p;
                        }
                    }
                }

                if (attacker != null && !attacker.equals(victim)) {
                    this.combatManager.tag(victim, attacker);
                    this.combatManager.tag(attacker, victim);
                }
            }

        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (this.plugin.getConfig().getBoolean("combat-log.enabled") && this.plugin.getConfig().getBoolean("combat-log.kill-on-quit") && this.combatManager.isInCombat(p)) {
            Player attacker = this.combatManager.getLastAttacker(p);
            p.setHealth((double)0.0F);
            Bukkit.broadcast(CC.get("combat.tagged", new String[]{"%player%", p.getName()}));
            if (attacker != null && attacker.isOnline()) {
                attacker.sendMessage(CC.get("combat.opponent-quit", new String[0]));
                this.combatManager.removeTag(attacker);
            }

            this.combatManager.removeTag(p);
        }

    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        lastLocations.put(victim.getUniqueId(), victim.getLocation());
        if (this.combatManager.isInCombat(victim)) {
            Player attacker = this.combatManager.getLastAttacker(victim);
            this.combatManager.removeTag(victim);
            if (attacker != null) {
                this.combatManager.removeTag(attacker);
            }
        }

        if (this.plugin.getConfig().getBoolean("death-commands.enabled")) {
            String w = victim.getWorld().getName();

            for(String cmd : this.plugin.getConfig().getStringList("death-commands.worlds." + w)) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", victim.getName()));
            }
        }

        if (this.plugin.getConfig().getBoolean("kill-system.enabled")) {
            Player killer = victim.getKiller();
            if (killer != null) {
                if (!killer.equals(victim) || !this.plugin.getConfig().getBoolean("kill-system.prevent-self-kill-title")) {
                    if (this.plugin.getConfig().getBoolean("kill-system.title.enabled")) {
                        String main = this.plugin.getConfig().getString("kill-system.title.main").replace("%victim%", victim.getName());
                        String sub = this.plugin.getConfig().getString("kill-system.title.sub").replace("%victim%", victim.getName());
                        killer.showTitle(Title.title(CC.parse(main), CC.parse(sub)));
                    }

                    try {
                        String soundName = this.plugin.getConfig().getString("kill-system.sound");
                        if (soundName != null && !soundName.isEmpty()) {
                            Sound sound = Sound.valueOf(soundName.toUpperCase());
                            killer.playSound(killer.getLocation(), sound, 1.0F, 1.0F);
                        }
                    } catch (IllegalArgumentException var7) {
                    }

                }
            }
        }
    }
}
