package Factions.miniFactions.managers;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.models.Clan;
import Factions.miniFactions.models.CoreBlock;
import Factions.miniFactions.models.DefenseBlock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ConcurrentHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages raiding mechanics including explosive placement and defense block damage
 */
public class RaidManager {

    private final MiniFactions plugin;
    private final Map<Location, BukkitTask> activeExplosives = new ConcurrentHashMap<>();
    private BukkitTask particleTask;
    private final DefenseBlockVisualManager visualManager;

    public RaidManager(MiniFactions plugin) {
        this.plugin = plugin;
        this.visualManager = new DefenseBlockVisualManager(plugin);
        startParticleTask();
    }

    /**
     * Start the task that shows particles around defense blocks
     */
    private void startParticleTask() {
        try {
            // Run task every 5 seconds (100 ticks)
            particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                try {
                    // Get all defense blocks
                    Map<Location, DefenseBlock> defenseBlocks = plugin.getDataStorage().getDefenseBlocks();
                    if (defenseBlocks == null || defenseBlocks.isEmpty()) {
                        return;
                    }

                    // Show particles for each defense block
                    for (DefenseBlock defense : defenseBlocks.values()) {
                        if (defense == null || defense.getLocation() == null) {
                            continue;
                        }

                        Block block = defense.getLocation().getBlock();
                        Material blockType = block.getType();

                        // Check if the block is a valid defense block material
                        if (isDefenseBlockMaterial(blockType)) {
                            // Only show particles occasionally to reduce visual clutter
                            if (Math.random() < 0.3) { // 30% chance each cycle
                                showTierParticles(block, defense.getTier());
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Error in particle task: " + e.getMessage());
                    e.printStackTrace();
                }
            }, 100L, 100L); // Initial delay: 100 ticks, Period: 100 ticks

            plugin.getLogger().info("Started defense block particle effects task");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to start particle task: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle explosive placement on a defense block
     * @param player Player placing the explosive
     * @param defenseBlock Defense block being targeted
     * @param explosiveItem Explosive item being used
     * @return true if successful
     */
    public boolean placeExplosive(Player player, Block defenseBlock, ItemStack explosiveItem) {
        // Get the defense block from storage
        DefenseBlock defense = plugin.getDataStorage().getDefenseBlock(defenseBlock.getLocation());
        if (defense == null) {
            player.sendMessage(ChatColor.RED + "This is not a valid defense block.");
            return false;
        }

        // Get the explosive tier
        int explosiveTier = 1;
        if (explosiveItem.hasItemMeta() && explosiveItem.getItemMeta().hasCustomModelData()) {
            explosiveTier = explosiveItem.getItemMeta().getCustomModelData();
        }

        // Get the defense block tier
        int defenseTier = defense.getTier();

        // Check if explosive tier is high enough
        if (explosiveTier < defenseTier) {
            player.sendMessage(ChatColor.RED + "This defense block requires a Tier " + defenseTier +
                    " or higher explosive to damage.");
            return false;
        }

        // Get the clan that owns the defense block
        Clan targetClan = defense.getClan();

        // Get the player's clan
        Clan playerClan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());

        // Check if player is in a clan
        if (playerClan == null) {
            player.sendMessage(ChatColor.RED + "You must be in a clan to raid other clans.");
            return false;
        }

        // Check if player is trying to raid their own clan
        if (playerClan.equals(targetClan)) {
            player.sendMessage(ChatColor.RED + "You cannot raid your own clan.");
            return false;
        }

        // Get fuse time from config
        int fuseTime = plugin.getConfigManager().getConfig().getInt("raiding.explosives." + explosiveTier + ".fuse-time", 5);

        // Consume the explosive item
        if (player.getGameMode() != GameMode.CREATIVE) {
            ItemStack handItem = player.getInventory().getItemInMainHand();
            if (handItem != null) {
                if (handItem.getAmount() > 1) {
                    handItem.setAmount(handItem.getAmount() - 1);
                    player.getInventory().setItemInMainHand(handItem);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
            }
        }

        // Notify players
        player.sendMessage(ChatColor.GREEN + "Explosive placed! It will detonate in " + fuseTime + " seconds.");

        // Notify target clan members
        for (UUID memberUUID : targetClan.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatColor.RED + "Your clan is being raided by " + player.getName() +
                        "! A defense block is under attack!");
            }
        }

        // Create final copies of variables for lambda
        final Location blockLocation = defenseBlock.getLocation();
        final int finalExplosiveTier = explosiveTier;
        final Player finalPlayer = player;

        // Schedule explosion
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Remove from active explosives
            activeExplosives.remove(blockLocation);

            // Handle explosion
            handleExplosion(blockLocation, finalExplosiveTier, finalPlayer);
        }, fuseTime * 20L); // Convert seconds to ticks

        // Add to active explosives
        activeExplosives.put(defenseBlock.getLocation(), task);

        return true;
    }

    /**
     * Handle an explosion at a location
     * @param location Location of the explosion
     * @param explosiveTier Tier of the explosive
     * @param player Player who placed the explosive
     */
    private void handleExplosion(Location location, int explosiveTier, Player player) {
        // Get the block at the location
        Block block = location.getBlock();

        // Check if it's a defense block (any of the terracotta colors)
        if (isDefenseBlockMaterial(block.getType())) {
            // Get the defense block from storage
            DefenseBlock defense = plugin.getDataStorage().getDefenseBlock(location);
            if (defense != null) {
                // Get the clan that owns the defense block
                Clan targetClan = defense.getClan();

                // Create explosion effect
                location.getWorld().createExplosion(location, 0, false, false);

                // Reduce defense block tier by 1
                if (defense.getTier() > 1) {
                    // Reduce tier
                    defense.reduceTier();

                    // Update the block appearance to match the tier
                    updateDefenseBlockMaterial(block, defense);

                    // Show particles for the tier change effect
                    showTierChangeParticles(block.getLocation(), defense.getTier());

                    // Notify players
                    if (player != null && player.isOnline()) {
                        player.sendMessage(ChatColor.GREEN + "Defense block damaged! Reduced to Tier " + defense.getTier() + ".");
                    }

                    // Notify target clan members
                    for (UUID memberUUID : targetClan.getMembers().keySet()) {
                        Player member = Bukkit.getPlayer(memberUUID);
                        if (member != null && member.isOnline()) {
                            member.sendMessage(ChatColor.RED + "One of your defense blocks has been damaged to Tier " + defense.getTier() + "!");
                        }
                    }
                } else {
                    // If already at tier 1, remove the block
                    block.setType(Material.AIR);

                    // Remove from storage
                    targetClan.removeDefenseBlock(defense);
                    plugin.getDataStorage().removeDefenseBlock(location);

                    // Notify players
                    if (player != null && player.isOnline()) {
                        player.sendMessage(ChatColor.GREEN + "Defense block destroyed!");
                    }

                    // Notify target clan members
                    for (UUID memberUUID : targetClan.getMembers().keySet()) {
                        Player member = Bukkit.getPlayer(memberUUID);
                        if (member != null && member.isOnline()) {
                            member.sendMessage(ChatColor.RED + "One of your defense blocks has been destroyed!");
                        }
                    }
                }

                // Notification for clan members is now handled in the tier reduction logic above

                // Check if this was the last defense block
                if (targetClan.getDefenseBlockCount() == 0) {
                    // Check if there's a core block
                    CoreBlock coreBlock = targetClan.getCoreBlock();
                    if (coreBlock != null) {
                        // Notify target clan members
                        for (UUID memberUUID : targetClan.getMembers().keySet()) {
                            Player member = Bukkit.getPlayer(memberUUID);
                            if (member != null && member.isOnline()) {
                                member.sendMessage(ChatColor.RED + "All your defense blocks have been destroyed! " +
                                        "Your core block is now vulnerable!");
                            }
                        }
                    }
                }
            }
        } else if (block.getType() == CoreBlockManager.getCoreBlockMaterial()) {
            // Get the core block from storage
            CoreBlock coreBlock = plugin.getDataStorage().getCoreBlock(location);
            if (coreBlock != null) {
                // Get the clan that owns the core block
                Clan targetClan = coreBlock.getClan();

                // Check if all defense blocks are destroyed
                if (targetClan.getDefenseBlockCount() == 0) {
                    // Create explosion effect
                    location.getWorld().createExplosion(location, 0, false, false);

                    // Break the core block
                    plugin.getCoreBlockManager().breakCoreBlock(location, player);

                    // Notify players
                    if (player != null && player.isOnline()) {
                        player.sendMessage(ChatColor.GREEN + "Core block destroyed!");

                        // Get player's clan
                        Clan playerClan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                        if (playerClan != null) {
                            // Award points to raider's clan
                            int raidPoints = targetClan.getPoints() / 4; // 25% of target clan's points
                            playerClan.addPoints(raidPoints);
                            player.sendMessage(ChatColor.GREEN + "Your clan gained " + raidPoints +
                                    " points for successfully raiding " + targetClan.getName() + "!");
                        }
                    }
                } else {
                    // Notify player that they need to destroy all defense blocks first
                    if (player != null && player.isOnline()) {
                        player.sendMessage(ChatColor.RED + "You must destroy all defense blocks before attacking the core block!");
                    }
                }
            }
        }
    }

    /**
     * Cancel all active explosives
     */
    public void cancelAllExplosives() {
        for (BukkitTask task : activeExplosives.values()) {
            task.cancel();
        }
        activeExplosives.clear();
    }

    /**
     * Clean up resources when the plugin is disabled
     */
    public void cleanup() {
        cancelAllExplosives();

        // Cancel the particle task
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }

        // Clean up visual manager
        visualManager.cleanup();
    }

    /**
     * Get the defense block visual manager
     * @return The visual manager
     */
    public DefenseBlockVisualManager getVisualManager() {
        return visualManager;
    }

    /**
     * Update the material of a defense block to match its current tier
     * @param block The block to update
     * @param defense The defense block data
     */
    private void updateDefenseBlockMaterial(Block block, DefenseBlock defense) {
        // Change the block material based on tier
        Material material = getTierMaterial(defense.getTier());
        block.setType(material);

        // Update the defense block material in the model
        defense.setMaterial(material);
    }

    /**
     * Get the material to use for a specific defense block tier
     * @param tier The tier level
     * @return The material to use
     */
    private Material getTierMaterial(int tier) {
        return CraftingManager.getTierMaterial(tier);
    }

    /**
     * Check if a material is a valid defense block material
     * @param material The material to check
     * @return true if it's a defense block material
     */
    private boolean isDefenseBlockMaterial(Material material) {
        return material == Material.WHITE_TERRACOTTA ||
               material == Material.LIGHT_GRAY_TERRACOTTA ||
               material == Material.BROWN_TERRACOTTA ||
               material == Material.GRAY_TERRACOTTA ||
               material == Material.BLACK_TERRACOTTA;
    }

    /**
     * Show particles around a defense block to indicate its tier
     * @param block The block to show particles around
     * @param tier The tier of the defense block
     */
    private void showTierParticles(Block block, int tier) {
        Location loc = block.getLocation().add(0.5, 0.5, 0.5); // Center of block

        // Display different colored particles based on tier
        Particle primaryParticle;
        Particle secondaryParticle;

        if (tier >= 5) {
            // Tier 5: Purple particles
            primaryParticle = Particle.PORTAL;
            secondaryParticle = Particle.DRAGON_BREATH;
            // Spawn more particles for higher tiers
            block.getWorld().spawnParticle(primaryParticle, loc, 40, 0.4, 0.4, 0.4, 0.05);
            block.getWorld().spawnParticle(secondaryParticle, loc, 20, 0.3, 0.3, 0.3, 0.01);
        } else if (tier >= 4) {
            // Tier 4: Blue particles
            primaryParticle = Particle.PORTAL;
            secondaryParticle = Particle.DRAGON_BREATH;
            block.getWorld().spawnParticle(primaryParticle, loc, 30, 0.4, 0.4, 0.4, 0.05);
            block.getWorld().spawnParticle(secondaryParticle, loc, 15, 0.3, 0.3, 0.3, 0.01);
        } else if (tier >= 3) {
            // Tier 3: Cyan particles
            primaryParticle = Particle.CLOUD;
            secondaryParticle = Particle.CRIT;
            block.getWorld().spawnParticle(primaryParticle, loc, 25, 0.3, 0.3, 0.3, 0.05);
            block.getWorld().spawnParticle(secondaryParticle, loc, 10, 0.2, 0.2, 0.2, 0.01);
        } else if (tier >= 2) {
            // Tier 2: Green particles
            primaryParticle = Particle.CRIT;
            secondaryParticle = Particle.CLOUD;
            block.getWorld().spawnParticle(primaryParticle, loc, 20, 0.3, 0.3, 0.3, 0.05);
            block.getWorld().spawnParticle(secondaryParticle, loc, 10, 0.2, 0.2, 0.2, 0.01);
        } else {
            // Tier 1: Red particles
            primaryParticle = Particle.FLAME;
            block.getWorld().spawnParticle(primaryParticle, loc, 15, 0.2, 0.2, 0.2, 0.05);
        }
    }

    /**
     * Show special particles when a defense block changes tier
     * @param location The location of the block
     * @param tier The new tier of the defense block
     */
    private void showTierChangeParticles(Location location, int tier) {
        Location loc = location.clone().add(0.5, 0.5, 0.5); // Center of block

        // Create a more dramatic effect for tier changes
        // First, create an explosion-like effect
        location.getWorld().spawnParticle(Particle.FLAME, loc, 10, 0.3, 0.3, 0.3, 0.05);

        // Then add tier-specific particles in a spiral pattern
        Particle particle;
        switch (tier) {
            case 5:
                particle = Particle.DRAGON_BREATH;
                break;
            case 4:
                particle = Particle.PORTAL;
                break;
            case 3:
                particle = Particle.CLOUD;
                break;
            case 2:
                particle = Particle.CRIT;
                break;
            default:
                particle = Particle.FLAME;
                break;
        }

        // Create a spiral effect
        for (double y = 0; y < 2; y += 0.1) {
            double radius = 0.5 - (y / 4);
            double x = Math.sin(y * Math.PI * 4) * radius;
            double z = Math.cos(y * Math.PI * 4) * radius;
            location.getWorld().spawnParticle(particle, loc.clone().add(x, y, z), 2, 0.05, 0.05, 0.05, 0.01);
        }

        // Play a sound effect
        location.getWorld().playSound(location, Sound.BLOCK_ANVIL_LAND, 0.5f, 1.0f);
    }
}
