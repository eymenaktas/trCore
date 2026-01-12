//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.mcore.managers;

import com.mcore.mCore;
import com.mcore.utils.CC;
import com.mcore.utils.ItemBuilder;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class MenuManager implements Listener {
    private final mCore plugin;
    private final Map<String, Inventory> templates = new HashMap();
    private final Map<String, Map<Integer, String>> actions = new HashMap();
    private final Map<String, Component> titles = new HashMap();
    private final Map<String, String> headNameFormats = new HashMap();

    public MenuManager(mCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.templates.clear();
        this.actions.clear();
        this.titles.clear();
        this.headNameFormats.clear();
        File folder = new File(this.plugin.getDataFolder(), "menus");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        if (folder.listFiles() != null) {
            for(File f : folder.listFiles()) {
                if (f.getName().endsWith(".yml")) {
                    this.loadFile(f);
                }
            }
        }

    }

    private void loadFile(File f) {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
        String id = f.getName().replace(".yml", "");
        int size = yml.getInt("size", 27);
        Component title = CC.parse(yml.getString("title", "Menu"));
        this.titles.put(id, title);
        CustomHolder holder = new CustomHolder(id);
        Inventory inv = Bukkit.createInventory(holder, size, title);
        Map<Integer, String> acts = new HashMap();
        ConfigurationSection items = yml.getConfigurationSection("items");
        if (items != null) {
            for(String key : items.getKeys(false)) {
                if (items.isList(key + ".slot")) {
                    for(int s : items.getIntegerList(key + ".slot")) {
                        this.setItem(inv, acts, s, items, key, id);
                    }
                } else {
                    this.setItem(inv, acts, items.getInt(key + ".slot"), items, key, id);
                }
            }
        }

        this.templates.put(id, inv);
        this.actions.put(id, acts);
    }

    private void setItem(Inventory inv, Map<Integer, String> acts, int slot, ConfigurationSection sec, String key, String menuId) {
        inv.setItem(slot, ItemBuilder.fromConfig(sec.getConfigurationSection(key)));
        if (sec.contains(key + ".action")) {
            acts.put(slot, sec.getString(key + ".action"));
        }

        if (slot == 13 && sec.contains(key + ".name")) {
            this.headNameFormats.put(menuId, sec.getString(key + ".name"));
        }

    }

    public Inventory create(String id, Player headOwner) {
        if (!this.templates.containsKey(id)) {
            return null;
        } else {
            Inventory temp = (Inventory)this.templates.get(id);
            Component title = (Component)this.titles.getOrDefault(id, CC.parse("Menu"));
            Inventory inv = Bukkit.createInventory(temp.getHolder(), temp.getSize(), title);
            inv.setContents(temp.getContents());
            if (headOwner != null) {
                ItemStack item = inv.getItem(13);
                if (item != null && item.getType().name().contains("HEAD")) {
                    SkullMeta meta = (SkullMeta)item.getItemMeta();
                    meta.setOwningPlayer(headOwner);
                    if (this.headNameFormats.containsKey(id)) {
                        String rawName = ((String)this.headNameFormats.get(id)).replace("%target%", headOwner.getName());
                        meta.displayName(CC.parse(rawName));
                    } else {
                        meta.displayName(CC.parse("<yellow>" + headOwner.getName()));
                    }

                    item.setItemMeta(meta);
                }
            }

            return inv;
        }
    }

    public String getAction(String id, int slot) {
        return this.actions.containsKey(id) ? (String)((Map)this.actions.get(id)).get(slot) : null;
    }

    public static class CustomHolder implements InventoryHolder {
        private final String id;

        public CustomHolder(String id) {
            this.id = id;
        }

        public String getId() {
            return this.id;
        }

        public Inventory getInventory() {
            return null;
        }
    }
}
