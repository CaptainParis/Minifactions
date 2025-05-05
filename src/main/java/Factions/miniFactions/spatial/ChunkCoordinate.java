package Factions.miniFactions.spatial;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

/**
 * Represents the coordinates of a chunk for efficient spatial indexing
 */
public class ChunkCoordinate {
    private final int x;
    private final int z;
    
    /**
     * Create a new chunk coordinate
     * @param x Chunk X coordinate
     * @param z Chunk Z coordinate
     */
    public ChunkCoordinate(int x, int z) {
        this.x = x;
        this.z = z;
    }
    
    /**
     * Create a chunk coordinate from a location
     * @param location Location
     */
    public ChunkCoordinate(Location location) {
        this.x = location.getBlockX() >> 4; // Divide by 16
        this.z = location.getBlockZ() >> 4; // Divide by 16
    }
    
    /**
     * Create a chunk coordinate from a chunk
     * @param chunk Chunk
     */
    public ChunkCoordinate(Chunk chunk) {
        this.x = chunk.getX();
        this.z = chunk.getZ();
    }
    
    /**
     * Get the chunk X coordinate
     * @return X coordinate
     */
    public int getX() {
        return x;
    }
    
    /**
     * Get the chunk Z coordinate
     * @return Z coordinate
     */
    public int getZ() {
        return z;
    }
    
    /**
     * Get the chunk at this coordinate
     * @param world World
     * @return Chunk
     */
    public Chunk getChunk(World world) {
        return world.getChunkAt(x, z);
    }
    
    /**
     * Check if a location is in this chunk
     * @param location Location
     * @return true if in this chunk
     */
    public boolean contains(Location location) {
        return x == location.getBlockX() >> 4 && z == location.getBlockZ() >> 4;
    }
    
    /**
     * Get the distance to another chunk coordinate
     * @param other Other chunk coordinate
     * @return Distance in chunks
     */
    public double distanceTo(ChunkCoordinate other) {
        int dx = other.x - x;
        int dz = other.z - z;
        return Math.sqrt(dx * dx + dz * dz);
    }
    
    /**
     * Get the squared distance to another chunk coordinate
     * @param other Other chunk coordinate
     * @return Squared distance in chunks
     */
    public int distanceSquaredTo(ChunkCoordinate other) {
        int dx = other.x - x;
        int dz = other.z - z;
        return dx * dx + dz * dz;
    }
    
    /**
     * Get the chunk coordinates in a radius
     * @param radius Radius in chunks
     * @return Array of chunk coordinates
     */
    public ChunkCoordinate[] getChunksInRadius(int radius) {
        int diameter = radius * 2 + 1;
        ChunkCoordinate[] chunks = new ChunkCoordinate[diameter * diameter];
        int index = 0;
        
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                chunks[index++] = new ChunkCoordinate(x + dx, z + dz);
            }
        }
        
        return chunks;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkCoordinate that = (ChunkCoordinate) o;
        return x == that.x && z == that.z;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }
    
    @Override
    public String toString() {
        return "ChunkCoordinate{" +
                "x=" + x +
                ", z=" + z +
                '}';
    }
}
