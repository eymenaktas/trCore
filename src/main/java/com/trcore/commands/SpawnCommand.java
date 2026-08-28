package com.trcore.commands;

import com.trcore.base.BaseCommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand extends BaseCommand {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        String cmdName = command.getName().toLowerCase();
        String prefix = plugin.getConfig().getString("prefix", "");

        if (cmdName.equals("setspawn")) {
            if (!has(player, "TRCore.setspawn")) return true;
            plugin.getSpawnManager().setSpawn(player.getLocation());
            player.sendMessage(comp(prefix + "<green>Spawn noktası başarıyla kaydedildi!"));
            return true;
        }

        if (cmdName.equals("setboxspawn")) {
            if (!has(player, "TRCore.setspawn")) return true;
            World boxWorld = findWorld("box");

            if (boxWorld == null) {
                player.sendMessage(comp(prefix + "<red>'box' dünyası sistemde yüklü değil!"));
                return true;
            }

            Location loc = player.getLocation().clone();
            loc.setWorld(boxWorld);
            plugin.getSpawnManager().setWorldSpawn(loc);
            player.sendMessage(comp(prefix + "<green>Box dünyası başlangıç noktası başarıyla kaydedildi! (Dünya: " + boxWorld.getName() + ")"));
            return true;
        }

        if (cmdName.equals("box")) {
            World boxWorld = findWorld("box");
            if (boxWorld == null) {
                player.sendMessage(comp(prefix + "<red>Box dünyası bulunamadı!"));
                return true;
            }
            Location boxSpawn = plugin.getSpawnManager().getWorldSpawn(boxWorld);
            if (boxSpawn == null) {
                player.sendMessage(comp(prefix + "<red>Box dünyası başlangıç noktası henüz ayarlanmamış!"));
                return true;
            }
            player.sendMessage(comp(prefix + "<green>Box dünyasına ışınlanıyorsunuz..."));
            player.teleportAsync(boxSpawn);
            return true;
        }

        if (cmdName.equals("spawn")) {
            Location mainSpawn = plugin.getSpawnManager().getSpawnLocation();
            if (mainSpawn == null) {
                player.sendMessage(comp(prefix + "<red>Spawn noktası henüz ayarlanmamış!"));
                return true;
            }
            player.sendMessage(comp(prefix + "<green>Başlangıç noktasına ışınlanıyorsunuz..."));
            player.teleportAsync(mainSpawn);
            return true;
        }

        return true;
    }

    private World findWorld(String name) {
        World world = Bukkit.getWorld(name);
        if (world != null) return world;
        for (World w : Bukkit.getWorlds()) {
            if (w.getName().equalsIgnoreCase(name)) return w;
        }
        return null;
    }
}
