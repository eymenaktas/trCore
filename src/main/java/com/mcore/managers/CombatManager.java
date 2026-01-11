package com.mcore.managers;
import com.mcore.mCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CombatManager {
    private final mCore plugin;
    private final Map<UUID, Long> combatTag = new HashMap<>();
    private final Map<UUID, UUID> lastAttacker = new HashMap<>();

    public CombatManager(mCore plugin) { this.plugin = plugin; }

    public void tag(Player victim, Player attacker) {
        // WHITELIST KONTROLÜ
        // Config.yml'den izinli dünyaları çekiyoruz
        List<String> allowedWorlds = plugin.getConfig().getStringList("combat-log.whitelisted-worlds");

        // Eğer oyuncunun bulunduğu dünya listede YOKSA, işlem yapma (return)
        if (!allowedWorlds.contains(victim.getWorld().getName())) {
            return;
        }

        combatTag.put(victim.getUniqueId(), System.currentTimeMillis() + (plugin.getConfig().getInt("combat-log.duration") * 1000L));
        if (attacker != null) {
            lastAttacker.put(victim.getUniqueId(), attacker.getUniqueId());
        }
    }

    public boolean isInCombat(Player p) {
        return combatTag.containsKey(p.getUniqueId()) && combatTag.get(p.getUniqueId()) > System.currentTimeMillis();
    }

    public Player getLastAttacker(Player victim) {
        if (lastAttacker.containsKey(victim.getUniqueId())) {
            return Bukkit.getPlayer(lastAttacker.get(victim.getUniqueId()));
        }
        return null;
    }

    public void removeTag(Player p) {
        combatTag.remove(p.getUniqueId());
        lastAttacker.remove(p.getUniqueId());
    }

    public void startTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            combatTag.entrySet().removeIf(entry -> entry.getValue() < System.currentTimeMillis());
        }, 20L, 20L);
    }
}