package com.trcore.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.trcore.TRCore;
import com.trcore.utils.CC;

/**
 * Sebebe bagli otomatik ceza. Yetkili sebebi secer, kademeyi gecmis belirler:
 * ayni sebepten kacinci ceza ise cezalar.yml'deki o siradaki komut calisir.
 *
 * Sicak yolda (komut calistirma) hicbir dosya okunmaz: sebepler ve mesajlar
 * yuklemede onbellege alinir, gecmis RAM'de tutulur ve diske gecikmeli olarak
 * asenkron yazilir.
 */
public final class CezaManager {

    /** Bir sebebin onbellege alinmis hali. */
    public record Sebep(String anahtar, String ad, String izin, String efekt,
                        boolean duyur, List<String> kademeler) {}

    /**
     * LiteBans'in --sender bayragi bosluga kadar okunuyor; ad temizlenmezse
     * geri kalani sebebe karisir.
     */
    public static String adTemizle(String ad) {
        String t = ad.replaceAll("[^A-Za-z0-9_]", "");
        return t.isEmpty() ? "Konsol" : t.substring(0, Math.min(16, t.length()));
    }

    /** Bir oyuncunun bir sebepten gecmisi. */
    private static final class Kayit {
        int sayi;
        long son;
    }

    private final TRCore plugin;

    /** anahtar -> sebep. Yuklemede kurulur, sonrasinda yalniz okunur. */
    private volatile Map<String, Sebep> sebepler = Collections.emptyMap();
    /** Tab tamamlama icin hazir liste; her cagrida yeniden uretilmesin. */
    private volatile List<String> sebepAdlari = Collections.emptyList();
    private volatile Map<String, String> mesajlar = Collections.emptyMap();

    private volatile long gecmisPencereMs = 90L * 24 * 60 * 60 * 1000;
    private volatile long efektGecikmeTik = 60L;
    private volatile boolean duyuruAcik = true;
    private volatile boolean yetkiliGorunsun = true;

    private final Map<UUID, Map<String, Kayit>> gecmis = new ConcurrentHashMap<>();
    private final AtomicBoolean kirli = new AtomicBoolean(false);
    private File gecmisDosyasi;

    public CezaManager(TRCore plugin) {
        this.plugin = plugin;
    }

    // ---- Yukleme -----------------------------------------------------------

    public void load() {
        File dosya = new File(plugin.getDataFolder(), "cezalar.yml");
        if (!dosya.exists()) plugin.saveResource("cezalar.yml", false);

        FileConfiguration cfg;
        try (InputStreamReader r = new InputStreamReader(new FileInputStream(dosya), StandardCharsets.UTF_8)) {
            cfg = YamlConfiguration.loadConfiguration(r);
        } catch (Exception e) {
            cfg = YamlConfiguration.loadConfiguration(dosya);
        }

        gecmisPencereMs = Math.max(1L, cfg.getLong("ayarlar.gecmis-gun", 90L)) * 24 * 60 * 60 * 1000L;
        efektGecikmeTik = Math.max(1L, cfg.getLong("ayarlar.efekt-gecikme-tik", 60L));
        duyuruAcik = cfg.getBoolean("ayarlar.duyuru", true);
        yetkiliGorunsun = cfg.getBoolean("ayarlar.yetkili-adi-gorunsun", true);

        Map<String, String> yeniMesajlar = new LinkedHashMap<>();
        ConfigurationSection ms = cfg.getConfigurationSection("mesajlar");
        if (ms != null) for (String k : ms.getKeys(false)) yeniMesajlar.put(k, ms.getString(k, ""));
        mesajlar = Collections.unmodifiableMap(yeniMesajlar);

        Map<String, Sebep> yeni = new LinkedHashMap<>();
        ConfigurationSection ss = cfg.getConfigurationSection("sebepler");
        if (ss != null) {
            for (String anahtar : ss.getKeys(false)) {
                ConfigurationSection s = ss.getConfigurationSection(anahtar);
                if (s == null) continue;
                List<String> kademeler = s.getStringList("kademeler");
                if (kademeler.isEmpty()) {
                    plugin.getLogger().warning("cezalar.yml: '" + anahtar + "' kademesiz, atlandi.");
                    continue;
                }
                String kucuk = anahtar.toLowerCase(Locale.ROOT);
                yeni.put(kucuk, new Sebep(
                        kucuk,
                        s.getString("ad", anahtar),
                        s.getString("izin", "iocore.punish." + kucuk),
                        s.getString("efekt", ""),
                        s.getBoolean("duyur", true),
                        List.copyOf(kademeler)));
            }
        }
        sebepler = Collections.unmodifiableMap(yeni);
        sebepAdlari = List.copyOf(yeni.keySet());

        gecmisiYukle();
        plugin.getLogger().info("Ceza sistemi: " + sebepler.size() + " sebep yuklendi.");
    }

    private void gecmisiYukle() {
        gecmisDosyasi = new File(plugin.getDataFolder(), "cezalar-gecmis.yml");
        gecmis.clear();
        if (!gecmisDosyasi.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(gecmisDosyasi);
        ConfigurationSection kok = cfg.getConfigurationSection("gecmis");
        if (kok == null) return;
        for (String uuidStr : kok.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection sb = kok.getConfigurationSection(uuidStr);
            if (sb == null) continue;
            Map<String, Kayit> harita = new ConcurrentHashMap<>();
            for (String sebep : sb.getKeys(false)) {
                Kayit k = new Kayit();
                k.sayi = sb.getInt(sebep + ".sayi", 0);
                k.son = sb.getLong(sebep + ".son", 0L);
                if (k.sayi > 0) harita.put(sebep, k);
            }
            if (!harita.isEmpty()) gecmis.put(uuid, harita);
        }
    }

    /** Degisiklik varsa diske yazar. Kapanista senkron, normalde asenkron cagrilir. */
    public void gecmisiKaydet() {
        if (!kirli.compareAndSet(true, false)) return;
        YamlConfiguration cfg = new YamlConfiguration();
        gecmis.forEach((uuid, harita) -> harita.forEach((sebep, k) -> {
            cfg.set("gecmis." + uuid + "." + sebep + ".sayi", k.sayi);
            cfg.set("gecmis." + uuid + "." + sebep + ".son", k.son);
        }));
        try {
            cfg.save(gecmisDosyasi);
        } catch (Exception e) {
            kirli.set(true);
            plugin.getLogger().warning("Ceza gecmisi yazilamadi: " + e.getMessage());
        }
    }

    // ---- Sorgular ----------------------------------------------------------

    public Sebep sebep(String anahtar) {
        return anahtar == null ? null : sebepler.get(anahtar.toLowerCase(Locale.ROOT));
    }

    public List<String> sebepAdlari() {
        return sebepAdlari;
    }

    public String mesaj(String anahtar, String varsayilan) {
        return mesajlar.getOrDefault(anahtar, varsayilan);
    }

    /** Sifirdan baslar: 0 = ilk ceza. Pencere disinda kalan gecmis sayilmaz. */
    public int siradakiKademe(UUID uuid, String sebep) {
        Map<String, Kayit> harita = gecmis.get(uuid);
        if (harita == null) return 0;
        Kayit k = harita.get(sebep);
        if (k == null) return 0;
        if (System.currentTimeMillis() - k.son > gecmisPencereMs) return 0;
        return k.sayi;
    }

    private void kademeArtir(UUID uuid, String sebep) {
        Map<String, Kayit> harita = gecmis.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>());
        Kayit k = harita.computeIfAbsent(sebep, s -> new Kayit());
        long simdi = System.currentTimeMillis();
        if (simdi - k.son > gecmisPencereMs) k.sayi = 0;
        k.sayi++;
        k.son = simdi;
        kirli.set(true);
    }

    /** Yetkili gecmisi temizlemek isterse. */
    public boolean gecmisiSil(UUID uuid) {
        boolean vardi = gecmis.remove(uuid) != null;
        if (vardi) kirli.set(true);
        return vardi;
    }

    // ---- Uygulama ----------------------------------------------------------

    /**
     * Cezayi uygular. Kademe -1 verilirse gecmisten hesaplanir.
     * @return calistirilan komut, sebep bulunamazsa null
     */
    public String uygula(CommandSender yetkili, OfflinePlayer hedef, Sebep sebep, int zorlananKademe) {
        int kademe = zorlananKademe >= 0
                ? Math.min(zorlananKademe, sebep.kademeler().size() - 1)
                : Math.min(siradakiKademe(hedef.getUniqueId(), sebep.anahtar()), sebep.kademeler().size() - 1);

        String ad = hedef.getName() != null ? hedef.getName() : hedef.getUniqueId().toString();
        String aciklama = sebep.ad();
        String komut = sebep.kademeler().get(kademe)
                .replace("%player%", ad)
                .replace("%uuid%", hedef.getUniqueId().toString())
                .replace("%reason%", aciklama)
                .replace("%sebep%", aciklama)
                .replace("%staff%", yetkili.getName());
        // Komut konsoldan calisiyor; cezayi veren yetkili duyuruda gorunsun.
        final String calisacak = (yetkiliGorunsun && !komut.contains("--sender="))
                ? komut + " --sender=" + adTemizle(yetkili.getName())
                : komut;

        if (zorlananKademe < 0) kademeArtir(hedef.getUniqueId(), sebep.anahtar());

        Player online = hedef.isOnline() ? hedef.getPlayer() : null;
        boolean efektVar = online != null && sebep.efekt() != null && !sebep.efekt().isBlank()
                && plugin.getBanEfekti() != null;

        if (efektVar) {
            plugin.getBanEfekti().oynat(online, sebep.efekt());
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin,
                    t -> calistirVeDuyur(calisacak, ad, aciklama, yetkili, sebep, kademe), efektGecikmeTik);
        } else {
            calistirVeDuyur(calisacak, ad, aciklama, yetkili, sebep, kademe);
        }
        return calisacak;
    }

    private void calistirVeDuyur(String komut, String ad, String aciklama,
                                 CommandSender yetkili, Sebep sebep, int kademe) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), komut);
        plugin.getLogger().info("Ceza: " + yetkili.getName() + " -> " + ad
                + " [" + sebep.anahtar() + " #" + (kademe + 1) + "] " + komut);

        if (!duyuruAcik || !sebep.duyur()) return;
        String sablon = mesaj("duyuru",
                "<#8ccefe>%player% <gray>» <white>%sebep% <dark_gray>(<gray>%kademe%<dark_gray>)");
        String metin = sablon.replace("%player%", ad)
                .replace("%sebep%", aciklama)
                .replace("%staff%", yetkili.getName())
                .replace("%kademe%", String.valueOf(kademe + 1));
        Bukkit.broadcast(CC.parse(metin));
    }

    /**
     * Ban ve susturmayi birlikte kaldirir. Yetkiliye ayri ayri LiteBans
     * yetkisi vermeden calissin diye komutlar konsoldan gidiyor.
     */
    public void kaldir(CommandSender yetkili, String ad, boolean sessiz) {
        String ek = (sessiz ? " -s" : "")
                + (yetkiliGorunsun ? " --sender=" + adTemizle(yetkili.getName()) : "");
        for (String k : new String[] { "unban " + ad, "unmute " + ad }) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), k + ek);
        }
        plugin.getLogger().info("Ceza kaldirildi: " + yetkili.getName() + " -> " + ad);
    }

    /** Yuklemede cagrilir: gecikmeli, asenkron yazma dongusu. */
    public void kaydetmeDongusu() {
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, t -> gecmisiKaydet(),
                60L, 60L, java.util.concurrent.TimeUnit.SECONDS);
    }

    public List<String> tamamlama(String onEk) {
        String kucuk = onEk.toLowerCase(Locale.ROOT);
        List<String> cikti = new ArrayList<>();
        for (String s : sebepAdlari) if (s.startsWith(kucuk)) cikti.add(s);
        return cikti;
    }
}
