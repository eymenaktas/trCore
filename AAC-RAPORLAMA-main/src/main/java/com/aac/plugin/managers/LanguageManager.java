package com.aac.plugin.managers;

import com.aac.plugin.AAC;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    
    private final AAC plugin;
    private final Map<String, FileConfiguration> languages;
    private String currentLanguage;
    
    public LanguageManager(AAC plugin) {
        this.plugin = plugin;
        this.languages = new HashMap<>();
        loadLanguages();
    }
    
    private void loadLanguages() {
        File languagesDir = new File(plugin.getDataFolder(), "languages");
        if (!languagesDir.exists()) {
            languagesDir.mkdirs();
        }
        
        String[] languageFiles = {
            "en.yml", "tr.yml"
        };
        
        for (String fileName : languageFiles) {
            File langFile = new File(languagesDir, fileName);
            if (!langFile.exists()) {
                saveDefaultLanguageFile(fileName);
            }
            
            FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
            
            String langCode = fileName.replace(".yml", "");
            FileConfiguration defaults = new YamlConfiguration();
            
            switch (langCode) {
                case "tr":
                    setupTurkishMessages(defaults);
                    break;
                case "en":
                    setupEnglishMessages(defaults);
                    break;
                default:
                    setupEnglishMessages(defaults);
                    break;
            }
            
            boolean changed = false;
            for (String key : defaults.getKeys(true)) {
                if (!config.isSet(key) && defaults.isSet(key)) {
                    config.set(key, defaults.get(key));
                    changed = true;
                }
            }
            
            if (changed) {
                try {
                    config.save(langFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("raporlama sistemi hatası Could not save updated language file " + fileName + " " + e.getMessage());
                }
            }
            
            languages.put(langCode, config);
        }
        
        currentLanguage = plugin.getConfigManager().getLanguage();
    }
    
    private void saveDefaultLanguageFile(String fileName) {
        File langFile = new File(plugin.getDataFolder(), "languages/" + fileName);
        
        try {
            InputStream inputStream = plugin.getResource("languages/" + fileName);
            if (inputStream != null) {
                Files.copy(inputStream, langFile.toPath());
            } else {
                createDefaultLanguageFile(langFile, fileName);
            }
        } catch (IOException e) {
            createDefaultLanguageFile(langFile, fileName);
        }
    }
    
    private void createDefaultLanguageFile(File file, String fileName) {
        FileConfiguration config = new YamlConfiguration();
        
        String langCode = fileName.replace(".yml", "");
        
        switch (langCode) {
            case "tr":
                setupTurkishMessages(config);
                break;
            case "en":
                setupEnglishMessages(config);
                break;
            default:
                setupEnglishMessages(config);
                break;
        }
        
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("raporlama sistemi dil hatası Could not save language file" + fileName);
        }
    }
    
    private void setupTurkishMessages(FileConfiguration config) {
        config.set("prefix", "&6[AAC] ");
        config.set("no-permission", "&cBu komutu kullanmak için yetkiniz yok");
        config.set("commands.no-permission", "&cBu komutu kullanmak için yetkiniz yok");
        config.set("player-not-found", "&cOyuncu bulunamadı");
        config.set("report-sent", "&aRapor başarıyla gönderildi");
        config.set("report-cooldown", "&cRapor göndermek için &6{time} &csaniye beklemelisiniz");
        config.set("cannot-report-self", "&cKendinizi raporlayamazsınız");
        config.set("report-alert", "&6{reporter} &fOyuncu &6{reported} &fAdlı Oyuncuyu &4{reason} &fSebebiyle Raporladı İncelemek için &6/raporlar");
        
        config.set("commands.aacsystem.usage", "&a/aacsystem reload help alerts bunlardan birini kullanın");
        config.set("commands.aacsystem.unknown-subcommand", "&4Bilinmeyen komut");
        config.set("commands.aacsystem.reload.success", "&aAAC dosyaları yeniden yüklendi.");
        config.set("commands.aacsystem.reload.error", "&cyeniden yüklenirken bir hata oluştu.");
        config.set("commands.aacsystem.help.header", "&6AACSystem Yardım");
        config.set("commands.aacsystem.help.reload", "&e/aacsystem reload &7- Config ve dil dosyalarını yeniden yükler");
        config.set("commands.aacsystem.help.help", "&e/aacsystem help &7- Bu bilgi ekranını göstermeye yardımcı olur");
        config.set("commands.aacsystem.help.alerts", "&e/aacsystem alerts &7- Rapor uyarı mesajlarını aç/kapat");
        config.set("commands.aacsystem.player-only", "&cBu komut sadece oyuncular tarafından kullanılabilir");
        config.set("commands.aacsystem.alerts.enabled", "&aRapor uyarıları senin için etkinleştirildi");
        config.set("commands.aacsystem.alerts.disabled", "&4Rapor uyarıları senin için devre dışı bırakıldı");
        
        config.set("gui.report-player.title", "&cOyuncu Raporla");
        config.set("gui.report-player.next-page", "&aSonraki Sayfa");
        config.set("gui.report-player.previous-page", "&aÖnceki Sayfa");
        config.set("gui.report-player.player-head-lore", "&7Raporlamak için tıklayın");
        
        config.set("gui.report-reasons.title", "&cRapor Sebebi Seçin");
        config.set("gui.report-reasons.hacking", "&4Hile Kullanımı");
        config.set("gui.report-reasons.hacking-lore", "&7Oyuncu hile kullanıyor");
        config.set("gui.report-reasons.swearing", "&4Küfür Kullanımı");
        config.set("gui.report-reasons.swearing-lore", "&7Oyuncu küfür ediyor");
        config.set("gui.report-reasons.inappropriate-name", "&4Uygunsuz İsim kullanıyor");
        config.set("gui.report-reasons.inappropriate-name-lore", "&7Oyuncunun ismi uygunsuz");
        config.set("gui.report-reasons.spam", "&4Spam");
        config.set("gui.report-reasons.spam-lore", "&7Oyuncu spam yapıyor");
        config.set("gui.report-reasons.griefing", "&cElitra Hilesi");
        config.set("gui.report-reasons.griefing-lore", "&7Oyuncu Elitra Hilesi kullanmakta");
        config.set("gui.report-reasons.other", "&4Özel Sebep");
        config.set("gui.report-reasons.other-lore", "&7Özel sebep girmek için tıklayın");
        
        config.set("gui.reports.title", "&6Raporlar");
        config.set("gui.reports.report-lore", 
            "&fRapor Eden &6{reporter} &fSebep &6{reason} &fTarih &6{date} &fSaat &6{time} &fSon Mesajları &6{messages} &4Detaylı incelemek için tıklayın &fSilmek için Shift + Sağ Tık");
        
        config.set("gui.aacwatch.title", "&6AAC Watch");
        config.set("gui.aacwatch.player-lore", "&7İzlemek için tıklayın");
        
        config.set("gui.report-detail.title", "&6Rapor Detayları");
        config.set("gui.report-detail.back", "&cGeri Dön");
        config.set("gui.report-detail.back-lore", "&7Raporlar menüsüne geri dönmek için tıklayın");
        
        config.set("gui.glass-name", " ");
    }
    
    private void setupEnglishMessages(FileConfiguration config) {
        config.set("prefix", "&6[AAC] ");
        config.set("no-permission", "&cYou don't have permission to use this command");
        config.set("commands.no-permission", "&cYou don't have permission to use this command");
        config.set("player-not-found", "&cPlayer not found");
        config.set("report-sent", "&aReport sent successfully");
        config.set("report-cooldown", "&cYou must wait &6{time} &cseconds before reporting again");
        config.set("cannot-report-self", "&cYou cannot report yourself");
        config.set("report-alert", "&6{reporter} &freported &6{reported} &ffor &c{reason} &fUse &6/raporlar &fto inspect reports.");
        
        config.set("commands.aacsystem.usage", "&4/aacsystem reload help alerts");
        config.set("commands.aacsystem.unknown-subcommand", "&cUnknown subcommand.");
        config.set("commands.aacsystem.reload.success", "&aAAC and language files reloaded.");
        config.set("commands.aacsystem.reload.error", "&cAn error occurred while reloading configuration.");
        config.set("commands.aacsystem.help.header", "&6AACSystem Help");
        config.set("commands.aacsystem.help.reload", "&e/aacsystem reload &7- Reload config and language files");
        config.set("commands.aacsystem.help.help", "&e/aacsystem help &7- Show this help menu");
        config.set("commands.aacsystem.help.alerts", "&e/aacsystem alerts &7- Toggle report alert messages");
        config.set("commands.aacsystem.player-only", "&cThis subcommand can only be used by players");
        config.set("commands.aacsystem.alerts.enabled", "&aReport alerts have been enabled for you.");
        config.set("commands.aacsystem.alerts.disabled", "&cReport alerts have been disabled for you.");
        
        config.set("gui.report-player.title", "&cReport Player");
        config.set("gui.report-player.next-page", "&aNEXT PAGE");
        config.set("gui.report-player.previous-page", "&cPREVIOUS PAGE");
        config.set("gui.report-player.player-head-lore", "&7Click to report");
        
        config.set("gui.report-reasons.title", "&cSelect Report Reason");
        config.set("gui.report-reasons.hacking", "&cHacking");
        config.set("gui.report-reasons.hacking-lore", "&7Player is using hacks");
        config.set("gui.report-reasons.swearing", "&cSwearing");
        config.set("gui.report-reasons.swearing-lore", "&7Player is swearing");
        config.set("gui.report-reasons.inappropriate-name", "&cInappropriate Name");
        config.set("gui.report-reasons.inappropriate-name-lore", "&7Player has inappropriate name");
        config.set("gui.report-reasons.spam", "&cSpam");
        config.set("gui.report-reasons.spam-lore", "&7Player is spamming");
        config.set("gui.report-reasons.griefing", "&cElitra Hacking Usee");
        config.set("gui.report-reasons.griefing-lore", "&7Player is Eliytra Hackings");
        config.set("gui.report-reasons.other", "&eCustom Reason");
        config.set("gui.report-reasons.other-lore", "&7Click to enter custom reason");
        
        config.set("gui.reports.title", "&6Reports");
        config.set("gui.reports.report-lore", 
            "&7Reporter: &6{reporter}|&7Reason: &6{reason}|&7Date: &6{date}|&7Time: &6{time}|&7Recent Messages:|{messages}||&eClick to inspect|&cShift + Right Click to delete");
        
        config.set("gui.aacwatch.title", "&6AAC Watch");
        config.set("gui.aacwatch.player-lore", "&7Click to watch");
        
        config.set("gui.report-detail.title", "&6Report Details");
        config.set("gui.report-detail.back", "&cBack");
        config.set("gui.report-detail.back-lore", "&7Click to return to reports menu");
        
        config.set("gui.glass-name", " ");
    }
    
    public String getMessage(String key) {
        FileConfiguration config = languages.get(currentLanguage);
        if (config == null) {
            config = languages.get("en");
        }
        
        String message = config.getString(key, key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);
        
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        return message;
    }
    
    public void setLanguage(String language) {
        if (languages.containsKey(language)) {
            this.currentLanguage = language;
        }
    }
    
    public String getCurrentLanguage() {
        return currentLanguage;
    }
    
    public void reloadLanguages() {
        languages.clear();
        loadLanguages();
        plugin.getLogger().info("Language files reloaded successfully Dil Dosyaları Başarı ile yüklendi");
    }
}
