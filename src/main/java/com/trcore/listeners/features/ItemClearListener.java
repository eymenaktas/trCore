package com.trcore.listeners.features;

import com.trcore.TRCore;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Yere düşen eşyaları, her dünya için ayrı ayrı ayarlanabilir gecikmeden sonra siler.
 * -1 = o dünya için kapalı, pozitif sayı = silme gecikmesi (saniye cinsinden).
 *
 * enabled-worlds listesi doluysa SADECE o dünyalarda aktiftir.
 * Liste boşsa worldDelays map'inde tanımlı olan tüm dünyalar çalışır.
 *
 * FOLIA GÜVENL: Sadece ItemSpawnEvent ile tetiklenir,
 * EntityScheduler yalnızca Item entity'sini hedef alır;
 * oyuncu veya herhangi bir LivingEntity hiçbir zaman etkilenmez.
 */
public class ItemClearListener implements Listener {

    private final TRCore plugin;
    // Dünya adı -> gecikme (saniye); -1 = kapalı
    private final Map<String, Integer> worldDelays = new HashMap<>();
    // Boşsa = tüm tanımlı dünyalar; doluysa sadece bu dünyalar
    private final Set<String> enabledWorlds = new HashSet<>();

    public ItemClearListener(TRCore plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /** /TRCore reload çağrıldığında bu metot çağrılır. */
    public void loadConfig() {
        worldDelays.clear();
        enabledWorlds.clear();

        // Whitelist — sadece bu dünyalarda aktif (boşsa hepsinde)
        enabledWorlds.addAll(plugin.getConfig().getStringList("item-clear.enabled-worlds"));

        var section = plugin.getConfig().getConfigurationSection("item-clear.worlds");
        if (section == null) return;

        for (String worldName : section.getKeys(false)) {
            int delay = section.getInt(worldName, -1);
            worldDelays.put(worldName, delay);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        // Kesin tip kontrolü — sadece yere düşen eşya (Item) entity'si
        if (!(event.getEntity() instanceof Item item)) return;

        String worldName = item.getWorld().getName();

        // Whitelist kontrolü: liste doluysa sadece o dünyalarda çalış
        if (!enabledWorlds.isEmpty() && !enabledWorlds.contains(worldName)) return;

        Integer delaySec = worldDelays.get(worldName);

        // Dünya tanımlı değilse veya -1 ise hiçbir şey yapma
        if (delaySec == null || delaySec < 0) return;

        long delayTicks = delaySec * 20L;

        // Folia EntityScheduler: entity ölürse/despawn olursa task otomatik iptal edilir.
        // Oyuncu veya mob asla bu scheduler üzerinden etkilenmez.
        item.getScheduler().runDelayed(plugin, task -> {
            if (item.isValid() && !item.isDead()) {
                item.remove();
            }
        }, null, delayTicks);
    }
}
