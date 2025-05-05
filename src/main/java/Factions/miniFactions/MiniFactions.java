package Factions.miniFactions;

import Factions.miniFactions.commands.ClanCommandManager;
import Factions.miniFactions.config.ConfigManager;
import Factions.miniFactions.listeners.BlockListeners;
import Factions.miniFactions.listeners.PlayerListeners;
import Factions.miniFactions.managers.ClanManager;
import Factions.miniFactions.managers.CoreBlockManager;
import Factions.miniFactions.managers.CraftingManager;
import Factions.miniFactions.managers.RaidManager;
import Factions.miniFactions.storage.DataStorage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class MiniFactions extends JavaPlugin {

    private static MiniFactions instance;
    private ConfigManager configManager;
    private DataStorage dataStorage;
    private ClanManager clanManager;
    private CoreBlockManager coreBlockManager;
    private CraftingManager craftingManager;
    private RaidManager raidManager;

    @Override
    public void onEnable() {
        // Set instance
        instance = this;

        // Initialize config
        configManager = new ConfigManager(this);
        configManager.loadConfigs();

        // Initialize data storage
        dataStorage = new DataStorage(this);
        dataStorage.initialize();

        // Initialize managers
        clanManager = new ClanManager(this);
        coreBlockManager = new CoreBlockManager(this);
        craftingManager = new CraftingManager(this);
        raidManager = new RaidManager(this);

        // Register commands
        ClanCommandManager commandManager = new ClanCommandManager(this);
        commandManager.registerCommands();

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new BlockListeners(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListeners(this), this);

        // Load data
        dataStorage.loadData();

        // Setup custom crafting recipes
        craftingManager.registerRecipes();

        getLogger().info("MiniFactions has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save all data
        if (dataStorage != null) {
            dataStorage.saveData();
        }

        // Cleanup resources
        if (coreBlockManager != null) {
            coreBlockManager.cleanup();
        }

        // Cancel any active explosives
        if (raidManager != null) {
            raidManager.cancelAllExplosives();
        }

        getLogger().info("MiniFactions has been disabled!");
    }

    /**
     * Get the plugin instance
     * @return MiniFactions instance
     */
    public static MiniFactions getInstance() {
        return instance;
    }

    /**
     * Get the config manager
     * @return ConfigManager instance
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Get the data storage
     * @return DataStorage instance
     */
    public DataStorage getDataStorage() {
        return dataStorage;
    }

    /**
     * Get the clan manager
     * @return ClanManager instance
     */
    public ClanManager getClanManager() {
        return clanManager;
    }

    /**
     * Get the core block manager
     * @return CoreBlockManager instance
     */
    public CoreBlockManager getCoreBlockManager() {
        return coreBlockManager;
    }

    /**
     * Get the crafting manager
     * @return CraftingManager instance
     */
    public CraftingManager getCraftingManager() {
        return craftingManager;
    }

    /**
     * Get the raid manager
     * @return RaidManager instance
     */
    public RaidManager getRaidManager() {
        return raidManager;
    }
}
