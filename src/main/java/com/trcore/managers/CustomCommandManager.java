package com.trcore.managers;

import com.trcore.TRCore;
import com.trcore.utils.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * config.yml'daki custom-commands bölümünü okuyarak
 * dinamik komutlar (ör: /discord) kaydeder.
 *
 * Config yapısı:
 * custom-commands:
 * discord:
 * aliases:
 * - "dc"
 * lines:
 * - "<newline><#5865F2><bold>Discord</bold></#5865F2>"
 * - "<click:open_url:'https://discord.gg/...'>[Katıl]</click>"
 */
public class CustomCommandManager {

    private final TRCore plugin;
    // Komut adı â†’ mesaj satırları
    private final Map<String, List<String>> commands = new HashMap<>();

    public CustomCommandManager(TRCore plugin) {
        this.plugin = plugin;
    }

    /**
     * config.yml'ı okur, tüm custom komutları plugin'in komut haritasına kaydeder.
     * Yalnızca plugin ilk yüklendiğinde çağrılmalıdır.
     */
    public void registerAll(TRCore plugin) {
        commands.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("custom-commands");
        if (section == null)
            return;

        for (String cmdName : section.getKeys(false)) {
            List<String> lines = section.getStringList(cmdName + ".lines");
            List<String> aliases = section.getStringList(cmdName + ".aliases");

            // Cache'e kaydet
            commands.put(cmdName.toLowerCase(), lines);

            // Executor oluştur
            CommandExecutor executor = buildExecutor(cmdName.toLowerCase());

            // Komut nesnesi oluştur ve kaydet
            org.bukkit.command.Command cmd = new org.bukkit.command.Command(cmdName.toLowerCase()) {
                @Override
                public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                    return executor.onCommand(sender, this, label, args);
                }
            };

            if (!aliases.isEmpty()) {
                cmd.setAliases(aliases);
            }

            plugin.getServer().getCommandMap().register(plugin.getName().toLowerCase(), cmd);
        }
    }

    /**
     * Reload sonrasında config'i yeniden okur ve mevcut komutların mesajlarını
     * günceller.
     * Komutlar zaten kayıtlı olduğu için yeni register gerekmez, sadece cache
     * güncellenir.
     */
    public void reload() {
        commands.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("custom-commands");
        if (section == null)
            return;

        for (String cmdName : section.getKeys(false)) {
            List<String> lines = section.getStringList(cmdName + ".lines");
            commands.put(cmdName.toLowerCase(), lines);
        }
    }

    /**
     * Verilen komut adı için executor üretir.
     * Executor çağrıldığında o anki cache'i okur â†’ reload sonrası yeni mesaj
     * geçerli olur.
     */
    private CommandExecutor buildExecutor(String cmdName) {
        return (sender, command, label, args) -> {
            List<String> lines = commands.getOrDefault(cmdName, new ArrayList<>());
            if (lines.isEmpty())
                return true;

            // Tüm satırları birleştirerek tek bir MiniMessage stringi oluştur
            // "\n" ile birleştirince MiniMessage <newline> yerine çalışır
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                sb.append(lines.get(i));
                if (i < lines.size() - 1)
                    sb.append("<newline>");
            }

            sender.sendMessage(CC.parse(sb.toString()));
            return true;
        };
    }

    public Map<String, List<String>> getCommands() {
        return commands;
    }
}


