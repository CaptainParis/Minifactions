package Factions.miniFactions.storage;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.models.Clan;
import Factions.miniFactions.models.CoreBlock;
import Factions.miniFactions.models.DefenseBlock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataStorage {

    private final MiniFactions plugin;
    private final Map<String, Clan> clans = new HashMap<>();
    private final Map<Location, CoreBlock> coreBlocks = new HashMap<>();
    private final Map<Location, DefenseBlock> defenseBlocks = new HashMap<>();

    public DataStorage(MiniFactions plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize the data storage
     */
    public void initialize() {
        // Create data folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
    }

    /**
     * Load all data from storage
     */
    public void loadData() {
        loadClans();
        loadCoreBlocks();
        loadDefenseBlocks();
    }

    /**
     * Save all data to storage
     */
    public void saveData() {
        saveClans();
        saveCoreBlocks();
        saveDefenseBlocks();
    }

    /**
     * Load clans from storage
     */
    private void loadClans() {
        FileConfiguration config = plugin.getConfigManager().getConfig("clans");
        ConfigurationSection clansSection = config.getConfigurationSection("clans");

        if (clansSection == null) {
            return;
        }

        for (String clanName : clansSection.getKeys(false)) {
            ConfigurationSection clanSection = clansSection.getConfigurationSection(clanName);
            if (clanSection == null) continue;

            String id = clanSection.getString("id");
            String name = clanSection.getString("name");
            UUID leaderId = UUID.fromString(clanSection.getString("leader"));
            int points = clanSection.getInt("points");

            Clan clan = new Clan(id, name, leaderId);
            clan.setPoints(points);

            // Load members
            ConfigurationSection membersSection = clanSection.getConfigurationSection("members");
            if (membersSection != null) {
                for (String memberId : membersSection.getKeys(false)) {
                    UUID memberUUID = UUID.fromString(memberId);
                    String role = membersSection.getString(memberId);
                    clan.addMember(memberUUID, role);
                }
            }

            clans.put(id, clan);
        }

        plugin.getLogger().info("Loaded " + clans.size() + " clans from storage.");
    }

    /**
     * Save clans to storage
     */
    private void saveClans() {
        FileConfiguration config = plugin.getConfigManager().getConfig("clans");
        config.set("clans", null); // Clear existing data

        for (Clan clan : clans.values()) {
            String path = "clans." + clan.getId();
            config.set(path + ".id", clan.getId());
            config.set(path + ".name", clan.getName());
            config.set(path + ".leader", clan.getLeader().toString());
            config.set(path + ".points", clan.getPoints());

            // Save members
            for (Map.Entry<UUID, String> entry : clan.getMembers().entrySet()) {
                config.set(path + ".members." + entry.getKey().toString(), entry.getValue());
            }
        }

        plugin.getConfigManager().saveConfig("clans");
        plugin.getLogger().info("Saved " + clans.size() + " clans to storage.");
    }

    /**
     * Load core blocks from storage
     */
    private void loadCoreBlocks() {
        FileConfiguration config = plugin.getConfigManager().getConfig("blocks");
        ConfigurationSection coreBlocksSection = config.getConfigurationSection("core-blocks");

        if (coreBlocksSection == null) {
            return;
        }

        for (String key : coreBlocksSection.getKeys(false)) {
            ConfigurationSection blockSection = coreBlocksSection.getConfigurationSection(key);
            if (blockSection == null) continue;

            String world = blockSection.getString("world");
            int x = blockSection.getInt("x");
            int y = blockSection.getInt("y");
            int z = blockSection.getInt("z");
            String clanId = blockSection.getString("clan-id");
            int level = blockSection.getInt("level");

            Location location = new Location(Bukkit.getWorld(world), x, y, z);
            Clan clan = clans.get(clanId);

            if (clan != null) {
                CoreBlock coreBlock = new CoreBlock(location, clan);
                coreBlock.setLevel(level);
                coreBlocks.put(location, coreBlock);
                clan.setCoreBlock(coreBlock);
            }
        }

        plugin.getLogger().info("Loaded " + coreBlocks.size() + " core blocks from storage.");
    }

    /**
     * Save core blocks to storage
     */
    private void saveCoreBlocks() {
        FileConfiguration config = plugin.getConfigManager().getConfig("blocks");
        config.set("core-blocks", null); // Clear existing data

        int i = 0;
        for (CoreBlock coreBlock : coreBlocks.values()) {
            String path = "core-blocks.block" + i;
            Location loc = coreBlock.getLocation();

            config.set(path + ".world", loc.getWorld().getName());
            config.set(path + ".x", loc.getBlockX());
            config.set(path + ".y", loc.getBlockY());
            config.set(path + ".z", loc.getBlockZ());
            config.set(path + ".clan-id", coreBlock.getClan().getId());
            config.set(path + ".level", coreBlock.getLevel());

            i++;
        }

        plugin.getConfigManager().saveConfig("blocks");
        plugin.getLogger().info("Saved " + coreBlocks.size() + " core blocks to storage.");
    }

    /**
     * Get all clans
     * @return Map of clan ID to Clan
     */
    public Map<String, Clan> getClans() {
        return clans;
    }

    /**
     * Get a clan by ID
     * @param id Clan ID
     * @return Clan or null if not found
     */
    public Clan getClan(String id) {
        return clans.get(id);
    }

    /**
     * Get a clan by name
     * @param name Clan name
     * @return Clan or null if not found
     */
    public Clan getClanByName(String name) {
        for (Clan clan : clans.values()) {
            if (clan.getName().equalsIgnoreCase(name)) {
                return clan;
            }
        }
        return null;
    }

    /**
     * Get a clan by player UUID
     * @param playerUUID Player UUID
     * @return Clan or null if not found
     */
    public Clan getClanByPlayer(UUID playerUUID) {
        for (Clan clan : clans.values()) {
            if (clan.isMember(playerUUID)) {
                return clan;
            }
        }
        return null;
    }

    /**
     * Add a clan to storage
     * @param clan Clan to add
     */
    public void addClan(Clan clan) {
        clans.put(clan.getId(), clan);
    }

    /**
     * Remove a clan from storage
     * @param id Clan ID
     */
    public void removeClan(String id) {
        clans.remove(id);
    }

    /**
     * Get all core blocks
     * @return Map of location to CoreBlock
     */
    public Map<Location, CoreBlock> getCoreBlocks() {
        return coreBlocks;
    }

    /**
     * Get a core block by location
     * @param location Block location
     * @return CoreBlock or null if not found
     */
    public CoreBlock getCoreBlock(Location location) {
        return coreBlocks.get(location);
    }

    /**
     * Add a core block to storage
     * @param coreBlock CoreBlock to add
     */
    public void addCoreBlock(CoreBlock coreBlock) {
        coreBlocks.put(coreBlock.getLocation(), coreBlock);
    }

    /**
     * Remove a core block from storage
     * @param location Block location
     */
    public void removeCoreBlock(Location location) {
        coreBlocks.remove(location);
    }

    /**
     * Load defense blocks from storage
     */
    private void loadDefenseBlocks() {
        FileConfiguration config = plugin.getConfigManager().getConfig("blocks");
        ConfigurationSection defenseBlocksSection = config.getConfigurationSection("defense-blocks");

        if (defenseBlocksSection == null) {
            return;
        }

        for (String key : defenseBlocksSection.getKeys(false)) {
            ConfigurationSection blockSection = defenseBlocksSection.getConfigurationSection(key);
            if (blockSection == null) continue;

            String world = blockSection.getString("world");
            int x = blockSection.getInt("x");
            int y = blockSection.getInt("y");
            int z = blockSection.getInt("z");
            String clanId = blockSection.getString("clan-id");
            int tier = blockSection.getInt("tier");
            long placementTime = blockSection.getLong("placement-time");

            Location location = new Location(Bukkit.getWorld(world), x, y, z);
            Clan clan = clans.get(clanId);

            if (clan != null) {
                DefenseBlock defenseBlock = new DefenseBlock(location, clan, tier);
                defenseBlock.setPlacementTime(placementTime);
                defenseBlocks.put(location, defenseBlock);
                clan.addDefenseBlock(defenseBlock);
            }
        }

        plugin.getLogger().info("Loaded " + defenseBlocks.size() + " defense blocks from storage.");
    }

    /**
     * Save defense blocks to storage
     */
    private void saveDefenseBlocks() {
        FileConfiguration config = plugin.getConfigManager().getConfig("blocks");
        config.set("defense-blocks", null); // Clear existing data

        int i = 0;
        for (DefenseBlock defenseBlock : defenseBlocks.values()) {
            String path = "defense-blocks.block" + i;
            Location loc = defenseBlock.getLocation();

            config.set(path + ".world", loc.getWorld().getName());
            config.set(path + ".x", loc.getBlockX());
            config.set(path + ".y", loc.getBlockY());
            config.set(path + ".z", loc.getBlockZ());
            config.set(path + ".clan-id", defenseBlock.getClan().getId());
            config.set(path + ".tier", defenseBlock.getTier());
            config.set(path + ".placement-time", defenseBlock.getPlacementTime());

            i++;
        }

        plugin.getConfigManager().saveConfig("blocks");
        plugin.getLogger().info("Saved " + defenseBlocks.size() + " defense blocks to storage.");
    }

    /**
     * Get all defense blocks
     * @return Map of location to DefenseBlock
     */
    public Map<Location, DefenseBlock> getDefenseBlocks() {
        return defenseBlocks;
    }

    /**
     * Get a defense block by location
     * @param location Block location
     * @return DefenseBlock or null if not found
     */
    public DefenseBlock getDefenseBlock(Location location) {
        return defenseBlocks.get(location);
    }

    /**
     * Add a defense block to storage
     * @param defenseBlock DefenseBlock to add
     */
    public void addDefenseBlock(DefenseBlock defenseBlock) {
        defenseBlocks.put(defenseBlock.getLocation(), defenseBlock);
    }

    /**
     * Remove a defense block from storage
     * @param location Block location
     */
    public void removeDefenseBlock(Location location) {
        defenseBlocks.remove(location);
    }
}
