//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.mcore.listeners;

import com.mcore.mCore;
import com.mcore.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

public class ActionbarListener implements Listener {
    private final mCore plugin;

    public ActionbarListener(mCore p) {
        this.plugin = p;
        (new BukkitRunnable() {
            public void run() {
                if (ActionbarListener.this.plugin.getConfig().getBoolean("actionbar.enabled")) {
                    String rawMsg = ActionbarListener.this.plugin.getConfig().getString("actionbar.message");

                    for(Player p : Bukkit.getOnlinePlayers()) {
                        p.sendActionBar(CC.parse(rawMsg.replace("%tps%", String.format("%.2f", Bukkit.getTPS()[0]))));
                    }

                }
            }
        }).runTaskTimerAsynchronously(this.plugin, 0L, this.plugin.getConfig().getLong("actionbar.interval"));
    }
}
