package Factions.miniFactions.models;

import Factions.miniFactions.MiniFactions;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

public class DefenseBlock {

    private final Location location;
    private final Clan clan;
    private int tier;
    private long placementTime;

    /**
     * Create a new defense block
     * @param location Block location
     * @param clan Owning clan
     * @param tier Block tier
     */
    public DefenseBlock(Location location, Clan clan, int tier) {
        this.location = location;
        this.clan = clan;
        this.tier = tier;
        this.placementTime = System.currentTimeMillis();
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
     * Get the block tier
     * @return Tier
     */
    public int getTier() {
        return tier;
    }

    /**
     * Set the block tier
     * @param tier New tier
     */
    public void setTier(int tier) {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        int maxTier = config.getInt("defense.max-tier", 10);

        if (tier < 1) {
            this.tier = 1;
        } else if (tier > maxTier) {
            this.tier = maxTier;
        } else {
            this.tier = tier;
        }
    }

    /**
     * Reduce the block tier by 1
     * @return true if successful, false if already at tier 1
     */
    public boolean reduceTier() {
        if (tier > 1) {
            tier--;
            return true;
        }
        return false;
    }

    /**
     * Get the placement time
     * @return Placement time in milliseconds
     */
    public long getPlacementTime() {
        return placementTime;
    }

    /**
     * Set the placement time
     * @param placementTime New placement time in milliseconds
     */
    public void setPlacementTime(long placementTime) {
        this.placementTime = placementTime;
    }

    /**
     * Update the placement time to now
     */
    public void updatePlacementTime() {
        this.placementTime = System.currentTimeMillis();
    }

    /**
     * Get the cost of the defense block
     * @return Cost in points
     */
    public int getCost() {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        return config.getInt("defense.tiers." + tier + ".cost", 100 * (int) Math.pow(2, tier - 1));
    }

    /**
     * Get the recipe for the current tier
     * @return List of material:amount strings
     */
    public java.util.List<String> getTierRecipe() {
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        return config.getStringList("defense.tiers." + tier + ".recipe");
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

    /**
     * Check if the block should decay
     * @return true if the block should decay
     */
    public boolean shouldDecay() {
        // Check if core block exists
        if (clan.getCoreBlock() == null) {
            return true;
        }

        // Check if within AOI
        if (!isWithinCoreBlockAOI()) {
            return true;
        }

        // Check if upkeep is paid
        if (!clan.getCoreBlock().payUpkeep()) {
            return true;
        }

        // Check if within max defense blocks
        return clan.getDefenseBlockCount() > clan.getCoreBlock().getMaxDefenseBlocks();
    }

    /**
     * Get the time until decay in milliseconds
     * @return Time until decay in milliseconds, or -1 if should not decay
     */
    public long getTimeUntilDecay() {
        if (!shouldDecay()) {
            return -1;
        }

        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        int decayHours = config.getInt("defense.tiers." + tier + ".decay-time", 24);

        // Convert to milliseconds
        long decayMillis = decayHours * 60 * 60 * 1000L;
        long elapsedMillis = System.currentTimeMillis() - placementTime;

        return Math.max(0, decayMillis - elapsedMillis);
    }

    /**
     * Check if the block has decayed
     * @return true if the block has decayed
     */
    public boolean hasDecayed() {
        return getTimeUntilDecay() == 0;
    }
}
