package Factions.miniFactions.managers;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.models.Clan;
import Factions.miniFactions.models.CoreBlock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CoreBlockManager {

    private final MiniFactions plugin;
    private final Map<UUID, BukkitTask> beaconEffectTasks = new HashMap<>();

    // Material for core blocks
    private static final Material CORE_BLOCK_MATERIAL = Material.BEACON;

    public CoreBlockManager(MiniFactions plugin) {
        this.plugin = plugin;
        startBeaconEffectTask();
        startUpkeepTask();
    }

    /**
     * Create a core block
     * @param location Block location
     * @param clan Owning clan
     * @param player Player creating the block
     * @return The new core block, or null if creation failed
     */
    public CoreBlock createCoreBlock(Location location, Clan clan, Player player) {
        // Check if clan already has a core block
        if (clan.getCoreBlock() != null) {
            player.sendMessage(ChatColor.RED + "Your clan already has a core block. Destroy it first.");
            return null;
        }

        // Check if location is valid
        if (!isValidCoreLocation(location)) {
            player.sendMessage(ChatColor.RED + "Invalid core block location. It must have air above it and not be floating.");
            return null;
        }

        // Check if within another clan's AOI
        if (isWithinOtherClanAOI(location, clan)) {
            player.sendMessage(ChatColor.RED + "You cannot place a core block within another clan's area of influence.");
            return null;
        }

        // Create the core block
        CoreBlock coreBlock = new CoreBlock(location, clan);
        clan.setCoreBlock(coreBlock);

        // Set the block
        Block block = location.getBlock();
        block.setType(CORE_BLOCK_MATERIAL);

        // Add to storage
        plugin.getDataStorage().addCoreBlock(coreBlock);

        // Start beacon effect if enabled
        if (plugin.getConfigManager().getConfig().getBoolean("core.beacon-effect", true)) {
            startBeaconEffect(clan);
        }

        player.sendMessage(ChatColor.GREEN + "Core block created successfully!");
        return coreBlock;
    }

    /**
     * Remove a core block
     * @param location Block location
     * @return true if successful
     */
    public boolean removeCoreBlock(Location location) {
        CoreBlock coreBlock = plugin.getDataStorage().getCoreBlock(location);
        if (coreBlock == null) {
            return false;
        }

        Clan clan = coreBlock.getClan();

        // Remove from clan
        clan.setCoreBlock(null);

        // Remove from storage
        plugin.getDataStorage().removeCoreBlock(location);

        // Stop beacon effect
        stopBeaconEffect(clan);

        // Set block to air
        location.getBlock().setType(Material.AIR);

        return true;
    }

    /**
     * Break a core block
     * @param location Block location
     * @param breaker Player breaking the block
     * @return true if successful
     */
    public boolean breakCoreBlock(Location location, Player breaker) {
        CoreBlock coreBlock = plugin.getDataStorage().getCoreBlock(location);
        if (coreBlock == null) {
            return false;
        }

        Clan clan = coreBlock.getClan();
        Clan breakerClan = plugin.getClanManager().getClanByPlayer(breaker.getUniqueId());

        // Check if breaker is from the same clan
        boolean isClanMember = breakerClan != null && breakerClan.equals(clan);

        // If breaker is not a clan member, handle as enemy raid
        if (!isClanMember) {
            // Calculate points to give to breaker's clan
            int pointsPercentage = plugin.getConfigManager().getConfig().getInt("core.points-on-break-percentage", 50);
            int pointsToGive = (clan.getPoints() * pointsPercentage) / 100;

            // Check if breaker is from another clan (enemy)
            boolean isEnemy = breakerClan != null;

            // Give points to breaker's clan if they have one
            if (breakerClan != null && pointsToGive > 0) {
                breakerClan.addPoints(pointsToGive);
                breaker.sendMessage(ChatColor.GREEN + "Your clan gained " + pointsToGive + " points from breaking " +
                        clan.getName() + "'s core block!");
            }

            // Notify clan members
            for (UUID memberUUID : clan.getMembers().keySet()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && member.isOnline()) {
                    member.sendMessage(ChatColor.RED + "Your clan's core block has been destroyed by " + breaker.getName() + "!");
                }
            }

            // Remove the core block
            boolean removed = removeCoreBlock(location);

            // If destroyed by an enemy, give the clan leader a new core block
            if (removed && isEnemy) {
                plugin.getClanManager().giveCoreBlockToLeader(clan);
            }

            return removed;
        } else {
            // Breaker is a clan member
            // Check if breaker is leader or co-leader
            if (!clan.isLeader(breaker.getUniqueId()) && !clan.isCoLeader(breaker.getUniqueId())) {
                breaker.sendMessage(ChatColor.RED + "Only the clan leader or co-leaders can break the core block.");
                return false;
            }

            // Notify clan members
            for (UUID memberUUID : clan.getMembers().keySet()) {
                if (memberUUID.equals(breaker.getUniqueId())) continue; // Skip the breaker

                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && member.isOnline()) {
                    member.sendMessage(ChatColor.YELLOW + "Your clan's core block has been removed by " + breaker.getName() + ".");
                }
            }

            // Remove the core block
            boolean removed = removeCoreBlock(location);

            // Give the core block back to the player
            if (removed) {
                int coreLevel = coreBlock.getLevel();
                ItemStack coreBlockItem = plugin.getCraftingManager().createCoreBlock(coreLevel);
                breaker.getInventory().addItem(coreBlockItem);
                breaker.sendMessage(ChatColor.GREEN + "You have received your clan's core block.");
            }

            return removed;
        }
    }

    /**
     * Upgrade a core block
     * @param coreBlock Core block to upgrade
     * @param player Player upgrading
     * @return true if successful
     */
    public boolean upgradeCoreBlock(CoreBlock coreBlock, Player player) {
        Clan clan = coreBlock.getClan();

        // Check if player is in the clan
        if (!clan.isMember(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are not a member of this clan.");
            return false;
        }

        // Check if player has permission
        if (!clan.isLeader(player.getUniqueId()) && !clan.isCoLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the clan leader or co-leaders can upgrade the core block.");
            return false;
        }

        // Check if already at max level
        int upgradeCost = coreBlock.getUpgradeCost();
        if (upgradeCost == -1) {
            player.sendMessage(ChatColor.RED + "Your core block is already at maximum level.");
            return false;
        }

        // Check if clan has enough points
        if (clan.getPoints() < upgradeCost) {
            player.sendMessage(ChatColor.RED + "Your clan doesn't have enough points. You need " + upgradeCost + " points.");
            return false;
        }

        // Upgrade the core block
        clan.removePoints(upgradeCost);
        coreBlock.upgrade();

        // Notify clan members
        for (UUID memberUUID : clan.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatColor.GREEN + "Your clan's core block has been upgraded to level " +
                        coreBlock.getLevel() + " by " + player.getName() + "!");
            }
        }

        player.sendMessage(ChatColor.GREEN + "Core block upgraded to level " + coreBlock.getLevel() + "!");
        return true;
    }

    /**
     * Check if a location is valid for a core block
     * @param location Location to check
     * @return true if valid
     */
    public boolean isValidCoreLocation(Location location) {
        Block block = location.getBlock();

        // Check if there's air above
        Block blockAbove = block.getRelative(0, 1, 0);
        if (!blockAbove.getType().isAir()) {
            return false;
        }

        // Check if not floating
        Block blockBelow = block.getRelative(0, -1, 0);
        return !blockBelow.getType().isAir();
    }

    /**
     * Check if a location is within another clan's area of influence
     * @param location Location to check
     * @param excludeClan Clan to exclude from the check
     * @return true if within another clan's AOI
     */
    public boolean isWithinOtherClanAOI(Location location, Clan excludeClan) {
        for (CoreBlock coreBlock : plugin.getDataStorage().getCoreBlocks().values()) {
            if (excludeClan != null && coreBlock.getClan().equals(excludeClan)) {
                continue;
            }

            Location coreLoc = coreBlock.getLocation();
            int radius = coreBlock.getBuildableArea();

            // Check if in same world
            if (!location.getWorld().equals(coreLoc.getWorld())) {
                continue;
            }

            // Check if within radius
            double distanceSquared = location.distanceSquared(coreLoc);
            if (distanceSquared <= radius * radius) {
                return true;
            }
        }

        return false;
    }

    /**
     * Start the beacon effect for a clan
     * @param clan Clan to start effect for
     */
    private void startBeaconEffect(Clan clan) {
        if (clan.getCoreBlock() == null) {
            return;
        }

        // Stop existing task if any
        stopBeaconEffect(clan);

        // Start new task
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (clan.getCoreBlock() == null) {
                stopBeaconEffect(clan);
                return;
            }

            // Apply effect to nearby clan members
            Location coreLoc = clan.getCoreBlock().getLocation();
            int radius = clan.getCoreBlock().getBuildableArea();

            for (UUID memberUUID : clan.getMembers().keySet()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member == null || !member.isOnline()) {
                    continue;
                }

                // Check if in same world
                if (!member.getWorld().equals(coreLoc.getWorld())) {
                    continue;
                }

                // Check if within radius
                double distanceSquared = member.getLocation().distanceSquared(coreLoc);
                if (distanceSquared <= radius * radius) {
                    // Apply effect
                    member.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false));
                }
            }
        }, 20L, 20L); // Run every second

        beaconEffectTasks.put(clan.getLeader(), task);
    }

    /**
     * Stop the beacon effect for a clan
     * @param clan Clan to stop effect for
     */
    private void stopBeaconEffect(Clan clan) {
        BukkitTask task = beaconEffectTasks.remove(clan.getLeader());
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Start the beacon effect task for all clans
     */
    private void startBeaconEffectTask() {
        if (!plugin.getConfigManager().getConfig().getBoolean("core.beacon-effect", true)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Clan clan : plugin.getDataStorage().getClans().values()) {
                if (clan.getCoreBlock() != null) {
                    startBeaconEffect(clan);
                }
            }
        }, 20L); // Delay by 1 second to ensure all data is loaded
    }

    /**
     * Start the upkeep task
     */
    private void startUpkeepTask() {
        // Run every hour
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Clan clan : plugin.getDataStorage().getClans().values()) {
                if (clan.getCoreBlock() != null && clan.getCoreBlock().isUpkeepDue()) {
                    if (!clan.getCoreBlock().payUpkeep()) {
                        // Notify clan members
                        for (UUID memberUUID : clan.getMembers().keySet()) {
                            Player member = Bukkit.getPlayer(memberUUID);
                            if (member != null && member.isOnline()) {
                                member.sendMessage(ChatColor.RED + "Your clan could not pay the upkeep cost! " +
                                        "Defense blocks will start to decay.");
                            }
                        }
                    }
                }
            }
        }, 20L * 60L * 60L, 20L * 60L * 60L); // Run every hour
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        // Cancel all beacon effect tasks
        for (BukkitTask task : beaconEffectTasks.values()) {
            task.cancel();
        }
        beaconEffectTasks.clear();
    }

    /**
     * Get the core block material
     * @return Core block material
     */
    public static Material getCoreBlockMaterial() {
        return CORE_BLOCK_MATERIAL;
    }
}
