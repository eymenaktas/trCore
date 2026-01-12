//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.mcore;

import com.mcore.commands.AdminCommands;
import com.mcore.commands.TPACommand;
import com.mcore.listeners.ConnectionListener;
import com.mcore.listeners.PlayerListener;
import com.mcore.managers.CombatManager;
import com.mcore.managers.ConfigManager;
import com.mcore.managers.DuelManager;
import com.mcore.managers.MenuManager;
import com.mcore.managers.QueueManager;
import com.mcore.managers.TPAManager;
import org.bukkit.plugin.java.JavaPlugin;

public class mCore extends JavaPlugin {
    private static mCore instance;
    private ConfigManager configManager;
    private MenuManager menuManager;
    private TPAManager tpaManager;
    private QueueManager queueManager;
    private DuelManager duelManager;
    private CombatManager combatManager;

    public void onEnable() {
        instance = this;
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();
        this.configManager.loadMessages();
        this.menuManager = new MenuManager(this);
        this.combatManager = new CombatManager(this);
        this.tpaManager = new TPAManager(this, this.menuManager);
        this.queueManager = new QueueManager(this);
        this.duelManager = new DuelManager(this, this.menuManager);
        this.getCommand("admin").setExecutor(new AdminCommands(this));
        this.getCommand("tpa").setExecutor(new TPACommand(this, this.tpaManager));
        this.getCommand("tpahere").setExecutor(new TPACommand(this, this.tpaManager));
        this.getCommand("tpacancel").setExecutor(new TPACommand(this, this.tpaManager));
        this.getCommand("tpaevent").setExecutor(new TPACommand(this, this.tpaManager));
        this.getServer().getPluginManager().registerEvents(new ConnectionListener(this, this.tpaManager, this.queueManager, this.duelManager), this);
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this, this.combatManager), this);
        this.getServer().getPluginManager().registerEvents(this.menuManager, this);
        this.getLogger().info("mCore aktif edildi!");
    }

    public void onDisable() {
        if (this.tpaManager != null) {
            this.tpaManager.stopEvent(true);
        }

        this.getLogger().info("mCore devre disi birakildi!");
    }

    public static mCore getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }
}
