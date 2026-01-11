package com.mcore;

import com.mcore.commands.*;
import com.mcore.listeners.ConnectionListener;
import com.mcore.listeners.PlayerListener;
import com.mcore.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

public class mCore extends JavaPlugin {

    private static mCore instance; // CC hatası için gerekli
    private ConfigManager configManager;
    private MenuManager menuManager;
    private TPAManager tpaManager;
    private QueueManager queueManager;
    private DuelManager duelManager;
    private CombatManager combatManager; // PlayerListener için gerekli

    @Override
    public void onEnable() {
        instance = this;

        // Config
        configManager = new ConfigManager(this);
        configManager.loadConfig(); // ConfigManager içine bu metodu ekleyeceğiz
        configManager.loadMessages(); // ConfigManager içine bu metodu ekleyeceğiz

        // Managers
        // Sıralama önemlidir!
        menuManager = new MenuManager(this); // Hata çözümü: this eklendi
        combatManager = new CombatManager(this); // PlayerListener için önce bu lazım
        tpaManager = new TPAManager(this, menuManager);
        queueManager = new QueueManager(this);
        duelManager = new DuelManager(this, menuManager); // Hata çözümü: menuManager eklendi

        // Commands
        getCommand("admin").setExecutor(new AdminCommands(this));
        getCommand("tpa").setExecutor(new TPACommand(this, tpaManager));
        getCommand("tpahere").setExecutor(new TPACommand(this, tpaManager));
        getCommand("tpacancel").setExecutor(new TPACommand(this, tpaManager));
        getCommand("tpaevent").setExecutor(new TPACommand(this, tpaManager));

        // Listeners
        getServer().getPluginManager().registerEvents(new ConnectionListener(this, tpaManager, queueManager, duelManager), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this, combatManager), this); // Hata çözümü: combatManager eklendi
        getServer().getPluginManager().registerEvents(menuManager, this); // Hata çözümü: MenuManager Listener implement etmeli

        getLogger().info("mCore aktif edildi!");
    }

    @Override
    public void onDisable() {
        if (tpaManager != null) tpaManager.stopEvent(true);
        getLogger().info("mCore devre disi birakildi!");
    }

    public static mCore getInstance() { return instance; } // CC hatası için
    public ConfigManager getConfigManager() { return configManager; }
}