package com.trcore.managers;

import com.trcore.TRCore;
import com.trcore.utils.CC;
import com.trcore.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ZMenu tarzı menü sistemi.
 *
 * - menus/ klasöründeki tüm .yml dosyalarını yükler
 * - config.yml menu-commands → komut haritası tutar
 * - Menü kapanınca player inventory GC'ye bırakılır (RAM sızıntısı yok)
 * - Açma işlemleri player.getScheduler() ile Folia-safe
 * - reload() → tüm state temizlenir, yeniden yüklenir
 * - Settings menüsü SettingsManager'ın kendi holder/listener'ı ile yönetilir
 */
public class MenuManager implements Listener {

    private final TRCore plugin;

    private final Map<String, Inventory>            templates   = new HashMap<>();
    private final Map<String, Map<Integer, String>> actions     = new HashMap<>();
    private final Map<String, String>               titles      = new HashMap<>(); // Artık String tutuyoruz (PAPI için)
    private final Map<String, String>               headFormats = new HashMap<>();
    private final Map<String, List<MenuItemDefinition>> menuDefinitions = new HashMap<>();

    // komut adı (lowercase) → menü id
    private final Map<String, String> commandMenuMap = new HashMap<>();

    public MenuManager(TRCore plugin) {
        this.plugin = plugin;
    }

    private static class MenuItemDefinition {
        ConfigurationSection section;
        List<Integer> slots;
        String action;
        String leftCmd;
        String rightCmd;
        boolean closeOnExecute;
    }

    // ------------------------------------------------------------------
    // Yükleme — reload'da tam reset
    // ------------------------------------------------------------------

    public void load() {
        templates.clear();
        actions.clear();
        titles.clear();
        headFormats.clear();
        commandMenuMap.clear();
        menuDefinitions.clear();

        File folder = new File(plugin.getDataFolder(), "menus");
        if (!folder.exists()) folder.mkdirs();

        // Eksik varsayılan menüleri çıkar
        for (String res : new String[]{
                "menus/tpa-accept-menu.yml",
                "menus/tpa-here-accept-menu.yml",
                "menus/tpa-send-menu.yml",
                "menus/tpahere-send-menu.yml",
                "menus/duel-accept-menu.yml",
                "menus/rtp-menu.yml",
                "menus/report-player-select.yml",
                "menus/report-reason.yml",
                "menus/admin-reports.yml"
        }) {
            File f = new File(plugin.getDataFolder(), res);
            if (!f.exists()) {
                try { plugin.saveResource(res, false); }
                catch (Exception e) { plugin.getLogger().warning(res + " jar'da bulunamadi."); }
            }
        }

        // Tüm .yml dosyalarını yükle
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".yml")) loadFile(f);
            }
        }

        // menu-commands bloğu → commandMenuMap
        ConfigurationSection cmdSec = plugin.getConfig()
                .getConfigurationSection("menu-commands");
        if (cmdSec != null) {
            for (String cmdName : cmdSec.getKeys(false)) {
                String menuId = cmdSec.getString(cmdName + ".menu", cmdName);
                commandMenuMap.put(cmdName.toLowerCase(), menuId);
                for (String alias : cmdSec.getStringList(cmdName + ".aliases")) {
                    commandMenuMap.put(alias.toLowerCase(), menuId);
                }
            }
        }
    }

    private void loadFile(File f) {
        YamlConfiguration yml;
        try (InputStreamReader r = new InputStreamReader(
                new FileInputStream(f), StandardCharsets.UTF_8)) {
            yml = YamlConfiguration.loadConfiguration(r);
        } catch (Exception ex) {
            yml = YamlConfiguration.loadConfiguration(f);
        }

        String id   = f.getName().replace(".yml", "");
        int    size = yml.getInt("size", 27);
        String titleStr = yml.getString("title", "Menu");
        titles.put(id, titleStr);

        CustomHolder holder = new CustomHolder(id);
        Inventory inv = Bukkit.createInventory(holder, size, CC.parse(titleStr));
        Map<Integer, String> acts = new HashMap<>();
        List<MenuItemDefinition> defs = new ArrayList<>();

        ConfigurationSection items = yml.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection item = items.getConfigurationSection(key);
                if (item == null) continue;
                
                MenuItemDefinition def = new MenuItemDefinition();
                def.section = item;
                def.slots = item.isList("slot") ? item.getIntegerList("slot") : Collections.singletonList(item.getInt("slot"));
                def.action = item.getString("action");
                def.leftCmd = item.getString("left_cmd", "");
                def.rightCmd = item.getString("right_cmd", "");
                def.closeOnExecute = item.getBoolean("close_on_execute", false);
                defs.add(def);

                for (int s : def.slots) {
                    setItem(inv, acts, s, item, id);
                }
            }
        }
        templates.put(id, inv);
        actions.put(id, acts);
        menuDefinitions.put(id, defs);
    }

    private void setItem(Inventory inv, Map<Integer, String> acts,
                         int slot, ConfigurationSection item, String menuId) {
        inv.setItem(slot, ItemBuilder.fromConfig(item));
        if (item.contains("action")) acts.put(slot, item.getString("action"));
        if (slot == 13 && item.contains("name")) headFormats.put(menuId, item.getString("name"));
    }

    // ------------------------------------------------------------------
    // Menü oluşturma (Placeholder Destekli)
    // ------------------------------------------------------------------

    public Inventory create(String id, Player viewer) {
        return create(id, viewer, null);
    }

    public Inventory create(String id, Player viewer, Player target) {
        if (!menuDefinitions.containsKey(id)) return null;
        
        List<MenuItemDefinition> defs = menuDefinitions.get(id);
        String titleRaw = titles.getOrDefault(id, "Menu");
        
        // Başlıkta placeholder varsa çöz
        if (target != null) {
            titleRaw = titleRaw.replace("%target%", target.getName());
        }
        if (viewer != null) {
            titleRaw = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(viewer, titleRaw);
        }
        
        Inventory temp = templates.get(id);
        CustomHolder holder = new CustomHolder(id);
        Inventory inv = Bukkit.createInventory(holder, temp.getSize(), CC.parse(titleRaw));

        for (MenuItemDefinition def : defs) {
            ItemStack item = ItemBuilder.fromConfig(def.section, viewer, target);
            
            for (int s : def.slots) {
                inv.setItem(s, item);
            }
        }
        
        // Geriye dönük uyumluluk ve TPA heads için
        if (target != null) {
            ItemStack item = inv.getItem(13);
            if (item != null && item.getType().name().contains("HEAD")) {
                org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) item.getItemMeta();
                meta.setOwningPlayer(target);
                // Artık ItemBuilder %target% içeriklerini replacediği için isim ayarı gereksiz olabilir ancak title gibi şeyler ekstradan parse edilmiş olabilir.
                item.setItemMeta(meta);
            }
        }
        return inv;
    }

    /**
     * Menüyü player-thread'de aç (Folia: player.getScheduler() zorunlu).
     */
    public void open(Player player, String menuId) {
        player.getScheduler().run(plugin, t -> {
            Inventory inv = create(menuId, player);
            if (inv == null) {
                player.sendMessage(CC.parse("<red>Menu bulunamadi: " + menuId));
                return;
            }
            player.openInventory(inv);
        }, null);
    }

    public String getMenuIdForCommand(String command) {
        return commandMenuMap.get(command.toLowerCase());
    }

    public Map<String, String> getCommandMenuMap() {
        return Collections.unmodifiableMap(commandMenuMap);
    }

    // ------------------------------------------------------------------
    // Event: Tıklama
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!plugin.checkGuiCooldown(player.getUniqueId())) {
            e.setCancelled(true);
            return;
        }

        // SettingsHolder → SettingsManager kendi listener'ında halleder
        if (e.getInventory().getHolder() instanceof SettingsManager.SettingsHolder) return;

        // SetupHolder → DuelManager halleder
        if (e.getInventory().getHolder() instanceof DuelManager.SetupHolder) {
            e.setCancelled(true);
            plugin.getDuelManager().handleSetupClick(player, e.getSlot());
            return;
        }

        if (!(e.getInventory().getHolder() instanceof CustomHolder holder)) return;
        e.setCancelled(true);

        Map<Integer, String> menuActions = actions.get(holder.getId());
        if (menuActions == null) return;

        String action = menuActions.get(e.getSlot());
        if (action == null) return;

        handleAction(player, holder.getId(), action, e.getSlot(), e.isLeftClick());
    }

    // ------------------------------------------------------------------
    // Aksiyon işleyici
    // ------------------------------------------------------------------

    private void handleAction(Player player, String menuId, String action, int slot, boolean isLeftClick) {

        // ── rtp-random: rtp.yml'den rastgele dünya ─────────────────────
        if (action.equalsIgnoreCase("rtp-random")) {
            player.closeInventory();
            RTPManager rtpManager = plugin.getRTPManager();
            if (rtpManager == null || rtpManager.getWorlds().isEmpty()) {
                player.sendMessage(CC.get("rtp.no-world", "%world%", "random"));
                return;
            }
            List<String> worldKeys = new ArrayList<>(rtpManager.getWorlds().keySet());
            String randomWorld = worldKeys.get(ThreadLocalRandom.current().nextInt(worldKeys.size()));
            doRtp(player, randomWorld);
            return;
        }

        // ── rtp-<dünya>: belirli dünya ──────────────────────────────────
        if (action.startsWith("rtp-")) {
            player.closeInventory();
            doRtp(player, action.substring(4));
            return;
        }

        // ── Queue ────────────────────────────────────────────────────────
        if (action.equalsIgnoreCase("rtp_join")) {
            player.closeInventory();
            plugin.getQueueManager().join(player);
            return;
        }
        if (action.equalsIgnoreCase("rtp_leave")) {
            player.closeInventory();
            plugin.getQueueManager().leave(player);
            return;
        }

        // ── TPA / Duel ───────────────────────────────────────────────────
        if (action.equalsIgnoreCase("accept")) {
            player.closeInventory();
            if (menuId.contains("tpa"))       player.performCommand("tpa accept");
            else if (menuId.contains("duel")) player.performCommand("rtpduel accept");
            return;
        }
        if (action.equalsIgnoreCase("deny")) {
            player.closeInventory();
            if (menuId.contains("tpa"))       player.performCommand("tpa deny");
            else if (menuId.contains("duel")) player.performCommand("rtpduel deny");
            return;
        }
        if (action.equalsIgnoreCase("confirm_send")) {
            player.closeInventory();
            player.performCommand("tpa confirm_send");
            return;
        }
        if (action.equalsIgnoreCase("cancel_send")) {
            player.closeInventory();
            player.performCommand("tpa cancel_send");
            return;
        }

        // ── external_cmd: settings.yml tarzı left_cmd / right_cmd ────────
        if (action.equalsIgnoreCase("external_cmd")) {
            List<MenuItemDefinition> defs = menuDefinitions.get(menuId);
            if (defs == null) return;
            for (MenuItemDefinition def : defs) {
                if (def.slots.contains(slot)) {
                    String cmd = isLeftClick ? def.leftCmd : def.rightCmd;
                    if (cmd != null && !cmd.isEmpty()) {
                        if (def.closeOnExecute) player.closeInventory();
                        player.performCommand(cmd);
                    }
                    return;
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // RTP yardımcısı
    // ------------------------------------------------------------------
    // RTP yardımcısı
    // ------------------------------------------------------------------

    private void doRtp(Player player, String worldName) {
        RTPManager rtpManager = plugin.getRTPManager();
        if (rtpManager == null || !rtpManager.hasWorld(worldName)) {
            player.sendMessage(CC.get("rtp.no-world", "%world%", worldName));
            return;
        }
        // messages.yml'den "searching" mesajı — zaten player thread'indeyiz
        player.sendMessage(CC.get("rtp.searching"));
        rtpManager.teleport(player, worldName, dest -> {
            // Callback player.getScheduler() üzerinden gelir → güvenli
            if (dest == null) {
                player.sendMessage(CC.get("rtp.fail"));
            } else {
                String msg = CC.getRaw("rtp.success")
                        .replace("%x%", String.valueOf(dest.getBlockX()))
                        .replace("%y%", String.valueOf(dest.getBlockY()))
                        .replace("%z%", String.valueOf(dest.getBlockZ()));
                player.sendMessage(CC.parse(msg));
            }
        });
    }

    // ------------------------------------------------------------------
    // Inner class
    // ------------------------------------------------------------------

    public static class CustomHolder implements InventoryHolder {
        private final String id;
        public CustomHolder(String id) { this.id = id; }
        public String getId()          { return id; }
        @Override public Inventory getInventory() { return null; }
    }
}
