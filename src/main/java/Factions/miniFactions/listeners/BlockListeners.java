package Factions.miniFactions.listeners;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.managers.ClaimBlockGUIManager;
import Factions.miniFactions.managers.ClaimBlockVisualManager;
import Factions.miniFactions.managers.CoreBlockManager;
import Factions.miniFactions.managers.CraftingManager;
import Factions.miniFactions.models.Clan;
import Factions.miniFactions.models.ClaimBlock;
import Factions.miniFactions.models.ClanDoor;
import Factions.miniFactions.models.CoreBlock;
import Factions.miniFactions.models.DefenseBlock;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.logging.Level;

public class BlockListeners implements Listener {

    private final MiniFactions plugin;
    private final ClaimBlockVisualManager claimVisualManager;

    public BlockListeners(MiniFactions plugin) {
        this.plugin = plugin;
        this.claimVisualManager = new ClaimBlockVisualManager(plugin);
    }

    /**
     * Handle block placement events
     * @param event The block place event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        try {
            Player player = event.getPlayer();
            Block block = event.getBlock();
            ItemStack item = event.getItemInHand();

            // Check if player is in a clan
            Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
            if (clan == null) {
                return;
            }

            // Handle core block placement
            if (block.getType() == CoreBlockManager.getCoreBlockMaterial()) {
                handleCoreBlockPlacement(player, block, clan);
                return;
            }

            // Handle claim block placement
            if (block.getType() == CraftingManager.getClaimBlockMaterial() &&
                    item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().getDisplayName().contains("Claim Block")) {
                handleClaimBlockPlacement(player, block, clan, item);
                return;
            }

            // Handle defense block placement
            if (isDefenseBlockMaterial(block.getType()) &&
                    item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().getDisplayName().contains("Defense Block")) {
                handleDefenseBlockPlacement(player, block, clan, item);
                return;
            }

            // Handle clan door placement
            if (block.getType() == CraftingManager.getClanDoorMaterial() &&
                    item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().getDisplayName().contains("Clan Door")) {
                handleClanDoorPlacement(player, block, clan, item);
                return;
            }

            // Check if within buildable area
            if (clan.getCoreBlock() != null) {
                Location coreLoc = clan.getCoreBlock().getLocation();
                int radius = clan.getCoreBlock().getBuildableArea();

                // Check if in same world
                if (!block.getWorld().equals(coreLoc.getWorld())) {
                    return;
                }

                // Check if within radius
                double distanceSquared = block.getLocation().distanceSquared(coreLoc);
                if (distanceSquared > radius * radius) {
                    // Allow building outside core area, but track the block for decay
                    if (plugin.getConfigManager().getConfig().getBoolean("core.outside-blocks.enabled", true)) {
                        // Track the block for decay
                        plugin.getOutsideBlockManager().trackBlock(block.getLocation(), clan, block.getType());

                        // Notify player
                        player.sendMessage(ChatColor.YELLOW + "You are building outside your clan's area of influence. This block will decay over time.");
                    } else {
                        // If outside blocks are disabled, prevent building
                        player.sendMessage(ChatColor.RED + "You can only build within your clan's area of influence.");
                        event.setCancelled(true);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling block place event: " + e.getMessage(), e);
            event.getPlayer().sendMessage(ChatColor.RED + "An error occurred while placing this block. Please contact an administrator.");
            event.setCancelled(true);
        }
    }

    /**
     * Handle block break events
     * @param event The block break event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        try {
            Player player = event.getPlayer();
            Block block = event.getBlock();

            // Check for core block
            if (block.getType() == CoreBlockManager.getCoreBlockMaterial()) {
                CoreBlock coreBlock = plugin.getDataStorage().getCoreBlock(block.getLocation());
                if (coreBlock != null) {
                    // Cancel event and handle core block breaking
                    event.setCancelled(true);
                    plugin.getCoreBlockManager().breakCoreBlock(block.getLocation(), player);
                    return;
                }
            }

            // Check for claim block
            if (block.getType() == CraftingManager.getClaimBlockMaterial()) {
                ClaimBlock claimBlock = plugin.getDataStorage().getClaimBlock(block.getLocation());
                if (claimBlock != null) {
                    // Get the clan that owns the claim block
                    Clan claimBlockClan = claimBlock.getClan();
                    // Get the player's clan
                    Clan playerClan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());

                    // Check if player is in the same clan as the claim block
                    if (playerClan != null && playerClan.equals(claimBlockClan)) {
                        // Allow clan members to break their own claim blocks
                        // Remove from storage
                        claimBlockClan.removeClaimBlock(claimBlock);
                        plugin.getDataStorage().removeClaimBlock(block.getLocation());

                        player.sendMessage(ChatColor.GREEN + "Claim block removed.");
                    } else {
                        // Claim blocks can only be broken by clan members
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "Claim blocks can only be broken by clan members.");
                    }
                    return;
                }
            }

            // Check for defense block (any of the terracotta colors)
            if (isDefenseBlockMaterial(block.getType())) {
            // Get the defense block from storage
            DefenseBlock defenseBlock = plugin.getDataStorage().getDefenseBlock(block.getLocation());
            if (defenseBlock != null) {
                // Get the clan that owns the defense block
                Clan defenseBlockClan = defenseBlock.getClan();
                // Get the player's clan
                Clan playerClan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());

                // Check if player is in the same clan as the defense block
                if (playerClan != null && playerClan.equals(defenseBlockClan)) {
                    // Allow clan members to break their own defense blocks
                    // Remove from storage
                    defenseBlockClan.removeDefenseBlock(defenseBlock);
                    plugin.getDataStorage().removeDefenseBlock(block.getLocation());

                    player.sendMessage(ChatColor.GREEN + "Defense block removed.");
                    return;
                } else {
                    // Defense blocks can only be broken by explosives or clan members
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Defense blocks can only be broken by explosives or clan members.");
                    return;
                }
            }
        }

            // Check if this is a tracked outside block
            if (plugin.getOutsideBlockManager().isTrackedBlock(block.getLocation())) {
                // Remove from tracking when broken
                plugin.getOutsideBlockManager().removeBlock(block.getLocation());
            }

            // Check for clan door
            if (block.getType() == CraftingManager.getClanDoorMaterial()) {
                ClanDoor clanDoor = plugin.getDataStorage().getClanDoor(block.getLocation());
                if (clanDoor != null) {
                    // Get the clan that owns the door
                    Clan doorClan = clanDoor.getClan();
                    // Get the player's clan
                    Clan playerClan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());

                    // Check if player is in the same clan as the door
                    if (playerClan != null && playerClan.equals(doorClan)) {
                        // Allow clan members to break their own doors
                        // Remove from storage
                        doorClan.removeClanDoor(clanDoor);
                        plugin.getDataStorage().removeClanDoor(block.getLocation());
                        player.sendMessage(ChatColor.GREEN + "Clan trapdoor removed.");
                    } else {
                        // Clan trapdoors can only be broken by clan members
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "Clan trapdoors can only be broken by clan members.");
                    }
                    return;
                }
            }

            // Allow players to break blocks in other clans' areas
            // Only core blocks, claim blocks, defense blocks, and clan doors are protected (handled above)
            // No need to restrict regular block breaking
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling block break event: " + e.getMessage(), e);
            event.getPlayer().sendMessage(ChatColor.RED + "An error occurred while breaking this block. Please contact an administrator.");
            event.setCancelled(true);
        }
    }

    /**
     * Handle block explosion events
     * @param event The block explode event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        try {
            // Get the blocks that will be affected by the explosion
            for (Block block : event.blockList()) {
                // Check if it's a defense block (any of the terracotta colors)
                if (isDefenseBlockMaterial(block.getType())) {
                    // Get the defense block from storage
                    DefenseBlock defenseBlock = plugin.getDataStorage().getDefenseBlock(block.getLocation());
                    if (defenseBlock != null) {
                        // Defense blocks can only be broken by explosives placed by players or clan members
                        // Natural explosions should not break defense blocks
                        // This is handled by the RaidManager for player-placed explosives
                        event.blockList().remove(block);
                    }
                }

                // Check if it's a core block
                if (block.getType() == CoreBlockManager.getCoreBlockMaterial()) {
                    // Get the core block from storage
                    CoreBlock coreBlock = plugin.getDataStorage().getCoreBlock(block.getLocation());
                    if (coreBlock != null) {
                        // Core blocks can only be broken by explosives placed by players or clan members
                        // Natural explosions should not break core blocks
                        // This is handled by the RaidManager for player-placed explosives
                        event.blockList().remove(block);
                    }
                }

                // Check if it's a claim block
                if (block.getType() == CraftingManager.getClaimBlockMaterial()) {
                    ClaimBlock claimBlock = plugin.getDataStorage().getClaimBlock(block.getLocation());
                    if (claimBlock != null) {
                        // Claim blocks can only be broken by clan members
                        // Natural explosions should not break claim blocks
                        event.blockList().remove(block);
                    }
                }

                // Check if it's a clan trapdoor
                if (block.getType() == CraftingManager.getClanDoorMaterial()) {
                    ClanDoor clanDoor = plugin.getDataStorage().getClanDoor(block.getLocation());
                    if (clanDoor != null) {
                        // Clan trapdoors can only be broken by clan members
                        // Natural explosions should not break clan trapdoors
                        event.blockList().remove(block);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling block explode event: " + e.getMessage(), e);
            // Cancel the explosion to be safe
            event.setCancelled(true);
        }
    }

    /**
     * Handle core block placement
     * @param player Player placing the block
     * @param block Block being placed
     * @param clan Player's clan
     */
    private void handleCoreBlockPlacement(Player player, Block block, Clan clan) {
        // Create core block
        plugin.getCoreBlockManager().createCoreBlock(block.getLocation(), clan, player);
    }

    /**
     * Handle claim block placement
     * @param player Player placing the block
     * @param block Block being placed
     * @param clan Player's clan
     * @param item Item used to place the block
     */
    private void handleClaimBlockPlacement(Player player, Block block, Clan clan, ItemStack item) {
        // Check if clan has a core block
        if (clan.getCoreBlock() == null) {
            player.sendMessage(ChatColor.RED + "Your clan needs a core block before placing claim blocks.");
            block.setType(Material.AIR);
            // The event will automatically return the item to the player
            return;
        }

        // Check if within core block area
        Location coreLoc = clan.getCoreBlock().getLocation();
        int radius = clan.getCoreBlock().getBuildableArea();

        // Check if in same world
        if (!block.getWorld().equals(coreLoc.getWorld())) {
            player.sendMessage(ChatColor.RED + "Claim blocks must be placed within your clan's area of influence.");
            block.setType(Material.AIR);
            // The event will automatically return the item to the player
            return;
        }

        // Check if within radius
        double distanceSquared = block.getLocation().distanceSquared(coreLoc);
        if (distanceSquared > radius * radius) {
            player.sendMessage(ChatColor.RED + "Claim blocks must be placed within your clan's area of influence.");
            block.setType(Material.AIR);
            // The event will automatically return the item to the player
            return;
        }

        // Check if clan has reached max claim blocks
        if (clan.getClaimBlockCount() >= clan.getCoreBlock().getMaxClaimBlocks()) {
            player.sendMessage(ChatColor.RED + "Your clan has reached the maximum number of claim blocks. Upgrade your core to place more.");
            block.setType(Material.AIR);
            // The event will automatically return the item to the player
            return;
        }

        // Create claim block
        // Extract level from item if available, default to level 1
        int level = 1;
        // We already have the item from the method parameter
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            level = item.getItemMeta().getCustomModelData();
        }

        ClaimBlock claimBlock = new ClaimBlock(block.getLocation(), clan);
        claimBlock.setLevel(level);
        clan.addClaimBlock(claimBlock);

        // Add to storage
        plugin.getDataStorage().addClaimBlock(claimBlock);

        player.sendMessage(ChatColor.GREEN + "Claim block placed successfully! It will generate " +
                claimBlock.getPointsPerDay() + " points per day.");
    }

    /**
     * Handle defense block placement
     * @param player Player placing the block
     * @param block Block being placed
     * @param clan Player's clan
     * @param item Item used to place the block
     */
    private void handleDefenseBlockPlacement(Player player, Block block, Clan clan, ItemStack item) {
        // Check if clan has a core block
        if (clan.getCoreBlock() == null) {
            player.sendMessage(ChatColor.RED + "Your clan needs a core block before placing defense blocks.");
            block.setType(Material.AIR);
            // The event will automatically return the item to the player
            return;
        }

        // Check if within core block area
        Location coreLoc = clan.getCoreBlock().getLocation();
        int radius = clan.getCoreBlock().getBuildableArea();

        // Check if in same world
        if (!block.getWorld().equals(coreLoc.getWorld())) {
            player.sendMessage(ChatColor.RED + "Defense blocks must be placed within your clan's area of influence.");
            block.setType(Material.AIR);
            // The event will automatically return the item to the player
            return;
        }

        // Check if within radius
        double distanceSquared = block.getLocation().distanceSquared(coreLoc);
        if (distanceSquared > radius * radius) {
            player.sendMessage(ChatColor.RED + "Defense blocks must be placed within your clan's area of influence.");
            block.setType(Material.AIR);
            // The event will automatically return the item to the player
            return;
        }

        // Check if clan has reached max defense blocks
        if (clan.getDefenseBlockCount() >= clan.getCoreBlock().getMaxDefenseBlocks()) {
            player.sendMessage(ChatColor.RED + "Your clan has reached the maximum number of defense blocks. Upgrade your core to place more.");
            block.setType(Material.AIR);
            // The event will automatically return the item to the player
            return;
        }

        // Get tier from item
        int tier = 1;
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            tier = item.getItemMeta().getCustomModelData();
        }

        // Create defense block
        DefenseBlock defenseBlock = new DefenseBlock(block.getLocation(), clan, tier);
        clan.addDefenseBlock(defenseBlock);

        // Add to storage
        plugin.getDataStorage().addDefenseBlock(defenseBlock);

        player.sendMessage(ChatColor.GREEN + "Defense block (Tier " + tier + ") placed successfully!");
    }

    /**
     * Handle clan trapdoor placement
     * @param player Player placing the door
     * @param block Block being placed
     * @param clan Player's clan
     * @param item Item used to place the door
     */
    private void handleClanDoorPlacement(Player player, Block block, Clan clan, ItemStack item) {
        // Check if clan has a core block
        if (clan.getCoreBlock() == null) {
            player.sendMessage(ChatColor.RED + "Your clan needs a core block before placing clan trapdoors.");
            block.setType(Material.AIR);
            // The event will automatically return the item to the player
            return;
        }

        // Check if within core block area
        Location coreLoc = clan.getCoreBlock().getLocation();
        int radius = clan.getCoreBlock().getBuildableArea();

        // Check if in same world
        if (!block.getWorld().equals(coreLoc.getWorld())) {
            player.sendMessage(ChatColor.RED + "Clan trapdoors must be placed within your clan's area of influence.");
            block.setType(Material.AIR);
            // The event will automatically return the item to the player
            return;
        }

        // Check if within radius
        double distanceSquared = block.getLocation().distanceSquared(coreLoc);
        if (distanceSquared > radius * radius) {
            player.sendMessage(ChatColor.RED + "Clan trapdoors must be placed within your clan's area of influence.");
            block.setType(Material.AIR);
            // The event will automatically return the item to the player
            return;
        }

        // Check if clan has reached max clan doors
        if (clan.getClanDoorCount() >= clan.getCoreBlock().getMaxClanDoors()) {
            player.sendMessage(ChatColor.RED + "Your clan has reached the maximum number of clan trapdoors. Upgrade your core to place more.");
            block.setType(Material.AIR);
            // The event will automatically return the item to the player
            return;
        }

        // Get tier from item (default to 1 if not specified)
        int tier = 1;
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            tier = item.getItemMeta().getCustomModelData();
        }

        // Create clan door
        ClanDoor clanDoor = new ClanDoor(block.getLocation(), clan, tier);
        clan.addClanDoor(clanDoor);

        // Add to storage
        plugin.getDataStorage().addClanDoor(clanDoor);

        player.sendMessage(ChatColor.GREEN + "Clan trapdoor placed successfully! Only clan members can open or close it.");
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
}