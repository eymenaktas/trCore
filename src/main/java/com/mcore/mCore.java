package com.mcore;

import com.mcore.commands.*;
import com.mcore.listeners.*;
import com.mcore.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

public class mCore extends JavaPlugin {

    private static mCore instance;
    private ConfigManager configManager;
    private MenuManager menuManager;
    private TPAManager tpaManager;
    private QueueManager queueManager;
    private DuelManager duelManager;
    private CombatManager combatManager;
    private BackManager backManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Config ve Dosyalar (ÖNCE BU)
        configManager = new ConfigManager(this);
        configManager.load(); // Menü dosyalarını oluşturur

        // 2. Menü Yöneticisi (Dosyalar oluştuktan SONRA)
        menuManager = new MenuManager(this);
        menuManager.load();

        // 3. Diğer Yöneticiler
        combatManager = new CombatManager(this);
        tpaManager = new TPAManager(this, menuManager);
        queueManager = new QueueManager(this);
        duelManager = new DuelManager(this, menuManager);
        backManager = new BackManager();

        // 4. Komutlar
        AdminCommands adminExecutor = new AdminCommands(this);
        getCommand("gmc").setExecutor(adminExecutor);
        getCommand("gms").setExecutor(adminExecutor);
        getCommand("gmsp").setExecutor(adminExecutor);
        getCommand("fly").setExecutor(adminExecutor);
        getCommand("walkspeed").setExecutor(adminExecutor);
        getCommand("flyspeed").setExecutor(adminExecutor);
        getCommand("lightning").setExecutor(adminExecutor);
        getCommand("sudo").setExecutor(adminExecutor);
        getCommand("playerinfo").setExecutor(adminExecutor);
        getCommand("alts").setExecutor(adminExecutor);

        getCommand("clearchat").setExecutor(new ChatClearCommand());
        getCommand("mcore").setExecutor(new MCoreCommand(this));
        getCommand("back").setExecutor(new BackCommand(backManager));

        // TPA & RTP
        TPACommand tpaCmd = new TPACommand(this, tpaManager);
        getCommand("tpa").setExecutor(tpaCmd);
        getCommand("tpahere").setExecutor(tpaCmd);
        getCommand("tpacancel").setExecutor(tpaCmd);
        getCommand("tpaevent").setExecutor(tpaCmd);

        getCommand("rtpqueue").setExecutor(new RTPQueueCommand(queueManager));
        getCommand("rtpduel").setExecutor(new RTPDuelCommand(duelManager));

        // 5. Listenerlar
        getServer().getPluginManager().registerEvents(new ConnectionListener(this, tpaManager, queueManager, duelManager), this);
        getServer().getPluginManager().registerEvents(menuManager, this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this, combatManager, backManager), this);
        getServer().getPluginManager().registerEvents(new ActionbarListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldChangeListener(this), this);

        getLogger().info("mCore v4.5 aktif edildi!");
    }

    @Override
    public void onDisable() {
        if (tpaManager != null) tpaManager.stopAllEvents();
        getLogger().info("mCore devre disi birakildi!");
    }

    public static mCore getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
}