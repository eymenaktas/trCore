//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.mcore.listeners;

import com.mcore.mCore;
import com.mcore.managers.DuelManager;
import com.mcore.managers.QueueManager;
import com.mcore.managers.TPAManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ConnectionListener implements Listener {
    private final mCore plugin;
    private final TPAManager tpaManager;
    private final QueueManager queueManager;
    private final DuelManager duelManager;

    public ConnectionListener(mCore plugin, TPAManager tpaManager, QueueManager queueManager, DuelManager duelManager) {
        this.plugin = plugin;
        this.tpaManager = tpaManager;
        this.queueManager = queueManager;
        this.duelManager = duelManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        String name = e.getPlayer().getName();
        String ip = e.getPlayer().getAddress().getHostString();
        this.plugin.getConfigManager().savePlayerIP(name, ip);
        FileConfiguration data = this.plugin.getConfigManager().getPlayerData();
        if (data.contains("speeds." + name)) {
            float walk = (float)data.getDouble("speeds." + name + ".walk", (double)0.2F);
            float fly = (float)data.getDouble("speeds." + name + ".fly", (double)0.1F);
            e.getPlayer().setWalkSpeed(walk);
            e.getPlayer().setFlySpeed(fly);
        }

    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (this.tpaManager != null) {
            this.tpaManager.cleanup(e.getPlayer());
        }

        if (this.queueManager != null) {
            this.queueManager.cleanup(e.getPlayer());
        }

        if (this.duelManager != null) {
            this.duelManager.cleanup(e.getPlayer());
        }

    }
}
