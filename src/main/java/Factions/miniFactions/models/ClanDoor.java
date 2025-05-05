package Factions.miniFactions.models;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.spatial.BlockType;
import Factions.miniFactions.spatial.SpatiallyIndexable;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

public class ClanDoor implements SpatiallyIndexable {

    private final Location location;
    private final Clan clan;
    private int tier;
    private long lastUpkeepTime;

    /**
     * Create a new clan trapdoor
     * @param location Trapdoor location
     * @param clan Owning clan
     * @param tier Trapdoor tier
     * @throws IllegalArgumentException if location or clan is null, or tier is invalid
     */
    public ClanDoor(Location location, Clan clan, int tier) {
        if (location == null) {
            throw new IllegalArgumentException("Clan trapdoor location cannot be null");
        }
        if (clan == null) {
            throw new IllegalArgumentException("Clan trapdoor clan cannot be null");
        }
        if (tier < 1) {
            throw new IllegalArgumentException("Clan trapdoor tier cannot be less than 1");
        }

        this.location = location;
        this.clan = clan;
        this.tier = tier;
        this.lastUpkeepTime = System.currentTimeMillis();
    }

    /**
     * Get the trapdoor location
     * @return Location (never null)
     */
    public Location getLocation() {
        return location.clone(); // Return a clone to prevent modification
    }

    /**
     * Get the owning clan
     * @return Clan
     */
    public Clan getClan() {
        return clan;
    }

    /**
     * Get the trapdoor tier
     * @return Tier
     */
    public int getTier() {
        return tier;
    }

    /**
     * Set the trapdoor tier
     * @param tier New tier
     * @return The actual tier that was set (may be clamped to valid range)
     */
    public int setTier(int tier) {
        if (tier < 1) {
            this.tier = 1;
        } else {
            this.tier = tier;
        }
        return this.tier;
    }

    /**
     * Get the last upkeep time
     * @return Last upkeep time in milliseconds
     */
    public long getLastUpkeepTime() {
        return lastUpkeepTime;
    }

    /**
     * Set the last upkeep time
     * @param lastUpkeepTime New upkeep time in milliseconds
     * @throws IllegalArgumentException if lastUpkeepTime is in the future
     */
    public void setLastUpkeepTime(long lastUpkeepTime) {
        long currentTime = System.currentTimeMillis();
        if (lastUpkeepTime > currentTime) {
            throw new IllegalArgumentException("Upkeep time cannot be in the future");
        }
        this.lastUpkeepTime = lastUpkeepTime;
    }

    /**
     * Update the last upkeep time to now
     */
    public void updateUpkeepTime() {
        this.lastUpkeepTime = System.currentTimeMillis();
    }

    /**
     * Check if the trapdoor is within the clan's core block area of influence
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClanDoor clanDoor = (ClanDoor) o;
        return Objects.equals(location, clanDoor.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location);
    }

    /**
     * Check if upkeep is due for this trapdoor
     * @return true if upkeep is due
     */
    public boolean isUpkeepDue() {
        // Check if core block exists and its upkeep is due
        CoreBlock coreBlock = clan.getCoreBlock();
        if (coreBlock == null || coreBlock.isUpkeepDue()) {
            return true;
        }

        // Check trapdoor-specific upkeep settings
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        boolean upkeepEnabled = config.getBoolean("upkeep.door.enabled", true);

        if (!upkeepEnabled) {
            return false;
        }

        // Check grace period
        long gracePeriod = config.getLong("upkeep.door.grace-period", 6) * 60 * 60 * 1000L; // Convert hours to milliseconds
        long timeSinceUpkeepDue = System.currentTimeMillis() - (coreBlock.getLastUpkeepTime() +
                config.getLong("core.upkeep.payment-interval", 24) * 60 * 60 * 1000L);

        // Only consider upkeep due if outside grace period
        return timeSinceUpkeepDue > gracePeriod;
    }

    /**
     * Get the upkeep cost for this trapdoor
     * @return Upkeep cost in points
     */
    public int getUpkeepCost() {
        // Trapdoor upkeep is included in core block upkeep
        return 0;
    }

    @Override
    public String toString() {
        return "ClanTrapdoor{" +
                "location=" + location +
                ", clan=" + clan.getName() +
                ", tier=" + tier +
                ", lastUpkeepTime=" + lastUpkeepTime +
                '}';
    }

    /**
     * Get the block type for spatial indexing
     * @return BlockType.DOOR
     */
    @Override
    public BlockType getBlockType() {
        return BlockType.DOOR;
    }
}