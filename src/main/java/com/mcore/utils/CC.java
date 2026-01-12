//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.mcore.utils;

import com.mcore.mCore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;

public class CC {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static Component get(String path, String... replacements) {
        String message = mCore.getInstance().getConfigManager().getMessages().getString(path);
        if (message == null) {
            return Component.text("Mesaj bulunamadı: " + path);
        } else {
            if (!path.startsWith("admin.playerinfo")) {
                String prefix = mCore.getInstance().getConfig().getString("prefix", "");
                message = prefix + message;
            }

            for(int i = 0; i < replacements.length; i += 2) {
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
    }

    public static Component parse(String text) {
        text = ChatColor.translateAlternateColorCodes('&', text);
        text = translateHexColorCodes(text);
        return MiniMessage.miniMessage().deserialize(text);
    }

    private static String translateHexColorCodes(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder(message.length() + 32);

        while(matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, "<#" + group + ">");
        }

        return matcher.appendTail(buffer).toString();
    }
}
