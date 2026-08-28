package com.trcore.listeners;

import com.trcore.TRCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Dünya değişikliğinde komut çalıştırma.
 *
 * Optimizasyon:
 *  - enabled flag ve command cache'lenir, her event'te config okunmaz.
 */
public class WorldChangeListener implements Listener {
    private final TRCore plugin;

    // Cached config
    private boolean enabled;
    private String command;

    public WorldChangeListener(TRCore plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /** Reload sırasında çağrılır. */
    public void loadConfig() {
        this.enabled = plugin.getConfig().getBoolean("world-change.enabled", false);
        this.command = plugin.getConfig().getString("world-change.commands.spawn", "");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) { execute(e.getPlayer()); }

    @EventHandler
    public void onChange(PlayerChangedWorldEvent e) { execute(e.getPlayer()); }

    private void execute(Player p) {
        if (!enabled) return;
        if (command != null && !command.isEmpty()) {
            p.performCommand(command);
        }
    }
}
