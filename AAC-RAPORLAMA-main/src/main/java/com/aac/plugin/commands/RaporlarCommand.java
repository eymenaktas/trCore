package com.aac.plugin.commands;

import com.aac.plugin.AAC;
import com.aac.plugin.guis.ReportsGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RaporlarCommand implements CommandExecutor {
    
    private final AAC plugin;
    
    public RaporlarCommand(AAC plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("no console command ??");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("aac.reports.view")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("prefix") + 
                             plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }
        
        ReportsGUI gui = new ReportsGUI(plugin, player);
        plugin.getGUIListener().registerReportsGUI(player, gui);
        gui.open();
        
        return true;
    }
}