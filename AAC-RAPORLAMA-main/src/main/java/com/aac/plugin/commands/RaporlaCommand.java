package com.aac.plugin.commands;

import com.aac.plugin.AAC;
import com.aac.plugin.guis.ReportPlayerGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RaporlaCommand implements CommandExecutor {
    
    private final AAC plugin;
    
    public RaporlaCommand(AAC plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("no consol command??");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("aac.report")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("prefix") + 
                             plugin.getLanguageManager().getMessage("no-permission"));
            return true;
        }
        
        if (!plugin.getReportManager().canReport(player)) {
            long remaining = plugin.getReportManager().getRemainingCooldown(player);
            player.sendMessage(plugin.getLanguageManager().getMessage("prefix") + 
                             plugin.getLanguageManager().getMessage("report-cooldown")
                             .replace("{time}", String.valueOf(remaining)));
            return true;
        }
        
        if (args.length > 0) {
            String targetName = args[0];
            
            if (targetName.equalsIgnoreCase(player.getName())) {
                player.sendMessage(plugin.getLanguageManager().getMessage("prefix") + 
                                 plugin.getLanguageManager().getMessage("cannot-report-self"));
                return true;
            }
            
            plugin.getChatListener().addPendingReport(player, targetName);
            return true;
        }

        ReportPlayerGUI gui = new ReportPlayerGUI(plugin, player);
        plugin.getGUIListener().registerReportPlayerGUI(player, gui);
        gui.open();
        
        return true;
    }
}