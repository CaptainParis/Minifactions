package Factions.miniFactions;

import Factions.miniFactions.commands.AdminCommands;
import Factions.miniFactions.commands.ClanCommandManager;
import Factions.miniFactions.config.ConfigManager;
import Factions.miniFactions.listeners.BlockListeners;
import Factions.miniFactions.listeners.PlayerListeners;
import Factions.miniFactions.managers.ClanManager;
import Factions.miniFactions.managers.ClaimBlockGUIManager;
import Factions.miniFactions.managers.ClaimBlockVisualManager;
import Factions.miniFactions.managers.CoreBlockManager;
import Factions.miniFactions.managers.CraftingManager;
import Factions.miniFactions.managers.DefenseBlockVisualManager;
import Factions.miniFactions.managers.GUIManager;
import Factions.miniFactions.managers.OutsideBlockManager;
import Factions.miniFactions.managers.RaidManager;
import Factions.miniFactions.managers.UpkeepManager;
import Factions.miniFactions.storage.DataStorage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class MiniFactions extends JavaPlugin {

    private static MiniFactions instance;
    private ConfigManager configManager;
    private DataStorage dataStorage;
    private ClanManager clanManager;
    private CoreBlockManager coreBlockManager;
    private CraftingManager craftingManager;
    private RaidManager raidManager;
    private GUIManager guiManager;
    private ClaimBlockGUIManager claimBlockGUIManager;
    private OutsideBlockManager outsideBlockManager;
    private UpkeepManager upkeepManager;
    private AdminCommands adminCommands;

    @Override
    public void onEnable() {
        try {
            // Set instance
            instance = this;
            getLogger().info("Initializing MiniFactions plugin...");

            // Initialize config
            getLogger().info("Loading configuration...");
            configManager = new ConfigManager(this);
            configManager.loadConfigs();

            // Initialize data storage
            getLogger().info("Initializing data storage...");
            dataStorage = new DataStorage(this);
            dataStorage.initialize();

            // Initialize managers
            getLogger().info("Initializing managers...");
            initializeManagers();

            // Register commands
            getLogger().info("Registering commands...");
            registerCommands();

            // Register listeners
            getLogger().info("Registering event listeners...");
            registerEventListeners();

            // Load data
            getLogger().info("Loading saved data...");
            dataStorage.loadData();

            // Setup custom crafting recipes
            getLogger().info("Registering custom recipes...");
            craftingManager.registerRecipes();

            // Initialize visual effects for core blocks only
            getLogger().info("Setting up visual effects for core blocks...");
            Bukkit.getScheduler().runTaskLater(this, () -> {
                // Only core blocks should have holograms
                coreBlockManager.getVisualManager().updateAllTextDisplays();
            }, 40L); // Delay by 2 seconds to ensure all data is loaded

            getLogger().info("MiniFactions has been enabled successfully!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable MiniFactions: " + e.getMessage(), e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Initialize all managers
     */
    private void initializeManagers() {
        clanManager = new ClanManager(this);
        coreBlockManager = new CoreBlockManager(this);
        craftingManager = new CraftingManager(this);
        raidManager = new RaidManager(this);
        guiManager = new GUIManager(this);
        claimBlockGUIManager = new ClaimBlockGUIManager(this);
        outsideBlockManager = new OutsideBlockManager(this);
        upkeepManager = new UpkeepManager(this);
    }

    /**
     * Register all commands
     */
    private void registerCommands() {
        // Register clan commands
        ClanCommandManager commandManager = new ClanCommandManager(this);
        commandManager.registerCommands();

        // Register admin commands
        adminCommands = new AdminCommands(this);
        getCommand("admin").setExecutor(adminCommands);
        getCommand("admin").setTabCompleter(adminCommands);
    }

    /**
     * Register all event listeners
     */
    private void registerEventListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new BlockListeners(this), this);
        pm.registerEvents(new PlayerListeners(this), this);
    }

    @Override
    public void onDisable() {
        try {
            getLogger().info("Disabling MiniFactions...");

            // Save all data
            if (dataStorage != null) {
                getLogger().info("Saving data...");
                dataStorage.saveData();
            }

            // Cleanup resources
            getLogger().info("Cleaning up resources...");
            if (coreBlockManager != null) {
                coreBlockManager.cleanup();
            }
            if (upkeepManager != null) {
                upkeepManager.cleanup();
            }

            // Cleanup raid manager (cancels explosives and particle tasks)
            if (raidManager != null) {
                raidManager.cleanup();
            }

            // Cleanup outside block manager
            if (outsideBlockManager != null) {
                outsideBlockManager.cleanup();
            }

            // Unregister crafting recipes
            if (craftingManager != null) {
                craftingManager.unregisterRecipes();
            }

            // Log spatial index statistics before shutdown
            if (dataStorage != null && dataStorage.getSpatialIndexManager() != null) {
                getLogger().info("Spatial index statistics before shutdown:");
                dataStorage.getSpatialIndexManager().logStatistics();
            }

            getLogger().info("MiniFactions has been disabled successfully!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during plugin shutdown: " + e.getMessage(), e);
        } finally {
            // Clear instance reference
            instance = null;
        }
    }

    /**
     * Get the plugin instance
     * @return MiniFactions instance
     * @throws IllegalStateException if the plugin instance is not available
     */
    public static MiniFactions getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MiniFactions plugin instance is not available");
        }
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

    /**
     * Get the GUI manager
     * @return GUIManager instance
     */
    public GUIManager getGUIManager() {
        return guiManager;
    }

    /**
     * Get the claim block GUI manager
     * @return ClaimBlockGUIManager instance
     */
    public ClaimBlockGUIManager getClaimBlockGUIManager() {
        return claimBlockGUIManager;
    }

    /**
     * Get the admin commands
     * @return AdminCommands instance
     */
    public AdminCommands getAdminCommands() {
        return adminCommands;
    }

    /**
     * Get the outside block manager
     * @return OutsideBlockManager instance
     */
    public OutsideBlockManager getOutsideBlockManager() {
        return outsideBlockManager;
    }

    /**
     * Get the upkeep manager
     * @return UpkeepManager
     */
    public UpkeepManager getUpkeepManager() {
        return upkeepManager;
    }
}
