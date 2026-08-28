package com.trcore.base;

import com.trcore.TRCore;
import com.trcore.utils.CC;
import com.trcore.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.Listener;

public abstract class BaseListener implements Listener {

    protected final TRCore plugin;

    public BaseListener() {
        this.plugin = TRCore.get();
    }

    protected Component comp(String text) { return TRCore.comp(text); }
    protected ItemBuilder item(Material mat) { return TRCore.item(mat); }
}
