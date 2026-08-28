package com.trcore.managers;

import com.trcore.TRCore;
import org.bukkit.entity.Player;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.UUID;

/**
 * Combat-log yöneticisi.
 *
 * Optimizasyon:
 *  - Combat süresi yapıcıda cache'lenir, her tag() çağrısında config okunmaz.
 *  - ConcurrentHashMap thread-safe erişim sağlar.
 */
public class CombatManager {
    private final TRCore plugin;
    private final Map<UUID, Long> combatTime = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastAttacker = new ConcurrentHashMap<>();

    // Cached config
    private int combatDurationSec;

    public CombatManager(TRCore plugin) {
        this.plugin = plugin;
        this.combatDurationSec = plugin.getConfig().getInt("combat-log.duration", 15);

        // Süresi dolan combat tag'leri temizleee
        org.bukkit.Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            long now = System.currentTimeMillis();
            combatTime.entrySet().removeIf(entry -> entry.getValue() < now);
        }, 1L, 1L, TimeUnit.SECONDS);
    }

    /** Reload sırasında çağrılır. */
    public void loadConfig() {
        this.combatDurationSec = plugin.getConfig().getInt("combat-log.duration", 15);
    }

    public void tag(Player victim, Player attacker) {
        // Cache'lenmiş süre kullanılır — config okunmaz
        long expiry = System.currentTimeMillis() + (combatDurationSec * 1000L);
        combatTime.put(victim.getUniqueId(), expiry);
        lastAttacker.put(victim.getUniqueId(), attacker.getUniqueId());
    }

    public void removeTag(Player p) {
        combatTime.remove(p.getUniqueId());
        lastAttacker.remove(p.getUniqueId());
    }

    public boolean isInCombat(Player p) {
        Long expiry = combatTime.get(p.getUniqueId());
        if (expiry == null) return false;
        if (expiry > System.currentTimeMillis()) return true;
        // Süresi dolmuş — temizle
        combatTime.remove(p.getUniqueId());
        lastAttacker.remove(p.getUniqueId());
        return false;
    }

    public Player getLastAttacker(Player p) {
        UUID attackerUUID = lastAttacker.get(p.getUniqueId());
        if (attackerUUID == null) return null;
        return plugin.getServer().getPlayer(attackerUUID);
    }
}
