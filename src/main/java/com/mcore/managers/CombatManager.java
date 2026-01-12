package com.mcore.managers;

import com.mcore.mCore;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatManager {
    private final mCore plugin;
    private final Map<UUID, Long> combatTime = new HashMap<>();
    private final Map<UUID, UUID> lastAttacker = new HashMap<>();

    public CombatManager(mCore plugin) {
        this.plugin = plugin;
        // Combat task
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                combatTime.entrySet().removeIf(entry -> entry.getValue() < now);
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void tag(Player victim, Player attacker) {
        int duration = plugin.getConfig().getInt("combat-log.duration", 15);
        long expiry = System.currentTimeMillis() + (duration * 1000L);
        combatTime.put(victim.getUniqueId(), expiry);
        lastAttacker.put(victim.getUniqueId(), attacker.getUniqueId());
    }

    public void removeTag(Player p) {
        combatTime.remove(p.getUniqueId());
        lastAttacker.remove(p.getUniqueId());
    }

    public boolean isInCombat(Player p) {
        return combatTime.containsKey(p.getUniqueId()) && combatTime.get(p.getUniqueId()) > System.currentTimeMillis();
    }

    public Player getLastAttacker(Player p) {
        if (!lastAttacker.containsKey(p.getUniqueId())) return null;
        return plugin.getServer().getPlayer(lastAttacker.get(p.getUniqueId()));
    }
}