package Factions.miniFactions.models;

import Factions.miniFactions.MiniFactions;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

public class ClaimBlock {

    private final Location location;
    private final Clan clan;
    private int level;
    private long lastPointGenerationTime;

    /**
     * Create a new claim block
     * @param location Block location
     * @param clan Owning clan
     */
    public ClaimBlock(Location location, Clan clan) {
        this.location = location;
        this.clan = clan;
        this.level = 1;
        this.lastPointGenerationTime = System.currentTimeMillis();
    }

    /**
     * Get the block location
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
     * Get the claim level
     * @return Level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Set the claim level
     * @param level New level
     */
    public void setLevel(int level) {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        int maxLevel = config.getInt("claim.max-upgrade-level", 5);

        if (level < 1) {
            this.level = 1;
        } else if (level > maxLevel) {
            this.level = maxLevel;
        } else {
            this.level = level;
        }
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
     */
    public void setLastPointGenerationTime(long lastPointGenerationTime) {
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
     * Get the recipe for the current level
     * @return List of material:amount strings
     */
    public java.util.List<String> getLevelRecipe() {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        return config.getStringList("claim.levels." + level + ".recipe");
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
}
