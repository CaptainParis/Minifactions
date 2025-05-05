package Factions.miniFactions.listeners;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.managers.CraftingManager;
import Factions.miniFactions.managers.RaidManager;
import Factions.miniFactions.models.Clan;
import Factions.miniFactions.models.ClaimBlock;
import Factions.miniFactions.models.ClanDoor;
import Factions.miniFactions.models.CoreBlock;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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
                ClaimBlock claimBlock = plugin.getDataStorage().getClaimBlock(block.getLocation());
                if (claimBlock != null) {
                    handleClaimBlockInteraction(player, claimBlock);
                    event.setCancelled(true);
                    return;
                }
            }

            // Handle clan trapdoor interaction
            if (block.getType() == CraftingManager.getClanDoorMaterial()) {
                // Check if it's a registered clan door
                ClanDoor clanDoor = plugin.getDataStorage().getClanDoor(block.getLocation());
                if (clanDoor != null) {
                    // Get the clan that owns the door
                    Clan doorClan = clanDoor.getClan();
                    // Get the player's clan
                    Clan playerClan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());

                    // Check if player is in the same clan as the door
                    if (playerClan != null && playerClan.equals(doorClan)) {
                        // Allow clan members to toggle the trapdoor
                        // Get the current state of the trapdoor
                        org.bukkit.block.data.type.TrapDoor trapDoor = (org.bukkit.block.data.type.TrapDoor) block.getBlockData();
                        // Toggle the open state
                        trapDoor.setOpen(!trapDoor.isOpen());
                        // Update the block
                        block.setBlockData(trapDoor);
                        // Play sound
                        block.getWorld().playSound(block.getLocation(),
                            trapDoor.isOpen() ? org.bukkit.Sound.BLOCK_IRON_TRAPDOOR_OPEN : org.bukkit.Sound.BLOCK_IRON_TRAPDOOR_CLOSE,
                            1.0f, 1.0f);
                        // Cancel the event to prevent default behavior
                        event.setCancelled(true);
                        return;
                    } else {
                        // Non-clan members cannot interact with the trapdoor
                        player.sendMessage(ChatColor.RED + "This trapdoor can only be opened by members of clan " + doorClan.getName() + ".");
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        // Check for explosive placement
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && block != null &&
                player.getInventory().getItemInMainHand().getType() == CraftingManager.getExplosiveMaterial() &&
                player.getInventory().getItemInMainHand().hasItemMeta() &&
                player.getInventory().getItemInMainHand().getItemMeta().hasDisplayName() &&
                player.getInventory().getItemInMainHand().getItemMeta().getDisplayName().contains("Explosive")) {

            // Check if clicked on a defense block (any of the terracotta colors)
            if (block.getType() == Material.RED_TERRACOTTA ||
                block.getType() == Material.GREEN_TERRACOTTA ||
                block.getType() == Material.CYAN_TERRACOTTA ||
                block.getType() == Material.BLUE_TERRACOTTA ||
                block.getType() == Material.PURPLE_TERRACOTTA) {

                // Check if it's a registered defense block
                if (plugin.getDataStorage().getDefenseBlock(block.getLocation()) != null) {
                    // Handle explosive placement with RaidManager
                    RaidManager raidManager = plugin.getRaidManager();
                    if (raidManager != null) {
                        boolean success = raidManager.placeExplosive(player, block, player.getInventory().getItemInMainHand());
                        if (success) {
                            // Explosive placed successfully
                            event.setCancelled(true);
                            return;
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Raiding is not enabled on this server.");
                    }
                    event.setCancelled(true);
                    return;
                }
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
        plugin.getGUIManager().openCoreBlockGUI(player, coreBlock);
    }

    /**
     * Handle claim block interaction
     * @param player Player interacting
     * @param claimBlock Claim block being interacted with
     */
    private void handleClaimBlockInteraction(Player player, ClaimBlock claimBlock) {
        Clan clan = claimBlock.getClan();

        // Check if player is in the clan
        if (!clan.isMember(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "This claim block belongs to clan " + clan.getName() + ".");
            return;
        }

        // Check if player is close enough
        if (player.getLocation().distanceSquared(claimBlock.getLocation()) > 4) {
            player.sendMessage(ChatColor.RED + "You must be within 2 blocks to use the claim block.");
            return;
        }

        // Open claim block upgrade GUI
        plugin.getClaimBlockGUIManager().openClaimBlockUpgradeGUI(player, claimBlock);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Check if this is a core block GUI
        if (title.startsWith(ChatColor.GOLD + "Core Block -")) {
            event.setCancelled(true); // Prevent item movement

            // Handle the click
            if (plugin.getGUIManager().handleCoreBlockGUIClick(player, event.getRawSlot())) {
                // Click was handled by the GUI manager
                return;
            }
        }

        // Check if this is a claim block upgrade GUI
        if (title.equals(ChatColor.GREEN + "Claim Block Upgrade")) {
            event.setCancelled(true); // Prevent item movement

            // Handle the click
            if (plugin.getClaimBlockGUIManager().handleClaimBlockGUIClick(player, event.getRawSlot())) {
                // Click was handled by the GUI manager
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();

        // Check if this is a core block GUI
        if (title.startsWith(ChatColor.GOLD + "Core Block -")) {
            // Remove player from the GUI manager
            plugin.getGUIManager().removePlayer(player);
        }

        // Check if this is a claim block upgrade GUI
        if (title.equals(ChatColor.GREEN + "Claim Block Upgrade")) {
            // Remove player from the GUI manager
            plugin.getClaimBlockGUIManager().removePlayer(player);
        }
    }
}
