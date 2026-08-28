package com.trcore.listeners;

import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class SpectatorListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpectatorTeleport(PlayerTeleportEvent event) {
        // Iptal edilmesi istenen: spectator mode'da hotbar veya menü uzerinden oyunculara tplenme
        if (event.getCause() == TeleportCause.SPECTATE) {
            // Sadece spec menusunden tplenmeyi kapat demek isteniyorsa ve spectator da ise:
            if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
                event.setCancelled(true);
            }
        }
    }
}
