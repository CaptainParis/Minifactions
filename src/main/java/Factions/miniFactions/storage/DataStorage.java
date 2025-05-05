package Factions.miniFactions.storage;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.models.Clan;
import Factions.miniFactions.models.ClaimBlock;
import Factions.miniFactions.models.ClanDoor;
import Factions.miniFactions.models.CoreBlock;
import Factions.miniFactions.models.DefenseBlock;
import Factions.miniFactions.spatial.BlockType;
import Factions.miniFactions.spatial.SpatialIndexManager;
import Factions.miniFactions.spatial.SpatiallyIndexable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class DataStorage {

    private final MiniFactions plugin;
    private final Map<String, Clan> clans = new HashMap<>();
    private final Map<Location, CoreBlock> coreBlocks = new HashMap<>();
    private final Map<Location, DefenseBlock> defenseBlocks = new HashMap<>();
    private final Map<Location, ClaimBlock> claimBlocks = new HashMap<>();
    private final Map<Location, ClanDoor> clanDoors = new HashMap<>();

    // Spatial index manager for efficient spatial queries
    private SpatialIndexManager spatialIndexManager;

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

        // Initialize spatial index manager
        spatialIndexManager = new SpatialIndexManager(plugin);
    }

    /**
     * Load all data from storage
     */
    public void loadData() {
        loadClans();
        loadCoreBlocks();
        loadDefenseBlocks();
        loadClaimBlocks();
        loadClanDoors();

        // Rebuild spatial index after loading all blocks
        rebuildSpatialIndex();
    }

    /**
     * Save all data to storage
     */
    public void saveData() {
        saveClans();
        saveCoreBlocks();
        saveDefenseBlocks();
        saveClaimBlocks();
        saveClanDoors();
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

        // Add to spatial index
        spatialIndexManager.addBlock(coreBlock);
    }

    /**
     * Remove a core block from storage
     * @param location Block location
     */
    public void removeCoreBlock(Location location) {
        coreBlocks.remove(location);

        // Remove from spatial index
        spatialIndexManager.removeBlock(location, BlockType.CORE);
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
            String materialStr = blockSection.getString("material");

            Location location = new Location(Bukkit.getWorld(world), x, y, z);
            Clan clan = clans.get(clanId);

            if (clan != null) {
                DefenseBlock defenseBlock = new DefenseBlock(location, clan, tier);
                defenseBlock.setPlacementTime(placementTime);

                // Set material if it was saved
                if (materialStr != null && !materialStr.isEmpty()) {
                    try {
                        org.bukkit.Material material = org.bukkit.Material.valueOf(materialStr);
                        defenseBlock.setMaterial(material);
                    } catch (IllegalArgumentException e) {
                        // If material is invalid, it will use the default for the tier
                        plugin.getLogger().warning("Invalid material for defense block: " + materialStr);
                    }
                }

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
            config.set(path + ".material", defenseBlock.getMaterial().toString());

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

        // Add to spatial index
        spatialIndexManager.addBlock(defenseBlock);
    }

    /**
     * Remove a defense block from storage
     * @param location Block location
     */
    public void removeDefenseBlock(Location location) {
        defenseBlocks.remove(location);

        // Remove from spatial index
        spatialIndexManager.removeBlock(location, BlockType.DEFENSE);
    }

    /**
     * Load claim blocks from storage
     */
    private void loadClaimBlocks() {
        FileConfiguration config = plugin.getConfigManager().getConfig("blocks");
        ConfigurationSection claimBlocksSection = config.getConfigurationSection("claim-blocks");

        if (claimBlocksSection == null) {
            plugin.getLogger().info("No claim blocks found in storage.");
            return;
        }

        for (String key : claimBlocksSection.getKeys(false)) {
            ConfigurationSection blockSection = claimBlocksSection.getConfigurationSection(key);
            if (blockSection == null) continue;

            String world = blockSection.getString("world");
            int x = blockSection.getInt("x");
            int y = blockSection.getInt("y");
            int z = blockSection.getInt("z");
            String clanId = blockSection.getString("clan-id");
            int level = blockSection.getInt("level");
            long lastPointGenerationTime = blockSection.getLong("last-point-generation-time");

            Location location = new Location(Bukkit.getWorld(world), x, y, z);
            Clan clan = clans.get(clanId);

            if (clan != null) {
                ClaimBlock claimBlock = new ClaimBlock(location, clan);
                claimBlock.setLevel(level);
                claimBlock.setLastPointGenerationTime(lastPointGenerationTime);
                claimBlocks.put(location, claimBlock);
                clan.addClaimBlock(claimBlock);
            }
        }

        plugin.getLogger().info("Loaded " + claimBlocks.size() + " claim blocks from storage.");
    }

    /**
     * Save claim blocks to storage
     */
    private void saveClaimBlocks() {
        FileConfiguration config = plugin.getConfigManager().getConfig("blocks");
        config.set("claim-blocks", null); // Clear existing data

        int i = 0;
        for (ClaimBlock claimBlock : claimBlocks.values()) {
            String path = "claim-blocks.block" + i;
            Location loc = claimBlock.getLocation();

            config.set(path + ".world", loc.getWorld().getName());
            config.set(path + ".x", loc.getBlockX());
            config.set(path + ".y", loc.getBlockY());
            config.set(path + ".z", loc.getBlockZ());
            config.set(path + ".clan-id", claimBlock.getClan().getId());
            config.set(path + ".level", claimBlock.getLevel());
            config.set(path + ".last-point-generation-time", claimBlock.getLastPointGenerationTime());

            i++;
        }

        plugin.getConfigManager().saveConfig("blocks");
        plugin.getLogger().info("Saved " + claimBlocks.size() + " claim blocks to storage.");
    }

    /**
     * Get all claim blocks
     * @return Map of location to ClaimBlock
     */
    public Map<Location, ClaimBlock> getClaimBlocks() {
        return claimBlocks;
    }

    /**
     * Get a claim block by location
     * @param location Block location
     * @return ClaimBlock or null if not found
     */
    public ClaimBlock getClaimBlock(Location location) {
        return claimBlocks.get(location);
    }

    /**
     * Add a claim block to storage
     * @param claimBlock ClaimBlock to add
     */
    public void addClaimBlock(ClaimBlock claimBlock) {
        claimBlocks.put(claimBlock.getLocation(), claimBlock);

        // Add to spatial index
        spatialIndexManager.addBlock(claimBlock);
    }

    /**
     * Remove a claim block from storage
     * @param location Block location
     */
    public void removeClaimBlock(Location location) {
        claimBlocks.remove(location);

        // Remove from spatial index
        spatialIndexManager.removeBlock(location, BlockType.CLAIM);
    }

    /**
     * Rebuild the spatial index from scratch
     */
    public void rebuildSpatialIndex() {
        // Clear the spatial index
        spatialIndexManager.clearIndex();

        // Add all blocks to the spatial index
        for (CoreBlock block : coreBlocks.values()) {
            spatialIndexManager.addBlock(block);
        }

        for (DefenseBlock block : defenseBlocks.values()) {
            spatialIndexManager.addBlock(block);
        }

        for (ClaimBlock block : claimBlocks.values()) {
            spatialIndexManager.addBlock(block);
        }

        for (ClanDoor door : clanDoors.values()) {
            spatialIndexManager.addBlock(door);
        }

        plugin.getLogger().info("Rebuilt spatial index with " +
                (coreBlocks.size() + defenseBlocks.size() + claimBlocks.size() + clanDoors.size()) + " blocks");
    }

    /**
     * Get blocks within a radius of a location
     * @param center Center location
     * @param radius Radius in blocks
     * @param blockType Block type, or null for all types
     * @return List of blocks within the radius
     */
    public List<SpatiallyIndexable> getBlocksInRadius(Location center, int radius, BlockType blockType) {
        return spatialIndexManager.getBlocksInRadius(center, radius, blockType);
    }

    /**
     * Get blocks in a specific direction from a location
     * @param origin Origin location
     * @param direction Direction to search
     * @param distance Maximum distance in blocks
     * @param blockType Block type, or null for all types
     * @return List of blocks in the direction
     */
    public List<SpatiallyIndexable> getBlocksInDirection(Location origin, BlockFace direction, int distance, BlockType blockType) {
        return spatialIndexManager.getBlocksInDirection(origin, direction, distance, blockType);
    }

    /**
     * Get adjacent blocks to a location
     * @param location Center location
     * @param blockType Block type, or null for all types
     * @return List of adjacent blocks
     */
    public List<SpatiallyIndexable> getAdjacentBlocks(Location location, BlockType blockType) {
        return spatialIndexManager.getAdjacentBlocks(location, blockType);
    }

    /**
     * Get the nearest block to a location
     * @param location Center location
     * @param blockType Block type, or null for all types
     * @param maxDistance Maximum search distance
     * @return Nearest block, or null if none found
     */
    public SpatiallyIndexable getNearestBlock(Location location, BlockType blockType, int maxDistance) {
        return spatialIndexManager.getNearestBlock(location, blockType, maxDistance);
    }

    /**
     * Get blocks owned by a specific clan
     * @param clan Clan
     * @param blockType Block type, or null for all types
     * @return List of blocks owned by the clan
     */
    public List<SpatiallyIndexable> getBlocksByClan(Clan clan, BlockType blockType) {
        return spatialIndexManager.getBlocksByClan(clan, blockType);
    }

    /**
     * Get core blocks within a radius of a location
     * @param center Center location
     * @param radius Radius in blocks
     * @return List of core blocks within the radius
     */
    public List<CoreBlock> getCoreBlocksInRadius(Location center, int radius) {
        return spatialIndexManager.getBlocksInRadius(center, radius, BlockType.CORE).stream()
                .map(block -> (CoreBlock) block)
                .collect(Collectors.toList());
    }

    /**
     * Get defense blocks within a radius of a location
     * @param center Center location
     * @param radius Radius in blocks
     * @return List of defense blocks within the radius
     */
    public List<DefenseBlock> getDefenseBlocksInRadius(Location center, int radius) {
        return spatialIndexManager.getBlocksInRadius(center, radius, BlockType.DEFENSE).stream()
                .map(block -> (DefenseBlock) block)
                .collect(Collectors.toList());
    }

    /**
     * Get claim blocks within a radius of a location
     * @param center Center location
     * @param radius Radius in blocks
     * @return List of claim blocks within the radius
     */
    public List<ClaimBlock> getClaimBlocksInRadius(Location center, int radius) {
        return spatialIndexManager.getBlocksInRadius(center, radius, BlockType.CLAIM).stream()
                .map(block -> (ClaimBlock) block)
                .collect(Collectors.toList());
    }

    /**
     * Get the nearest core block to a location
     * @param location Center location
     * @param maxDistance Maximum search distance
     * @return Nearest core block, or null if none found
     */
    public CoreBlock getNearestCoreBlock(Location location, int maxDistance) {
        SpatiallyIndexable block = spatialIndexManager.getNearestBlock(location, BlockType.CORE, maxDistance);
        return block != null ? (CoreBlock) block : null;
    }

    /**
     * Get the nearest defense block to a location
     * @param location Center location
     * @param maxDistance Maximum search distance
     * @return Nearest defense block, or null if none found
     */
    public DefenseBlock getNearestDefenseBlock(Location location, int maxDistance) {
        SpatiallyIndexable block = spatialIndexManager.getNearestBlock(location, BlockType.DEFENSE, maxDistance);
        return block != null ? (DefenseBlock) block : null;
    }

    /**
     * Get the nearest claim block to a location
     * @param location Center location
     * @param maxDistance Maximum search distance
     * @return Nearest claim block, or null if none found
     */
    public ClaimBlock getNearestClaimBlock(Location location, int maxDistance) {
        SpatiallyIndexable block = spatialIndexManager.getNearestBlock(location, BlockType.CLAIM, maxDistance);
        return block != null ? (ClaimBlock) block : null;
    }

    /**
     * Get the spatial index manager
     * @return SpatialIndexManager
     */
    public SpatialIndexManager getSpatialIndexManager() {
        return spatialIndexManager;
    }

    /**
     * Load clan doors from storage
     */
    private void loadClanDoors() {
        FileConfiguration config = plugin.getConfigManager().getConfig("blocks");
        ConfigurationSection clanDoorsSection = config.getConfigurationSection("clan-doors");

        if (clanDoorsSection == null) {
            plugin.getLogger().info("No clan doors found in storage.");
            return;
        }

        for (String key : clanDoorsSection.getKeys(false)) {
            ConfigurationSection doorSection = clanDoorsSection.getConfigurationSection(key);
            if (doorSection == null) continue;

            String world = doorSection.getString("world");
            int x = doorSection.getInt("x");
            int y = doorSection.getInt("y");
            int z = doorSection.getInt("z");
            String clanId = doorSection.getString("clan-id");
            int tier = doorSection.getInt("tier", 1);

            Location location = new Location(Bukkit.getWorld(world), x, y, z);
            Clan clan = clans.get(clanId);

            if (clan != null) {
                ClanDoor clanDoor = new ClanDoor(location, clan, tier);
                clanDoors.put(location, clanDoor);
                clan.addClanDoor(clanDoor);
            }
        }

        plugin.getLogger().info("Loaded " + clanDoors.size() + " clan doors from storage.");
    }

    /**
     * Save clan doors to storage
     */
    private void saveClanDoors() {
        FileConfiguration config = plugin.getConfigManager().getConfig("blocks");
        config.set("clan-doors", null); // Clear existing data

        int i = 0;
        for (ClanDoor clanDoor : clanDoors.values()) {
            String path = "clan-doors.door" + i;
            Location loc = clanDoor.getLocation();

            config.set(path + ".world", loc.getWorld().getName());
            config.set(path + ".x", loc.getBlockX());
            config.set(path + ".y", loc.getBlockY());
            config.set(path + ".z", loc.getBlockZ());
            config.set(path + ".clan-id", clanDoor.getClan().getId());
            config.set(path + ".tier", clanDoor.getTier());

            i++;
        }

        plugin.getConfigManager().saveConfig("blocks");
        plugin.getLogger().info("Saved " + clanDoors.size() + " clan doors to storage.");
    }

    /**
     * Get all clan doors
     * @return Map of location to ClanDoor
     */
    public Map<Location, ClanDoor> getClanDoors() {
        return clanDoors;
    }

    /**
     * Get a clan door by location
     * @param location Door location
     * @return ClanDoor or null if not found
     */
    public ClanDoor getClanDoor(Location location) {
        return clanDoors.get(location);
    }

    /**
     * Add a clan door to storage
     * @param clanDoor ClanDoor to add
     */
    public void addClanDoor(ClanDoor clanDoor) {
        clanDoors.put(clanDoor.getLocation(), clanDoor);

        // Add to spatial index
        spatialIndexManager.addBlock(clanDoor);
    }

    /**
     * Remove a clan door from storage
     * @param location Door location
     */
    public void removeClanDoor(Location location) {
        clanDoors.remove(location);

        // Remove from spatial index
        spatialIndexManager.removeBlock(location, BlockType.DOOR);
    }
}
