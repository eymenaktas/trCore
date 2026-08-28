package com.trcore.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder name(String name) {
        meta.displayName(CC.parse("<!italic>" + name));
        return this;
    }

    public ItemBuilder lore(String... lines) {
        List<Component> components = new ArrayList<>();
        for (String line : lines) components.add(CC.parse("<!italic>" + line));
        meta.lore(components);
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        meta.lore(lines.stream().map(l -> CC.parse("<!italic>" + l)).collect(Collectors.toList()));
        return this;
    }

    public ItemBuilder glow() {
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack fromConfig(ConfigurationSection section) {
        return fromConfig(section, null);
    }

    public static ItemStack fromConfig(ConfigurationSection section, org.bukkit.entity.Player player) {
        return fromConfig(section, player, null);
    }

    public static ItemStack fromConfig(ConfigurationSection section, org.bukkit.entity.Player player, org.bukkit.entity.Player target) {
        String matName = section.getString("material", "STONE");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.STONE;

        ItemBuilder builder = new ItemBuilder(mat);

        if (section.contains("name")) {
            String name = section.getString("name");
            if (target != null) {
                name = name.replace("%target%", target.getName());
            }
            if (player != null) {
                name = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, name);
            }
            builder.name(name);
        }

        if (section.contains("lore")) {
            List<String> lore = section.getStringList("lore");
            if (target != null) {
                lore = lore.stream().map(l -> l.replace("%target%", target.getName())).collect(Collectors.toList());
            }
            if (player != null) {
                lore = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, lore);
            }
            builder.lore(lore);
        }

        return builder.build();
    }
}
