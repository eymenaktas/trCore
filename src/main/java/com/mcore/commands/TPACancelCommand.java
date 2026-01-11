package com.mcore.commands;

import com.mcore.managers.TPAManager;
import com.mcore.utils.CC; // Eğer CC kullanıyorsan import et, yoksa sil
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TPACancelCommand implements CommandExecutor {

    private final TPAManager tpaManager;

    public TPACancelCommand(TPAManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player p) {
            tpaManager.cancel(p);
        }
        return true;
    }
}