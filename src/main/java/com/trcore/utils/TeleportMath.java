package com.trcore.utils;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class TeleportMath {
    public static Location[] getFacingLocs(Location center, double distance) {
        double half = distance / 2.0;
        Location loc1 = center.clone().add(half, 0, 0);
        Location loc2 = center.clone().subtract(half, 0, 0);
        lookAt(loc1, loc2);
        lookAt(loc2, loc1);
        return new Location[]{loc1, loc2};
    }

    private static void lookAt(Location loc, Location target) {
        Vector dir = target.toVector().subtract(loc.toVector()).normalize();
        loc.setDirection(dir);
    }
}
