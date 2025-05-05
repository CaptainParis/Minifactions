package Factions.miniFactions.managers;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.models.Clan;
import Factions.miniFactions.models.CoreBlock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages blocks placed outside of clan core areas
 */
public class OutsideBlockManager {

    private final MiniFactions plugin;
    private final Map<Location, OutsideBlock> outsideBlocks = new ConcurrentHashMap<>();
    private BukkitTask decayTask;
    private final Random random = new Random();

    public OutsideBlockManager(MiniFactions plugin) {
        this.plugin = plugin;
        startDecayTask();
    }

    /**
     * Track a block placed outside a clan's core area
     * @param location Block location
     * @param clan Clan that placed the block
     * @param material Block material
     * @return The tracked outside block
     */
    public OutsideBlock trackBlock(Location location, Clan clan, Material material) {
        // Check if block is within safe distance of core
        if (isWithinSafeDistance(location, clan)) {
            return null; // Don't track blocks within safe distance
        }

        // Calculate random decay time
        FileConfiguration config = plugin.getConfigManager().getConfig();
        int minDecayTime = config.getInt("core.outside-blocks.min-decay-time", 30);
        int maxDecayTime = config.getInt("core.outside-blocks.max-decay-time", 120);
        
        // Random decay time between min and max
        int decayTime = minDecayTime + random.nextInt(maxDecayTime - minDecayTime + 1);
        
        // Create and store outside block
        OutsideBlock outsideBlock = new OutsideBlock(location, clan, material, decayTime);
        outsideBlocks.put(location, outsideBlock);
        
        return outsideBlock;
    }

    /**
     * Remove a tracked outside block
     * @param location Block location
     */
    public void removeBlock(Location location) {
        outsideBlocks.remove(location);
    }

    /**
     * Check if a block is tracked as an outside block
     * @param location Block location
     * @return true if tracked
     */
    public boolean isTrackedBlock(Location location) {
        return outsideBlocks.containsKey(location);
    }

    /**
     * Get an outside block by location
     * @param location Block location
     * @return OutsideBlock or null if not found
     */
    public OutsideBlock getOutsideBlock(Location location) {
        return outsideBlocks.get(location);
    }

    /**
     * Check if a location is within the safe distance of a clan's core
     * @param location Location to check
     * @param clan Clan to check
     * @return true if within safe distance
     */
    public boolean isWithinSafeDistance(Location location, Clan clan) {
        CoreBlock coreBlock = clan.getCoreBlock();
        if (coreBlock == null) {
            return false;
        }

        Location coreLoc = coreBlock.getLocation();
        FileConfiguration config = plugin.getConfigManager().getConfig();
        int safeDistance = config.getInt("core.outside-blocks.safe-distance", 5);

        // Check if in same world
        if (!location.getWorld().equals(coreLoc.getWorld())) {
            return false;
        }

        // Check if within safe distance
        double distanceSquared = location.distanceSquared(coreLoc);
        return distanceSquared <= safeDistance * safeDistance;
    }

    /**
     * Start the decay task for outside blocks
     */
    private void startDecayTask() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        if (!config.getBoolean("core.outside-blocks.enabled", true)) {
            return;
        }

        int checkInterval = config.getInt("core.outside-blocks.check-interval", 5);
        long intervalTicks = checkInterval * 60 * 20L; // Convert minutes to ticks

        decayTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkDecay, intervalTicks, intervalTicks);
    }

    /**
     * Check all outside blocks for decay
     */
    private void checkDecay() {
        List<Location> toRemove = new ArrayList<>();

        for (OutsideBlock outsideBlock : outsideBlocks.values()) {
            if (outsideBlock.shouldDecay()) {
                Location location = outsideBlock.getLocation();
                Block block = location.getBlock();

                // Check if block still exists and matches
                if (block.getType() == outsideBlock.getMaterial()) {
                    // Decay the block
                    block.setType(Material.AIR);
                    
                    // Notify nearby players
                    notifyNearbyPlayers(location, outsideBlock.getClan());
                }
                
                toRemove.add(location);
            }
        }

        // Remove decayed blocks
        for (Location location : toRemove) {
            outsideBlocks.remove(location);
        }
    }

    /**
     * Notify players near a decayed block
     * @param location Block location
     * @param clan Clan that owned the block
     */
    private void notifyNearbyPlayers(Location location, Clan clan) {
        int notifyRadius = 20; // Notify players within 20 blocks
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(location.getWorld()) && 
                    player.getLocation().distanceSquared(location) <= notifyRadius * notifyRadius) {
                
                if (clan.isMember(player.getUniqueId())) {
                    player.sendMessage(ChatColor.YELLOW + "One of your clan's blocks outside the core area has decayed.");
                }
            }
        }
    }

    /**
     * Stop the decay task
     */
    public void cleanup() {
        if (decayTask != null) {
            decayTask.cancel();
        }
        outsideBlocks.clear();
    }

    /**
     * Class representing a block placed outside a clan's core area
     */
    public static class OutsideBlock {
        private final Location location;
        private final Clan clan;
        private final Material material;
        private final long placementTime;
        private final int decayTimeMinutes;

        /**
         * Create a new outside block
         * @param location Block location
         * @param clan Clan that placed the block
         * @param material Block material
         * @param decayTimeMinutes Time until decay in minutes
         */
        public OutsideBlock(Location location, Clan clan, Material material, int decayTimeMinutes) {
            this.location = location;
            this.clan = clan;
            this.material = material;
            this.placementTime = System.currentTimeMillis();
            this.decayTimeMinutes = decayTimeMinutes;
        }

        /**
         * Get the block location
         * @return Location
         */
        public Location getLocation() {
            return location;
        }

        /**
         * Get the clan that placed the block
         * @return Clan
         */
        public Clan getClan() {
            return clan;
        }

        /**
         * Get the block material
         * @return Material
         */
        public Material getMaterial() {
            return material;
        }

        /**
         * Get the placement time
         * @return Placement time in milliseconds
         */
        public long getPlacementTime() {
            return placementTime;
        }

        /**
         * Get the decay time
         * @return Decay time in minutes
         */
        public int getDecayTimeMinutes() {
            return decayTimeMinutes;
        }

        /**
         * Check if the block should decay
         * @return true if the block should decay
         */
        public boolean shouldDecay() {
            long elapsedMillis = System.currentTimeMillis() - placementTime;
            long decayMillis = decayTimeMinutes * 60 * 1000L; // Convert minutes to milliseconds
            
            return elapsedMillis >= decayMillis;
        }

        /**
         * Get the time until decay in milliseconds
         * @return Time until decay in milliseconds
         */
        public long getTimeUntilDecay() {
            long elapsedMillis = System.currentTimeMillis() - placementTime;
            long decayMillis = decayTimeMinutes * 60 * 1000L; // Convert minutes to milliseconds
            
            return Math.max(0, decayMillis - elapsedMillis);
        }
    }
}
