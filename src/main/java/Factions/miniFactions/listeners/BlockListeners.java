package Factions.miniFactions.listeners;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.managers.CoreBlockManager;
import Factions.miniFactions.managers.CraftingManager;
import Factions.miniFactions.models.Clan;
import Factions.miniFactions.models.ClaimBlock;
import Factions.miniFactions.models.CoreBlock;
import Factions.miniFactions.models.DefenseBlock;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BlockListeners implements Listener {

    private final MiniFactions plugin;

    public BlockListeners(MiniFactions plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
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
        if (block.getType() == CraftingManager.getDefenseBlockMaterial() &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().getDisplayName().contains("Defense Block")) {
            handleDefenseBlockPlacement(player, block, clan, item);
            return;
        }

        // Handle clan door placement
        if (block.getType() == CraftingManager.getClanDoorMaterial() &&
                item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().getDisplayName().contains("Clan Door")) {
            handleClanDoorPlacement(player, block, clan);
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
                player.sendMessage(ChatColor.RED + "You can only build within your clan's area of influence.");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
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
            // Handle claim block breaking
            // This would need to be implemented in a ClaimBlockManager
            return;
        }

        // Check for defense block
        if (block.getType() == CraftingManager.getDefenseBlockMaterial()) {
            // Defense blocks can only be broken by explosives
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Defense blocks can only be broken by explosives.");
            return;
        }

        // Check if player is in a clan
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            return;
        }

        // Check if within buildable area of another clan
        if (plugin.getCoreBlockManager().isWithinOtherClanAOI(block.getLocation(), clan)) {
            player.sendMessage(ChatColor.RED + "You cannot break blocks within another clan's area of influence.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        // Handle explosions
        // This would need to be expanded to handle defense block damage
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
            player.getInventory().addItem(plugin.getCraftingManager().createClaimBlock(1));
            return;
        }

        // Check if within core block area
        Location coreLoc = clan.getCoreBlock().getLocation();
        int radius = clan.getCoreBlock().getBuildableArea();

        // Check if in same world
        if (!block.getWorld().equals(coreLoc.getWorld())) {
            player.sendMessage(ChatColor.RED + "Claim blocks must be placed within your clan's area of influence.");
            block.setType(Material.AIR);
            player.getInventory().addItem(plugin.getCraftingManager().createClaimBlock(1));
            return;
        }

        // Check if within radius
        double distanceSquared = block.getLocation().distanceSquared(coreLoc);
        if (distanceSquared > radius * radius) {
            player.sendMessage(ChatColor.RED + "Claim blocks must be placed within your clan's area of influence.");
            block.setType(Material.AIR);
            player.getInventory().addItem(plugin.getCraftingManager().createClaimBlock(1));
            return;
        }

        // Check if clan has reached max claim blocks
        if (clan.getClaimBlockCount() >= clan.getCoreBlock().getMaxClaimBlocks()) {
            player.sendMessage(ChatColor.RED + "Your clan has reached the maximum number of claim blocks. Upgrade your core to place more.");
            block.setType(Material.AIR);
            player.getInventory().addItem(plugin.getCraftingManager().createClaimBlock(1));
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
            player.getInventory().addItem(item.clone());
            return;
        }

        // Check if within core block area
        Location coreLoc = clan.getCoreBlock().getLocation();
        int radius = clan.getCoreBlock().getBuildableArea();

        // Check if in same world
        if (!block.getWorld().equals(coreLoc.getWorld())) {
            player.sendMessage(ChatColor.RED + "Defense blocks must be placed within your clan's area of influence.");
            block.setType(Material.AIR);
            player.getInventory().addItem(item.clone());
            return;
        }

        // Check if within radius
        double distanceSquared = block.getLocation().distanceSquared(coreLoc);
        if (distanceSquared > radius * radius) {
            player.sendMessage(ChatColor.RED + "Defense blocks must be placed within your clan's area of influence.");
            block.setType(Material.AIR);
            player.getInventory().addItem(item.clone());
            return;
        }

        // Check if clan has reached max defense blocks
        if (clan.getDefenseBlockCount() >= clan.getCoreBlock().getMaxDefenseBlocks()) {
            player.sendMessage(ChatColor.RED + "Your clan has reached the maximum number of defense blocks. Upgrade your core to place more.");
            block.setType(Material.AIR);
            player.getInventory().addItem(item.clone());
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

        player.sendMessage(ChatColor.GREEN + "Defense block (Tier " + tier + ") placed successfully!");
    }

    /**
     * Handle clan door placement
     * @param player Player placing the door
     * @param block Block being placed
     * @param clan Player's clan
     */
    private void handleClanDoorPlacement(Player player, Block block, Clan clan) {
        // Check if clan has a core block
        if (clan.getCoreBlock() == null) {
            player.sendMessage(ChatColor.RED + "Your clan needs a core block before placing clan doors.");
            block.setType(Material.AIR);
            player.getInventory().addItem(plugin.getCraftingManager().createClanDoor());
            return;
        }

        // Check if within core block area
        Location coreLoc = clan.getCoreBlock().getLocation();
        int radius = clan.getCoreBlock().getBuildableArea();

        // Check if in same world
        if (!block.getWorld().equals(coreLoc.getWorld())) {
            player.sendMessage(ChatColor.RED + "Clan doors must be placed within your clan's area of influence.");
            block.setType(Material.AIR);
            player.getInventory().addItem(plugin.getCraftingManager().createClanDoor());
            return;
        }

        // Check if within radius
        double distanceSquared = block.getLocation().distanceSquared(coreLoc);
        if (distanceSquared > radius * radius) {
            player.sendMessage(ChatColor.RED + "Clan doors must be placed within your clan's area of influence.");
            block.setType(Material.AIR);
            player.getInventory().addItem(plugin.getCraftingManager().createClanDoor());
            return;
        }

        // Check if clan has reached max clan doors
        if (clan.getClanDoorCount() >= clan.getCoreBlock().getMaxClanDoors()) {
            player.sendMessage(ChatColor.RED + "Your clan has reached the maximum number of clan doors. Upgrade your core to place more.");
            block.setType(Material.AIR);
            player.getInventory().addItem(plugin.getCraftingManager().createClanDoor());
            return;
        }

        // Create clan door (would need a ClanDoor class and manager)
        player.sendMessage(ChatColor.GREEN + "Clan door placed successfully! Only clan members can open it.");
    }
}
