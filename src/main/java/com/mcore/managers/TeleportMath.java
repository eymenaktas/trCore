package com.mcore.utils;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class TeleportMath {

    /**
     * Verilen merkez noktasından belirtilen mesafe kadar uzaklıkta,
     * birbirine bakan iki konum oluşturur.
     *
     * @param center   Merkez konum (yüksekliği ayarlanmış olmalı)
     * @param distance İki oyuncu arasındaki mesafe
     * @return 2 elemanlı Location dizisi [loc1, loc2]
     */
    public static Location[] getFacingLocs(Location center, double distance) {
        double half = distance / 2.0;

        // X ekseninde oyuncuları konumlandır (Biri sağda biri solda)
        Location loc1 = center.clone().add(half, 0, 0);
        Location loc2 = center.clone().subtract(half, 0, 0);

        // Yükseklik güvenliği (yere gömülmemeleri için +1 blok yukarı olabilir,
        // ancak çağıran metodun zaten Y ayarını yaptığı varsayılır)

        // Oyuncuların birbirine bakmasını sağla
        lookAt(loc1, loc2);
        lookAt(loc2, loc1);

        return new Location[]{loc1, loc2};
    }

    private static void lookAt(Location loc, Location target) {
        // Hedef vektörü hesapla: Hedef - Kaynak
        Vector dir = target.toVector().subtract(loc.toVector()).normalize();
        loc.setDirection(dir);
    }
}