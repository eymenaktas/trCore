package com.trcore.managers;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackManager {
    private final Map<UUID, Location> lastLocations = new HashMap<>();

    public void setLastLocation(Player player, Location loc) {
        lastLocations.put(player.getUniqueId(), loc);
    }

    public Location popLastLocation(Player player) {
        return lastLocations.remove(player.getUniqueId());
    }
}
