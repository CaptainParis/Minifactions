package Factions.miniFactions.managers;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.models.Clan;
import Factions.miniFactions.models.CoreBlock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
 * Manages GUI interfaces for the plugin
 */
public class GUIManager {

    private final MiniFactions plugin;
    private final Map<Player, CoreBlock> openCoreBlockGUIs = new HashMap<>();

    public GUIManager(MiniFactions plugin) {
        this.plugin = plugin;
    }

    /**
     * Open the core block GUI for a player
     * @param player Player to open the GUI for
     * @param coreBlock Core block to display
     */
    public void openCoreBlockGUI(Player player, CoreBlock coreBlock) {
        Clan clan = coreBlock.getClan();

        // Create inventory - using 3 rows (27 slots) for better centering
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Core Block - " + clan.getName());

        // Fill with glass panes for decoration
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, filler);
        }

        // Add clan stats in center top
        ItemStack clanInfo = createItem(Material.BOOK,
                ChatColor.GOLD + "Clan Information",
                ChatColor.YELLOW + "Name: " + ChatColor.WHITE + clan.getName(),
                ChatColor.YELLOW + "Level: " + ChatColor.WHITE + coreBlock.getLevel(),
                ChatColor.YELLOW + "Points: " + ChatColor.WHITE + clan.getPoints(),
                ChatColor.YELLOW + "Members: " + ChatColor.WHITE + clan.getMemberCount() + "/" + coreBlock.getMaxClanMembers());
        gui.setItem(4, clanInfo);

        // Add core block stats in center left
        ItemStack coreInfo = createItem(Material.BEACON,
                ChatColor.AQUA + "Core Block Stats",
                ChatColor.YELLOW + "Level: " + ChatColor.WHITE + coreBlock.getLevel(),
                ChatColor.YELLOW + "Area: " + ChatColor.WHITE + coreBlock.getBuildableArea() + " blocks radius",
                ChatColor.YELLOW + "Defense Slots: " + ChatColor.WHITE + clan.getDefenseBlockCount() + "/" + coreBlock.getMaxDefenseBlocks(),
                ChatColor.YELLOW + "Claim Slots: " + ChatColor.WHITE + clan.getClaimBlockCount() + "/" + coreBlock.getMaxClaimBlocks(),
                ChatColor.YELLOW + "Door Slots: " + ChatColor.WHITE + clan.getClanDoorCount() + "/" + coreBlock.getMaxClanDoors());
        gui.setItem(11, coreInfo);

        // Add upkeep information in center
        boolean isExempt = plugin.getUpkeepManager().isExempt(clan.getLeader());
        List<String> upkeepLore = new ArrayList<>();
        upkeepLore.add(ChatColor.YELLOW + "Cost: " + ChatColor.WHITE + coreBlock.getUpkeepCost() + " points");

        if (isExempt) {
            upkeepLore.add(ChatColor.GREEN + "Your clan is exempt from upkeep");
        } else if (coreBlock.isUpkeepDue()) {
            upkeepLore.add(ChatColor.RED + "Upkeep is due now!");
            upkeepLore.add(ChatColor.RED + "Pay immediately to avoid penalties");
        } else {
            // Calculate time until next upkeep
            FileConfiguration config = plugin.getConfigManager().getConfig();
            int paymentInterval = config.getInt("core.upkeep.payment-interval", 24);
            long intervalMillis = paymentInterval * 60 * 60 * 1000L; // Convert hours to milliseconds
            long timeUntilUpkeep = (coreBlock.getLastUpkeepTime() + intervalMillis) - System.currentTimeMillis();

            if (timeUntilUpkeep > 0) {
                long hoursRemaining = timeUntilUpkeep / (60 * 60 * 1000L);
                upkeepLore.add(ChatColor.YELLOW + "Next payment due in: " + ChatColor.WHITE + hoursRemaining + " hours");
            } else {
                upkeepLore.add(ChatColor.RED + "Upkeep is overdue!");
            }
        }

        upkeepLore.add(ChatColor.YELLOW + "Days of upkeep: " + ChatColor.WHITE + coreBlock.getDaysOfUpkeep());

        ItemStack upkeepInfo = createItem(Material.CLOCK, ChatColor.GOLD + "Upkeep Information", upkeepLore.toArray(new String[0]));
        gui.setItem(13, upkeepInfo);

        // Add upgrade option in center right
        int upgradeCost = coreBlock.getUpgradeCost();
        if (upgradeCost > 0) {
            ItemStack upgradeItem = createItem(Material.EXPERIENCE_BOTTLE,
                    ChatColor.GREEN + "Upgrade Core Block",
                    ChatColor.YELLOW + "Current Level: " + ChatColor.WHITE + coreBlock.getLevel(),
                    ChatColor.YELLOW + "Cost: " + ChatColor.WHITE + upgradeCost + " points",
                    "",
                    ChatColor.GRAY + "Click to upgrade your core block");
            gui.setItem(15, upgradeItem);
        } else {
            ItemStack maxLevelItem = createItem(Material.BARRIER,
                    ChatColor.RED + "Maximum Level Reached",
                    ChatColor.GRAY + "Your core block is already at maximum level");
            gui.setItem(15, maxLevelItem);
        }

        // Add close button in center bottom
        ItemStack closeItem = createItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Close");
        gui.setItem(22, closeItem);

        // Store the core block for this player
        openCoreBlockGUIs.put(player, coreBlock);

        // Open the GUI
        player.openInventory(gui);
    }

    /**
     * Handle a click in the core block GUI
     * @param player Player who clicked
     * @param slot Slot that was clicked
     * @return true if the click was handled
     */
    public boolean handleCoreBlockGUIClick(Player player, int slot) {
        CoreBlock coreBlock = openCoreBlockGUIs.get(player);
        if (coreBlock == null) {
            return false;
        }

        Clan clan = coreBlock.getClan();

        // Handle upgrade button (slot 15 in the new layout)
        if (slot == 15) {
            int upgradeCost = coreBlock.getUpgradeCost();
            if (upgradeCost > 0) {
                // Check if clan has enough points
                if (clan.getPoints() >= upgradeCost) {
                    // Deduct points
                    clan.removePoints(upgradeCost);

                    // Upgrade core block
                    coreBlock.upgrade();

                    // Notify player
                    player.sendMessage(ChatColor.GREEN + "Core block upgraded to level " + coreBlock.getLevel() + "!");

                    // Reopen the GUI with updated information
                    openCoreBlockGUI(player, coreBlock);
                } else {
                    player.sendMessage(ChatColor.RED + "Your clan doesn't have enough points to upgrade the core block.");
                    player.sendMessage(ChatColor.RED + "Required: " + upgradeCost + " points, Available: " + clan.getPoints() + " points");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Your core block is already at maximum level.");
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
        openCoreBlockGUIs.remove(player);
    }

    /**
     * Create an item for the GUI
     * @param material Item material
     * @param name Item name
     * @param lore Item lore lines
     * @return The created item
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
