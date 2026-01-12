//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.mcore.managers;

import com.mcore.mCore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CombatManager {
    private final mCore plugin;
    private final Map<UUID, Long> combatTag = new HashMap();
    private final Map<UUID, UUID> lastAttacker = new HashMap();

    public CombatManager(mCore plugin) {
        this.plugin = plugin;
    }

    public void tag(Player victim, Player attacker) {
        List<String> allowedWorlds = this.plugin.getConfig().getStringList("combat-log.whitelisted-worlds");
        if (allowedWorlds.contains(victim.getWorld().getName())) {
            this.combatTag.put(victim.getUniqueId(), System.currentTimeMillis() + (long)this.plugin.getConfig().getInt("combat-log.duration") * 1000L);
            if (attacker != null) {
                this.lastAttacker.put(victim.getUniqueId(), attacker.getUniqueId());
            }

        }
    }

    public boolean isInCombat(Player p) {
        return this.combatTag.containsKey(p.getUniqueId()) && (Long)this.combatTag.get(p.getUniqueId()) > System.currentTimeMillis();
    }

    public Player getLastAttacker(Player victim) {
        return this.lastAttacker.containsKey(victim.getUniqueId()) ? Bukkit.getPlayer((UUID)this.lastAttacker.get(victim.getUniqueId())) : null;
    }

    public void removeTag(Player p) {
        this.combatTag.remove(p.getUniqueId());
        this.lastAttacker.remove(p.getUniqueId());
    }

    public void startTask() {
        Bukkit.getScheduler().runTaskTimer(this.plugin, () -> this.combatTag.entrySet().removeIf((entry) -> (Long)entry.getValue() < System.currentTimeMillis()), 20L, 20L);
    }
}
