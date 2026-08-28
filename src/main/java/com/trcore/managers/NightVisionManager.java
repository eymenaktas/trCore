package com.trcore.managers;

import com.trcore.TRCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class NightVisionManager {

    private final TRCore plugin;
    // UUID buradaysa Night Vision AÇIK demektir (Varsayılan Kapalı)
    private final Set<UUID> nightVisionEnabled = new HashSet<>();

    // Night Vision potion efekti (partiküller ve ikon kapalı â†’ en temiz)
    private static final PotionEffect NV_EFFECT = new PotionEffect(
            PotionEffectType.NIGHT_VISION,
            Integer.MAX_VALUE, // Süre: Sonsuz
            0, // Amplifier: 0 (Seviye 1)
            false, // Ambient (particle density)
            false, // showParticles
            false // showIcon (HUD ikonu)
    );

    public NightVisionManager(TRCore plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Temel Toggle API
    // -----------------------------------------------------------------------

    public boolean isNightVisionEnabled(UUID uuid) {
        return nightVisionEnabled.contains(uuid);
    }

    /**
     * Toggle eder ve yeni durumu döner (true = açıldı).
     */
    public boolean toggleNightVision(Player player) {
        UUID uuid = player.getUniqueId();
        if (nightVisionEnabled.contains(uuid)) {
            nightVisionEnabled.remove(uuid);
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            return false;
        } else {
            nightVisionEnabled.add(uuid);
            player.addPotionEffect(NV_EFFECT);
            return true;
        }
    }

    /**
     * Efekti doğrudan uygular (set açık zaten içerdeyse bile).
     * Ölüm sonrası / join sonrası restore için kullanılır.
     */
    public void applyEffect(Player player) {
        player.addPotionEffect(NV_EFFECT);
    }

    // -----------------------------------------------------------------------
    // Kaydet / Yükle (Global)
    // -----------------------------------------------------------------------

    public void load() {
        nightVisionEnabled.clear();
        FileConfiguration data = plugin.getConfigManager().getPlayerData();
        List<String> list = data.getStringList("toggles.nightvision");
        for (String s : list) {
            try {
                nightVisionEnabled.add(UUID.fromString(s));
            } catch (Exception ignored) {
            }
        }
    }

    public void saveData() {
        FileConfiguration data = plugin.getConfigManager().getPlayerData();
        List<String> list = nightVisionEnabled.stream().map(UUID::toString).toList();
        data.set("toggles.nightvision", list);
        plugin.getConfigManager().savePlayerDataFile();
    }

    /**
     * Sadece o oyuncunun NV durumunu dosyadan yükler (join'de).
     * Dünya değişikliğinde ÇAÄIRILMAZ.
     */
    public void loadPlayer(UUID uuid) {
        FileConfiguration data = plugin.getConfigManager().getPlayerData();
        List<String> list = data.getStringList("toggles.nightvision");
        if (list.contains(uuid.toString())) {
            nightVisionEnabled.add(uuid);
        } else {
            nightVisionEnabled.remove(uuid);
        }
    }

    /**
     * Sadece o oyuncunun NV durumunu dosyaya kaydeder (quit'te).
     * Dünya değişikliğinde ÇAÄIRILMAZ.
     */
    public void savePlayer(UUID uuid) {
        FileConfiguration data = plugin.getConfigManager().getPlayerData();
        List<String> list = new java.util.ArrayList<>(data.getStringList("toggles.nightvision"));
        String id = uuid.toString();
        if (nightVisionEnabled.contains(uuid)) {
            if (!list.contains(id))
                list.add(id);
        } else {
            list.remove(id);
        }
        data.set("toggles.nightvision", list);
        plugin.getConfigManager().savePlayerDataFile();
    }

    /**
     * Oyuncu çıktıktan sonra RAM'deki NV verisini temizler.
     */
    public void unloadPlayer(UUID uuid) {
        nightVisionEnabled.remove(uuid);
    }

    /**
     * Online olan tüm oyunculara NV açıksa efekti (yeniden) uygular.
     * Reload sonrası çağır.
     */
    public void applyToOnlinePlayers() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (isNightVisionEnabled(p.getUniqueId())) {
                p.getScheduler().run(plugin, task -> applyEffect(p), null);
            }
        }
    }
}


