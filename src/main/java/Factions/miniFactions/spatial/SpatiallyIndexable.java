package Factions.miniFactions.spatial;

import Factions.miniFactions.models.Clan;
import org.bukkit.Location;

/**
 * Interface for objects that can be spatially indexed
 */
public interface SpatiallyIndexable {
    /**
     * Get the location of the object
     * @return Location
     */
    Location getLocation();
    
    /**
     * Get the type of block
     * @return BlockType
     */
    BlockType getBlockType();
    
    /**
     * Get the clan that owns this block
     * @return Clan
     */
    Clan getClan();
}
