package com.trcore;

import com.trcore.commands.*;
import com.trcore.listeners.*;
import com.trcore.managers.*;
import com.trcore.utils.*;
import com.trcore.listeners.pda.*;
import com.tcoded.folialib.FoliaLib;
import com.github.retrooper.packetevents.PacketEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * TRCore - Centralized Practice Core
 * High-performance, modular, and easy to use.
 */
public class TRCore extends JavaPlugin {

    private static TRCore instance;

    // --- Utilities & API ---
    private FoliaLib foliaLib;
    private final Map<UUID, Long> guiClickCooldown = new HashMap<>();

    // --- Managers ---
    private ConfigManager configManager;
    private MenuManager menuManager;
    private TPAManager tpaManager;
    private QueueManager queueManager;
    private DuelManager duelManager;
    private CombatManager combatManager;
    private BackManager backManager;
    private CooldownManager cooldownManager;
    private ToggleManager toggleManager;
    private SettingsManager settingsManager;
    private NightVisionManager nightVisionManager;
    private CustomCommandManager customCommandManager;
    private DeathDropManager deathDropManager;
    private SpawnManager spawnManager;
    private RTPManager rtpManager;
    private TrimManager trimManager;
    private DisguiseManager disguiseManager;

    private com.trcore.elo.EloManager eloManager;
    private com.trcore.elo.RankedMatchManager rankedMatchManager;
    private com.trcore.elo.RankedQueueManager rankedQueueManager;

    // --- Listeners ---
    private DeathListener deathListener;
    private ChatListener chatListener;
    private com.trcore.listeners.features.ItemClearListener itemClearListener;
    private com.trcore.listeners.features.SpawnListener spawnListener;
    private ActionbarListener actionbarListener;
    private PlayerListener playerListener;
    private WorldChangeListener worldChangeListener;

    @Override
    public void onEnable() {
        // --- Migration (vtCore -> trCore) --- (Disabled as per user request)
        // com.trcore.utils.MigrationUtil.handleMigration(getDataFolder().getParentFile(), getLogger());

        instance = this;
        this.foliaLib = new FoliaLib(this);

        initManagers();
        registerCommands();
        registerListeners();

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new com.trcore.elo.EloPlaceholderExpansion(eloManager).register();
        }

        getLogger().info("trCore v2.0 aktif edildi!");
    }

    @Override
    public void onDisable() {
        if (tpaManager != null) tpaManager.stopAllEvents();
        if (toggleManager != null) toggleManager.saveData();
        if (nightVisionManager != null) nightVisionManager.saveData();
        if (eloManager != null) eloManager.saveAllData();
        getLogger().info("trCore devre disi birakildi!");
    }

    // ------------------------------------------------------------------
    // Centralized API Methods (Facade Pattern)
    // ------------------------------------------------------------------

    public static TRCore get() { return instance; }
    public static TRCore getInstance() { return instance; }

    /** Parses MiniMessage or Legacy colors into Component */
    public static Component comp(String text) {
        return CC.parse(text);
    }

    /** Creates an ItemBuilder instance */
    public static ItemBuilder item(Material material) {
        return new ItemBuilder(material);
    }

    /** Opens a menu for a player */
    public void openMenu(Player player, String menuId) {
        if (menuManager != null) menuManager.open(player, menuId);
    }

    /** Sends a formatted message from config */
    public void msg(CommandSender sender, String path, String... replacements) {
        if (sender instanceof Player p) {
            p.sendMessage(CC.get(path, replacements));
        } else {
            sender.sendMessage(CC.translate(CC.getRaw(path)));
        }
    }

    /** 80ms GUI Click Cooldown Check */
    public boolean checkGuiCooldown(UUID uuid) {
        long now = System.currentTimeMillis();
        long last = guiClickCooldown.getOrDefault(uuid, 0L);
        if (now - last < 80) return false;
        guiClickCooldown.put(uuid, now);
        return true;
    }

    // ------------------------------------------------------------------
    // Initialization Logic
    // ------------------------------------------------------------------

    private void initManagers() {
        configManager = new ConfigManager(this);
        configManager.load();

        cooldownManager = new CooldownManager(this);
        cooldownManager.load();

        toggleManager = new ToggleManager(this);
        toggleManager.load();

        disguiseManager = new DisguiseManager(this);
        disguiseManager.load();

        menuManager = new MenuManager(this);
        menuManager.load();

        settingsManager = new SettingsManager(this);
        nightVisionManager = new NightVisionManager(this);
        nightVisionManager.load();

        customCommandManager = new CustomCommandManager(this);
        deathDropManager = new DeathDropManager(this);
        deathListener = new DeathListener(this);
        spawnManager = new SpawnManager(this);
        rtpManager = new RTPManager(this);
        trimManager = new TrimManager(this);
        combatManager = new CombatManager(this);
        tpaManager = new TPAManager(this, menuManager);
        queueManager = new QueueManager(this);
        duelManager = new DuelManager(this, menuManager);
        backManager = new BackManager();

        eloManager = new com.trcore.elo.EloManager(this);
        eloManager.loadOnlinePlayers();
        rankedMatchManager = new com.trcore.elo.RankedMatchManager(this, eloManager);
        rankedQueueManager = new com.trcore.elo.RankedQueueManager(this, eloManager, rankedMatchManager);
    }

    private void registerCommands() {
        AdminCommands adminExecutor = new AdminCommands(this);
        registerCmd("gmc", adminExecutor);
        registerCmd("gms", adminExecutor);
        registerCmd("gmsp", adminExecutor);
        registerCmd("fly", adminExecutor);
        registerCmd("walkspeed", adminExecutor);
        registerCmd("flyspeed", adminExecutor);
        registerCmd("lightning", adminExecutor);
        registerCmd("sudo", adminExecutor);
        registerCmd("playerinfo", adminExecutor);
        registerCmd("alts", adminExecutor);

        registerCmd("clearchat", new ChatClearCommand());
        registerCmd("trcore", new TRCoreCommand(this));
        registerCmd("toggle", new ToggleCommand(this));
        registerCmd("back", new BackCommand(this));
        registerCmd("tpauto", new TPAutoCommand(this));
        registerCmd("ayarlar", new AyarlarCommand(this), "settings", "ayar");

        TPACommand tpaCmd = new TPACommand(this);
        registerCmd("tpa", tpaCmd);
        registerCmd("tpahere", tpaCmd);
        registerCmd("tpaccept", tpaCmd, "tpyes", "tpkabul");
        registerCmd("tpadeny", tpaCmd, "tpno", "tpred");
        registerCmd("tpacancel", tpaCmd);
        registerCmd("tpaevent", tpaCmd);

        registerCmd("rtpqueue", new RTPQueueCommand(this));
        registerCmd("rtpduel", new RTPDuelCommand(this));
        registerCmd("rankedqueue", new com.trcore.commands.RankedQueueCommand(this), "rtpranked", "rq");
        registerCmd("elom", new com.trcore.commands.EloCommand(this), "rank");
        registerCmd("rankedhistory", new com.trcore.commands.EloCommand(this), "rh");

        registerCmd("spawn", new SpawnCommand());
        registerCmd("setspawn", new SpawnCommand());
        registerCmd("setboxspawn", new SpawnCommand());
        registerCmd("box", new SpawnCommand());

        NightVisionCommand nvCmd = new NightVisionCommand(this);
        registerCmd("gamma", nvCmd, "nv");

        registerCmd("trim", new TrimCommand(this), "trimer");
        registerCmd("disguise", new DisguiseCommand(this));

        customCommandManager.registerAll(this);
        registerMenuCommands();
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new ConnectionListener(this, tpaManager, queueManager, duelManager), this);
        pm.registerEvents(menuManager, this);

        playerListener = new PlayerListener(this, combatManager, backManager);
        pm.registerEvents(playerListener, this);

        actionbarListener = new ActionbarListener(this);
        pm.registerEvents(actionbarListener, this);

        worldChangeListener = new WorldChangeListener(this);
        pm.registerEvents(worldChangeListener, this);

        pm.registerEvents(new CooldownListener(this), this);
        pm.registerEvents(settingsManager, this);

        chatListener = new com.trcore.listeners.ChatListener();
        pm.registerEvents(chatListener, this);

        pm.registerEvents(deathDropManager, this);
        pm.registerEvents(deathListener, this);
        pm.registerEvents(new SpectatorListener(), this);
        pm.registerEvents(new com.trcore.listeners.features.OldDeathDropListener(this), this);
        pm.registerEvents(new com.trcore.elo.RankedMatchListener(eloManager, rankedMatchManager, rankedQueueManager), this);

        itemClearListener = new com.trcore.listeners.features.ItemClearListener(this);
        pm.registerEvents(itemClearListener, this);

        spawnListener = new com.trcore.listeners.features.SpawnListener(this);
        pm.registerEvents(spawnListener, this);

        PacketEvents.getAPI().getEventManager()
                .registerListener(new OutgoingPlayerDeathListener(this, deathListener));
    }

    private void registerMenuCommands() {
        if (menuManager == null) return;
        menuManager.getCommandMenuMap().forEach((cmdName, menuId) -> {
            boolean alreadyRegistered = getServer().getCommandMap()
                    .getKnownCommands().containsKey(cmdName.toLowerCase());
            if (!alreadyRegistered) {
                final String finalCmdName = cmdName;
                final String finalMenuId = menuId;
                registerCmd(cmdName, (sender, cmd, label, args) -> {
                    if (!(sender instanceof Player player)) return true;
                    if (finalCmdName.equalsIgnoreCase("rtp") && toggleManager.isQuickRtpEnabled(player.getUniqueId())) {
                        RTPManager rtp = rtpManager;
                        if (rtp == null || rtp.getWorlds().isEmpty()) {
                            player.sendMessage(CC.get("rtp.no-world", "%world%", "random"));
                            return true;
                        }
                        java.util.List<String> queueWorlds = getConfig().getStringList("rtp-queue.world");
                        java.util.List<String> validWorlds = new java.util.ArrayList<>();
                        for (String w : queueWorlds) {
                            if (rtp.hasWorld(w)) validWorlds.add(w);
                        }
                        if (validWorlds.isEmpty()) validWorlds = new java.util.ArrayList<>(rtp.getWorlds().keySet());
                        String randomWorld = validWorlds.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(validWorlds.size()));
                        player.sendMessage(CC.get("rtp.searching"));
                        rtp.teleport(player, randomWorld, dest -> {
                            if (dest == null) {
                                player.sendMessage(CC.get("rtp.fail"));
                            } else {
                                String msg = CC.getRaw("rtp.success")
                                        .replace("%x%", String.valueOf(dest.getBlockX()))
                                        .replace("%y%", String.valueOf(dest.getBlockY()))
                                        .replace("%z%", String.valueOf(dest.getBlockZ()));
                                player.sendMessage(CC.parse(msg));
                            }
                        });
                        return true;
                    }
                    String mid = menuManager.getMenuIdForCommand(label);
                    if (mid != null) menuManager.open(player, mid);
                    return true;
                });
            }
        });
    }

    private void registerCmd(String name, CommandExecutor executor, String... aliases) {
        org.bukkit.command.Command cmd = new org.bukkit.command.Command(name) {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
                return executor.onCommand(sender, this, commandLabel, args);
            }

            @Override
            public @NotNull java.util.List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
                if (executor instanceof org.bukkit.command.TabCompleter tc) {
                    java.util.List<String> completions = tc.onTabComplete(sender, this, alias, args);
                    if (completions != null) return completions;
                }
                return super.tabComplete(sender, alias, args);
            }
        };
        if (aliases != null && aliases.length > 0) cmd.setAliases(java.util.Arrays.asList(aliases));
        getServer().getCommandMap().register(getName().toLowerCase(), cmd);
    }

    // --- Getters ---
    public FoliaLib getFoliaLib() { return foliaLib; }
    public ConfigManager getConfigManager() { return configManager; }
    public MenuManager getMenuManager() { return menuManager; }
    public TPAManager getTpaManager() { return tpaManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public DuelManager getDuelManager() { return duelManager; }
    public CombatManager getCombatManager() { return combatManager; }
    public BackManager getBackManager() { return backManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public ToggleManager getToggleManager() { return toggleManager; }
    public SettingsManager getSettingsManager() { return settingsManager; }
    public NightVisionManager getNightVisionManager() { return nightVisionManager; }
    public CustomCommandManager getCustomCommandManager() { return customCommandManager; }
    public DeathDropManager getDeathDropManager() { return deathDropManager; }
    public SpawnManager getSpawnManager() { return spawnManager; }
    public RTPManager getRTPManager() { return rtpManager; }
    public TrimManager getTrimManager() { return trimManager; }
    public DisguiseManager getDisguiseManager() { return disguiseManager; }
    public com.trcore.elo.EloManager getEloManager() { return eloManager; }
    public com.trcore.elo.RankedQueueManager getRankedQueueManager() { return rankedQueueManager; }
    public com.trcore.elo.RankedMatchManager getRankedMatchManager() { return rankedMatchManager; }

    public ChatListener getChatListener() { return chatListener; }
    public com.trcore.listeners.features.ItemClearListener getItemClearListener() { return itemClearListener; }
    public com.trcore.listeners.features.SpawnListener getSpawnListener() { return spawnListener; }
    public ActionbarListener getActionbarListener() { return actionbarListener; }
    public PlayerListener getPlayerListener() { return playerListener; }
    public WorldChangeListener getWorldChangeListener() { return worldChangeListener; }
}
