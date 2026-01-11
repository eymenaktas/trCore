package com.mcore.utils;
import com.mcore.mCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;

public class CC {
    private static final MiniMessage mm = MiniMessage.miniMessage();

    public static Component parse(String text) {
        if (text == null) return Component.empty();
        return mm.deserialize(text);
    }

    public static Component get(String path) {
        return get(path, null, null);
    }

    public static Component get(String path, String placeholder, String value) {
        FileConfiguration config = mCore.getInstance().getConfig();
        FileConfiguration messages = mCore.getInstance().getConfigManager().getMessages();

        String prefix = config.getString("prefix", ""); // Configden prefix'i al
        String msg = messages.getString(path);

        if (msg == null) return parse(prefix + "<red>Mesaj yok: " + path);

        String finalMsg = prefix + msg;
        if (placeholder != null && value != null) {
            finalMsg = finalMsg.replace(placeholder, value);
        }
        return mm.deserialize(finalMsg);
    }
}