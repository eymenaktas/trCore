package com.mcore.listeners;
import com.mcore.mCore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class WorldChangeListener implements Listener {
    private final mCore plugin;
    public WorldChangeListener(mCore plugin) { this.plugin = plugin; }

    @EventHandler
    public void onChange(PlayerChangedWorldEvent e) {
        if (!plugin.getConfig().getBoolean("world-change.enabled")) return;

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("world-change.commands");
        if (sec != null) {
            String toWorld = e.getPlayer().getWorld().getName();
            if (sec.contains(toWorld)) {
                String cmd = sec.getString(toWorld).replace("%player%", e.getPlayer().getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }
    }
}