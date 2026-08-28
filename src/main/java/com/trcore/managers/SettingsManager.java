package com.trcore.managers;

import com.trcore.TRCore;
import com.trcore.utils.CC;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SettingsManager implements Listener {
    private final TRCore plugin;
    // YENİ NESİL ADVENTURE BOSSBAR SİSTEMİ
    private final Map<UUID, BossBar> autoTpaBars = new HashMap<>();

    private final Map<Integer, CachedMenuItem> menuCache = new HashMap<>();
    private String menuTitle = "<gray>Ayarlar";
    private int menuSize = 36;
    private boolean hasPapi = false;
    private ItemStack fillerItem; // Bir kez oluşturulur, her render'da yeniden yaratılmaz

    public SettingsManager(TRCore plugin) {
        this.plugin = plugin;
        this.hasPapi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        loadConfig();
    }

    public void loadConfig() {
        File file = new File(plugin.getDataFolder(), "menus/settings.yml");
        if (!file.exists())
            plugin.saveResource("menus/settings.yml", false);

        // Explicit UTF-8 okuma - IBM857 sistem charset'in etkisini engeller
        YamlConfiguration config;
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            config = YamlConfiguration.loadConfiguration(reader);
        } catch (Exception ex) {
            config = YamlConfiguration.loadConfiguration(file);
        }
        menuSize = config.getInt("size", 36);
        menuTitle = sanitizeLegacy(config.getString("title", "<gray>Ayarlar"));
        menuCache.clear();

        ConfigurationSection itemsSec = config.getConfigurationSection("items");
        if (itemsSec != null) {
            for (String key : itemsSec.getKeys(false)) {
                ConfigurationSection item = itemsSec.getConfigurationSection(key);
                if (item == null)
                    continue;

                CachedMenuItem cached = new CachedMenuItem();
                cached.slot = item.getInt("slot");
                cached.action = item.getString("action", "");
                cached.name = item.getString("name", "Eşya");
                cached.lore = item.getStringList("lore");
                cached.leftCmd = item.getString("left_cmd", "");
                cached.rightCmd = item.getString("right_cmd", "");
                cached.papiRadiusCheck = item.getString("papi_radius_check", "");
                // Material config'de yüklenirken çözülür — her render'da matchMaterial()
                // çağrısı yok
                String matName = item.getString("material", "PAPER");
                cached.material = Material.matchMaterial(matName);
                if (cached.material == null)
                    cached.material = Material.PAPER;

                menuCache.put(cached.slot, cached);
            }
        }

        // Filler item bir kez oluşturulur
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fMeta = filler.getItemMeta();
        if (fMeta != null) {
            fMeta.displayName(CC.parse(" "));
            filler.setItemMeta(fMeta);
        }
        this.fillerItem = filler;
    }

    private String sanitizeLegacy(String text) {
        if (text == null) return "";
        return text
                .replace("§0", "<black>").replace("&0", "<black>")
                .replace("§1", "<dark_blue>").replace("&1", "<dark_blue>")
                .replace("§a", "<green>").replace("&a", "<green>")
                .replace("§c", "<red>").replace("&c", "<red>")
                .replace("§e", "<yellow>").replace("&e", "<yellow>")
                .replace("§f", "<white>").replace("&f", "<white>")
                .replace("§7", "<gray>").replace("&7", "<gray>")
                .replace("§8", "<dark_gray>").replace("&8", "<dark_gray>")
                .replace("§l", "<bold>").replace("&l", "<bold>")
                .replace("§r", "<reset>").replace("&r", "<reset>");
    }

    public void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new SettingsHolder(), menuSize, CC.parse(menuTitle));
        updateMenu(player, inv);
        player.openInventory(inv);
        // Menü açılış sesi
        String soundName = plugin.getConfig().getString("settings.open-sound", "");
        if (soundName != null && !soundName.isEmpty()) {
            CC.playSound(player, soundName.toUpperCase());
        }
    }

    public void updateMenu(Player player, Inventory inv) {
        ToggleManager tm = plugin.getToggleManager();
        UUID uuid = player.getUniqueId();

        for (Map.Entry<Integer, CachedMenuItem> entry : menuCache.entrySet()) {
            int slot = entry.getKey();
            CachedMenuItem cache = entry.getValue();

            boolean status = false;
            String statusText = "<red>Kapalı";
            int radius = 0;

            switch (cache.action) {
                case "tpa_auto":
                    status = tm.isAutoTPA(uuid);
                    statusText = status ? "<green>Açık" : "<red>Kapalı";
                    break;
                case "death_msgs":
                    status = tm.isDeathMessageEnabled(uuid);
                    statusText = status ? "<green>Açık" : "<red>Kapalı";
                    radius = status ? tm.getDeathMessageRadius(uuid) : 0;
                    break;
                case "scoreboard":
                    status = tm.isScoreboardEnabled(uuid);
                    statusText = status ? "<green>Açık" : "<red>Kapalı";
                    break;
                case "duel":
                    status = tm.isDuelEnabled(uuid);
                    statusText = status ? "<green>Açık" : "<red>Kapalı";
                    break;
                case "tpa":
                    status = tm.isTPAEnabled(uuid);
                    statusText = status ? "<green>Açık" : "<red>Kapalı";
                    break;
                case "tpahere":
                    status = tm.isTPAHereEnabled(uuid);
                    statusText = status ? "<green>Açık" : "<red>Kapalı";
                    break;
                case "night_vision":
                    status = plugin.getNightVisionManager().isNightVisionEnabled(uuid);
                    statusText = status ? "<green>Açık" : "<red>Kapalı";
                    break;
                case "death_drop_hide":
                    status = tm.isDeathDropHideEnabled(uuid);
                    statusText = status ? "<green>Açık" : "<red>Kapalı";
                    break;
                case "rekit_death":
                    if (hasPapi) {
                        String res = PlaceholderAPI.setPlaceholders(player, "%perplayerkit_rekit_on_death%")
                                .toLowerCase();
                        status = res.contains("true") || res.contains("on")
                                || res.contains("yes") || res.contains("açık");
                    } else {
                        status = false;
                    }
                    statusText = status ? "<green>Açık" : "<red>Kapalı";
                    break;
                case "rekit_kill":
                    if (hasPapi) {
                        String res = PlaceholderAPI.setPlaceholders(player, "%perplayerkit_rekit_on_kill%")
                                .toLowerCase();
                        status = res.contains("true") || res.contains("on")
                                || res.contains("yes") || res.contains("açık");
                    } else {
                        status = false;
                    }
                    statusText = status ? "<green>Açık" : "<red>Kapalı";
                    break;

                case "external_cmd":
                    if (hasPapi && cache.papiRadiusCheck != null && !cache.papiRadiusCheck.isEmpty()) {
                        String parsedRad = PlaceholderAPI.setPlaceholders(player, cache.papiRadiusCheck);
                        String cleanNum = parsedRad.replaceAll("[^0-9]", "");
                        if (!cleanNum.isEmpty()) {
                            try {
                                radius = Integer.parseInt(cleanNum);
                            } catch (Exception ignored) {
                                radius = 0;
                            }
                        }
                    }
                    status = (radius > 0);
                    statusText = status ? "<green>Açık" : "<red>Kapalı";
                    break;
                case "quick_rtp":
                    status = tm.isQuickRtpEnabled(uuid);
                    statusText = status ? "<green>Açık" : "<red>Kapalı";
                    break;
            }

            // --- AŞIRI AKILLI LORE PARSER (Kit Kapalıysa Mesafeyi Siler) ---
            List<String> rawLore = new ArrayList<>();
            boolean isEffectivelyOff = !status;

            for (String line : cache.lore) {
                String parsedLine = line.replace("%status%", statusText);
                if (hasPapi && parsedLine.contains("%")) {
                    parsedLine = PlaceholderAPI.setPlaceholders(player, parsedLine);
                }

                parsedLine = parsedLine.replace("Durum: on", "Durum: <green>Açık")
                        .replace("Durum: off", "Durum: <red>Kapalı")
                        .replace("Durum: On", "Durum: <green>Açık")
                        .replace("Durum: Off", "Durum: <red>Kapalı")
                        .replace("Durum: true", "Durum: <green>Açık")
                        .replace("Durum: false", "Durum: <red>Kapalı");

                if (parsedLine.contains("Kapalı")) {
                    isEffectivelyOff = true;
                }
                rawLore.add(parsedLine);
            }

            List<net.kyori.adventure.text.Component> finalLore = new ArrayList<>();
            for (String line : rawLore) {
                if (line.contains("%radius_")) {
                    if (isEffectivelyOff || radius == 0)
                        continue; // KAPALIYSA RADIUS SATIRLARINI YOK ET
                    if (line.contains("100"))
                        line = formatRadius(radius, 100);
                    else if (line.contains("150"))
                        line = formatRadius(radius, 150);
                    else if (line.contains("200"))
                        line = formatRadius(radius, 200);
                    else if (line.contains("250"))
                        line = formatRadius(radius, 250);
                }
                line = sanitizeLegacy(line);
                finalLore.add(CC.parse(line));
            }

            String finalName = cache.name;
            if (hasPapi && finalName.contains("%"))
                finalName = PlaceholderAPI.setPlaceholders(player, finalName);
            finalName = sanitizeLegacy(finalName);

            Material mat = cache.material;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(CC.parse(finalName));
                meta.lore(finalLore);
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
        }

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, fillerItem);
        }
    }

    private String formatRadius(int currentRadius, int targetRadius) {
        if (currentRadius == targetRadius)
            return "  <!italic><#92f7a1>▶ " + targetRadius + " Blok";
        return "  <dark_gray>▪ " + targetRadius + " Blok";
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof SettingsHolder))
            return;
        if (!(e.getWhoClicked() instanceof Player player))
            return;

        if (!plugin.checkGuiCooldown(player.getUniqueId())) {
            e.setCancelled(true);
            return;
        }

        e.setCancelled(true);
        int slot = e.getSlot();

        CachedMenuItem cache = menuCache.get(slot);
        if (cache == null)
            return;

        ToggleManager tm = plugin.getToggleManager();
        UUID uuid = player.getUniqueId();
        boolean needsUpdate = false;
        boolean isExternal = false;

        switch (cache.action) {
            case "tpa_auto":
                tm.toggleAutoTPA(uuid);
                needsUpdate = true;
                if (tm.isAutoTPA(uuid)) {
                    CC.playSound(player, "BLOCK_BEACON_ACTIVATE");
                    showAutoTpaBossBar(player);
                    player.sendMessage(CC.get("toggle.tpauto-enabled"));
                } else {
                    hideAutoTpaBossBar(player);
                    player.sendMessage(CC.get("toggle.tpauto-disabled"));
                }
                break;
            case "death_msgs":
                needsUpdate = true;
                if (e.isLeftClick())
                    tm.toggleDeathMessages(uuid);
                else if (e.isRightClick() && tm.isDeathMessageEnabled(uuid))
                    tm.cycleDeathMessageRadius(uuid);
                break;
            case "scoreboard":
                tm.toggleScoreboard(uuid);
                needsUpdate = true;
                player.sendMessage(CC.get(
                        tm.isScoreboardEnabled(uuid) ? "toggle.scoreboard-enabled" : "toggle.scoreboard-disabled"));
                break;
            case "duel":
                tm.toggleDuel(uuid);
                needsUpdate = true;
                player.sendMessage(CC.get(tm.isDuelEnabled(uuid) ? "toggle.duel-enabled" : "toggle.duel-disabled"));
                break;
            case "tpa":
                tm.toggleTPA(uuid);
                needsUpdate = true;
                player.sendMessage(CC.get(tm.isTPAEnabled(uuid) ? "toggle.tpa-enabled" : "toggle.tpa-disabled"));
                break;
            case "tpahere":
                tm.toggleTPAHere(uuid);
                needsUpdate = true;
                player.sendMessage(
                        CC.get(tm.isTPAHereEnabled(uuid) ? "toggle.tpahere-enabled" : "toggle.tpahere-disabled"));
                break;
            case "night_vision":
                boolean nvEnabled = plugin.getNightVisionManager().toggleNightVision(player);
                needsUpdate = true;
                player.sendMessage(CC.get(nvEnabled ? "night-vision.enabled" : "night-vision.disabled"));
                break;
            case "messages":
                player.performCommand("msgtoggle");
                isExternal = true;
                break;
            case "mentions":
                player.performCommand("mentiontoggle");
                isExternal = true;
                break;
            case "death_drop_hide":
                needsUpdate = true;
                boolean ddhNowOn = tm.toggleDeathDropHide(uuid);
                if (ddhNowOn) {
                    plugin.getDeathDropManager().hideAllCurrentDropsFor(player);
                    player.sendMessage(CC.get("time-weather.drop-hide-enabled"));
                } else {
                    plugin.getDeathDropManager().showAllCurrentDropsFor(player);
                    player.sendMessage(CC.get("time-weather.drop-hide-disabled"));
                }
                break;
            case "rekit_death":
                player.performCommand("rekittoggle death");
                isExternal = true;
                break;
            case "rekit_kill":
                player.performCommand("rekittoggle kill");
                isExternal = true;
                break;

            case "external_cmd":
                String cmdToRun = e.isLeftClick() ? cache.leftCmd : cache.rightCmd;
                if (cmdToRun != null && !cmdToRun.isEmpty()) {
                    player.performCommand(cmdToRun);
                    isExternal = true;
                }
                break;
            case "quick_rtp":
                needsUpdate = true;
                boolean qrNowOn = tm.toggleQuickRtp(uuid);
                player.sendMessage(CC.get(qrNowOn ? "toggle.quickrtp-enabled" : "toggle.quickrtp-disabled"));
                break;
        }

        CC.playSound(player, "UI_BUTTON_CLICK");

        if (isExternal) {
            player.getScheduler().runDelayed(plugin, task -> {
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof SettingsHolder) {
                    updateMenu(player, player.getOpenInventory().getTopInventory());
                }
            }, null, 2L);
        } else if (needsUpdate) {
            updateMenu(player, e.getInventory());
        }
    }

    // --- YENİ NESİL ADVENTURE BOSSBAR (Config Uyumlu) ---
    public void showAutoTpaBossBar(Player player) {
        hideAutoTpaBossBar(player);
        net.kyori.adventure.text.Component title = CC.get("tpa.bossbar"); // Configden okur
        BossBar bar = BossBar.bossBar(title, 1.0f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
        player.showBossBar(bar);
        autoTpaBars.put(player.getUniqueId(), bar);
    }

    public void hideAutoTpaBossBar(Player player) {
        if (autoTpaBars.containsKey(player.getUniqueId())) {
            player.hideBossBar(autoTpaBars.remove(player.getUniqueId()));
        }
    }

    public static class SettingsHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static class CachedMenuItem {
        int slot;
        String action, name, leftCmd, rightCmd, papiRadiusCheck;
        Material material; // Yükleme sırasında çözülür
        List<String> lore;
    }
}
