//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

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

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(CC.parse("<red>Bu komutu sadece oyuncular kullanabilir."));
            return true;
        } else {
            this.queueManager.toggle((Player)sender);
            return true;
        }
    }
}
