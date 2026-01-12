package com.mcore.commands;

import com.mcore.managers.QueueManager;
import com.mcore.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RTPQueueCommand implements CommandExecutor {
    private final QueueManager queueManager;

    public RTPQueueCommand(QueueManager queueManager) {
        this.queueManager = queueManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.parse("<red>Sadece oyuncular kullanabilir."));
            return true;
        }
        queueManager.toggle((Player) sender);
        return true;
    }
}