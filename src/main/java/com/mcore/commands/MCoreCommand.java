package com.mcore.commands;
import com.mcore.mCore;
import com.mcore.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import java.util.Collections;
import java.util.List;

public class MCoreCommand implements CommandExecutor, TabCompleter {
    private final mCore plugin;
    public MCoreCommand(mCore p) { plugin = p; }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("mcore.admin")) return true;
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.getConfigManager().load();
            plugin.reloadConfig();
            sender.sendMessage(CC.get("reload"));
        }
        return true;
    }
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("mcore.admin")) return Collections.singletonList("reload");
        return null;
    }
}