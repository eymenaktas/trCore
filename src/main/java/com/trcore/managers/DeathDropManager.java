package com.trcore.managers;

import com.trcore.TRCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.world.EntitiesLoadEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Bir oyuncu öldüğünde düşürdüğü itemleri kayıt altına alır.
 * toggle-açık oyuncular için bu itemler player.hideEntity() ile gizlenir.
 * Performans: sadece UUID set, heap-friendly.
 */
public class DeathDropManager implements Listener {

    private final TRCore plugin;
    // spawn olan ölüm dropu item entity UUID'leri
    private final Set<UUID> deathDrops = Collections.synchronizedSet(new HashSet<>());

    public DeathDropManager(TRCore plugin) {
        this.plugin = plugin;
    }

    // --- Oyuncu öldüğünde drop liste snapshot'ını al ---
    // HIGH priority ile diğer eklentilerin listeyi değiştirmesine izin ver
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (e.getDrops().isEmpty())
            return;
        if (!plugin.getToggleManager().hasAnyDeathDropHider())
            return; // Kimse açmamışsa işlem yok

        // Paper: drop edilen itemler spawn sonrası entity olarak ortaya çıkar.
        // Bir tick sonra spawn edilen itemleri bul ve gizle.
        org.bukkit.Location loc = e.getEntity().getLocation();

        Bukkit.getRegionScheduler().run(plugin, loc, task -> {
            // Lokasyona yakın Item entity'lerini bul (1 tick sonra spawnlanmış olurlar)
            for (Entity entity : loc.getWorld().getNearbyEntities(loc, 4, 4, 4)) {
                if (!(entity instanceof Item))
                    continue;
                UUID itemId = entity.getUniqueId();
                deathDrops.add(itemId);

                // toggle açık tüm oyunculardan gizle
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    if (plugin.getToggleManager().isDeathDropHideEnabled(viewer.getUniqueId())) {
                        viewer.hideEntity(plugin, entity);
                    }
                }
            }
        });
    }

    /**
     * Manuel olarak spawn edilen drop'ları sisteme kaydeder.
     * OldDeathDropListener ile uyumluluk için.
     */
    public void registerManualDrop(Item item) {
        if (!plugin.getToggleManager().hasAnyDeathDropHider())
            return;

        deathDrops.add(item.getUniqueId());

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (plugin.getToggleManager().isDeathDropHideEnabled(viewer.getUniqueId())) {
                viewer.hideEntity(plugin, item);
            }
        }
    }

    /**
     * Yeni bir oyuncu bağlandığında veya toggle açıldığında mevcut açık
     * death drop'ları ona gizle.
     */
    public void hideAllCurrentDropsFor(Player player) {
        if (deathDrops.isEmpty())
            return;
        for (UUID id : deathDrops) {
            Entity en = Bukkit.getEntity(id);
            if (en != null) {
                player.hideEntity(plugin, en);
            }
        }
    }

    /**
     * Toggle kapandığında mevcut tüm death drop'ları oyuncuya tekrar göster.
     */
    public void showAllCurrentDropsFor(Player player) {
        if (deathDrops.isEmpty())
            return;
        for (UUID id : deathDrops) {
            Entity en = Bukkit.getEntity(id);
            if (en != null) {
                player.showEntity(plugin, en);
            }
        }
    }

    // --- Item yerden alındığında set'ten çıkar ---
    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        deathDrops.remove(e.getItem().getUniqueId());
    }

    // --- Item yok olduğunda set'ten çıkar ---
    @EventHandler
    public void onDespawn(ItemDespawnEvent e) {
        deathDrops.remove(e.getEntity().getUniqueId());
    }
}


