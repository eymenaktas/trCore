package com.trcore.managers;

import com.trcore.TRCore;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class ToggleManager {
    private final TRCore plugin;

    // -----------------------------------------------------------------------
    // Veri yapıları
    // UUID buradaysa KAPALI demektir (Varsayılan Açık)
    private final Set<UUID> tpaDisabled = new HashSet<>();
    private final Set<UUID> tpaHereDisabled = new HashSet<>();
    private final Set<UUID> duelDisabled = new HashSet<>();
    private final Set<UUID> queueMusicDisabled = new HashSet<>();


    // UUID buradaysa AÇIK demektir (Varsayılan Kapalı)
    private final Set<UUID> autoTpaEnabled = new HashSet<>();
    private final Set<UUID> rtpQueueNotify = new HashSet<>();
    private final Set<UUID> quickRtpEnabled = new HashSet<>();
    // UUID buradaysa drop gizleme AÇIK demektir (Varsayılan Kapalı)
    private final Set<UUID> deathDropHideEnabled = new HashSet<>();

    // Boolean map'ler: yoksa varsayılan döner (Açık)
    private final Map<UUID, Boolean> deathMessages = new HashMap<>();
    private final Map<UUID, Integer> deathMsgRadius = new HashMap<>();
    private final Map<UUID, Boolean> scoreboardEnabled = new HashMap<>();
    // -----------------------------------------------------------------------

    public ToggleManager(TRCore plugin) {
        this.plugin = plugin;
    }

    // -----------------------------------------------------------------------
    // Kaydet / Yükle (Global â€” plugin başlatma/kapatma için)
    // -----------------------------------------------------------------------
    public void load() {
        FileConfiguration data = plugin.getConfigManager().getPlayerData();

        loadSet(data, "toggles.tpa", tpaDisabled);
        loadSet(data, "toggles.tpahere", tpaHereDisabled);
        loadSet(data, "toggles.duel", duelDisabled);
        loadSet(data, "toggles.tpauto", autoTpaEnabled);
        loadSet(data, "toggles.rtpqueue", rtpQueueNotify);
        loadSet(data, "toggles.quickrtp", quickRtpEnabled);
        loadSet(data, "toggles.queuemusic", queueMusicDisabled);

        loadSet(data, "toggles.deathdrophide", deathDropHideEnabled);

        deathMessages.clear();
        deathMsgRadius.clear();
        scoreboardEnabled.clear();

        if (data.isConfigurationSection("toggles.deathmsgs")) {
            for (String key : data.getConfigurationSection("toggles.deathmsgs").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    deathMessages.put(uuid, data.getBoolean("toggles.deathmsgs." + key + ".enabled", true));
                    deathMsgRadius.put(uuid, data.getInt("toggles.deathmsgs." + key + ".radius", 150));
                } catch (Exception ignored) {
                }
            }
        }

        if (data.isConfigurationSection("toggles.scoreboard")) {
            for (String key : data.getConfigurationSection("toggles.scoreboard").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    scoreboardEnabled.put(uuid, data.getBoolean("toggles.scoreboard." + key, true));
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Sadece tek bir oyuncunun verisini dosyadan yükler (join'de kullanılır).
     * Dünya değişikliğinde ÇAÄIRILMAZ â€” sadece gerçek giriş/çıkış olaylarında.
     */
    public void loadPlayer(UUID uuid) {
        FileConfiguration data = plugin.getConfigManager().getPlayerData();
        String id = uuid.toString();

        if (data.getStringList("toggles.tpa").contains(id))
            tpaDisabled.add(uuid);
        else
            tpaDisabled.remove(uuid);

        if (data.getStringList("toggles.tpahere").contains(id))
            tpaHereDisabled.add(uuid);
        else
            tpaHereDisabled.remove(uuid);

        if (data.getStringList("toggles.duel").contains(id))
            duelDisabled.add(uuid);
        else
            duelDisabled.remove(uuid);

        if (data.getStringList("toggles.tpauto").contains(id))
            autoTpaEnabled.add(uuid);
        else
            autoTpaEnabled.remove(uuid);

        if (data.getStringList("toggles.rtpqueue").contains(id))
            rtpQueueNotify.add(uuid);
        else
            rtpQueueNotify.remove(uuid);

        if (data.getStringList("toggles.quickrtp").contains(id))
            quickRtpEnabled.add(uuid);
        else
            quickRtpEnabled.remove(uuid);

        if (data.getStringList("toggles.queuemusic").contains(id))
            queueMusicDisabled.add(uuid);
        else
            queueMusicDisabled.remove(uuid);



        if (data.getStringList("toggles.deathdrophide").contains(id))
            deathDropHideEnabled.add(uuid);
        else
            deathDropHideEnabled.remove(uuid);

        String dmPath = "toggles.deathmsgs." + id;
        if (data.isConfigurationSection(dmPath)) {
            deathMessages.put(uuid, data.getBoolean(dmPath + ".enabled", true));
            deathMsgRadius.put(uuid, data.getInt(dmPath + ".radius", 150));
        }
        String sbPath = "toggles.scoreboard." + id;
        if (data.contains(sbPath)) {
            scoreboardEnabled.put(uuid, data.getBoolean(sbPath, true));
        }
    }

    /**
     * Sadece tek bir oyuncunun verisini dosyaya kaydeder (quit'te kullanılır).
     * Dünya değişikliğinde ÇAÄIRILMAZ â€” gereksiz I/O'dan kaçınılır.
     */
    public void savePlayer(UUID uuid) {
        FileConfiguration data = plugin.getConfigManager().getPlayerData();
        String id = uuid.toString();

        updateUUIDInList(data, "toggles.tpa", id, tpaDisabled.contains(uuid));
        updateUUIDInList(data, "toggles.tpahere", id, tpaHereDisabled.contains(uuid));
        updateUUIDInList(data, "toggles.duel", id, duelDisabled.contains(uuid));
        updateUUIDInList(data, "toggles.tpauto", id, autoTpaEnabled.contains(uuid));
        updateUUIDInList(data, "toggles.rtpqueue", id, rtpQueueNotify.contains(uuid));
        updateUUIDInList(data, "toggles.quickrtp", id, quickRtpEnabled.contains(uuid));
        updateUUIDInList(data, "toggles.queuemusic", id, queueMusicDisabled.contains(uuid));

        updateUUIDInList(data, "toggles.deathdrophide", id, deathDropHideEnabled.contains(uuid));

        String dmPath = "toggles.deathmsgs." + id;
        if (deathMessages.containsKey(uuid)) {
            data.set(dmPath + ".enabled", deathMessages.get(uuid));
            data.set(dmPath + ".radius", deathMsgRadius.getOrDefault(uuid, 150));
        }
        if (scoreboardEnabled.containsKey(uuid)) {
            data.set("toggles.scoreboard." + id, scoreboardEnabled.get(uuid));
        }

        plugin.getConfigManager().savePlayerDataFile();
    }

    /**
     * Oyuncu çıktıktan sonra RAM'deki o oyuncuya ait veriyi siler (bellek
     * tasarrufu).
     */
    public void unloadPlayer(UUID uuid) {
        tpaDisabled.remove(uuid);
        tpaHereDisabled.remove(uuid);
        duelDisabled.remove(uuid);
        autoTpaEnabled.remove(uuid);
        rtpQueueNotify.remove(uuid);
        quickRtpEnabled.remove(uuid);
        queueMusicDisabled.remove(uuid);

        deathDropHideEnabled.remove(uuid);
        deathMessages.remove(uuid);
        deathMsgRadius.remove(uuid);
        scoreboardEnabled.remove(uuid);
    }

    /** UUID'yi listeye ekler ya da çıkarır ve config'i günceller. */
    private void updateUUIDInList(FileConfiguration data, String path, String id, boolean shouldBeIn) {
        List<String> list = new ArrayList<>(data.getStringList(path));
        if (shouldBeIn) {
            if (!list.contains(id))
                list.add(id);
        } else {
            list.remove(id);
        }
        data.set(path, list);
    }

    public void saveData() {
        FileConfiguration data = plugin.getConfigManager().getPlayerData();

        saveSet(data, "toggles.tpa", tpaDisabled);
        saveSet(data, "toggles.tpahere", tpaHereDisabled);
        saveSet(data, "toggles.duel", duelDisabled);
        saveSet(data, "toggles.tpauto", autoTpaEnabled);
        saveSet(data, "toggles.rtpqueue", rtpQueueNotify);
        saveSet(data, "toggles.quickrtp", quickRtpEnabled);
        saveSet(data, "toggles.queuemusic", queueMusicDisabled);

        saveSet(data, "toggles.deathdrophide", deathDropHideEnabled);

        data.set("toggles.deathmsgs", null);
        for (Map.Entry<UUID, Boolean> e : deathMessages.entrySet()) {
            String path = "toggles.deathmsgs." + e.getKey();
            data.set(path + ".enabled", e.getValue());
            data.set(path + ".radius", deathMsgRadius.getOrDefault(e.getKey(), 150));
        }

        data.set("toggles.scoreboard", null);
        for (Map.Entry<UUID, Boolean> e : scoreboardEnabled.entrySet()) {
            data.set("toggles.scoreboard." + e.getKey(), e.getValue());
        }

        plugin.getConfigManager().savePlayerDataFile();
    }

    // -----------------------------------------------------------------------
    // Yardımcı: Set kaydet/yükle (UUID listesi)
    // -----------------------------------------------------------------------
    private void loadSet(FileConfiguration data, String path, Set<UUID> set) {
        set.clear();
        for (String s : data.getStringList(path)) {
            try {
                set.add(UUID.fromString(s));
            } catch (Exception ignored) {
            }
        }
    }

    private void saveSet(FileConfiguration data, String path, Set<UUID> set) {
        List<String> list = new ArrayList<>(set.size());
        for (UUID uuid : set)
            list.add(uuid.toString());
        data.set(path, list);
    }

    // -----------------------------------------------------------------------
    // TPA
    // -----------------------------------------------------------------------
    public boolean isTPAEnabled(UUID uuid) {
        return !tpaDisabled.contains(uuid);
    }

    public boolean toggleTPA(UUID uuid) {
        if (tpaDisabled.remove(uuid))
            return true;
        tpaDisabled.add(uuid);
        return false;
    }

    // -----------------------------------------------------------------------
    // TPAHERE
    // -----------------------------------------------------------------------
    public boolean isTPAHereEnabled(UUID uuid) {
        return !tpaHereDisabled.contains(uuid);
    }

    public boolean toggleTPAHere(UUID uuid) {
        if (tpaHereDisabled.remove(uuid))
            return true;
        tpaHereDisabled.add(uuid);
        return false;
    }

    // -----------------------------------------------------------------------
    // DUEL
    // -----------------------------------------------------------------------
    public boolean isDuelEnabled(UUID uuid) {
        return !duelDisabled.contains(uuid);
    }

    public boolean toggleDuel(UUID uuid) {
        if (duelDisabled.remove(uuid))
            return true;
        duelDisabled.add(uuid);
        return false;
    }

    // -----------------------------------------------------------------------
    // AUTO TPA
    // -----------------------------------------------------------------------
    public boolean isAutoTPA(UUID uuid) {
        return autoTpaEnabled.contains(uuid);
    }

    public boolean toggleAutoTPA(UUID uuid) {
        if (autoTpaEnabled.remove(uuid))
            return false;
        autoTpaEnabled.add(uuid);
        return true;
    }

    // -----------------------------------------------------------------------
    // RTP QUEUE NOTIFY
    // -----------------------------------------------------------------------
    public boolean isRTPQueueNotifyEnabled(UUID uuid) {
        return rtpQueueNotify.contains(uuid);
    }

    public void toggleRTPQueueNotify(UUID uuid) {
        if (!rtpQueueNotify.remove(uuid))
            rtpQueueNotify.add(uuid);
    }

    // -----------------------------------------------------------------------
    // QUICK RTP
    // -----------------------------------------------------------------------
    public boolean isQuickRtpEnabled(UUID uuid) {
        return quickRtpEnabled.contains(uuid);
    }

    public boolean toggleQuickRtp(UUID uuid) {
        if (quickRtpEnabled.remove(uuid))
            return false;
        quickRtpEnabled.add(uuid);
        return true;
    }

    // -----------------------------------------------------------------------
    // QUEUE MUSIC
    // -----------------------------------------------------------------------
    public boolean isQueueMusicEnabled(UUID uuid) {
        return !queueMusicDisabled.contains(uuid);
    }

    public boolean toggleQueueMusic(UUID uuid) {
        if (queueMusicDisabled.remove(uuid))
            return true;
        queueMusicDisabled.add(uuid);
        return false;
    }

    // -----------------------------------------------------------------------
    // DEATH MESSAGES
    // -----------------------------------------------------------------------
    public boolean isDeathMessageEnabled(UUID uuid) {
        return deathMessages.getOrDefault(uuid, true);
    }

    public void toggleDeathMessages(UUID uuid) {
        deathMessages.put(uuid, !isDeathMessageEnabled(uuid));
    }

    public int getDeathMessageRadius(UUID uuid) {
        return deathMsgRadius.getOrDefault(uuid, 150);
    }

    public void cycleDeathMessageRadius(UUID uuid) {
        int cur = getDeathMessageRadius(uuid);
        int next = (cur == 100) ? 150 : (cur == 150) ? 200 : (cur == 200) ? 250 : 100;
        deathMsgRadius.put(uuid, next);
    }

    // -----------------------------------------------------------------------
    // SCOREBOARD
    // -----------------------------------------------------------------------
    public boolean isScoreboardEnabled(UUID uuid) {
        return scoreboardEnabled.getOrDefault(uuid, true);
    }

    public void toggleScoreboard(UUID uuid) {
        scoreboardEnabled.put(uuid, !isScoreboardEnabled(uuid));
    }

    // -----------------------------------------------------------------------
    // DEATH DROP HIDE
    // -----------------------------------------------------------------------
    public boolean isDeathDropHideEnabled(UUID uuid) {
        return deathDropHideEnabled.contains(uuid);
    }

    public boolean toggleDeathDropHide(UUID uuid) {
        if (deathDropHideEnabled.remove(uuid))
            return false;
        deathDropHideEnabled.add(uuid);
        return true;
    }

    /**
     * DeathDropManager'ın erken çıkış optimizasyonu için: kimse açmamışsa işlem
     * yapma
     */
    public boolean hasAnyDeathDropHider() {
        return !deathDropHideEnabled.isEmpty();
    }
}


