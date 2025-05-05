package Factions.miniFactions.managers;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.models.Clan;
import Factions.miniFactions.models.ClaimBlock;
import Factions.miniFactions.models.ClanDoor;
import Factions.miniFactions.models.CoreBlock;
import Factions.miniFactions.models.DefenseBlock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manager for handling upkeep of all block types
 */
public class UpkeepManager {

    private final MiniFactions plugin;
    private BukkitTask upkeepTask;
    private final Map<UUID, Long> exemptClans = new HashMap<>();

    /**
     * Create a new upkeep manager
     * @param plugin Plugin instance
     */
    public UpkeepManager(MiniFactions plugin) {
        this.plugin = plugin;
        startUpkeepTask();
    }

    /**
     * Start the upkeep check task
     */
    private void startUpkeepTask() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        int checkInterval = config.getInt("upkeep.check-interval", 1);
        long intervalTicks = checkInterval * 60 * 60 * 20L; // Convert hours to ticks

        upkeepTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkAllUpkeep,
                20 * 60, // Start after 1 minute
                intervalTicks); // Run at configured interval

        plugin.getLogger().info("Started upkeep task with interval: " + checkInterval + " hours");
    }

    /**
     * Check upkeep for all clans and their blocks
     */
    public void checkAllUpkeep() {
        plugin.getLogger().info("Running upkeep check for all clans...");

        for (Clan clan : plugin.getDataStorage().getClans().values()) {
            // Skip exempt clans
            if (isExempt(clan.getLeader())) {
                continue;
            }

            checkCoreBlockUpkeep(clan);
            checkClaimBlocksUpkeep(clan);
            checkDefenseBlocksUpkeep(clan);
            checkClanDoorsUpkeep(clan);
        }
    }

    /**
     * Check upkeep for a clan's core block
     * @param clan The clan to check
     */
    private void checkCoreBlockUpkeep(Clan clan) {
        CoreBlock coreBlock = clan.getCoreBlock();
        if (coreBlock == null) {
            return;
        }

        FileConfiguration config = plugin.getConfigManager().getConfig();
        boolean upkeepEnabled = config.getBoolean("upkeep.core.enabled", true);

        if (!upkeepEnabled) {
            return;
        }

        if (coreBlock.isUpkeepDue()) {
            plugin.getLogger().info("Core block upkeep is due for clan: " + clan.getName());

            // Try to pay upkeep
            if (coreBlock.payUpkeep()) {
                // Upkeep paid successfully
                notifyClanMembers(clan, ChatColor.GREEN + "Your clan paid the core block upkeep cost of " +
                        coreBlock.getUpkeepCost() + " points.");
                return;
            }

            // Upkeep not paid, check grace period
            long gracePeriod = config.getLong("upkeep.core.grace-period", 48) * 60 * 60 * 1000L; // Convert hours to milliseconds
            long timeSinceUpkeepDue = System.currentTimeMillis() - (coreBlock.getLastUpkeepTime() +
                    config.getLong("core.upkeep.payment-interval", 24) * 60 * 60 * 1000L);

            if (timeSinceUpkeepDue > gracePeriod) {
                // Apply penalty
                String penaltyType = config.getString("upkeep.core.penalty-type", "LEVEL_REDUCTION");

                switch (penaltyType) {
                    case "LEVEL_REDUCTION":
                        int levelReduction = config.getInt("upkeep.core.level-reduction", 1);
                        int oldLevel = coreBlock.getLevel();
                        int newLevel = Math.max(1, oldLevel - levelReduction);

                        if (newLevel < oldLevel) {
                            coreBlock.setLevel(newLevel);
                            notifyClanMembers(clan, ChatColor.RED + "Your clan could not pay the core block upkeep! " +
                                    "Core level reduced from " + oldLevel + " to " + newLevel + ".");
                        }
                        break;

                    case "POINT_PENALTY":
                        int penaltyPercentage = config.getInt("upkeep.core.point-penalty-percentage", 10);
                        int pointPenalty = (clan.getPoints() * penaltyPercentage) / 100;

                        if (pointPenalty > 0) {
                            clan.removePoints(pointPenalty);
                            notifyClanMembers(clan, ChatColor.RED + "Your clan could not pay the core block upkeep! " +
                                    "You lost " + pointPenalty + " points as a penalty.");
                        }
                        break;

                    case "NONE":
                    default:
                        notifyClanMembers(clan, ChatColor.RED + "Your clan could not pay the core block upkeep! " +
                                "Please add more points to your clan.");
                        break;
                }

                // Reset upkeep time to avoid continuous penalties
                coreBlock.updateUpkeepTime();
            } else {
                // Still in grace period, send warning
                long hoursRemaining = (gracePeriod - timeSinceUpkeepDue) / (60 * 60 * 1000L);
                notifyClanMembers(clan, ChatColor.RED + "Your clan cannot pay the core block upkeep of " +
                        coreBlock.getUpkeepCost() + " points! Penalties will apply in " + hoursRemaining + " hours.");
            }
        } else {
            // Check if upkeep is approaching
            FileConfiguration coreConfig = plugin.getConfigManager().getConfig();
            int paymentInterval = coreConfig.getInt("core.upkeep.payment-interval", 24);
            long intervalMillis = paymentInterval * 60 * 60 * 1000L; // Convert hours to milliseconds
            long warningTime = config.getLong("upkeep.warning-time", 6) * 60 * 60 * 1000L; // Convert hours to milliseconds

            long timeUntilUpkeep = (coreBlock.getLastUpkeepTime() + intervalMillis) - System.currentTimeMillis();

            if (timeUntilUpkeep > 0 && timeUntilUpkeep < warningTime) {
                // Send warning
                long hoursRemaining = timeUntilUpkeep / (60 * 60 * 1000L);
                notifyClanMembers(clan, ChatColor.YELLOW + "Core block upkeep of " + coreBlock.getUpkeepCost() +
                        " points will be due in " + hoursRemaining + " hours.");
            }
        }
    }

    /**
     * Check upkeep for a clan's claim blocks
     * @param clan The clan to check
     */
    private void checkClaimBlocksUpkeep(Clan clan) {
        Set<ClaimBlock> claimBlocks = clan.getClaimBlocks();
        if (claimBlocks.isEmpty()) {
            return;
        }

        FileConfiguration config = plugin.getConfigManager().getConfig();
        boolean upkeepEnabled = config.getBoolean("upkeep.claim.enabled", true);

        if (!upkeepEnabled) {
            return;
        }

        // Check if core block upkeep is paid
        CoreBlock coreBlock = clan.getCoreBlock();
        if (coreBlock == null || (coreBlock.isUpkeepDue() && !coreBlock.payUpkeep())) {
            // Core block upkeep not paid, apply penalties to claim blocks
            String penaltyType = config.getString("upkeep.claim.penalty-type", "LEVEL_REDUCTION");
            long gracePeriod = config.getLong("upkeep.claim.grace-period", 24) * 60 * 60 * 1000L; // Convert hours to milliseconds

            // Only apply penalties if outside grace period
            if (coreBlock != null) {
                long timeSinceUpkeepDue = System.currentTimeMillis() - (coreBlock.getLastUpkeepTime() +
                        config.getLong("core.upkeep.payment-interval", 24) * 60 * 60 * 1000L);

                if (timeSinceUpkeepDue <= gracePeriod) {
                    return;
                }
            }

            switch (penaltyType) {
                case "LEVEL_REDUCTION":
                    int levelReduction = config.getInt("upkeep.claim.level-reduction", 1);

                    for (ClaimBlock claimBlock : claimBlocks) {
                        int oldLevel = claimBlock.getLevel();
                        int newLevel = Math.max(1, oldLevel - levelReduction);

                        if (newLevel < oldLevel) {
                            claimBlock.setLevel(newLevel);
                        }
                    }

                    notifyClanMembers(clan, ChatColor.RED + "Your clan could not pay upkeep! " +
                            "All claim blocks have been reduced by " + levelReduction + " level(s).");
                    break;

                case "POINT_GENERATION_STOP":
                    // This is handled in the ClaimBlock.canGeneratePoints() method
                    notifyClanMembers(clan, ChatColor.RED + "Your clan could not pay upkeep! " +
                            "Claim blocks have stopped generating points.");
                    break;

                case "DESTRUCTION":
                    // Remove all claim blocks
                    for (ClaimBlock claimBlock : claimBlocks) {
                        Location location = claimBlock.getLocation();
                        location.getBlock().setType(Material.AIR);
                        plugin.getDataStorage().removeClaimBlock(location);
                    }

                    claimBlocks.clear();
                    notifyClanMembers(clan, ChatColor.RED + "Your clan could not pay upkeep! " +
                            "All claim blocks have been destroyed.");
                    break;
            }
        }
    }

    /**
     * Check upkeep for a clan's defense blocks
     * @param clan The clan to check
     */
    private void checkDefenseBlocksUpkeep(Clan clan) {
        Set<DefenseBlock> defenseBlocks = clan.getDefenseBlocks();
        if (defenseBlocks.isEmpty()) {
            return;
        }

        FileConfiguration config = plugin.getConfigManager().getConfig();
        boolean upkeepEnabled = config.getBoolean("upkeep.defense.enabled", true);

        if (!upkeepEnabled) {
            return;
        }

        // Check if core block upkeep is paid
        CoreBlock coreBlock = clan.getCoreBlock();
        if (coreBlock == null || (coreBlock.isUpkeepDue() && !coreBlock.payUpkeep())) {
            // Core block upkeep not paid, apply penalties to defense blocks
            String penaltyType = config.getString("upkeep.defense.penalty-type", "TIER_REDUCTION");
            long gracePeriod = config.getLong("upkeep.defense.grace-period", 12) * 60 * 60 * 1000L; // Convert hours to milliseconds

            // Only apply penalties if outside grace period
            if (coreBlock != null) {
                long timeSinceUpkeepDue = System.currentTimeMillis() - (coreBlock.getLastUpkeepTime() +
                        config.getLong("core.upkeep.payment-interval", 24) * 60 * 60 * 1000L);

                if (timeSinceUpkeepDue <= gracePeriod) {
                    return;
                }
            }

            switch (penaltyType) {
                case "TIER_REDUCTION":
                    int tierReduction = config.getInt("upkeep.defense.tier-reduction", 1);
                    int reducedCount = 0;

                    for (DefenseBlock defenseBlock : defenseBlocks) {
                        if (defenseBlock.getTier() > 1) {
                            for (int i = 0; i < tierReduction; i++) {
                                if (defenseBlock.reduceTier()) {
                                    reducedCount++;
                                }
                            }

                            // Update the block material to match the new tier
                            Location location = defenseBlock.getLocation();
                            if (location.getBlock().getType() == CraftingManager.getDefenseBlockMaterial()) {
                                // The block is still there, update it
                                // No need to change material as it's the same for all tiers
                            }
                        }
                    }

                    if (reducedCount > 0) {
                        notifyClanMembers(clan, ChatColor.RED + "Your clan could not pay upkeep! " +
                                reducedCount + " defense blocks have been reduced in tier.");
                    }
                    break;

                case "DESTRUCTION":
                    // Remove all defense blocks
                    for (DefenseBlock defenseBlock : defenseBlocks) {
                        Location location = defenseBlock.getLocation();
                        location.getBlock().setType(Material.AIR);
                        plugin.getDataStorage().removeDefenseBlock(location);
                    }

                    defenseBlocks.clear();
                    notifyClanMembers(clan, ChatColor.RED + "Your clan could not pay upkeep! " +
                            "All defense blocks have been destroyed.");
                    break;
            }
        }
    }

    /**
     * Check upkeep for a clan's trapdoors
     * @param clan The clan to check
     */
    private void checkClanDoorsUpkeep(Clan clan) {
        Set<ClanDoor> clanDoors = clan.getClanDoors();
        if (clanDoors.isEmpty()) {
            return;
        }

        FileConfiguration config = plugin.getConfigManager().getConfig();
        boolean upkeepEnabled = config.getBoolean("upkeep.door.enabled", true);

        if (!upkeepEnabled) {
            return;
        }

        // Check if core block upkeep is paid
        CoreBlock coreBlock = clan.getCoreBlock();
        if (coreBlock == null || (coreBlock.isUpkeepDue() && !coreBlock.payUpkeep())) {
            // Core block upkeep not paid, apply penalties to clan trapdoors
            String penaltyType = config.getString("upkeep.door.penalty-type", "DESTRUCTION");
            long gracePeriod = config.getLong("upkeep.door.grace-period", 6) * 60 * 60 * 1000L; // Convert hours to milliseconds

            // Only apply penalties if outside grace period
            if (coreBlock != null) {
                long timeSinceUpkeepDue = System.currentTimeMillis() - (coreBlock.getLastUpkeepTime() +
                        config.getLong("core.upkeep.payment-interval", 24) * 60 * 60 * 1000L);

                if (timeSinceUpkeepDue <= gracePeriod) {
                    return;
                }
            }

            switch (penaltyType) {
                case "PUBLIC_ACCESS":
                    // This would require additional implementation to make trapdoors public
                    // For now, just notify players
                    notifyClanMembers(clan, ChatColor.RED + "Your clan could not pay upkeep! " +
                            "All clan trapdoors are now accessible to everyone.");
                    break;

                case "DESTRUCTION":
                    // Remove all clan trapdoors
                    for (ClanDoor clanDoor : clanDoors) {
                        Location location = clanDoor.getLocation();
                        location.getBlock().setType(Material.AIR);
                        plugin.getDataStorage().removeClanDoor(location);
                    }

                    clanDoors.clear();
                    notifyClanMembers(clan, ChatColor.RED + "Your clan could not pay upkeep! " +
                            "All clan trapdoors have been destroyed.");
                    break;
            }
        }
    }

    /**
     * Notify all online clan members with a message
     * @param clan The clan
     * @param message The message to send
     */
    private void notifyClanMembers(Clan clan, String message) {
        for (UUID memberUUID : clan.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                member.sendMessage(message);
            }
        }
    }

    /**
     * Check if a clan is exempt from upkeep
     * @param clanLeader The clan leader UUID
     * @return true if exempt
     */
    public boolean isExempt(UUID clanLeader) {
        return exemptClans.containsKey(clanLeader);
    }

    /**
     * Set a clan's exemption status
     * @param clanLeader The clan leader UUID
     * @param exempt true to exempt, false to remove exemption
     * @param duration Duration in hours (0 for permanent)
     */
    public void setExempt(UUID clanLeader, boolean exempt, long duration) {
        if (exempt) {
            long expiryTime = duration > 0 ? System.currentTimeMillis() + (duration * 60 * 60 * 1000L) : Long.MAX_VALUE;
            exemptClans.put(clanLeader, expiryTime);
        } else {
            exemptClans.remove(clanLeader);
        }
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (upkeepTask != null) {
            upkeepTask.cancel();
            upkeepTask = null;
        }
    }
}
