package com.mcore.listeners;

import com.mcore.mCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class WorldChangeListener implements Listener {
    private final mCore plugin;

    public WorldChangeListener(mCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) { execute(e.getPlayer()); }

    @EventHandler
    public void onChange(PlayerChangedWorldEvent e) { execute(e.getPlayer()); }

    private void execute(Player p) {
        if (!plugin.getConfig().getBoolean("world-change.enabled")) return;
        String cmd = plugin.getConfig().getString("world-change.commands.spawn");
        if (cmd != null && !cmd.isEmpty()) {
            p.performCommand(cmd);
        }
    }
}