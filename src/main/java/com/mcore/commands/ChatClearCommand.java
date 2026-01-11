package com.mcore.commands;
import com.mcore.mCore;
import com.mcore.utils.CC;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ChatClearCommand implements CommandExecutor {
    private final mCore plugin;
    public ChatClearCommand(mCore p) { plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("mcore.clearchat")) {
            sender.sendMessage(CC.get("no-perm"));
            return true;
        }

        Component blank = Component.text(" ");
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission("mcore.admin")) {
                for (int i = 0; i < 100; i++) p.sendMessage(blank);
            } else {
                p.sendMessage(CC.get("clearchat.admin-bypass"));
            }

            // HATA BURADAYDI: CC.get içine argüman olarak taşıdık
            p.sendMessage(CC.get("clearchat.broadcast", "%player%", sender.getName()));
        }
        return true;
    }
}