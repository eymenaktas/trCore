package com.mcore.listeners;

import com.mcore.mCore;
import com.mcore.managers.DuelManager;
import com.mcore.managers.QueueManager;
import com.mcore.managers.TPAManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class ConnectionListener implements Listener {
    private final mCore plugin;
    private final TPAManager tpaManager;
    private final QueueManager queueManager;
    private final DuelManager duelManager;

    // CONSTRUCTOR GÜNCELLENDİ: Artık 3 yöneticiyi de alıyor
    public ConnectionListener(mCore plugin, TPAManager tpaManager, QueueManager queueManager, DuelManager duelManager) {
        this.plugin = plugin;
        this.tpaManager = tpaManager;
        this.queueManager = queueManager;
        this.duelManager = duelManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // Hepsinden oyuncuyu temizle
        if (tpaManager != null) tpaManager.cleanup(e.getPlayer());
        if (queueManager != null) queueManager.cleanup(e.getPlayer());
        if (duelManager != null) duelManager.cleanup(e.getPlayer());
    }
}