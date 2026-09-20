package com.trcore.listeners.features;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.trcore.TRCore;

/**
 * Ban efekti: Ecstacy cezayi uygulamadan hemen once oyuncunun uzerinde bir olum
 * efekti oynatilir, ardindan ceza uygulanir.
 *
 * Neden FlagEvent: Ecstacy'de iptal edilebilen olay bu. PunishEvent ceza
 * calistiktan SONRA tetikleniyor. FlagEvent iptal edilince yalnizca ceza durur;
 * tespit, ihlal puani ve panel kaydi yerinde kalir.
 *
 * Her iki eklentiye de YANSIMA ile baglaniliyor. Paper eklenti sinif
 * yukleyicileri yalitilmis: Ecstacy'nin API sinifi derleme zamaninda eklense
 * bile calisma aninda gorunmuyor ve NoClassDefFoundError ioCore'un tamamini
 * dusuruyor. Sinif yukleyiciyi eklentinin kendisinden aliyoruz; eklentilerden
 * biri yoksa ozellik sessizce kapanir, sunucu etkilenmez.
 */
public final class BanEfekti {

    private static final String[] EXYLIA_API = {
            "net.exylia.lib.api.ExyliaAPI",
            "net.exylia.api.ExyliaAPI",
    };
    private static final String[] EXYLIA_SERVIS = {
            "net.exylia.lib.api.killeffect.KillEffectService",
            "net.exylia.api.killeffect.KillEffectService",
    };

    private final TRCore plugin;
    /** Ayni oyuncu icin ikinci kez tetiklenmesin. */
    private final Set<UUID> islemde = ConcurrentHashMap.newKeySet();

    private Object killEffectServisi;
    private Method playMetodu;

    // Ecstacy tarafi: yansima ile tutulan metotlar
    private Method mPunishable, mUser, mCheckName, mVl, mMaxVl, mCancelled;
    private Method mBan, mExempt, mPlayer;
    private Class<?> bukkitUserSinifi;

    public BanEfekti(TRCore plugin) {
        this.plugin = plugin;
    }

    public void kur() {
        if (!plugin.getConfig().getBoolean("ban-efekti.enabled", false)) return;
        try {
            exyliayaBaglan();
            ecstacyeBaglan();
        } catch (Throwable t) {
            // Hicbir kosulda eklentinin acilisini dusurme
            plugin.getLogger().warning("Ban efekti kurulamadi: " + t);
        }
    }

    private String efektAdi() {
        return plugin.getConfig().getString("ban-efekti.efekt", "banned");
    }

    // ---- Ecstacy -----------------------------------------------------------

    private void ecstacyeBaglan() throws Exception {
        Plugin ecstacy = Bukkit.getPluginManager().getPlugin("EcstacyAC-Spigot");
        if (ecstacy == null || !ecstacy.isEnabled()) {
            plugin.getLogger().info("Ban efekti: Ecstacy yok, ozellik kapali.");
            return;
        }
        ClassLoader cl = ecstacy.getClass().getClassLoader();

        Class<?> accessor = Class.forName("ac.ecstacy.api.EcstacyApiAccessor", true, cl);
        Object kutu = accessor.getMethod("access").invoke(null);
        Object api = (kutu instanceof Optional<?> opt) ? opt.orElse(null) : kutu;
        if (api == null) {
            plugin.getLogger().info("Ban efekti: Ecstacy API erisilemiyor (plan kapsamiyor olabilir).");
            return;
        }

        Class<?> apiSinifi = Class.forName("ac.ecstacy.api.EcstacyApi", true, cl);
        Class<?> eventsSinifi = Class.forName("ac.ecstacy.api.Events", true, cl);
        Class<?> flagSinifi = Class.forName("ac.ecstacy.api.event.FlagEvent", true, cl);
        Class<?> userSinifi = Class.forName("ac.ecstacy.api.EcstacyUser", true, cl);
        bukkitUserSinifi = Class.forName("ac.ecstacy.api.bukkit.BukkitUser", true, cl);

        mPunishable = flagSinifi.getMethod("punishable");
        mCheckName = flagSinifi.getMethod("checkName");
        mVl = flagSinifi.getMethod("vl");
        mMaxVl = flagSinifi.getMethod("maxVl");
        mUser = flagSinifi.getMethod("user");
        mCancelled = flagSinifi.getMethod("cancelled", boolean.class);
        mBan = userSinifi.getMethod("ban", String.class);
        mExempt = userSinifi.getMethod("exempt", boolean.class);
        mPlayer = bukkitUserSinifi.getMethod("player");

        Object events = apiSinifi.getMethod("events").invoke(api);
        Consumer<Object> dinleyici = this::bayrak;
        // Oncelik 100: karari biz veriyoruz, sonraki dinleyiciler iptali gorsun.
        eventsSinifi.getMethod("listen", Class.class, Consumer.class, int.class)
                .invoke(events, flagSinifi, dinleyici, 100);

        plugin.getLogger().info("Ban efekti acik: efekt=" + efektAdi()
                + (playMetodu == null ? " (kill effect API'si yok, efekt atlanacak)" : ""));
    }

    private void bayrak(Object olay) {
        try {
            if (!Boolean.TRUE.equals(mPunishable.invoke(olay))) return;

            Object kullanici = mUser.invoke(olay);
            if (!bukkitUserSinifi.isInstance(kullanici)) return;
            Player oyuncu = (Player) mPlayer.invoke(kullanici);
            if (oyuncu == null || !islemde.add(oyuncu.getUniqueId())) return;

            // Ecstacy'nin kendi cezasini durdur; tespit ve ihlal puani yerinde kalir.
            mCancelled.invoke(olay, true);

            String kontrol = String.valueOf(mCheckName.invoke(olay));
            String sebep = plugin.getConfig().getString("ban-efekti.sebep", "Hile tespit edildi (%check%)")
                    .replace("%check%", kontrol)
                    .replace("%vl%", String.valueOf(mVl.invoke(olay)))
                    .replace("%maxvl%", String.valueOf(mMaxVl.invoke(olay)));
            long gecikme = Math.max(1L, plugin.getConfig().getLong("ban-efekti.gecikme-tik", 45L));

            // Ecstacy dinleyicisi ana thread'de olmayabilir; efekt konumun thread'ini ister.
            Bukkit.getGlobalRegionScheduler().run(plugin, g1 -> {
                cagir(mExempt, kullanici, true); // gecikme boyunca tekrar bayraklanmasin
                efektiOynat(oyuncu);

                Bukkit.getGlobalRegionScheduler().runDelayed(plugin, g2 -> {
                    cezayiUygula(kullanici, oyuncu, sebep, kontrol);
                    Bukkit.getGlobalRegionScheduler().runDelayed(plugin, g3 -> {
                        islemde.remove(oyuncu.getUniqueId());
                        if (oyuncu.isOnline()) {
                            cagir(mExempt, kullanici, false);
                            plugin.getLogger().warning("Ban efekti: ceza oyuncuyu dusurmedi -> " + oyuncu.getName());
                        }
                    }, 20L);
                }, gecikme);
            });
        } catch (Throwable t) {
            plugin.getLogger().warning("Ban efekti islenemedi: " + t);
        }
    }

    /**
     * Komuttan cagrilan yol: efekti oynatir, gecikme sonunda cezayi uygular.
     * Ecstacy'nin punish.commands listesi bunu cagiriyor.
     */
    public void calistir(Player oyuncu, String kontrol) {
        if (!plugin.getConfig().getBoolean("ban-efekti.enabled", false)) return;
        if (!islemde.add(oyuncu.getUniqueId())) return;

        String sebep = plugin.getConfig().getString("ban-efekti.sebep", "Hile tespit edildi (%check%)")
                .replace("%check%", kontrol);
        long gecikme = Math.max(1L, plugin.getConfig().getLong("ban-efekti.gecikme-tik", 45L));
        boolean deneme = plugin.getConfig().getBoolean("ban-efekti.deneme", false);

        Bukkit.getGlobalRegionScheduler().run(plugin, g1 -> {
            efektiOynat(oyuncu);
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, g2 -> {
                islemde.remove(oyuncu.getUniqueId());
                if (deneme) {
                    plugin.getLogger().info("Ban efekti (deneme): " + oyuncu.getName() + " banlanmadi.");
                    return;
                }
                komutlaBanla(oyuncu, sebep, kontrol);
            }, gecikme);
        });
    }

    /**
     * Oyuncu cevrimdisiyken ceza komutunu calistirir. Ecstacy kendi banini
     * atip oyuncuyu dusurdugu icin efekt oynatilamiyor; komut yine de
     * calisiyor ve cezayi IP banina cevirir.
     */
    public void cevrimdisiUygula(String ad, String kontrol) {
        if (!plugin.getConfig().getBoolean("ban-efekti.enabled", false)) return;
        if (plugin.getConfig().getBoolean("ban-efekti.deneme", false)) {
            plugin.getLogger().info("Ban efekti (deneme): " + ad + " banlanmadi.");
            return;
        }
        String sebep = plugin.getConfig().getString("ban-efekti.sebep", "Hile kullanımı")
                .replace("%check%", kontrol);
        String komut = plugin.getConfig().getString("ban-efekti.komut", "ipban %player% 30d %reason%");
        String hazir = senderEkle(komut.replace("%player%", ad)
                .replace("%check%", kontrol)
                .replace("%reason%", sebep));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), hazir);
        plugin.getLogger().info("Ban efekti (cevrimdisi): " + ad + " -> " + hazir);
    }

    private void komutlaBanla(Player oyuncu, String sebep, String kontrol) {
        String komut = plugin.getConfig().getString("ban-efekti.komut", "tempban %player% 30d %reason%");
        String hazir = komut.replace("%player%", oyuncu.getName())
                .replace("%uuid%", oyuncu.getUniqueId().toString())
                .replace("%check%", kontrol)
                .replace("%reason%", sebep);
        hazir = senderEkle(hazir);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), hazir);
        plugin.getLogger().info("Ban efekti tamam: " + oyuncu.getName() + " -> " + hazir);
    }

    /**
     * Banlayan olarak antihilenin adi gorunsun. LiteBans --sender bayragini
     * boslukta kesiyor; ad bu yuzden temizleniyor.
     */
    private String senderEkle(String komut) {
        String ad = plugin.getConfig().getString("ban-efekti.yetkili-adi", "EcstacyAC");
        if (ad == null || ad.isBlank() || komut.contains("--sender=")) return komut;
        return komut + " --sender=" + com.trcore.managers.CezaManager.adTemizle(ad);
    }

    private void cezayiUygula(Object kullanici, Player oyuncu, String sebep, String kontrol) {
        String komut = plugin.getConfig().getString("ban-efekti.komut", "");
        if (komut == null || komut.isBlank()) {
            cagir(mBan, kullanici, sebep); // Ecstacy'nin kendi ban akisi: panelde de gorunur
        } else {
            String hazir = senderEkle(komut.replace("%player%", oyuncu.getName())
                    .replace("%uuid%", oyuncu.getUniqueId().toString())
                    .replace("%check%", kontrol)
                    .replace("%reason%", sebep));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), hazir);
        }
        plugin.getLogger().info("Ban efekti tamam: " + oyuncu.getName() + " -> " + sebep);
    }

    private void cagir(Method metot, Object hedef, Object arg) {
        try {
            metot.invoke(hedef, arg);
        } catch (Exception e) {
            plugin.getLogger().warning("Ban efekti cagrisi basarisiz (" + metot.getName() + "): " + e.getMessage());
        }
    }

    // ---- ExyliaKillEffect --------------------------------------------------

    /**
     * Efekt "killer" tarafina donuk ciziliyor (PIXELS face:true). Hileciyi killer
     * verirsek BANNED yazisi ona bakar; sahneyi izleyenler tersini gorur. Bu yuzden
     * en yakindaki baska oyuncu killer olarak veriliyor, kimse yoksa hilecinin kendisi.
     */
    private Player izleyici(Player hedef) {
        Player enYakin = null;
        double enKisa = Double.MAX_VALUE;
        for (Player p : hedef.getWorld().getPlayers()) {
            if (p.equals(hedef)) continue;
            double uzaklik = p.getLocation().distanceSquared(hedef.getLocation());
            if (uzaklik < enKisa && uzaklik <= 32 * 32) {
                enKisa = uzaklik;
                enYakin = p;
            }
        }
        return enYakin != null ? enYakin : hedef;
    }

    private void efektiOynat(Player oyuncu) {
        oynat(oyuncu, efektAdi());
    }

    /** Ceza sistemi de ayni efekt yolunu kullaniyor; efekt adini disaridan alir. */
    public void oynat(Player oyuncu, String efekt) {
        if (playMetodu == null || killEffectServisi == null) return;
        try {
            Location yer = oyuncu.getLocation();
            Object sonuc = playMetodu.invoke(killEffectServisi, efekt, yer, izleyici(oyuncu), (LivingEntity) oyuncu);
            if (Boolean.FALSE.equals(sonuc)) {
                plugin.getLogger().warning("Ban efekti oynatilamadi: " + efekt
                        + " (efekt yok, bolge susturmus ya da bir dinleyici iptal etmis olabilir)");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ban efekti hatasi: " + e.getMessage());
        }
    }

    private void exyliayaBaglan() {
        for (String apiAdi : EXYLIA_API) {
            for (String servisAdi : EXYLIA_SERVIS) {
                try {
                    Class<?> apiSinifi = Class.forName(apiAdi);
                    Class<?> servisSinifi = Class.forName(servisAdi);
                    Object kutu = apiSinifi.getMethod("get", Class.class).invoke(null, servisSinifi);
                    Object servis = (kutu instanceof Optional<?> opt) ? opt.orElse(null) : kutu;
                    if (servis == null) continue;
                    killEffectServisi = servis;
                    playMetodu = servisSinifi.getMethod("play", String.class, Location.class, Player.class, LivingEntity.class);
                    plugin.getLogger().info("Ban efekti: kill effect API'si baglandi (" + apiAdi + ")");
                    return;
                } catch (ClassNotFoundException | NoSuchMethodException yok) {
                    // sonraki adaya gec
                } catch (Exception e) {
                    plugin.getLogger().warning("Ban efekti: API baglanamadi (" + apiAdi + "): " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info("Ban efekti: ExyliaKillEffect API'si bulunamadi, ceza efektsiz uygulanacak.");
    }
}
