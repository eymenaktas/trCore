package com.trcore.base;

import com.trcore.TRCore;
import com.trcore.utils.CC;
import com.trcore.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    protected final TRCore plugin;

    public BaseCommand() {
        this.plugin = TRCore.get();
    }

    @Override
    public abstract boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args);

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return null;
    }

    // Utilities
    protected Component comp(String text) { return TRCore.comp(text); }
    protected ItemBuilder item(Material mat) { return TRCore.item(mat); }
    protected void msg(CommandSender s, String path, String... reps) { plugin.msg(s, path, reps); }
    protected boolean has(CommandSender s, String perm) {
        if (s.hasPermission(perm)) return true;
        msg(s, "no-perm");
        return false;
    }
}
