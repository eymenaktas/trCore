//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.mcore.commands;

import com.mcore.mCore;
import com.mcore.utils.CC;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

public class MCoreCommand implements CommandExecutor, TabCompleter {
    private final mCore plugin;

    public MCoreCommand(mCore p) {
        this.plugin = p;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("mcore.admin")) {
            return true;
        } else {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                this.plugin.getConfigManager().load();
                this.plugin.reloadConfig();
                sender.sendMessage(CC.get("reload", new String[0]));
            }

            return true;
        }
    }

    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return args.length == 1 && sender.hasPermission("mcore.admin") ? Collections.singletonList("reload") : null;
    }
}
