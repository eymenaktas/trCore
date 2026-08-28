package com.trcore.utils;

import com.trcore.TRCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CC {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static Component get(String path, String... replacements) {
        String message = TRCore.getInstance().getConfigManager().getMessages().getString(path);

        if (message == null) message = TRCore.getInstance().getConfig().getString(path);
        if (message == null) return Component.text("Mesaj yok: " + path);

        if (!path.startsWith("admin.playerinfo") && !path.startsWith("actionbar.message")) {
            String prefix = TRCore.getInstance().getConfig().getString("prefix", "");
            message = prefix + message;
        }

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String target = replacements[i];
                String replacement = replacements[i + 1];
                if (replacement != null) {
                    message = message.replace(target, replacement);
                }
            }
        }

        return parse(message);
    }

    /** Ham string döndür (parse etmeden) — koordinat replace'leri için */
    public static String getRaw(String path) {
        String message = TRCore.getInstance().getConfigManager().getMessages().getString(path);
        if (message == null) message = TRCore.getInstance().getConfig().getString(path);
        if (message == null) return path;
        String prefix = TRCore.getInstance().getConfig().getString("prefix", "");
        return prefix + message;
    }

    public static Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        // 1. HEX Kodlarini Cevir (&#RRGGBB -> <#RRGGBB>)
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder(text.length() + 32);
        while (matcher.find()) {
            matcher.appendReplacement(buffer, "<#" + matcher.group(1) + ">");
        }
        text = matcher.appendTail(buffer).toString();

        // 2. §/& renk kodu cevirisi (MiniMessage formatina)
        text = text
                .replace("§0", "<black>").replace("&0", "<black>")
                .replace("§1", "<dark_blue>").replace("&1", "<dark_blue>")
                .replace("§2", "<dark_green>").replace("&2", "<dark_green>")
                .replace("§3", "<dark_aqua>").replace("&3", "<dark_aqua>")
                .replace("§4", "<dark_red>").replace("&4", "<dark_red>")
                .replace("§5", "<dark_purple>").replace("&5", "<dark_purple>")
                .replace("§6", "<gold>").replace("&6", "<gold>")
                .replace("§7", "<gray>").replace("&7", "<gray>")
                .replace("§8", "<dark_gray>").replace("&8", "<dark_gray>")
                .replace("§9", "<blue>").replace("&9", "<blue>")
                .replace("§a", "<green>").replace("&a", "<green>")
                .replace("§b", "<aqua>").replace("&b", "<aqua>")
                .replace("§c", "<red>").replace("&c", "<red>")
                .replace("§d", "<light_purple>").replace("&d", "<light_purple>")
                .replace("§e", "<yellow>").replace("&e", "<yellow>")
                .replace("§f", "<white>").replace("&f", "<white>")
                .replace("§l", "<bold>").replace("&l", "<bold>")
                .replace("§m", "<strikethrough>").replace("&m", "<strikethrough>")
                .replace("§n", "<underlined>").replace("&n", "<underlined>")
                .replace("§o", "<italic>").replace("&o", "<italic>")
                .replace("§k", "<obfuscated>").replace("&k", "<obfuscated>")
                .replace("§r", "<reset>").replace("&r", "<reset>");

    // 3. Guvenli sekilde deserialize et
        return MiniMessage.miniMessage().deserialize(text);
    }

    /**
     * PlaceholderAPI gibi eklentiler için MiniMessage'ı klasik § renklerine çevirir.
     */
    public static String translate(String text) {
        if (text == null || text.isEmpty()) return "";
        
        // Önce MiniMessage -> Component -> Legacy §
        Component decompressed = parse(text);
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(decompressed);
    }

    /** Modern Sound oynatıcı (Registry-based) — Deprecation uyarılarını engeller */
    public static void playSound(Player player, String soundName) {
        playSound(player, soundName, 1.0f, 1.0f);
    }

    public static void playSound(Player player, String soundName, float volume, float pitch) {
        if (player == null || soundName == null || soundName.isEmpty()) return;

        // Folia: Ses oynatma ve konum alma işlemleri oyuncunun region thread'inde olmalıdır
        player.getScheduler().run(TRCore.getInstance(), task -> {
            try {
                // 1. Enum olarak dene (ENTITY_BAT_TAKEOFF gibi standart isimler için)
                try {
                    Sound sound = Sound.valueOf(soundName.toUpperCase());
                    player.playSound(player.getLocation(), sound, volume, pitch);
                    return;
                } catch (IllegalArgumentException ignored) {}

                // 2. String/Key olarak dene (minecraft:entity.bat.takeoff veya dots'lu format için)
                // Alt çizgi yerine nokta kullanarak modern formata çeviriyoruz
                String key = soundName.toLowerCase().replace("_", ".");
                player.playSound(player.getLocation(), key, volume, pitch);
            } catch (Exception ignored) {}
        }, null);
    }
}
