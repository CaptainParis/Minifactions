package Factions.miniFactions.config;

import Factions.miniFactions.MiniFactions;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
        // Load main config
        loadMainConfig();
        
        // Load other configs
        loadConfig("clans");
        loadConfig("blocks");
        loadConfig("crafting");
    }
    
    /**
     * Load the main config file
     */
    private void loadMainConfig() {
        if (mainConfigFile == null) {
            mainConfigFile = new File(plugin.getDataFolder(), "config.yml");
        }
        
        if (!mainConfigFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        
        mainConfig = YamlConfiguration.loadConfiguration(mainConfigFile);
        setDefaults(mainConfig);
    }
    
    /**
     * Load a specific config file
     * @param name Config name without .yml extension
     */
    private void loadConfig(String name) {
        File file = new File(plugin.getDataFolder(), name + ".yml");
        
        if (!file.exists()) {
            try {
                plugin.saveResource(name + ".yml", false);
            } catch (IllegalArgumentException e) {
                // Create empty file if not in resources
                try {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                } catch (IOException ex) {
                    plugin.getLogger().severe("Could not create " + name + ".yml: " + ex.getMessage());
                }
            }
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(name, config);
        configFiles.put(name, file);
    }
    
    /**
     * Set default values for the main config
     * @param config The config to set defaults for
     */
    private void setDefaults(FileConfiguration config) {
        // Core Block settings
        if (!config.contains("core.max-level")) config.set("core.max-level", 20);
        if (!config.contains("core.base-area")) config.set("core.base-area", 10);
        if (!config.contains("core.area-per-level")) config.set("core.area-per-level", 5);
        if (!config.contains("core.base-defense-slots")) config.set("core.base-defense-slots", 5);
        if (!config.contains("core.defense-slots-per-level")) config.set("core.defense-slots-per-level", 3);
        if (!config.contains("core.base-claim-slots")) config.set("core.base-claim-slots", 2);
        if (!config.contains("core.claim-slots-per-level")) config.set("core.claim-slots-per-level", 1);
        if (!config.contains("core.base-door-slots")) config.set("core.base-door-slots", 1);
        if (!config.contains("core.door-slots-per-level")) config.set("core.door-slots-per-level", 1);
        if (!config.contains("core.points-on-break-percentage")) config.set("core.points-on-break-percentage", 50);
        if (!config.contains("core.beacon-effect")) config.set("core.beacon-effect", true);
        
        // Claim Block settings
        if (!config.contains("claim.base-points-per-day")) config.set("claim.base-points-per-day", 100);
        if (!config.contains("claim.points-multiplier-per-upgrade")) config.set("claim.points-multiplier-per-upgrade", 1.5);
        if (!config.contains("claim.max-upgrade-level")) config.set("claim.max-upgrade-level", 5);
        
        // Defense Block settings
        if (!config.contains("defense.max-tier")) config.set("defense.max-tier", 10);
        if (!config.contains("defense.min-decay-time")) config.set("defense.min-decay-time", 24); // hours
        if (!config.contains("defense.max-decay-time")) config.set("defense.max-decay-time", 72); // hours
        
        // Raiding settings
        if (!config.contains("raiding.explosive-fuse-time")) config.set("raiding.explosive-fuse-time", 5); // seconds
        
        // Save defaults
        try {
            config.save(mainConfigFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
    }
    
    /**
     * Get the main config
     * @return Main FileConfiguration
     */
    public FileConfiguration getConfig() {
        if (mainConfig == null) {
            loadMainConfig();
        }
        return mainConfig;
    }
    
    /**
     * Get a specific config
     * @param name Config name without .yml extension
     * @return FileConfiguration for the specified config
     */
    public FileConfiguration getConfig(String name) {
        if (!configs.containsKey(name)) {
            loadConfig(name);
        }
        return configs.get(name);
    }
    
    /**
     * Save the main config
     */
    public void saveConfig() {
        if (mainConfig == null || mainConfigFile == null) {
            return;
        }
        
        try {
            getConfig().save(mainConfigFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
    }
    
    /**
     * Save a specific config
     * @param name Config name without .yml extension
     */
    public void saveConfig(String name) {
        if (!configs.containsKey(name) || !configFiles.containsKey(name)) {
            return;
        }
        
        try {
            configs.get(name).save(configFiles.get(name));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save " + name + ".yml: " + e.getMessage());
        }
    }
    
    /**
     * Save all configs
     */
    public void saveAllConfigs() {
        saveConfig();
        for (String name : configs.keySet()) {
            saveConfig(name);
        }
    }
}
