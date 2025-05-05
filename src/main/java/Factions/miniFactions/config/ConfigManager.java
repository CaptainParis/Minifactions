package Factions.miniFactions.config;

import Factions.miniFactions.MiniFactions;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager {

    private final MiniFactions plugin;
    private FileConfiguration mainConfig;
    private File mainConfigFile;

    // Additional config files
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final Map<String, File> configFiles = new HashMap<>();

    public ConfigManager(MiniFactions plugin) {
        this.plugin = plugin;
    }

    /**
     * Load all configuration files
     */
    public void loadConfigs() {
        try {
            // Load main config
            loadMainConfig();

            // Load other configs
            loadConfig("clans");
            loadConfig("blocks");
            loadConfig("crafting");

            // Validate configs
            validateConfigs();

            plugin.getLogger().info("All configuration files loaded successfully");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load configuration files: " + e.getMessage(), e);
        }
    }

    /**
     * Load the main config file
     * @throws IOException if there's an error loading the config
     */
    private void loadMainConfig() throws IOException {
        if (mainConfigFile == null) {
            mainConfigFile = new File(plugin.getDataFolder(), "config.yml");
        }

        if (!mainConfigFile.exists()) {
            plugin.getDataFolder().mkdirs(); // Ensure directory exists
            plugin.saveResource("config.yml", false);
            plugin.getLogger().info("Created default config.yml");
        }

        mainConfig = YamlConfiguration.loadConfiguration(mainConfigFile);
        setDefaults(mainConfig);
    }

    /**
     * Load a specific config file
     * @param name Config name without .yml extension
     * @throws IOException if there's an error loading the config
     */
    private void loadConfig(String name) throws IOException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Config name cannot be null or empty");
        }

        File file = new File(plugin.getDataFolder(), name + ".yml");

        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs(); // Ensure directory exists
                plugin.saveResource(name + ".yml", false);
                plugin.getLogger().info("Created default " + name + ".yml");
            } catch (IllegalArgumentException e) {
                // Create empty file if not in resources
                file.getParentFile().mkdirs();
                if (file.createNewFile()) {
                    plugin.getLogger().info("Created empty " + name + ".yml");
                } else {
                    throw new IOException("Could not create " + name + ".yml");
                }
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(name, config);
        configFiles.put(name, file);
        plugin.getLogger().info("Loaded " + name + ".yml");
    }

    /**
     * Set default values for the main config
     * @param config The config to set defaults for
     * @throws IOException if there's an error saving the config
     */
    private void setDefaults(FileConfiguration config) throws IOException {
        boolean changed = false;

        // Core Block settings
        if (!config.contains("core.max-level")) { config.set("core.max-level", 20); changed = true; }
        if (!config.contains("core.base-area")) { config.set("core.base-area", 10); changed = true; }
        if (!config.contains("core.area-per-level")) { config.set("core.area-per-level", 5); changed = true; }
        if (!config.contains("core.base-defense-slots")) { config.set("core.base-defense-slots", 5); changed = true; }
        if (!config.contains("core.defense-slots-per-level")) { config.set("core.defense-slots-per-level", 3); changed = true; }
        if (!config.contains("core.base-claim-slots")) { config.set("core.base-claim-slots", 2); changed = true; }
        if (!config.contains("core.claim-slots-per-level")) { config.set("core.claim-slots-per-level", 1); changed = true; }
        if (!config.contains("core.base-door-slots")) { config.set("core.base-door-slots", 1); changed = true; }
        if (!config.contains("core.door-slots-per-level")) { config.set("core.door-slots-per-level", 1); changed = true; }
        if (!config.contains("core.points-on-break-percentage")) { config.set("core.points-on-break-percentage", 50); changed = true; }
        if (!config.contains("core.beacon-effect")) { config.set("core.beacon-effect", true); changed = true; }
        if (!config.contains("core.upkeep.enabled")) { config.set("core.upkeep.enabled", true); changed = true; }
        if (!config.contains("core.upkeep.payment-interval")) { config.set("core.upkeep.payment-interval", 24); changed = true; }
        if (!config.contains("core.outside-blocks.enabled")) { config.set("core.outside-blocks.enabled", true); changed = true; }
        if (!config.contains("core.outside-blocks.min-decay-time")) { config.set("core.outside-blocks.min-decay-time", 30); changed = true; }
        if (!config.contains("core.outside-blocks.max-decay-time")) { config.set("core.outside-blocks.max-decay-time", 120); changed = true; }
        if (!config.contains("core.outside-blocks.safe-distance")) { config.set("core.outside-blocks.safe-distance", 5); changed = true; }
        if (!config.contains("core.outside-blocks.check-interval")) { config.set("core.outside-blocks.check-interval", 5); changed = true; }

        // Claim Block settings
        if (!config.contains("claim.base-points-per-day")) { config.set("claim.base-points-per-day", 100); changed = true; }
        if (!config.contains("claim.points-multiplier-per-upgrade")) { config.set("claim.points-multiplier-per-upgrade", 1.5); changed = true; }
        if (!config.contains("claim.max-upgrade-level")) { config.set("claim.max-upgrade-level", 5); changed = true; }

        // Defense Block settings
        if (!config.contains("defense.max-tier")) { config.set("defense.max-tier", 5); changed = true; }
        if (!config.contains("defense.min-decay-time")) { config.set("defense.min-decay-time", 24); changed = true; } // hours
        if (!config.contains("defense.max-decay-time")) { config.set("defense.max-decay-time", 72); changed = true; } // hours

        // Raiding settings
        if (!config.contains("raiding.explosive-fuse-time")) { config.set("raiding.explosive-fuse-time", 5); changed = true; } // seconds

        // PvP settings
        if (!config.contains("pvp.kill-points")) { config.set("pvp.kill-points", 50); changed = true; }
        if (!config.contains("pvp.death-penalty")) { config.set("pvp.death-penalty", 25); changed = true; }

        // Save defaults if changes were made
        if (changed) {
            config.save(mainConfigFile);
            plugin.getLogger().info("Updated config.yml with default values");
        }
    }

    /**
     * Get the main config
     * @return Main FileConfiguration
     */
    public FileConfiguration getConfig() {
        if (mainConfig == null) {
            try {
                loadMainConfig();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load main config: " + e.getMessage(), e);
                // Return empty config as fallback
                return new YamlConfiguration();
            }
        }
        return mainConfig;
    }

    /**
     * Get a specific config
     * @param name Config name without .yml extension
     * @return FileConfiguration for the specified config
     */
    public FileConfiguration getConfig(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Config name cannot be null or empty");
        }

        if (!configs.containsKey(name)) {
            try {
                loadConfig(name);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load config " + name + ": " + e.getMessage(), e);
                // Return empty config as fallback
                return new YamlConfiguration();
            }
        }
        return configs.get(name);
    }

    /**
     * Save the main config
     * @return true if saved successfully, false otherwise
     */
    public boolean saveConfig() {
        if (mainConfig == null || mainConfigFile == null) {
            plugin.getLogger().warning("Cannot save main config: config or file is null");
            return false;
        }

        try {
            getConfig().save(mainConfigFile);
            plugin.getLogger().info("Saved config.yml");
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config.yml: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Save a specific config
     * @param name Config name without .yml extension
     * @return true if saved successfully, false otherwise
     */
    public boolean saveConfig(String name) {
        if (name == null || name.trim().isEmpty()) {
            plugin.getLogger().warning("Cannot save config: name is null or empty");
            return false;
        }

        if (!configs.containsKey(name) || !configFiles.containsKey(name)) {
            plugin.getLogger().warning("Cannot save " + name + ".yml: config or file not found");
            return false;
        }

        try {
            configs.get(name).save(configFiles.get(name));
            plugin.getLogger().info("Saved " + name + ".yml");
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + name + ".yml: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Save all configs
     * @return true if all configs were saved successfully, false if any failed
     */
    public boolean saveAllConfigs() {
        boolean success = saveConfig();

        for (String name : configs.keySet()) {
            if (!saveConfig(name)) {
                success = false;
            }
        }

        return success;
    }

    /**
     * Validate all configs to ensure they have required values
     */
    private void validateConfigs() {
        // Validate main config
        FileConfiguration config = getConfig();

        // Check critical settings
        if (config.getInt("core.max-level", -1) <= 0) {
            plugin.getLogger().warning("Invalid core.max-level in config.yml: must be greater than 0");
        }

        if (config.getInt("defense.max-tier", -1) <= 0) {
            plugin.getLogger().warning("Invalid defense.max-tier in config.yml: must be greater than 0");
        }

        if (config.getInt("claim.max-upgrade-level", -1) <= 0) {
            plugin.getLogger().warning("Invalid claim.max-upgrade-level in config.yml: must be greater than 0");
        }

        // Check for core level configurations
        int maxLevel = config.getInt("core.max-level", 20);
        for (int i = 1; i <= maxLevel; i++) {
            if (!config.contains("core.levels." + i + ".area")) {
                plugin.getLogger().warning("Missing core.levels." + i + ".area in config.yml");
            }
            if (!config.contains("core.levels." + i + ".defense-slots")) {
                plugin.getLogger().warning("Missing core.levels." + i + ".defense-slots in config.yml");
            }
            if (!config.contains("core.levels." + i + ".claim-slots")) {
                plugin.getLogger().warning("Missing core.levels." + i + ".claim-slots in config.yml");
            }
        }
    }
}
