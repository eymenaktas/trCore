package com.trcore.managers;

import com.trcore.TRCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class CooldownManager {
    private final TRCore plugin;
    // Komut Stringi -> Grup Objesi (Hızlı Erişim)
    private final Map<String, CooldownGroup> commandMap = new HashMap<>();
    // Oyuncu UUID -> (Grup İsmi -> Bitiş Zamanı)
    private final Map<UUID, Map<String, Long>> activeCooldowns = new HashMap<>();

    public CooldownManager(TRCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File configFile = new File(plugin.getDataFolder(), "cooldowns.yml");
        if (!configFile.exists()) plugin.saveResource("cooldowns.yml", false);

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        commandMap.clear();

        if (config.contains("groups")) {
            for (String groupName : config.getConfigurationSection("groups").getKeys(false)) {
                int seconds = config.getInt("groups." + groupName + ".seconds");
                List<String> cmds = config.getStringList("groups." + groupName + ".commands");

                // Grubu bir kere oluştur, hafızada tut
                CooldownGroup group = new CooldownGroup(groupName, seconds);

                for (String cmd : cmds) {
                    // Config'de / koyduğun için ekstra kontrol yapmıyorum (Optimizasyon)
                    // Hepsini küçük harfe çevirip kaydediyoruz
                    commandMap.put(cmd.toLowerCase(), group);
                }
            }
        }
    }

    // O(1) Karmaşıklık - Anında Bulma
    public CooldownGroup getGroup(String command) {
        String lowerCmd = command.toLowerCase();

        // 1. Direkt eşleşme (/kit1)
        if (commandMap.containsKey(lowerCmd)) {
            return commandMap.get(lowerCmd);
        }

        // 2. Argümanlı eşleşme (/kit baslangic)
        // HashMap entrySet döngüsü sadece direkt eşleşme yoksa çalışır.
        for (Map.Entry<String, CooldownGroup> entry : commandMap.entrySet()) {
            // "/kit " ile başlıyor mu?
            if (lowerCmd.startsWith(entry.getKey() + " ")) {
                return entry.getValue();
            }
        }
        return null;
    }

    public long getRemainingTime(Player player, String groupName) {
        if (!activeCooldowns.containsKey(player.getUniqueId())) return 0;

        Map<String, Long> playerCooldowns = activeCooldowns.get(player.getUniqueId());
        if (!playerCooldowns.containsKey(groupName)) return 0;

        long expiry = playerCooldowns.get(groupName);
        long now = System.currentTimeMillis();

        // Süre dolduysa listeden sil (RAM Temizliği)
        if (now >= expiry) {
            playerCooldowns.remove(groupName);
            if (playerCooldowns.isEmpty()) {
                activeCooldowns.remove(player.getUniqueId());
            }
            return 0;
        }

        return (expiry - now) / 1000;
    }

    public void addCooldown(Player player, CooldownGroup group) {
        long expiry = System.currentTimeMillis() + (group.seconds * 1000L);
        activeCooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(group.name, expiry);
    }

    public void cleanup(Player player) {
        activeCooldowns.remove(player.getUniqueId());
    }

    // Veri tutucu
    public static class CooldownGroup {
        public final String name;
        public final int seconds;
        public CooldownGroup(String name, int seconds) {
            this.name = name;
            this.seconds = seconds;
        }
    }
}

