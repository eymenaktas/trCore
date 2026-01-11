package com.mcore.commands;
import com.mcore.mCore;
import com.mcore.managers.QueueManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RTPQueueCommand implements CommandExecutor {
    private final mCore plugin;
    private final QueueManager queueManager;
    public RTPQueueCommand(mCore p, QueueManager q) { plugin = p; queueManager = q; }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        // Toggle (Aç/Kapa)
        queueManager.toggle(player);
        return true;
    }
}