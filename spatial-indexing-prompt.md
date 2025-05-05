# MiniFactions Spatial Indexing System Implementation

## Overview
Implement an efficient spatial indexing system for MiniFactions that allows for quick lookups of blocks (Core, Defense, Claim) based on their relative positions. This system should enable queries like "find all defense blocks within X blocks of this location" or "is there a defense block 1 block northwest of this location" without having to iterate through all blocks in storage.

## Requirements

1. **Chunk-Based Indexing**:
   - Create a spatial index that organizes blocks by chunk coordinates
   - Each chunk should maintain a list of blocks contained within it
   - Use a nested map structure: `Map<ChunkCoordinate, Map<BlockType, List<BlockData>>>`

2. **Relative Position Queries**:
   - Implement methods to find blocks at specific relative positions (e.g., 1 block north, 2 blocks east)
   - Support directional queries (N, S, E, W, NE, NW, SE, SW)
   - Add methods to find all blocks within a certain radius

3. **Block Type Indexing**:
   - Index blocks by type (Core, Defense, Claim, Door)
   - Allow for type-specific queries (e.g., "find all defense blocks within 5 blocks")

4. **Efficient Storage**:
   - Create a `SpatialIndex` class to manage the indexing
   - Ensure all block additions, removals, and lookups update the index
   - Optimize for memory usage by storing minimal data in the index

5. **Integration with Existing Systems**:
   - Modify `DataStorage` to use the spatial index
   - Update all block placement and removal code to maintain the index
   - Ensure the index is saved and loaded with the rest of the plugin data

## Implementation Details

### 1. Create SpatialIndex Class

```java
public class SpatialIndex {
    // Maps chunk coordinates to a map of block types to block lists
    private Map<ChunkCoordinate, Map<BlockType, List<BlockData>>> chunkIndex;
    
    // Methods for adding, removing, and querying blocks
    public void addBlock(Location location, BlockType type, Object blockData);
    public void removeBlock(Location location, BlockType type);
    public List<BlockData> getBlocksInChunk(ChunkCoordinate chunk, BlockType type);
    public BlockData getBlockAtRelativePosition(Location origin, int xOffset, int yOffset, int zOffset, BlockType type);
    public List<BlockData> getBlocksWithinRadius(Location center, int radius, BlockType type);
    public List<BlockData> getBlocksInDirection(Location origin, Direction direction, int distance, BlockType type);
}
```

### 2. Create ChunkCoordinate Class

```java
public class ChunkCoordinate {
    private final int x;
    private final int z;
    private final String worldName;
    
    // Constructor, getters, hashCode, equals
}
```

### 3. Create BlockType Enum

```java
public enum BlockType {
    CORE,
    DEFENSE,
    CLAIM,
    DOOR
}
```

### 4. Create Direction Enum

```java
public enum Direction {
    NORTH(0, 0, -1),
    NORTHEAST(1, 0, -1),
    EAST(1, 0, 0),
    SOUTHEAST(1, 0, 1),
    SOUTH(0, 0, 1),
    SOUTHWEST(-1, 0, 1),
    WEST(-1, 0, 0),
    NORTHWEST(-1, 0, -1),
    UP(0, 1, 0),
    DOWN(0, -1, 0);
    
    private final int xOffset;
    private final int yOffset;
    private final int zOffset;
    
    // Constructor, getters
}
```

### 5. Modify DataStorage Class

Update the DataStorage class to use the spatial index:

```java
public class DataStorage {
    private SpatialIndex spatialIndex;
    
    // Existing fields
    
    public void initialize() {
        // Existing initialization
        spatialIndex = new SpatialIndex();
    }
    
    public void addCoreBlock(CoreBlock coreBlock) {
        // Existing code
        spatialIndex.addBlock(coreBlock.getLocation(), BlockType.CORE, coreBlock);
    }
    
    public void removeCoreBlock(Location location) {
        // Existing code
        spatialIndex.removeBlock(location, BlockType.CORE);
    }
    
    // Similar modifications for claim blocks, defense blocks, and doors
    
    public CoreBlock getCoreBlockAtRelativePosition(Location origin, int xOffset, int yOffset, int zOffset) {
        return (CoreBlock) spatialIndex.getBlockAtRelativePosition(origin, xOffset, yOffset, zOffset, BlockType.CORE);
    }
    
    public List<DefenseBlock> getDefenseBlocksWithinRadius(Location center, int radius) {
        return spatialIndex.getBlocksWithinRadius(center, radius, BlockType.DEFENSE)
                .stream()
                .map(block -> (DefenseBlock) block)
                .collect(Collectors.toList());
    }
    
    // Additional query methods
}
```

### 6. Performance Optimizations

1. **Lazy Loading**: Only load chunks into memory when they're accessed
2. **Caching**: Cache recent queries for faster repeated access
3. **Bulk Operations**: Support adding/removing multiple blocks at once
4. **Concurrent Access**: Make the index thread-safe for multi-threaded access

### 7. Example Usage

```java
// Check if there's a defense block 1 block northwest
DefenseBlock northwestBlock = dataStorage.getDefenseBlockInDirection(
    location, Direction.NORTHWEST, 1);
if (northwestBlock != null) {
    // Found a defense block to the northwest
}

// Get all claim blocks within 10 blocks
List<ClaimBlock> nearbyClaims = dataStorage.getClaimBlocksWithinRadius(
    location, 10);

// Find the closest core block
CoreBlock closestCore = dataStorage.getClosestCoreBlock(location);
```

## Testing

1. Create unit tests for the spatial index
2. Test performance with large numbers of blocks
3. Verify that all existing functionality works with the new system
4. Benchmark query performance compared to the old system

## Documentation

1. Document all new classes and methods
2. Create examples of common usage patterns
3. Update the plugin's documentation to explain the new spatial querying capabilities
