//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.mcore.utils;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class TeleportMath {
    public static Location[] getFacingLocs(Location center, double distance) {
        double half = distance / (double)2.0F;
        Location loc1 = center.clone().add(half, (double)0.0F, (double)0.0F);
        Location loc2 = center.clone().subtract(half, (double)0.0F, (double)0.0F);
        lookAt(loc1, loc2);
        lookAt(loc2, loc1);
        return new Location[]{loc1, loc2};
    }

    private static void lookAt(Location loc, Location target) {
        Vector dir = target.toVector().subtract(loc.toVector()).normalize();
        loc.setDirection(dir);
    }
}
