package com.trcore.commands;

import com.trcore.TRCore;
import com.trcore.managers.TrimGUI;
import com.trcore.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.jetbrains.annotations.NotNull;

public class TrimCommand implements CommandExecutor {

    private final TRCore plugin;

    public TrimCommand(TRCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Bu komut sadece oyuncular tarafından kullanılabilir.");
            return true;
        }

        if (!player.hasPermission("armortrimer.use")) {
            player.sendMessage(CC.parse(plugin.getConfigManager().getTrimsConfig().getString("messages.no-perm", "&cYetkiniz yok.")));
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        FileConfiguration config = plugin.getConfigManager().getTrimsConfig();

        if (item.getType().isAir() || !(item.getItemMeta() instanceof ArmorMeta)) {
            player.sendMessage(CC.parse(config.getString("messages.no-armor", "&cElinde zırh yok!")));
            return true;
        }

        new TrimGUI(plugin, player, item).open();
        return true;
    }
}
