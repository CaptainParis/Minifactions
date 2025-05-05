package Factions.miniFactions.models;

import org.bukkit.Location;

public class ClanDoor {

    private final Location location;
    private final Clan clan;
    private final int tier;
    
    /**
     * Create a new clan door
     * @param location Door location
     * @param clan Owning clan
     * @param tier Door tier
     */
    public ClanDoor(Location location, Clan clan, int tier) {
        this.location = location;
        this.clan = clan;
        this.tier = tier;
    }
    
    /**
     * Get the door location
     * @return Location
     */
    public Location getLocation() {
        return location;
    }
    
    /**
     * Get the owning clan
     * @return Clan
     */
    public Clan getClan() {
        return clan;
    }
    
    /**
     * Get the door tier
     * @return Tier
     */
    public int getTier() {
        return tier;
    }
    
    /**
     * Check if the door is within the clan's core block area of influence
     * @return true if within AOI
     */
    public boolean isWithinCoreBlockAOI() {
        CoreBlock coreBlock = clan.getCoreBlock();
        if (coreBlock == null) {
            return false;
        }
        
        Location coreLoc = coreBlock.getLocation();
        int radius = coreBlock.getBuildableArea();
        
        // Check if in same world
        if (!location.getWorld().equals(coreLoc.getWorld())) {
            return false;
        }
        
        // Check if within radius
        double distanceSquared = location.distanceSquared(coreLoc);
        return distanceSquared <= radius * radius;
    }
}
