package Factions.miniFactions.managers;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.models.Clan;
import Factions.miniFactions.models.CoreBlock;
import Factions.miniFactions.models.DefenseBlock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages raiding mechanics including explosive placement and defense block damage
 */
public class RaidManager {

    private final MiniFactions plugin;
    private final Map<Location, BukkitTask> activeExplosives = new HashMap<>();

    public RaidManager(MiniFactions plugin) {
        this.plugin = plugin;
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
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            ItemStack handItem = player.getInventory().getItemInMainHand();
            if (handItem.getAmount() > 1) {
                handItem.setAmount(handItem.getAmount() - 1);
                player.getInventory().setItemInMainHand(handItem);
            } else {
                player.getInventory().setItemInMainHand(null);
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

        // Check if it's a defense block
        if (block.getType() == CraftingManager.getDefenseBlockMaterial()) {
            // Get the defense block from storage
            DefenseBlock defense = plugin.getDataStorage().getDefenseBlock(location);
            if (defense != null) {
                // Get the clan that owns the defense block
                Clan targetClan = defense.getClan();

                // Create explosion effect
                location.getWorld().createExplosion(location, 0, false, false);

                // Remove the defense block
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
}
