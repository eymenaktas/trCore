package com.mcore.utils;

import net.kyori.adventure.text.Component;
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
        if (meta != null) {
            if (section.contains("name")) meta.displayName(CC.parse(section.getString("name")));
            if (section.contains("lore")) {
                List<Component> lore = new ArrayList<>();
                for (String line : section.getStringList("lore")) lore.add(CC.parse(line));
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}