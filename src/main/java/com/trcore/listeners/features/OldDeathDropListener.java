package com.trcore.listeners.features;

import com.trcore.TRCore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

public class OldDeathDropListener implements Listener {

    private final TRCore plugin;

    public OldDeathDropListener(TRCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent e) {
        if (!plugin.getConfig().getBoolean("old-death-drops.enabled", false))
            return;

        Location loc = e.getEntity().getLocation();
        World world = loc.getWorld();
        for (ItemStack item : e.getDrops()) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                Item droppedItem = world.dropItem(loc, item);
                // Loot gizleme sistemi ile uyumlu hale getir
                plugin.getDeathDropManager().registerManualDrop(droppedItem);
            }
        }
        e.getDrops().clear();
    }
}

