package com.trcore.managers;

import com.trcore.TRCore;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.*;
import java.util.stream.Collectors;

public class TrimManager {

    private final TRCore plugin;
    private final Map<String, TrimGroup> groups = new HashMap<>();
    private final List<TrimGroup> sortedGroups = new ArrayList<>();

    private List<TrimPattern> allPatterns;
    private List<TrimMaterial> allMaterials;

    public TrimManager(TRCore plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        groups.clear();
        sortedGroups.clear();

        ConfigurationSection section = plugin.getConfigManager().getTrimsConfig().getConfigurationSection("groups");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection groupSec = section.getConfigurationSection(key);
                if (groupSec == null) continue;

                int priority = groupSec.getInt("priority", 0);
                List<String> patterns = groupSec.getStringList("allowed_patterns");
                List<String> materials = groupSec.getStringList("allowed_materials");

                TrimGroup group = new TrimGroup(key, priority, patterns, materials);
                groups.put(key, group);
                sortedGroups.add(group);
            }
        }

        sortedGroups.sort(Comparator.comparingInt(TrimGroup::getPriority).reversed());

        // 1.21.1 Non-Deprecated Registry Access
        allPatterns = Registry.TRIM_PATTERN.stream().collect(Collectors.toList());
        allMaterials = Registry.TRIM_MATERIAL.stream().collect(Collectors.toList());
    }

    public TrimGroup getPlayerGroup(Player player) {
        for (TrimGroup group : sortedGroups) {
            if (player.hasPermission("trcore.trim." + group.getName())) {
                return group;
            }
        }
        return groups.get("default");
    }

    public List<TrimPattern> getAllowedPatterns(Player player) {
        TrimGroup group = getPlayerGroup(player);
        if (group == null) return Collections.emptyList();

        if (group.getAllowedPatterns().contains("*")) return allPatterns;

        return allPatterns.stream()
                .filter(p -> group.getAllowedPatterns().contains(Registry.TRIM_PATTERN.getKey(p).getKey().toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<TrimMaterial> getAllowedMaterials(Player player) {
        TrimGroup group = getPlayerGroup(player);
        if (group == null) return Collections.emptyList();

        if (group.getAllowedMaterials().contains("*")) return allMaterials;

        return allMaterials.stream()
                .filter(m -> group.getAllowedMaterials().contains(Registry.TRIM_MATERIAL.getKey(m).getKey().toLowerCase()))
                .collect(Collectors.toList());
    }

    public void applyTrim(ItemStack item, TrimPattern pattern, TrimMaterial material) {
        if (item == null || !(item.getItemMeta() instanceof ArmorMeta meta)) return;
        meta.setTrim(new ArmorTrim(material, pattern));
        item.setItemMeta(meta);
    }

    public void removeTrim(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof ArmorMeta meta)) return;
        meta.setTrim(null);
        item.setItemMeta(meta);
    }

    public String getPatternName(TrimPattern pattern) {
        String key = Registry.TRIM_PATTERN.getKey(pattern).getKey().toLowerCase();
        return plugin.getConfigManager().getTrimsConfig().getString("patterns." + key + ".display-name", key);
    }

    public String getMaterialName(TrimMaterial material) {
        String key = Registry.TRIM_MATERIAL.getKey(material).getKey().toLowerCase();
        return plugin.getConfigManager().getTrimsConfig().getString("materials." + key + ".display-name", key);
    }

    public Material getPatternIcon(TrimPattern pattern) {
        String key = Registry.TRIM_PATTERN.getKey(pattern).getKey().toLowerCase();
        String matName = plugin.getConfigManager().getTrimsConfig().getString("patterns." + key + ".icon", "PAPER");
        Material m = Material.matchMaterial(matName);
        return m != null ? m : Material.PAPER;
    }

    public Material getMaterialIcon(TrimMaterial material) {
        String key = Registry.TRIM_MATERIAL.getKey(material).getKey().toLowerCase();
        String matName = plugin.getConfigManager().getTrimsConfig().getString("materials." + key + ".icon", "IRON_INGOT");
        Material m = Material.matchMaterial(matName);
        return m != null ? m : Material.IRON_INGOT;
    }

    public static class TrimGroup {
        private final String name;
        private final int priority;
        private final List<String> allowedPatterns;
        private final List<String> allowedMaterials;

        public TrimGroup(String name, int priority, List<String> allowedPatterns, List<String> allowedMaterials) {
            this.name = name;
            this.priority = priority;
            this.allowedPatterns = allowedPatterns.stream().map(String::toLowerCase).collect(Collectors.toList());
            this.allowedMaterials = allowedMaterials.stream().map(String::toLowerCase).collect(Collectors.toList());
        }

        public String getName() { return name; }
        public int getPriority() { return priority; }
        public List<String> getAllowedPatterns() { return allowedPatterns; }
        public List<String> getAllowedMaterials() { return allowedMaterials; }
    }
}
