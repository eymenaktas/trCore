package com.aac.plugin.commands;

import com.aac.plugin.AAC;
import com.aac.plugin.guis.AACWatchGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AACWatchCommand implements CommandExecutor {
    
    private final AAC plugin;
    
    public AACWatchCommand(AAC plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("no console aac commands ?");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("aac.watch")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("prefix") + 
                             plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }
        
        AACWatchGUI gui = new AACWatchGUI(plugin, player);
        plugin.getGUIListener().registerAACWatchGUI(player, gui);
        gui.open();
        
        return true;
    }
}