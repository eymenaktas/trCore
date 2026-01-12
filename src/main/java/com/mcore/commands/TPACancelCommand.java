//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.mcore.commands;

import com.mcore.managers.TPAManager;
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

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player p) {
            this.tpaManager.cancel(p);
        }

        return true;
    }
}
