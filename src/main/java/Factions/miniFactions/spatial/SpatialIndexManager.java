package Factions.miniFactions.spatial;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.models.Clan;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages spatial indexing for MiniFactions blocks
 */
public class SpatialIndexManager {
    private final MiniFactions plugin;
    
    // Map of world name -> chunk coordinates -> block type -> list of blocks
    private final Map<String, Map<ChunkCoordinate, Map<BlockType, List<SpatiallyIndexable>>>> blockIndex;
    
    // Cache for recent queries
    private final SpatialQueryCache queryCache;
    
    // Array of all possible block faces for adjacent block queries
    private static final BlockFace[] ADJACENT_FACES = {
            BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH_EAST, BlockFace.NORTH_WEST,
            BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST
    };
    
    /**
     * Create a new spatial index manager
     * @param plugin MiniFactions plugin
     */
    public SpatialIndexManager(MiniFactions plugin) {
        this.plugin = plugin;
        this.blockIndex = new ConcurrentHashMap<>();
        this.queryCache = new SpatialQueryCache();
    }
    
    /**
     * Add a block to the spatial index
     * @param block Block to add
     */
    public void addBlock(SpatiallyIndexable block) {
        if (block == null || block.getLocation() == null) {
            return;
        }
        
        Location location = block.getLocation();
        String worldName = location.getWorld().getName();
        ChunkCoordinate chunkCoord = new ChunkCoordinate(location);
        BlockType blockType = block.getBlockType();
        
        // Get or create world map
        Map<ChunkCoordinate, Map<BlockType, List<SpatiallyIndexable>>> worldMap = 
                blockIndex.computeIfAbsent(worldName, k -> new ConcurrentHashMap<>());
        
        // Get or create chunk map
        Map<BlockType, List<SpatiallyIndexable>> chunkMap = 
                worldMap.computeIfAbsent(chunkCoord, k -> new ConcurrentHashMap<>());
        
        // Get or create block type list
        List<SpatiallyIndexable> blockList = 
                chunkMap.computeIfAbsent(blockType, k -> new ArrayList<>());
        
        // Add block to list if not already present
        if (!blockList.contains(block)) {
            blockList.add(block);
            
            // Clear cache since index has changed
            queryCache.clear();
        }
    }
    
    /**
     * Remove a block from the spatial index
     * @param location Block location
     * @param blockType Block type
     * @return true if removed
     */
    public boolean removeBlock(Location location, BlockType blockType) {
        if (location == null) {
            return false;
        }
        
        String worldName = location.getWorld().getName();
        ChunkCoordinate chunkCoord = new ChunkCoordinate(location);
        
        // Get world map
        Map<ChunkCoordinate, Map<BlockType, List<SpatiallyIndexable>>> worldMap = blockIndex.get(worldName);
        if (worldMap == null) {
            return false;
        }
        
        // Get chunk map
        Map<BlockType, List<SpatiallyIndexable>> chunkMap = worldMap.get(chunkCoord);
        if (chunkMap == null) {
            return false;
        }
        
        // Get block type list
        List<SpatiallyIndexable> blockList = chunkMap.get(blockType);
        if (blockList == null) {
            return false;
        }
        
        // Find and remove block
        boolean removed = blockList.removeIf(block -> 
                block.getLocation().getBlockX() == location.getBlockX() &&
                block.getLocation().getBlockY() == location.getBlockY() &&
                block.getLocation().getBlockZ() == location.getBlockZ());
        
        // Clean up empty lists and maps
        if (blockList.isEmpty()) {
            chunkMap.remove(blockType);
            
            if (chunkMap.isEmpty()) {
                worldMap.remove(chunkCoord);
                
                if (worldMap.isEmpty()) {
                    blockIndex.remove(worldName);
                }
            }
        }
        
        // Clear cache if block was removed
        if (removed) {
            queryCache.clear();
        }
        
        return removed;
    }
    
    /**
     * Get a block at a specific location
     * @param location Block location
     * @param blockType Block type, or null for any type
     * @return Block at location, or null if not found
     */
    public SpatiallyIndexable getBlockAt(Location location, BlockType blockType) {
        if (location == null) {
            return null;
        }
        
        // Check cache first
        SpatiallyIndexable cachedBlock = queryCache.getBlockAt(location, blockType);
        if (cachedBlock != null) {
            return cachedBlock;
        }
        
        String worldName = location.getWorld().getName();
        ChunkCoordinate chunkCoord = new ChunkCoordinate(location);
        
        // Get world map
        Map<ChunkCoordinate, Map<BlockType, List<SpatiallyIndexable>>> worldMap = blockIndex.get(worldName);
        if (worldMap == null) {
            return null;
        }
        
        // Get chunk map
        Map<BlockType, List<SpatiallyIndexable>> chunkMap = worldMap.get(chunkCoord);
        if (chunkMap == null) {
            return null;
        }
        
        // If block type is specified, check only that type
        if (blockType != null) {
            List<SpatiallyIndexable> blockList = chunkMap.get(blockType);
            if (blockList == null) {
                return null;
            }
            
            // Find block at exact location
            for (SpatiallyIndexable block : blockList) {
                Location blockLoc = block.getLocation();
                if (blockLoc.getBlockX() == location.getBlockX() &&
                    blockLoc.getBlockY() == location.getBlockY() &&
                    blockLoc.getBlockZ() == location.getBlockZ()) {
                    
                    // Cache result
                    queryCache.cacheBlockAt(location, blockType, block);
                    return block;
                }
            }
        } else {
            // Check all block types
            for (Map.Entry<BlockType, List<SpatiallyIndexable>> entry : chunkMap.entrySet()) {
                List<SpatiallyIndexable> blockList = entry.getValue();
                
                // Find block at exact location
                for (SpatiallyIndexable block : blockList) {
                    Location blockLoc = block.getLocation();
                    if (blockLoc.getBlockX() == location.getBlockX() &&
                        blockLoc.getBlockY() == location.getBlockY() &&
                        blockLoc.getBlockZ() == location.getBlockZ()) {
                        
                        // Cache result
                        queryCache.cacheBlockAt(location, block.getBlockType(), block);
                        return block;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get blocks in a specific chunk
     * @param world World
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @param blockType Block type, or null for all types
     * @return List of blocks in the chunk
     */
    public List<SpatiallyIndexable> getBlocksInChunk(World world, int chunkX, int chunkZ, BlockType blockType) {
        if (world == null) {
            return Collections.emptyList();
        }
        
        ChunkCoordinate chunkCoord = new ChunkCoordinate(chunkX, chunkZ);
        
        // Check cache first
        List<SpatiallyIndexable> cachedBlocks = queryCache.getBlocksInChunk(world.getName(), chunkCoord, blockType);
        if (cachedBlocks != null) {
            return cachedBlocks;
        }
        
        String worldName = world.getName();
        
        // Get world map
        Map<ChunkCoordinate, Map<BlockType, List<SpatiallyIndexable>>> worldMap = blockIndex.get(worldName);
        if (worldMap == null) {
            return Collections.emptyList();
        }
        
        // Get chunk map
        Map<BlockType, List<SpatiallyIndexable>> chunkMap = worldMap.get(chunkCoord);
        if (chunkMap == null) {
            return Collections.emptyList();
        }
        
        List<SpatiallyIndexable> result = new ArrayList<>();
        
        // If block type is specified, get only that type
        if (blockType != null) {
            List<SpatiallyIndexable> blockList = chunkMap.get(blockType);
            if (blockList != null) {
                result.addAll(blockList);
            }
        } else {
            // Get all block types
            for (List<SpatiallyIndexable> blockList : chunkMap.values()) {
                result.addAll(blockList);
            }
        }
        
        // Cache result
        queryCache.cacheBlocksInChunk(world.getName(), chunkCoord, blockType, result);
        
        return result;
    }
    
    /**
     * Get blocks within a radius of a location
     * @param center Center location
     * @param radius Radius in blocks
     * @param blockType Block type, or null for all types
     * @return List of blocks within the radius
     */
    public List<SpatiallyIndexable> getBlocksInRadius(Location center, int radius, BlockType blockType) {
        if (center == null || radius < 0) {
            return Collections.emptyList();
        }
        
        // Check cache first
        List<SpatiallyIndexable> cachedBlocks = queryCache.getBlocksInRadius(center, radius, blockType);
        if (cachedBlocks != null) {
            return cachedBlocks;
        }
        
        String worldName = center.getWorld().getName();
        ChunkCoordinate centerChunk = new ChunkCoordinate(center);
        
        // Calculate chunk radius (add 1 to ensure we cover all blocks)
        int chunkRadius = (radius >> 4) + 1;
        
        // Get all chunks in radius
        List<SpatiallyIndexable> result = new ArrayList<>();
        double radiusSquared = radius * radius;
        
        // Get world map
        Map<ChunkCoordinate, Map<BlockType, List<SpatiallyIndexable>>> worldMap = blockIndex.get(worldName);
        if (worldMap == null) {
            return Collections.emptyList();
        }
        
        // Check all chunks in radius
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                ChunkCoordinate chunkCoord = new ChunkCoordinate(centerChunk.getX() + dx, centerChunk.getZ() + dz);
                
                // Get chunk map
                Map<BlockType, List<SpatiallyIndexable>> chunkMap = worldMap.get(chunkCoord);
                if (chunkMap == null) {
                    continue;
                }
                
                // If block type is specified, get only that type
                if (blockType != null) {
                    List<SpatiallyIndexable> blockList = chunkMap.get(blockType);
                    if (blockList != null) {
                        // Check each block's distance
                        for (SpatiallyIndexable block : blockList) {
                            if (block.getLocation().distanceSquared(center) <= radiusSquared) {
                                result.add(block);
                            }
                        }
                    }
                } else {
                    // Get all block types
                    for (List<SpatiallyIndexable> blockList : chunkMap.values()) {
                        // Check each block's distance
                        for (SpatiallyIndexable block : blockList) {
                            if (block.getLocation().distanceSquared(center) <= radiusSquared) {
                                result.add(block);
                            }
                        }
                    }
                }
            }
        }
        
        // Cache result
        queryCache.cacheBlocksInRadius(center, radius, blockType, result);
        
        return result;
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
        if (origin == null || direction == null || distance <= 0) {
            return Collections.emptyList();
        }
        
        // Check cache first
        List<SpatiallyIndexable> cachedBlocks = queryCache.getBlocksInDirection(origin, direction, distance, blockType);
        if (cachedBlocks != null) {
            return cachedBlocks;
        }
        
        // Get direction vector
        int dx = direction.getModX();
        int dy = direction.getModY();
        int dz = direction.getModZ();
        
        // If direction is not a unit vector, normalize it
        if (dx != 0 || dy != 0 || dz != 0) {
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length > 0) {
                dx = (int) Math.round(dx / length);
                dy = (int) Math.round(dy / length);
                dz = (int) Math.round(dz / length);
            }
        }
        
        List<SpatiallyIndexable> result = new ArrayList<>();
        World world = origin.getWorld();
        
        // Check each block in the direction
        for (int i = 1; i <= distance; i++) {
            int x = origin.getBlockX() + dx * i;
            int y = origin.getBlockY() + dy * i;
            int z = origin.getBlockZ() + dz * i;
            
            Location loc = new Location(world, x, y, z);
            SpatiallyIndexable block = getBlockAt(loc, blockType);
            
            if (block != null) {
                result.add(block);
            }
        }
        
        // Cache result
        queryCache.cacheBlocksInDirection(origin, direction, distance, blockType, result);
        
        return result;
    }
    
    /**
     * Get adjacent blocks to a location
     * @param location Center location
     * @param blockType Block type, or null for all types
     * @return List of adjacent blocks
     */
    public List<SpatiallyIndexable> getAdjacentBlocks(Location location, BlockType blockType) {
        if (location == null) {
            return Collections.emptyList();
        }
        
        // Check cache first
        List<SpatiallyIndexable> cachedBlocks = queryCache.getAdjacentBlocks(location, blockType);
        if (cachedBlocks != null) {
            return cachedBlocks;
        }
        
        List<SpatiallyIndexable> result = new ArrayList<>();
        World world = location.getWorld();
        
        // Check each adjacent block
        for (BlockFace face : ADJACENT_FACES) {
            int x = location.getBlockX() + face.getModX();
            int y = location.getBlockY() + face.getModY();
            int z = location.getBlockZ() + face.getModZ();
            
            Location loc = new Location(world, x, y, z);
            SpatiallyIndexable block = getBlockAt(loc, blockType);
            
            if (block != null) {
                result.add(block);
            }
        }
        
        // Cache result
        queryCache.cacheAdjacentBlocks(location, blockType, result);
        
        return result;
    }
    
    /**
     * Get the nearest block to a location
     * @param location Center location
     * @param blockType Block type, or null for all types
     * @param maxDistance Maximum search distance
     * @return Nearest block, or null if none found
     */
    public SpatiallyIndexable getNearestBlock(Location location, BlockType blockType, int maxDistance) {
        if (location == null || maxDistance <= 0) {
            return null;
        }
        
        // Check cache first
        SpatiallyIndexable cachedBlock = queryCache.getNearestBlock(location, blockType, maxDistance);
        if (cachedBlock != null) {
            return cachedBlock;
        }
        
        // Get all blocks in radius
        List<SpatiallyIndexable> blocksInRadius = getBlocksInRadius(location, maxDistance, blockType);
        if (blocksInRadius.isEmpty()) {
            return null;
        }
        
        // Find nearest block
        SpatiallyIndexable nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;
        
        for (SpatiallyIndexable block : blocksInRadius) {
            double distanceSquared = block.getLocation().distanceSquared(location);
            if (distanceSquared < nearestDistanceSquared) {
                nearest = block;
                nearestDistanceSquared = distanceSquared;
            }
        }
        
        // Cache result
        if (nearest != null) {
            queryCache.cacheNearestBlock(location, blockType, maxDistance, nearest);
        }
        
        return nearest;
    }
    
    /**
     * Get blocks owned by a specific clan
     * @param clan Clan
     * @param blockType Block type, or null for all types
     * @return List of blocks owned by the clan
     */
    public List<SpatiallyIndexable> getBlocksByClan(Clan clan, BlockType blockType) {
        if (clan == null) {
            return Collections.emptyList();
        }
        
        // Check cache first
        List<SpatiallyIndexable> cachedBlocks = queryCache.getBlocksByClan(clan, blockType);
        if (cachedBlocks != null) {
            return cachedBlocks;
        }
        
        List<SpatiallyIndexable> result = new ArrayList<>();
        
        // Check all worlds
        for (Map<ChunkCoordinate, Map<BlockType, List<SpatiallyIndexable>>> worldMap : blockIndex.values()) {
            // Check all chunks
            for (Map<BlockType, List<SpatiallyIndexable>> chunkMap : worldMap.values()) {
                // If block type is specified, get only that type
                if (blockType != null) {
                    List<SpatiallyIndexable> blockList = chunkMap.get(blockType);
                    if (blockList != null) {
                        // Filter by clan
                        for (SpatiallyIndexable block : blockList) {
                            if (clan.equals(block.getClan())) {
                                result.add(block);
                            }
                        }
                    }
                } else {
                    // Get all block types
                    for (List<SpatiallyIndexable> blockList : chunkMap.values()) {
                        // Filter by clan
                        for (SpatiallyIndexable block : blockList) {
                            if (clan.equals(block.getClan())) {
                                result.add(block);
                            }
                        }
                    }
                }
            }
        }
        
        // Cache result
        queryCache.cacheBlocksByClan(clan, blockType, result);
        
        return result;
    }
    
    /**
     * Rebuild the spatial index from scratch
     * @param blocks All blocks to index
     */
    public void rebuildIndex(Collection<SpatiallyIndexable> blocks) {
        // Clear existing index
        blockIndex.clear();
        queryCache.clear();
        
        // Add all blocks
        for (SpatiallyIndexable block : blocks) {
            addBlock(block);
        }
        
        plugin.getLogger().info("Rebuilt spatial index with " + blocks.size() + " blocks");
    }
    
    /**
     * Clear the spatial index
     */
    public void clearIndex() {
        blockIndex.clear();
        queryCache.clear();
    }
    
    /**
     * Get statistics about the spatial index
     * @return Map of statistics
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        int totalBlocks = 0;
        int totalChunks = 0;
        int totalWorlds = 0;
        
        // Count blocks by type
        Map<BlockType, Integer> blockTypeCount = new EnumMap<>(BlockType.class);
        for (BlockType type : BlockType.values()) {
            blockTypeCount.put(type, 0);
        }
        
        // Count blocks, chunks, and worlds
        for (Map<ChunkCoordinate, Map<BlockType, List<SpatiallyIndexable>>> worldMap : blockIndex.values()) {
            totalWorlds++;
            totalChunks += worldMap.size();
            
            for (Map<BlockType, List<SpatiallyIndexable>> chunkMap : worldMap.values()) {
                for (Map.Entry<BlockType, List<SpatiallyIndexable>> entry : chunkMap.entrySet()) {
                    BlockType type = entry.getKey();
                    List<SpatiallyIndexable> blockList = entry.getValue();
                    
                    totalBlocks += blockList.size();
                    blockTypeCount.put(type, blockTypeCount.get(type) + blockList.size());
                }
            }
        }
        
        // Add statistics to map
        stats.put("totalBlocks", totalBlocks);
        stats.put("totalChunks", totalChunks);
        stats.put("totalWorlds", totalWorlds);
        
        for (Map.Entry<BlockType, Integer> entry : blockTypeCount.entrySet()) {
            stats.put(entry.getKey().name(), entry.getValue());
        }
        
        return stats;
    }
    
    /**
     * Log statistics about the spatial index
     */
    public void logStatistics() {
        Map<String, Integer> stats = getStatistics();
        
        plugin.getLogger().info("Spatial Index Statistics:");
        plugin.getLogger().info("Total Blocks: " + stats.get("totalBlocks"));
        plugin.getLogger().info("Total Chunks: " + stats.get("totalChunks"));
        plugin.getLogger().info("Total Worlds: " + stats.get("totalWorlds"));
        
        for (BlockType type : BlockType.values()) {
            plugin.getLogger().info(type.name() + " Blocks: " + stats.get(type.name()));
        }
    }
}
