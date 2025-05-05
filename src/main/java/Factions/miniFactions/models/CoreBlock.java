package Factions.miniFactions.models;

import Factions.miniFactions.MiniFactions;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

public class CoreBlock {

    private final Location location;
    private final Clan clan;
    private int level;
    private long lastUpkeepTime;

    /**
     * Create a new core block
     * @param location Block location
     * @param clan Owning clan
     */
    public CoreBlock(Location location, Clan clan) {
        this.location = location;
        this.clan = clan;
        this.level = 1;
        this.lastUpkeepTime = System.currentTimeMillis();
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
     * Get the core level
     * @return Level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Set the core level
     * @param level New level
     */
    public void setLevel(int level) {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        int maxLevel = config.getInt("core.max-level", 20);

        if (level < 1) {
            this.level = 1;
        } else if (level > maxLevel) {
            this.level = maxLevel;
        } else {
            this.level = level;
        }
    }

    /**
     * Upgrade the core level
     * @return true if successful, false if already at max level
     */
    public boolean upgrade() {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        int maxLevel = config.getInt("core.max-level", 20);

        if (level < maxLevel) {
            level++;
            return true;
        }
        return false;
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
     */
    public void setLastUpkeepTime(long lastUpkeepTime) {
        this.lastUpkeepTime = lastUpkeepTime;
    }

    /**
     * Update the last upkeep time to now
     */
    public void updateUpkeepTime() {
        this.lastUpkeepTime = System.currentTimeMillis();
    }

    /**
     * Get the buildable area radius
     * @return Area radius in blocks
     */
    public int getBuildableArea() {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        return config.getInt("core.levels." + level + ".area", 10);
    }

    /**
     * Get the maximum number of defense blocks
     * @return Max defense blocks
     */
    public int getMaxDefenseBlocks() {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        return config.getInt("core.levels." + level + ".defense-slots", 5);
    }

    /**
     * Get the maximum number of claim blocks
     * @return Max claim blocks
     */
    public int getMaxClaimBlocks() {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        return config.getInt("core.levels." + level + ".claim-slots", 2);
    }

    /**
     * Get the maximum number of clan doors
     * @return Max clan doors
     */
    public int getMaxClanDoors() {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        return config.getInt("core.levels." + level + ".door-slots", 1);
    }

    /**
     * Get the maximum number of clan members
     * @return Max clan members
     */
    public int getMaxClanMembers() {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        return config.getInt("core.levels." + level + ".member-slots", 10);
    }

    /**
     * Get the upkeep cost
     * @return Upkeep cost in points
     */
    public int getUpkeepCost() {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        return config.getInt("core.levels." + level + ".upkeep-cost", 100);
    }

    /**
     * Get the upgrade cost to the next level
     * @return Upgrade cost in points, or -1 if already at max level
     */
    public int getUpgradeCost() {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        int maxLevel = config.getInt("core.max-level", 20);

        if (level >= maxLevel) {
            return -1;
        }

        return config.getInt("core.levels." + (level + 1) + ".upgrade-cost", 1000 * (level + 1));
    }

    /**
     * Get the recipe for the current level
     * @return List of material:amount strings
     */
    public java.util.List<String> getLevelRecipe() {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        return config.getStringList("core.levels." + level + ".recipe");
    }

    /**
     * Check if upkeep is due
     * @return true if upkeep is due
     */
    public boolean isUpkeepDue() {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        boolean upkeepEnabled = config.getBoolean("core.upkeep.enabled", true);

        if (!upkeepEnabled) {
            return false;
        }

        int paymentInterval = config.getInt("core.upkeep.payment-interval", 24);
        long intervalMillis = paymentInterval * 60 * 60 * 1000L; // Convert hours to milliseconds

        return System.currentTimeMillis() - lastUpkeepTime >= intervalMillis;
    }

    /**
     * Pay upkeep
     * @return true if successful, false if not enough points
     */
    public boolean payUpkeep() {
        if (!isUpkeepDue()) {
            return true;
        }

        int cost = getUpkeepCost();
        if (clan.removePoints(cost)) {
            updateUpkeepTime();
            return true;
        }

        return false;
    }

    /**
     * Get days worth of upkeep
     * @return Number of days the clan can pay upkeep
     */
    public int getDaysOfUpkeep() {
        int upkeepCost = getUpkeepCost();
        if (upkeepCost <= 0) {
            return Integer.MAX_VALUE;
        }

        return clan.getPoints() / upkeepCost;
    }
}
