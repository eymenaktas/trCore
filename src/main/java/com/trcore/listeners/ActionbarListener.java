package com.trcore.listeners;

import com.trcore.TRCore;
import com.trcore.utils.CC;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Actionbar mesajını tüm oyunculara belirli aralıklarla gönderir.
 *
 * Optimizasyon:
 *  - Config değerleri yapıcıda cache'lenir; her tick'te YAML okunmaz.
 *  - MiniMessage parse tek sefer yapılır; N oyuncuya aynı Component gönderilir.
 *  - disabled-worlds HashSet'e alınır → O(1) contains.
 */
public class ActionbarListener implements Listener {

    private volatile Component cachedMessage;
    private final Set<String> disabledWorlds;

    public ActionbarListener(TRCore plugin) {
        long intervalTicks = plugin.getConfig().getLong("actionbar.interval", 20L);

        // Config değerleri bir kere oku & cache'le
        String rawMsg = plugin.getConfig().getString("actionbar.message", "");
        this.cachedMessage = (rawMsg != null && !rawMsg.isEmpty()) ? CC.parse(rawMsg) : null;

        List<String> dwList = plugin.getConfig().getStringList("actionbar.disabled-worlds");
        this.disabledWorlds = new java.util.HashSet<>(dwList);

        // Actionbar'ı async scheduler ile gönder — per-player parse YOK
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            Component msg = cachedMessage;
            if (msg == null) return;

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!disabledWorlds.contains(p.getWorld().getName())) {
                    p.sendActionBar(msg);
                }
            }
        }, 0L, intervalTicks * 50L, TimeUnit.MILLISECONDS);
    }

    /** /TRCore reload çağrıldığında mesajı yeniden cache'ler. */
    public void reload(TRCore plugin) {
        String rawMsg = plugin.getConfig().getString("actionbar.message", "");
        this.cachedMessage = (rawMsg != null && !rawMsg.isEmpty()) ? CC.parse(rawMsg) : null;

        disabledWorlds.clear();
        disabledWorlds.addAll(plugin.getConfig().getStringList("actionbar.disabled-worlds"));
    }
}
