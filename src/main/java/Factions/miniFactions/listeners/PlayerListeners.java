package Factions.miniFactions.listeners;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.managers.CraftingManager;
import Factions.miniFactions.models.Clan;
import Factions.miniFactions.models.CoreBlock;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerListeners implements Listener {

    private final MiniFactions plugin;
    
    public PlayerListeners(MiniFactions plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in a clan
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan != null) {
            player.sendMessage(ChatColor.GREEN + "Welcome back to clan " + clan.getName() + "!");
            
            // Check if clan has a core block
            if (clan.getCoreBlock() != null) {
                // Check if upkeep is due
                if (clan.getCoreBlock().isUpkeepDue()) {
                    player.sendMessage(ChatColor.RED + "Warning: Your clan's upkeep is due! Pay it to prevent defense blocks from decaying.");
                }
            } else {
                player.sendMessage(ChatColor.YELLOW + "Your clan doesn't have a core block. Place one to access more features.");
            }
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Ignore off-hand interactions
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        
        // Check for right-click on blocks
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && block != null) {
            // Handle core block interaction
            if (block.getType() == Material.BEACON) {
                CoreBlock coreBlock = plugin.getDataStorage().getCoreBlock(block.getLocation());
                if (coreBlock != null) {
                    handleCoreBlockInteraction(player, coreBlock);
                    event.setCancelled(true);
                    return;
                }
            }
            
            // Handle claim block interaction
            if (block.getType() == CraftingManager.getClaimBlockMaterial()) {
                // Handle claim block interaction
                // This would need to be implemented in a ClaimBlockManager
                event.setCancelled(true);
                return;
            }
            
            // Handle clan door interaction
            if (block.getType() == Material.IRON_DOOR) {
                // Check if it's a clan door
                // This would need to be implemented with a ClanDoor class and manager
                // For now, just check if within a clan's AOI
                Clan playerClan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
                
                // If player is not in a clan, check if door is within any clan's AOI
                if (playerClan == null && plugin.getCoreBlockManager().isWithinOtherClanAOI(block.getLocation(), null)) {
                    player.sendMessage(ChatColor.RED + "This door can only be opened by clan members.");
                    event.setCancelled(true);
                    return;
                }
                
                // If player is in a clan, check if door is within another clan's AOI
                if (playerClan != null && plugin.getCoreBlockManager().isWithinOtherClanAOI(block.getLocation(), playerClan)) {
                    player.sendMessage(ChatColor.RED + "This door can only be opened by members of the owning clan.");
                    event.setCancelled(true);
                    return;
                }
            }
        }
        
        // Check for explosive placement
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && block != null && 
                player.getInventory().getItemInMainHand().getType() == CraftingManager.getExplosiveMaterial() &&
                player.getInventory().getItemInMainHand().hasItemMeta() &&
                player.getInventory().getItemInMainHand().getItemMeta().hasDisplayName() &&
                player.getInventory().getItemInMainHand().getItemMeta().getDisplayName().contains("Explosive")) {
            
            // Check if clicked on a defense block
            if (block.getType() == CraftingManager.getDefenseBlockMaterial()) {
                // Handle explosive placement
                // This would need to be implemented with a RaidManager
                event.setCancelled(true);
                return;
            }
        }
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        // Check if killed by another player
        if (killer != null) {
            // Get clans
            Clan victimClan = plugin.getClanManager().getClanByPlayer(victim.getUniqueId());
            Clan killerClan = plugin.getClanManager().getClanByPlayer(killer.getUniqueId());
            
            // Check if both players are in different clans
            if (victimClan != null && killerClan != null && !victimClan.equals(killerClan)) {
                // Award points to killer's clan
                int killPoints = plugin.getConfigManager().getConfig().getInt("pvp.kill-points", 50);
                killerClan.addPoints(killPoints);
                
                // Deduct points from victim's clan
                int deathPenalty = plugin.getConfigManager().getConfig().getInt("pvp.death-penalty", 25);
                victimClan.removePoints(deathPenalty);
                
                // Notify players
                killer.sendMessage(ChatColor.GREEN + "Your clan gained " + killPoints + " points for killing " + victim.getName() + "!");
                victim.sendMessage(ChatColor.RED + "Your clan lost " + deathPenalty + " points for being killed by " + killer.getName() + "!");
            }
        }
    }
    
    /**
     * Handle core block interaction
     * @param player Player interacting
     * @param coreBlock Core block being interacted with
     */
    private void handleCoreBlockInteraction(Player player, CoreBlock coreBlock) {
        Clan clan = coreBlock.getClan();
        
        // Check if player is in the clan
        if (!clan.isMember(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "This core block belongs to clan " + clan.getName() + ".");
            return;
        }
        
        // Check if player is close enough
        if (player.getLocation().distanceSquared(coreBlock.getLocation()) > 4) {
            player.sendMessage(ChatColor.RED + "You must be within 2 blocks to use the core block.");
            return;
        }
        
        // Open core block GUI
        // This would need to be implemented with a GUI manager
        player.sendMessage(ChatColor.GREEN + "Core Block - Clan: " + clan.getName());
        player.sendMessage(ChatColor.YELLOW + "Level: " + ChatColor.WHITE + coreBlock.getLevel());
        player.sendMessage(ChatColor.YELLOW + "Points: " + ChatColor.WHITE + clan.getPoints());
        player.sendMessage(ChatColor.YELLOW + "Buildable Area: " + ChatColor.WHITE + coreBlock.getBuildableArea() + " blocks");
        player.sendMessage(ChatColor.YELLOW + "Defense Blocks: " + ChatColor.WHITE + clan.getDefenseBlockCount() + "/" + coreBlock.getMaxDefenseBlocks());
        player.sendMessage(ChatColor.YELLOW + "Claim Blocks: " + ChatColor.WHITE + clan.getClaimBlockCount() + "/" + coreBlock.getMaxClaimBlocks());
        player.sendMessage(ChatColor.YELLOW + "Clan Doors: " + ChatColor.WHITE + clan.getClanDoorCount() + "/" + coreBlock.getMaxClanDoors());
        
        // Show upgrade info if not at max level
        int upgradeCost = coreBlock.getUpgradeCost();
        if (upgradeCost != -1) {
            player.sendMessage(ChatColor.YELLOW + "Upgrade Cost: " + ChatColor.WHITE + upgradeCost + " points");
            player.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.WHITE + "/clan core upgrade" + ChatColor.YELLOW + " to upgrade.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Core is at maximum level.");
        }
        
        // Show upkeep info
        if (coreBlock.isUpkeepDue()) {
            player.sendMessage(ChatColor.RED + "Upkeep: " + ChatColor.WHITE + "Due now! Cost: " + coreBlock.getUpkeepCost() + " points");
            player.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.WHITE + "/clan core pay" + ChatColor.YELLOW + " to pay upkeep.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Upkeep: " + ChatColor.WHITE + coreBlock.getUpkeepCost() + " points");
            player.sendMessage(ChatColor.YELLOW + "Days of Upkeep: " + ChatColor.WHITE + coreBlock.getDaysOfUpkeep());
        }
    }
}
