package com.mcore.utils;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {
    public static ItemStack fromConfig(ConfigurationSection section) {
        String matName = section.getString("material", "STONE");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.STONE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (section.contains("name")) {
            meta.displayName(CC.parse(section.getString("name")));
        }

        if (section.contains("lore")) {
            List<String> lore = section.getStringList("lore");
            List<net.kyori.adventure.text.Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(CC.parse(line));
            }
            meta.lore(loreComponents);
        }

        item.setItemMeta(meta);
        return item;
    }
}