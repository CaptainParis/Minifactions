package Factions.miniFactions.spatial;

import Factions.miniFactions.models.Clan;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for spatial queries to improve performance
 */
public class SpatialQueryCache {
    // Maximum cache size
    private static final int MAX_CACHE_SIZE = 1000;
    
    // Cache expiration time in milliseconds (5 minutes)
    private static final long CACHE_EXPIRATION_TIME = 5 * 60 * 1000;
    
    // Cache for getBlockAt queries
    private final Map<BlockAtKey, CacheEntry<SpatiallyIndexable>> blockAtCache = new ConcurrentHashMap<>();
    
    // Cache for getBlocksInChunk queries
    private final Map<BlocksInChunkKey, CacheEntry<List<SpatiallyIndexable>>> blocksInChunkCache = new ConcurrentHashMap<>();
    
    // Cache for getBlocksInRadius queries
    private final Map<BlocksInRadiusKey, CacheEntry<List<SpatiallyIndexable>>> blocksInRadiusCache = new ConcurrentHashMap<>();
    
    // Cache for getBlocksInDirection queries
    private final Map<BlocksInDirectionKey, CacheEntry<List<SpatiallyIndexable>>> blocksInDirectionCache = new ConcurrentHashMap<>();
    
    // Cache for getAdjacentBlocks queries
    private final Map<AdjacentBlocksKey, CacheEntry<List<SpatiallyIndexable>>> adjacentBlocksCache = new ConcurrentHashMap<>();
    
    // Cache for getNearestBlock queries
    private final Map<NearestBlockKey, CacheEntry<SpatiallyIndexable>> nearestBlockCache = new ConcurrentHashMap<>();
    
    // Cache for getBlocksByClan queries
    private final Map<BlocksByClanKey, CacheEntry<List<SpatiallyIndexable>>> blocksByClanCache = new ConcurrentHashMap<>();
    
    /**
     * Clear all caches
     */
    public void clear() {
        blockAtCache.clear();
        blocksInChunkCache.clear();
        blocksInRadiusCache.clear();
        blocksInDirectionCache.clear();
        adjacentBlocksCache.clear();
        nearestBlockCache.clear();
        blocksByClanCache.clear();
    }
    
    /**
     * Cache a block at a location
     * @param location Location
     * @param blockType Block type
     * @param block Block
     */
    public void cacheBlockAt(Location location, BlockType blockType, SpatiallyIndexable block) {
        if (blockAtCache.size() >= MAX_CACHE_SIZE) {
            // Remove oldest entries if cache is full
            removeOldestEntries(blockAtCache);
        }
        
        BlockAtKey key = new BlockAtKey(location, blockType);
        blockAtCache.put(key, new CacheEntry<>(block));
    }
    
    /**
     * Get a cached block at a location
     * @param location Location
     * @param blockType Block type
     * @return Cached block, or null if not in cache or expired
     */
    public SpatiallyIndexable getBlockAt(Location location, BlockType blockType) {
        BlockAtKey key = new BlockAtKey(location, blockType);
        CacheEntry<SpatiallyIndexable> entry = blockAtCache.get(key);
        
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        
        // Remove expired entry
        if (entry != null) {
            blockAtCache.remove(key);
        }
        
        return null;
    }
    
    /**
     * Cache blocks in a chunk
     * @param worldName World name
     * @param chunkCoord Chunk coordinate
     * @param blockType Block type
     * @param blocks Blocks in the chunk
     */
    public void cacheBlocksInChunk(String worldName, ChunkCoordinate chunkCoord, BlockType blockType, List<SpatiallyIndexable> blocks) {
        if (blocksInChunkCache.size() >= MAX_CACHE_SIZE) {
            // Remove oldest entries if cache is full
            removeOldestEntries(blocksInChunkCache);
        }
        
        BlocksInChunkKey key = new BlocksInChunkKey(worldName, chunkCoord, blockType);
        blocksInChunkCache.put(key, new CacheEntry<>(blocks));
    }
    
    /**
     * Get cached blocks in a chunk
     * @param worldName World name
     * @param chunkCoord Chunk coordinate
     * @param blockType Block type
     * @return Cached blocks, or null if not in cache or expired
     */
    public List<SpatiallyIndexable> getBlocksInChunk(String worldName, ChunkCoordinate chunkCoord, BlockType blockType) {
        BlocksInChunkKey key = new BlocksInChunkKey(worldName, chunkCoord, blockType);
        CacheEntry<List<SpatiallyIndexable>> entry = blocksInChunkCache.get(key);
        
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        
        // Remove expired entry
        if (entry != null) {
            blocksInChunkCache.remove(key);
        }
        
        return null;
    }
    
    /**
     * Cache blocks in a radius
     * @param center Center location
     * @param radius Radius
     * @param blockType Block type
     * @param blocks Blocks in the radius
     */
    public void cacheBlocksInRadius(Location center, int radius, BlockType blockType, List<SpatiallyIndexable> blocks) {
        if (blocksInRadiusCache.size() >= MAX_CACHE_SIZE) {
            // Remove oldest entries if cache is full
            removeOldestEntries(blocksInRadiusCache);
        }
        
        BlocksInRadiusKey key = new BlocksInRadiusKey(center, radius, blockType);
        blocksInRadiusCache.put(key, new CacheEntry<>(blocks));
    }
    
    /**
     * Get cached blocks in a radius
     * @param center Center location
     * @param radius Radius
     * @param blockType Block type
     * @return Cached blocks, or null if not in cache or expired
     */
    public List<SpatiallyIndexable> getBlocksInRadius(Location center, int radius, BlockType blockType) {
        BlocksInRadiusKey key = new BlocksInRadiusKey(center, radius, blockType);
        CacheEntry<List<SpatiallyIndexable>> entry = blocksInRadiusCache.get(key);
        
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        
        // Remove expired entry
        if (entry != null) {
            blocksInRadiusCache.remove(key);
        }
        
        return null;
    }
    
    /**
     * Cache blocks in a direction
     * @param origin Origin location
     * @param direction Direction
     * @param distance Distance
     * @param blockType Block type
     * @param blocks Blocks in the direction
     */
    public void cacheBlocksInDirection(Location origin, BlockFace direction, int distance, BlockType blockType, List<SpatiallyIndexable> blocks) {
        if (blocksInDirectionCache.size() >= MAX_CACHE_SIZE) {
            // Remove oldest entries if cache is full
            removeOldestEntries(blocksInDirectionCache);
        }
        
        BlocksInDirectionKey key = new BlocksInDirectionKey(origin, direction, distance, blockType);
        blocksInDirectionCache.put(key, new CacheEntry<>(blocks));
    }
    
    /**
     * Get cached blocks in a direction
     * @param origin Origin location
     * @param direction Direction
     * @param distance Distance
     * @param blockType Block type
     * @return Cached blocks, or null if not in cache or expired
     */
    public List<SpatiallyIndexable> getBlocksInDirection(Location origin, BlockFace direction, int distance, BlockType blockType) {
        BlocksInDirectionKey key = new BlocksInDirectionKey(origin, direction, distance, blockType);
        CacheEntry<List<SpatiallyIndexable>> entry = blocksInDirectionCache.get(key);
        
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        
        // Remove expired entry
        if (entry != null) {
            blocksInDirectionCache.remove(key);
        }
        
        return null;
    }
    
    /**
     * Cache adjacent blocks
     * @param location Center location
     * @param blockType Block type
     * @param blocks Adjacent blocks
     */
    public void cacheAdjacentBlocks(Location location, BlockType blockType, List<SpatiallyIndexable> blocks) {
        if (adjacentBlocksCache.size() >= MAX_CACHE_SIZE) {
            // Remove oldest entries if cache is full
            removeOldestEntries(adjacentBlocksCache);
        }
        
        AdjacentBlocksKey key = new AdjacentBlocksKey(location, blockType);
        adjacentBlocksCache.put(key, new CacheEntry<>(blocks));
    }
    
    /**
     * Get cached adjacent blocks
     * @param location Center location
     * @param blockType Block type
     * @return Cached blocks, or null if not in cache or expired
     */
    public List<SpatiallyIndexable> getAdjacentBlocks(Location location, BlockType blockType) {
        AdjacentBlocksKey key = new AdjacentBlocksKey(location, blockType);
        CacheEntry<List<SpatiallyIndexable>> entry = adjacentBlocksCache.get(key);
        
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        
        // Remove expired entry
        if (entry != null) {
            adjacentBlocksCache.remove(key);
        }
        
        return null;
    }
    
    /**
     * Cache nearest block
     * @param location Center location
     * @param blockType Block type
     * @param maxDistance Maximum distance
     * @param block Nearest block
     */
    public void cacheNearestBlock(Location location, BlockType blockType, int maxDistance, SpatiallyIndexable block) {
        if (nearestBlockCache.size() >= MAX_CACHE_SIZE) {
            // Remove oldest entries if cache is full
            removeOldestEntries(nearestBlockCache);
        }
        
        NearestBlockKey key = new NearestBlockKey(location, blockType, maxDistance);
        nearestBlockCache.put(key, new CacheEntry<>(block));
    }
    
    /**
     * Get cached nearest block
     * @param location Center location
     * @param blockType Block type
     * @param maxDistance Maximum distance
     * @return Cached block, or null if not in cache or expired
     */
    public SpatiallyIndexable getNearestBlock(Location location, BlockType blockType, int maxDistance) {
        NearestBlockKey key = new NearestBlockKey(location, blockType, maxDistance);
        CacheEntry<SpatiallyIndexable> entry = nearestBlockCache.get(key);
        
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        
        // Remove expired entry
        if (entry != null) {
            nearestBlockCache.remove(key);
        }
        
        return null;
    }
    
    /**
     * Cache blocks by clan
     * @param clan Clan
     * @param blockType Block type
     * @param blocks Blocks owned by the clan
     */
    public void cacheBlocksByClan(Clan clan, BlockType blockType, List<SpatiallyIndexable> blocks) {
        if (blocksByClanCache.size() >= MAX_CACHE_SIZE) {
            // Remove oldest entries if cache is full
            removeOldestEntries(blocksByClanCache);
        }
        
        BlocksByClanKey key = new BlocksByClanKey(clan, blockType);
        blocksByClanCache.put(key, new CacheEntry<>(blocks));
    }
    
    /**
     * Get cached blocks by clan
     * @param clan Clan
     * @param blockType Block type
     * @return Cached blocks, or null if not in cache or expired
     */
    public List<SpatiallyIndexable> getBlocksByClan(Clan clan, BlockType blockType) {
        BlocksByClanKey key = new BlocksByClanKey(clan, blockType);
        CacheEntry<List<SpatiallyIndexable>> entry = blocksByClanCache.get(key);
        
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        
        // Remove expired entry
        if (entry != null) {
            blocksByClanCache.remove(key);
        }
        
        return null;
    }
    
    /**
     * Remove oldest entries from a cache
     * @param cache Cache to clean
     * @param <K> Key type
     * @param <V> Value type
     */
    private <K, V> void removeOldestEntries(Map<K, CacheEntry<V>> cache) {
        // Remove expired entries first
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        // If still too many entries, remove oldest ones
        if (cache.size() >= MAX_CACHE_SIZE) {
            // Sort entries by timestamp and remove oldest 25%
            int toRemove = cache.size() / 4;
            
            cache.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e1.getValue().getTimestamp(), e2.getValue().getTimestamp()))
                    .limit(toRemove)
                    .map(Map.Entry::getKey)
                    .forEach(cache::remove);
        }
    }
    
    /**
     * Cache entry with timestamp
     * @param <T> Value type
     */
    private static class CacheEntry<T> {
        private final T value;
        private final long timestamp;
        
        public CacheEntry(T value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
        
        public T getValue() {
            return value;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRATION_TIME;
        }
    }
    
    /**
     * Key for getBlockAt cache
     */
    private static class BlockAtKey {
        private final String worldName;
        private final int x;
        private final int y;
        private final int z;
        private final BlockType blockType;
        
        public BlockAtKey(Location location, BlockType blockType) {
            this.worldName = location.getWorld().getName();
            this.x = location.getBlockX();
            this.y = location.getBlockY();
            this.z = location.getBlockZ();
            this.blockType = blockType;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BlockAtKey that = (BlockAtKey) o;
            return x == that.x && y == that.y && z == that.z &&
                    Objects.equals(worldName, that.worldName) &&
                    blockType == that.blockType;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(worldName, x, y, z, blockType);
        }
    }
    
    /**
     * Key for getBlocksInChunk cache
     */
    private static class BlocksInChunkKey {
        private final String worldName;
        private final ChunkCoordinate chunkCoord;
        private final BlockType blockType;
        
        public BlocksInChunkKey(String worldName, ChunkCoordinate chunkCoord, BlockType blockType) {
            this.worldName = worldName;
            this.chunkCoord = chunkCoord;
            this.blockType = blockType;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BlocksInChunkKey that = (BlocksInChunkKey) o;
            return Objects.equals(worldName, that.worldName) &&
                    Objects.equals(chunkCoord, that.chunkCoord) &&
                    blockType == that.blockType;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(worldName, chunkCoord, blockType);
        }
    }
    
    /**
     * Key for getBlocksInRadius cache
     */
    private static class BlocksInRadiusKey {
        private final String worldName;
        private final int x;
        private final int y;
        private final int z;
        private final int radius;
        private final BlockType blockType;
        
        public BlocksInRadiusKey(Location center, int radius, BlockType blockType) {
            this.worldName = center.getWorld().getName();
            this.x = center.getBlockX();
            this.y = center.getBlockY();
            this.z = center.getBlockZ();
            this.radius = radius;
            this.blockType = blockType;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BlocksInRadiusKey that = (BlocksInRadiusKey) o;
            return x == that.x && y == that.y && z == that.z &&
                    radius == that.radius &&
                    Objects.equals(worldName, that.worldName) &&
                    blockType == that.blockType;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(worldName, x, y, z, radius, blockType);
        }
    }
    
    /**
     * Key for getBlocksInDirection cache
     */
    private static class BlocksInDirectionKey {
        private final String worldName;
        private final int x;
        private final int y;
        private final int z;
        private final BlockFace direction;
        private final int distance;
        private final BlockType blockType;
        
        public BlocksInDirectionKey(Location origin, BlockFace direction, int distance, BlockType blockType) {
            this.worldName = origin.getWorld().getName();
            this.x = origin.getBlockX();
            this.y = origin.getBlockY();
            this.z = origin.getBlockZ();
            this.direction = direction;
            this.distance = distance;
            this.blockType = blockType;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BlocksInDirectionKey that = (BlocksInDirectionKey) o;
            return x == that.x && y == that.y && z == that.z &&
                    distance == that.distance &&
                    Objects.equals(worldName, that.worldName) &&
                    direction == that.direction &&
                    blockType == that.blockType;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(worldName, x, y, z, direction, distance, blockType);
        }
    }
    
    /**
     * Key for getAdjacentBlocks cache
     */
    private static class AdjacentBlocksKey {
        private final String worldName;
        private final int x;
        private final int y;
        private final int z;
        private final BlockType blockType;
        
        public AdjacentBlocksKey(Location location, BlockType blockType) {
            this.worldName = location.getWorld().getName();
            this.x = location.getBlockX();
            this.y = location.getBlockY();
            this.z = location.getBlockZ();
            this.blockType = blockType;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AdjacentBlocksKey that = (AdjacentBlocksKey) o;
            return x == that.x && y == that.y && z == that.z &&
                    Objects.equals(worldName, that.worldName) &&
                    blockType == that.blockType;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(worldName, x, y, z, blockType);
        }
    }
    
    /**
     * Key for getNearestBlock cache
     */
    private static class NearestBlockKey {
        private final String worldName;
        private final int x;
        private final int y;
        private final int z;
        private final BlockType blockType;
        private final int maxDistance;
        
        public NearestBlockKey(Location location, BlockType blockType, int maxDistance) {
            this.worldName = location.getWorld().getName();
            this.x = location.getBlockX();
            this.y = location.getBlockY();
            this.z = location.getBlockZ();
            this.blockType = blockType;
            this.maxDistance = maxDistance;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NearestBlockKey that = (NearestBlockKey) o;
            return x == that.x && y == that.y && z == that.z &&
                    maxDistance == that.maxDistance &&
                    Objects.equals(worldName, that.worldName) &&
                    blockType == that.blockType;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(worldName, x, y, z, blockType, maxDistance);
        }
    }
    
    /**
     * Key for getBlocksByClan cache
     */
    private static class BlocksByClanKey {
        private final String clanId;
        private final BlockType blockType;
        
        public BlocksByClanKey(Clan clan, BlockType blockType) {
            this.clanId = clan.getId();
            this.blockType = blockType;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BlocksByClanKey that = (BlocksByClanKey) o;
            return Objects.equals(clanId, that.clanId) &&
                    blockType == that.blockType;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(clanId, blockType);
        }
    }
}
