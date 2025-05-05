package Factions.miniFactions.managers;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.models.Clan;
import Factions.miniFactions.models.ClaimBlock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages GUI interfaces for claim block upgrades
 */
public class ClaimBlockGUIManager {

    private final MiniFactions plugin;
    private final Map<Player, ClaimBlock> openClaimBlockGUIs = new HashMap<>();

    public ClaimBlockGUIManager(MiniFactions plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the claim block upgrade GUI for a player
     * @param player Player to open the GUI for
     * @param claimBlock Claim block to upgrade
     */
    public void openClaimBlockUpgradeGUI(Player player, ClaimBlock claimBlock) {
        Clan clan = claimBlock.getClan();

        // Create inventory - using 3 rows (27 slots) for better centering
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.GREEN + "Claim Block Upgrade");

        // Fill with glass panes for decoration
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, filler);
        }

        // Add claim block info in center top
        ItemStack claimInfo = createItem(Material.EMERALD_BLOCK,
                ChatColor.GREEN + "Claim Block Information",
                ChatColor.YELLOW + "Level: " + ChatColor.WHITE + claimBlock.getLevel(),
                ChatColor.YELLOW + "Points per day: " + ChatColor.WHITE + claimBlock.getPointsPerDay(),
                ChatColor.YELLOW + "Clan: " + ChatColor.WHITE + clan.getName(),
                ChatColor.YELLOW + "Clan Points: " + ChatColor.WHITE + clan.getPoints());
        gui.setItem(4, claimInfo);

        // Add upgrade option in center if not at max level
        int upgradeCost = claimBlock.getUpgradeCost();
        if (upgradeCost > 0) {
            List<String> requiredItems = getRequiredItems(claimBlock.getLevel() + 1);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Current Level: " + ChatColor.WHITE + claimBlock.getLevel());
            lore.add(ChatColor.YELLOW + "Next Level: " + ChatColor.WHITE + (claimBlock.getLevel() + 1));
            lore.add(ChatColor.YELLOW + "Cost: " + ChatColor.WHITE + upgradeCost + " points");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Required Items:");
            
            for (String item : requiredItems) {
                String[] parts = item.split(":");
                if (parts.length == 2) {
                    String materialName = parts[0];
                    String amount = parts[1];
                    lore.add(ChatColor.WHITE + "- " + materialName + " x" + amount);
                }
            }
            
            lore.add("");
            lore.add(ChatColor.GRAY + "Click to upgrade your claim block");
            
            ItemStack upgradeItem = createItem(Material.EXPERIENCE_BOTTLE,
                    ChatColor.GREEN + "Upgrade Claim Block",
                    lore.toArray(new String[0]));
            gui.setItem(13, upgradeItem);
        } else {
            ItemStack maxLevelItem = createItem(Material.BARRIER,
                    ChatColor.RED + "Maximum Level Reached",
                    ChatColor.GRAY + "Your claim block is already at maximum level");
            gui.setItem(13, maxLevelItem);
        }

        // Add close button in center bottom
        ItemStack closeItem = createItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Close");
        gui.setItem(22, closeItem);

        // Store the claim block for this player
        openClaimBlockGUIs.put(player, claimBlock);

        // Open the GUI
        player.openInventory(gui);
    }

    /**
     * Handle a click in the claim block upgrade GUI
     * @param player Player who clicked
     * @param slot Slot that was clicked
     * @return true if the click was handled
     */
    public boolean handleClaimBlockGUIClick(Player player, int slot) {
        ClaimBlock claimBlock = openClaimBlockGUIs.get(player);
        if (claimBlock == null) {
            return false;
        }

        Clan clan = claimBlock.getClan();

        // Handle upgrade button (slot 13 in the layout)
        if (slot == 13) {
            int upgradeCost = claimBlock.getUpgradeCost();
            if (upgradeCost > 0) {
                // Check if clan has enough points
                if (clan.getPoints() >= upgradeCost) {
                    // Check if player has required items
                    List<String> requiredItems = getRequiredItems(claimBlock.getLevel() + 1);
                    if (hasRequiredItems(player, requiredItems)) {
                        // Remove items from player inventory
                        removeRequiredItems(player, requiredItems);
                        
                        // Deduct points
                        clan.removePoints(upgradeCost);

                        // Upgrade claim block
                        claimBlock.upgrade();

                        // Notify player
                        player.sendMessage(ChatColor.GREEN + "Claim block upgraded to level " + claimBlock.getLevel() + "!");

                        // Reopen the GUI with updated information
                        openClaimBlockUpgradeGUI(player, claimBlock);
                    } else {
                        player.sendMessage(ChatColor.RED + "You don't have all the required items for this upgrade.");
                        player.closeInventory();
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Your clan doesn't have enough points to upgrade the claim block.");
                    player.sendMessage(ChatColor.RED + "Required: " + upgradeCost + " points, Available: " + clan.getPoints() + " points");
                    player.closeInventory();
                }
            } else {
                player.sendMessage(ChatColor.RED + "Your claim block is already at maximum level.");
                player.closeInventory();
            }
            return true;
        }

        // Handle close button
        if (slot == 22) {
            player.closeInventory();
            return true;
        }

        // Cancel all clicks in this GUI
        return true;
    }

    /**
     * Remove a player from the open GUIs map
     * @param player Player to remove
     */
    public void removePlayer(Player player) {
        openClaimBlockGUIs.remove(player);
    }

    /**
     * Get the required items for a claim block level
     * @param level Claim block level
     * @return List of required items in format "MATERIAL:AMOUNT"
     */
    private List<String> getRequiredItems(int level) {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        return config.getStringList("claim.levels." + level + ".upgrade-items");
    }

    /**
     * Check if a player has all required items
     * @param player Player to check
     * @param requiredItems List of required items in format "MATERIAL:AMOUNT"
     * @return true if player has all items
     */
    private boolean hasRequiredItems(Player player, List<String> requiredItems) {
        for (String item : requiredItems) {
            String[] parts = item.split(":");
            if (parts.length != 2) {
                continue;
            }

            String materialName = parts[0];
            int amount;
            try {
                amount = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                continue;
            }

            Material material = Material.getMaterial(materialName);
            if (material == null) {
                continue;
            }

            // Check if player has enough of this material
            if (!player.getInventory().containsAtLeast(new ItemStack(material), amount)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Remove required items from player inventory
     * @param player Player to remove items from
     * @param requiredItems List of required items in format "MATERIAL:AMOUNT"
     */
    private void removeRequiredItems(Player player, List<String> requiredItems) {
        for (String item : requiredItems) {
            String[] parts = item.split(":");
            if (parts.length != 2) {
                continue;
            }

            String materialName = parts[0];
            int amount;
            try {
                amount = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                continue;
            }

            Material material = Material.getMaterial(materialName);
            if (material == null) {
                continue;
            }

            // Remove items from inventory
            player.getInventory().removeItem(new ItemStack(material, amount));
        }
    }

    /**
     * Create an item with a custom name and lore
     * @param material Item material
     * @param name Item name
     * @param lore Item lore (optional)
     * @return Created ItemStack
     */
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        
        if (lore.length > 0) {
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);
        }
        
        item.setItemMeta(meta);
        return item;
    }
}
