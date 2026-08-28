package com.aac.plugin.commands;

import com.aac.plugin.AAC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AACSystemCommand implements CommandExecutor, TabCompleter {
    
    private final AAC plugin;
    
    public AACSystemCommand(AAC plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("commands.aacsystem.usage"));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            case "help":
                return handleHelp(sender);
            case "alerts":
                return handleAlerts(sender);
            default:
                sender.sendMessage(plugin.getLanguageManager().getMessage("commands.aacsystem.unknown-subcommand"));
                return true;
        }
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("aac.system.reload")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("commands.no-permission"));
            return true;
        }
        
        try {
            plugin.getConfigManager().reloadConfig();
            
            plugin.getLanguageManager().reloadLanguages();
            
            sender.sendMessage(plugin.getLanguageManager().getMessage("commands.aacsystem.reload.success"));
            plugin.getLogger().info("AAC configuration and language files reloaded by" + sender.getName());
            
        } catch (Exception e) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("commands.aacsystem.reload.error"));
            plugin.getLogger().severe("Error reloading AAC configyml " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
    
    private boolean handleHelp(CommandSender sender) {
        List<String> helpMessages = Arrays.asList(
            plugin.getLanguageManager().getMessage("commands.aacsystem.help.header"),
            plugin.getLanguageManager().getMessage("commands.aacsystem.help.reload"),
            plugin.getLanguageManager().getMessage("commands.aacsystem.help.help"),
            plugin.getLanguageManager().getMessage("commands.aacsystem.help.alerts")
        );
        
        for (String message : helpMessages) {
            sender.sendMessage(message);
        }
        
        return true;
    }
    
    private boolean handleAlerts(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("commands.aacsystem.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("aac.reports.view")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("commands.no-permission"));
            return true;
        }
        
        boolean currentlyEnabled = plugin.getReportManager().isAlertsEnabled(player);
        boolean nextState = !currentlyEnabled;
        plugin.getReportManager().setAlertsEnabled(player, nextState);
        
        String key = nextState ? "commands.aacsystem.alerts.enabled" : "commands.aacsystem.alerts.disabled";
        sender.sendMessage(plugin.getLanguageManager().getMessage(key));
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "help", "alerts");
            String input = args[0].toLowerCase();
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {
                    completions.add(subCommand);
                }
            }
        }
        
        return completions;
    }
}
