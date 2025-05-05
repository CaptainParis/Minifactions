package Factions.miniFactions.models;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.managers.CraftingManager;
import Factions.miniFactions.spatial.BlockType;
import Factions.miniFactions.spatial.SpatiallyIndexable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

public class DefenseBlock implements SpatiallyIndexable {

    private final Location location;
    private final Clan clan;
    private int tier;
    private long placementTime;
    private Material material;

    /**
     * Create a new defense block
     * @param location Block location
     * @param clan Owning clan
     * @param tier Block tier
     * @throws IllegalArgumentException if location or clan is null, or tier is invalid
     */
    public DefenseBlock(Location location, Clan clan, int tier) {
        if (location == null) {
            throw new IllegalArgumentException("Defense block location cannot be null");
        }
        if (clan == null) {
            throw new IllegalArgumentException("Defense block clan cannot be null");
        }
        if (tier < 1) {
            throw new IllegalArgumentException("Defense block tier cannot be less than 1");
        }

        // Get max tier from config
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        int maxTier = config.getInt("defense.max-tier", 5);

        if (tier > maxTier) {
            throw new IllegalArgumentException("Defense block tier cannot be greater than " + maxTier);
        }

        this.location = location;
        this.clan = clan;
        this.tier = tier;
        this.placementTime = System.currentTimeMillis();
        this.material = CraftingManager.getTierMaterial(tier);
    }

    /**
     * Get the material for this tier of defense block
     * @param tier The tier level
     * @return The appropriate material
     */
    private Material getTierMaterial(int tier) {
        return CraftingManager.getTierMaterial(tier);
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
     * Get the block tier
     * @return Tier
     */
    public int getTier() {
        return tier;
    }

    /**
     * Set the block tier
     * @param tier New tier
     * @return The actual tier that was set (may be clamped to valid range)
     */
    public int setTier(int tier) {
        // Get max tier from config
        FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
        int maxTier = config.getInt("defense.max-tier", 5);

        if (tier < 1) {
            this.tier = 1;
        } else if (tier > maxTier) {
            this.tier = maxTier;
        } else {
            this.tier = tier;
        }

        // Update material to match the new tier
        this.material = CraftingManager.getTierMaterial(this.tier);
        return this.tier;
    }

    /**
     * Reduce the block tier by 1
     * @return true if successful, false if already at tier 1
     */
    public boolean reduceTier() {
        if (tier > 1) {
            tier--;
            // Update material to match the new tier
            this.material = CraftingManager.getTierMaterial(tier);
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
     * @throws IllegalArgumentException if placementTime is in the future
     */
    public void setPlacementTime(long placementTime) {
        long currentTime = System.currentTimeMillis();
        if (placementTime > currentTime) {
            throw new IllegalArgumentException("Placement time cannot be in the future");
        }
        this.placementTime = placementTime;
    }

    /**
     * Update the placement time to now
     */
    public void updatePlacementTime() {
        this.placementTime = System.currentTimeMillis();
    }

    /**
     * Get the material of the defense block
     * @return Material (never null)
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * Set the material of the defense block
     * @param material New material
     * @throws IllegalArgumentException if material is null
     */
    public void setMaterial(Material material) {
        if (material == null) {
            throw new IllegalArgumentException("Defense block material cannot be null");
        }
        this.material = material;
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

        // Check if upkeep is due and not paid
        if (clan.getCoreBlock().isUpkeepDue() && !clan.getCoreBlock().payUpkeep()) {
            // Check upkeep settings
            FileConfiguration config = MiniFactions.getInstance().getConfigManager().getConfig();
            boolean upkeepEnabled = config.getBoolean("upkeep.defense.enabled", true);

            if (upkeepEnabled) {
                // Check grace period
                long gracePeriod = config.getLong("upkeep.defense.grace-period", 12) * 60 * 60 * 1000L; // Convert hours to milliseconds
                long timeSinceUpkeepDue = System.currentTimeMillis() - (clan.getCoreBlock().getLastUpkeepTime() +
                        config.getLong("core.upkeep.payment-interval", 24) * 60 * 60 * 1000L);

                // Only decay if outside grace period
                return timeSinceUpkeepDue > gracePeriod;
            }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefenseBlock that = (DefenseBlock) o;
        return Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location);
    }

    @Override
    public String toString() {
        return "DefenseBlock{" +
                "location=" + location +
                ", clan=" + clan.getName() +
                ", tier=" + tier +
                ", placementTime=" + placementTime +
                ", material=" + material +
                '}';
    }

    /**
     * Get the block type for spatial indexing
     * @return BlockType.DEFENSE
     */
    @Override
    public BlockType getBlockType() {
        return BlockType.DEFENSE;
    }
}