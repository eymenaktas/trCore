package com.trcore.elo;

import com.trcore.TRCore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EloConfig {
    private final TRCore plugin;
    private FileConfiguration config;
    private File file;

    private List<EloRank> ranks = new ArrayList<>();
    private EloRank defaultRank;

    public EloConfig(TRCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "elo.yml");
        if (!file.exists()) {
            plugin.saveResource("elo.yml", false);
        }

        try (InputStreamReader reader = new InputStreamReader(new java.io.FileInputStream(file), StandardCharsets.UTF_8)) {
            config = YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            config = YamlConfiguration.loadConfiguration(file);
        }

        loadRanks();
    }

    private void loadRanks() {
        ranks.clear();
        ConfigurationSection section = config.getConfigurationSection("ranks");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String name = section.getString(key + ".name", key);
                String numberColor = section.getString(key + ".number-color", "<white>");
                String suffix = section.getString(key + ".suffix", "");
                int minElo = section.getInt(key + ".min-elo", 0);
                ranks.add(new EloRank(key, name, numberColor, suffix, minElo));
            }
        }
        // Kucukten buyuge siralama (0 -> 1000 -> 1500)
        ranks.sort(Comparator.comparingInt(EloRank::getMinElo));
        
        if (!ranks.isEmpty()) {
            defaultRank = ranks.get(0);
        } else {
            // Failsafe
            defaultRank = new EloRank("none", "None", "<white>", "", 0);
        }
    }

    public EloRank getRankForElo(int elo) {
        EloRank current = defaultRank;
        for (EloRank rank : ranks) {
            if (elo >= rank.getMinElo()) {
                current = rank;
            } else {
                break;
            }
        }
        return current;
    }

    public FileConfiguration get() {
        return config;
    }
}
