package Factions.miniFactions.models;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.spatial.BlockType;
import Factions.miniFactions.spatial.SpatiallyIndexable;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

public class ClaimBlock implements SpatiallyIndexable {

    private final Location location;
    private final Clan clan;
    private int level;
    private long lastPointGenerationTime;

    /**
     * Create a new claim block
     * @param location Block location
     * @param clan Owning clan
     * @throws IllegalArgumentException if location or clan is null
     */
    public ClaimBlock(Location location, Clan clan) {
        if (location == null) {
            throw new IllegalArgumentException("Claim block location cannot be null");
        }
        if (clan == null) {
            throw new IllegalArgumentException("Claim block clan cannot be null");
        }

        this.location = location;
        this.clan = clan;
        this.level = 1;
        this.lastPointGenerationTime = System.currentTimeMillis();
    }

    /**
     * Get the block location
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
     * Get the claim level
     * @return Level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Set the claim level
     * @param level New level
     * @return The actual level that was set (may be clamped to valid range)
     */
    public int setLevel(int level) {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        int maxLevel = config.getInt("claim.max-upgrade-level", 5);

        if (level < 1) {
            this.level = 1;
        } else if (level > maxLevel) {
            this.level = maxLevel;
        } else {
            this.level = level;
        }

        return this.level;
    }

    /**
     * Upgrade the claim level
     * @return true if successful, false if already at max level
     */
    public boolean upgrade() {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        int maxLevel = config.getInt("claim.max-upgrade-level", 5);

        if (level < maxLevel) {
            level++;
            return true;
        }
        return false;
    }

    /**
     * Get the last point generation time
     * @return Last generation time in milliseconds
     */
    public long getLastPointGenerationTime() {
        return lastPointGenerationTime;
    }

    /**
     * Set the last point generation time
     * @param lastPointGenerationTime New generation time in milliseconds
     * @throws IllegalArgumentException if lastPointGenerationTime is in the future
     */
    public void setLastPointGenerationTime(long lastPointGenerationTime) {
        long currentTime = System.currentTimeMillis();
        if (lastPointGenerationTime > currentTime) {
            throw new IllegalArgumentException("Generation time cannot be in the future");
        }
        this.lastPointGenerationTime = lastPointGenerationTime;
    }

    /**
     * Update the last point generation time to now
     */
    public void updatePointGenerationTime() {
        this.lastPointGenerationTime = System.currentTimeMillis();
    }

    /**
     * Get the points generated per day
     * @return Points per day
     */
    public int getPointsPerDay() {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        return config.getInt("claim.levels." + level + ".points-per-day", 100);
    }

    /**
     * Get the upgrade cost to the next level
     * @return Upgrade cost in points, or -1 if already at max level
     */
    public int getUpgradeCost() {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        int maxLevel = config.getInt("claim.max-upgrade-level", 5);

        if (level >= maxLevel) {
            return -1;
        }

        return config.getInt("claim.levels." + (level + 1) + ".upgrade-cost", 500 * (level + 1));
    }

    /**
     * Get the required items for upgrading to the next level
     * @return List of material:amount strings
     */
    public java.util.List<String> getUpgradeItems() {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        return config.getStringList("claim.levels." + (level + 1) + ".upgrade-items");
    }

    /**
     * Check if points are ready to be generated
     * @return true if points are ready
     */
    public boolean canGeneratePoints() {
        // Check if core block exists
        if (clan.getCoreBlock() == null) {
            return false;
        }

        // Check if within max claim blocks
        if (clan.getClaimBlockCount() > clan.getCoreBlock().getMaxClaimBlocks()) {
            return false;
        }

        // Check if upkeep is due and not paid
        if (clan.getCoreBlock().isUpkeepDue() && !clan.getCoreBlock().payUpkeep()) {
            // Check if upkeep penalty is set to stop point generation
            FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
            String penaltyType = config.getString("upkeep.claim.penalty-type", "LEVEL_REDUCTION");
            if (penaltyType.equals("POINT_GENERATION_STOP")) {
                return false;
            }
        }

        // Check if 24 hours have passed since last generation
        long dayInMillis = 24 * 60 * 60 * 1000L;
        return System.currentTimeMillis() - lastPointGenerationTime >= dayInMillis;
    }

    /**
     * Generate points for the clan
     * @return Amount of points generated
     */
    public int generatePoints() {
        if (!canGeneratePoints()) {
            return 0;
        }

        int points = getPointsPerDay();
        clan.addPoints(points);
        updatePointGenerationTime();

        return points;
    }

    /**
     * Check if the block is within the clan's core block area of influence
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
        ClaimBlock that = (ClaimBlock) o;
        return Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location);
    }

    @Override
    public String toString() {
        return "ClaimBlock{" +
                "location=" + location +
                ", clan=" + clan.getName() +
                ", level=" + level +
                ", lastPointGenerationTime=" + lastPointGenerationTime +
                '}';
    }

    /**
     * Get the block type for spatial indexing
     * @return BlockType.CLAIM
     */
    @Override
    public BlockType getBlockType() {
        return BlockType.CLAIM;
    }
}