package Factions.miniFactions.commands;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.models.Clan;
import Factions.miniFactions.models.CoreBlock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin command for managing core blocks
 */
public class AdminCoreCommand implements CommandExecutor, TabCompleter {

    private final MiniFactions plugin;
    private final Map<Player, String> openAdminGUIs = new HashMap<>();

    public AdminCoreCommand(MiniFactions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        // Check permission
        if (!player.hasPermission("minifactions.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        // Open admin GUI
        openAdminGUI(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // No tab completions for this command
        return new ArrayList<>();
    }

    /**
     * Open the admin GUI for a player
     * @param player Player to open the GUI for
     */
    private void openAdminGUI(Player player) {
        // Create inventory
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "MiniFactions Admin");

        // Fill with glass panes for decoration
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, filler);
        }

        // Add core level management
        ItemStack coreLevel = createItem(Material.BEACON,
                ChatColor.GOLD + "Manage Core Levels",
                ChatColor.YELLOW + "Click to open core level management");
        gui.setItem(11, coreLevel);

        // Add plugin blocks
        ItemStack blocks = createItem(Material.CHEST,
                ChatColor.AQUA + "Get Plugin Blocks",
                ChatColor.YELLOW + "Click to get all plugin blocks");
        gui.setItem(13, blocks);

        // Add points management
        ItemStack points = createItem(Material.EMERALD,
                ChatColor.GREEN + "Manage Points",
                ChatColor.YELLOW + "Click to add points to your clan");
        gui.setItem(15, points);

        // Add close button
        ItemStack close = createItem(Material.BARRIER, ChatColor.RED + "Close");
        gui.setItem(22, close);

        // Store the GUI type for this player
        openAdminGUIs.put(player, "main");

        // Open the GUI
        player.openInventory(gui);
    }

    /**
     * Open the core level management GUI
     * @param player Player to open the GUI for
     */
    private void openCoreLevelGUI(Player player) {
        // Create inventory
        Inventory gui = Bukkit.createInventory(null, 36, ChatColor.DARK_RED + "Core Level Management");

        // Fill with glass panes for decoration
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++) {
            gui.setItem(i, filler);
        }

        // Get player's clan
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        CoreBlock coreBlock = clan != null ? clan.getCoreBlock() : null;

        // Add current level info
        if (clan != null && coreBlock != null) {
            ItemStack currentLevel = createItem(Material.BEACON,
                    ChatColor.GOLD + "Current Core Level: " + coreBlock.getLevel(),
                    ChatColor.YELLOW + "Clan: " + ChatColor.WHITE + clan.getName(),
                    ChatColor.YELLOW + "Level: " + ChatColor.WHITE + coreBlock.getLevel(),
                    ChatColor.YELLOW + "Area: " + ChatColor.WHITE + coreBlock.getBuildableArea() + " blocks radius",
                    ChatColor.YELLOW + "Defense Slots: " + ChatColor.WHITE + coreBlock.getMaxDefenseBlocks(),
                    ChatColor.YELLOW + "Claim Slots: " + ChatColor.WHITE + coreBlock.getMaxClaimBlocks(),
                    ChatColor.YELLOW + "Door Slots: " + ChatColor.WHITE + coreBlock.getMaxClanDoors(),
                    ChatColor.YELLOW + "Member Slots: " + ChatColor.WHITE + coreBlock.getMaxClanMembers());
            gui.setItem(4, currentLevel);
        } else {
            ItemStack noClan = createItem(Material.BARRIER,
                    ChatColor.RED + "No Clan or Core Block",
                    ChatColor.GRAY + "You are not in a clan or your clan doesn't have a core block.");
            gui.setItem(4, noClan);
        }

        // Add level buttons
        if (clan != null && coreBlock != null) {
            // Add level +1 button
            ItemStack levelPlus1 = createItem(Material.LIME_CONCRETE,
                    ChatColor.GREEN + "Add 1 Level",
                    ChatColor.YELLOW + "Click to add 1 level to your core block");
            gui.setItem(19, levelPlus1);

            // Add level +5 button
            ItemStack levelPlus5 = createItem(Material.LIME_CONCRETE,
                    ChatColor.GREEN + "Add 5 Levels",
                    ChatColor.YELLOW + "Click to add 5 levels to your core block");
            gui.setItem(20, levelPlus5);

            // Add level +10 button
            ItemStack levelPlus10 = createItem(Material.LIME_CONCRETE,
                    ChatColor.GREEN + "Add 10 Levels",
                    ChatColor.YELLOW + "Click to add 10 levels to your core block");
            gui.setItem(21, levelPlus10);

            // Add level -1 button
            ItemStack levelMinus1 = createItem(Material.RED_CONCRETE,
                    ChatColor.RED + "Remove 1 Level",
                    ChatColor.YELLOW + "Click to remove 1 level from your core block");
            gui.setItem(23, levelMinus1);

            // Add level -5 button
            ItemStack levelMinus5 = createItem(Material.RED_CONCRETE,
                    ChatColor.RED + "Remove 5 Levels",
                    ChatColor.YELLOW + "Click to remove 5 levels from your core block");
            gui.setItem(24, levelMinus5);

            // Add level -10 button
            ItemStack levelMinus10 = createItem(Material.RED_CONCRETE,
                    ChatColor.RED + "Remove 10 Levels",
                    ChatColor.YELLOW + "Click to remove 10 levels from your core block");
            gui.setItem(25, levelMinus10);

            // Add max level button
            ItemStack maxLevel = createItem(Material.DIAMOND_BLOCK,
                    ChatColor.AQUA + "Set to Max Level",
                    ChatColor.YELLOW + "Click to set your core block to max level");
            gui.setItem(13, maxLevel);
        }

        // Add back button
        ItemStack back = createItem(Material.ARROW, ChatColor.YELLOW + "Back to Admin Menu");
        gui.setItem(31, back);

        // Store the GUI type for this player
        openAdminGUIs.put(player, "core_level");

        // Open the GUI
        player.openInventory(gui);
    }

    /**
     * Open the plugin blocks GUI
     * @param player Player to open the GUI for
     */
    private void openBlocksGUI(Player player) {
        // Create inventory
        Inventory gui = Bukkit.createInventory(null, 36, ChatColor.DARK_RED + "Plugin Blocks");

        // Fill with glass panes for decoration
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++) {
            gui.setItem(i, filler);
        }

        // Add core blocks
        for (int i = 1; i <= 5; i++) {
            ItemStack coreBlock = plugin.getCraftingManager().createCoreBlock(i);
            gui.setItem(9 + i, coreBlock);
        }

        // Add claim blocks
        for (int i = 1; i <= 5; i++) {
            ItemStack claimBlock = plugin.getCraftingManager().createClaimBlock(i);
            gui.setItem(18 + i, claimBlock);
        }

        // Add defense blocks
        for (int i = 1; i <= 5; i++) {
            ItemStack defenseBlock = plugin.getCraftingManager().createDefenseBlock(i);
            gui.setItem(27 + i, defenseBlock);
        }

        // Add back button - moved to bottom row to avoid conflict with defense blocks
        ItemStack back = createItem(Material.ARROW, ChatColor.YELLOW + "Back to Admin Menu");
        gui.setItem(35, back);

        // Store the GUI type for this player
        openAdminGUIs.put(player, "blocks");

        // Open the GUI
        player.openInventory(gui);
    }

    /**
     * Open the points management GUI
     * @param player Player to open the GUI for
     */
    private void openPointsGUI(Player player) {
        // Create inventory
        Inventory gui = Bukkit.createInventory(null, 36, ChatColor.DARK_RED + "Points Management");

        // Fill with glass panes for decoration
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++) {
            gui.setItem(i, filler);
        }

        // Get player's clan
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());

        // Add current points info
        if (clan != null) {
            ItemStack currentPoints = createItem(Material.EMERALD,
                    ChatColor.GOLD + "Current Points: " + clan.getPoints(),
                    ChatColor.YELLOW + "Clan: " + ChatColor.WHITE + clan.getName());
            gui.setItem(4, currentPoints);
        } else {
            ItemStack noClan = createItem(Material.BARRIER,
                    ChatColor.RED + "No Clan",
                    ChatColor.GRAY + "You are not in a clan.");
            gui.setItem(4, noClan);
        }

        // Add points buttons
        if (clan != null) {
            // Add +100 points button
            ItemStack points100 = createItem(Material.LIME_CONCRETE,
                    ChatColor.GREEN + "Add 100 Points",
                    ChatColor.YELLOW + "Click to add 100 points to your clan");
            gui.setItem(19, points100);

            // Add +1000 points button
            ItemStack points1000 = createItem(Material.LIME_CONCRETE,
                    ChatColor.GREEN + "Add 1,000 Points",
                    ChatColor.YELLOW + "Click to add 1,000 points to your clan");
            gui.setItem(20, points1000);

            // Add +10000 points button
            ItemStack points10000 = createItem(Material.LIME_CONCRETE,
                    ChatColor.GREEN + "Add 10,000 Points",
                    ChatColor.YELLOW + "Click to add 10,000 points to your clan");
            gui.setItem(21, points10000);

            // Add +100000 points button
            ItemStack points100000 = createItem(Material.LIME_CONCRETE,
                    ChatColor.GREEN + "Add 100,000 Points",
                    ChatColor.YELLOW + "Click to add 100,000 points to your clan");
            gui.setItem(22, points100000);

            // Add -100 points button
            ItemStack pointsMinus100 = createItem(Material.RED_CONCRETE,
                    ChatColor.RED + "Remove 100 Points",
                    ChatColor.YELLOW + "Click to remove 100 points from your clan");
            gui.setItem(23, pointsMinus100);

            // Add -1000 points button
            ItemStack pointsMinus1000 = createItem(Material.RED_CONCRETE,
                    ChatColor.RED + "Remove 1,000 Points",
                    ChatColor.YELLOW + "Click to remove 1,000 points from your clan");
            gui.setItem(24, pointsMinus1000);

            // Add -10000 points button
            ItemStack pointsMinus10000 = createItem(Material.RED_CONCRETE,
                    ChatColor.RED + "Remove 10,000 Points",
                    ChatColor.YELLOW + "Click to remove 10,000 points from your clan");
            gui.setItem(25, pointsMinus10000);
        }

        // Add back button
        ItemStack back = createItem(Material.ARROW, ChatColor.YELLOW + "Back to Admin Menu");
        gui.setItem(31, back);

        // Store the GUI type for this player
        openAdminGUIs.put(player, "points");

        // Open the GUI
        player.openInventory(gui);
    }

    /**
     * Handle a click in the admin GUI
     * @param player Player who clicked
     * @param slot Slot that was clicked
     * @return true if the click was handled
     */
    public boolean handleAdminGUIClick(Player player, int slot) {
        String guiType = openAdminGUIs.get(player);
        if (guiType == null) {
            return false;
        }

        // Debug message to help troubleshoot
        player.sendMessage(ChatColor.DARK_GRAY + "Debug: Clicked slot " + slot + " in GUI type: " + guiType);

        // Handle main menu clicks
        if (guiType.equals("main")) {
            if (slot == 11) {
                // Core level management
                openCoreLevelGUI(player);
                return true;
            } else if (slot == 13) {
                // Get plugin blocks
                openBlocksGUI(player);
                return true;
            } else if (slot == 15) {
                // Points management
                openPointsGUI(player);
                return true;
            } else if (slot == 22) {
                // Close
                player.closeInventory();
                return true;
            }
        }

        // Handle core level management clicks
        else if (guiType.equals("core_level")) {
            Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
            CoreBlock coreBlock = clan != null ? clan.getCoreBlock() : null;

            if (clan != null && coreBlock != null) {
                try {
                    int maxLevel = plugin.getConfigManager().getConfig().getInt("core.max-level", 20);

                    if (slot == 19) {
                        // Add 1 level
                        int newLevel = Math.min(coreBlock.getLevel() + 1, maxLevel);
                        coreBlock.setLevel(newLevel);
                        player.sendMessage(ChatColor.GREEN + "Core block level set to " + newLevel + ". Previous level: " + (newLevel-1));
                        openCoreLevelGUI(player);
                        return true;
                    } else if (slot == 20) {
                        // Add 5 levels
                        int oldLevel = coreBlock.getLevel();
                        int newLevel = Math.min(oldLevel + 5, maxLevel);
                        coreBlock.setLevel(newLevel);
                        player.sendMessage(ChatColor.GREEN + "Core block level set to " + newLevel + ". Previous level: " + oldLevel);
                        openCoreLevelGUI(player);
                        return true;
                    } else if (slot == 21) {
                        // Add 10 levels
                        int oldLevel = coreBlock.getLevel();
                        int newLevel = Math.min(oldLevel + 10, maxLevel);
                        coreBlock.setLevel(newLevel);
                        player.sendMessage(ChatColor.GREEN + "Core block level set to " + newLevel + ". Previous level: " + oldLevel);
                        openCoreLevelGUI(player);
                        return true;
                    } else if (slot == 23) {
                        // Remove 1 level
                        int oldLevel = coreBlock.getLevel();
                        int newLevel = Math.max(oldLevel - 1, 1);
                        coreBlock.setLevel(newLevel);
                        player.sendMessage(ChatColor.GREEN + "Core block level set to " + newLevel + ". Previous level: " + oldLevel);
                        openCoreLevelGUI(player);
                        return true;
                    } else if (slot == 24) {
                        // Remove 5 levels
                        int oldLevel = coreBlock.getLevel();
                        int newLevel = Math.max(oldLevel - 5, 1);
                        coreBlock.setLevel(newLevel);
                        player.sendMessage(ChatColor.GREEN + "Core block level set to " + newLevel + ". Previous level: " + oldLevel);
                        openCoreLevelGUI(player);
                        return true;
                    } else if (slot == 25) {
                        // Remove 10 levels
                        int oldLevel = coreBlock.getLevel();
                        int newLevel = Math.max(oldLevel - 10, 1);
                        coreBlock.setLevel(newLevel);
                        player.sendMessage(ChatColor.GREEN + "Core block level set to " + newLevel + ". Previous level: " + oldLevel);
                        openCoreLevelGUI(player);
                        return true;
                    } else if (slot == 13) {
                        // Set to max level
                        int oldLevel = coreBlock.getLevel();
                        coreBlock.setLevel(maxLevel);
                        player.sendMessage(ChatColor.GREEN + "Core block level set to " + maxLevel + " (max). Previous level: " + oldLevel);
                        openCoreLevelGUI(player);
                        return true;
                    }
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Error managing core level: " + e.getMessage());
                    e.printStackTrace();
                    return true;
                }
            } else {
                if (clan == null) {
                    player.sendMessage(ChatColor.RED + "You are not in a clan. Create or join a clan first.");
                } else {
                    player.sendMessage(ChatColor.RED + "Your clan doesn't have a core block. Place one first.");
                }
                return true;
            }

            if (slot == 31) {
                // Back to admin menu
                openAdminGUI(player);
                return true;
            }
        }

        // Handle blocks GUI clicks
        else if (guiType.equals("blocks")) {
            // Check if clicked on a block
            if (slot >= 10 && slot <= 14) {
                try {
                    // Core blocks
                    int level = slot - 9;
                    ItemStack coreBlock = plugin.getCraftingManager().createCoreBlock(level);
                    if (coreBlock != null) {
                        player.getInventory().addItem(coreBlock);
                        player.sendMessage(ChatColor.GREEN + "You received a Level " + level + " Core Block");
                    } else {
                        player.sendMessage(ChatColor.RED + "Error creating core block");
                    }
                    return true;
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
                    e.printStackTrace();
                    return true;
                }
            } else if (slot >= 19 && slot <= 23) {
                try {
                    // Claim blocks
                    int level = slot - 18;
                    ItemStack claimBlock = plugin.getCraftingManager().createClaimBlock(level);
                    if (claimBlock != null) {
                        player.getInventory().addItem(claimBlock);
                        player.sendMessage(ChatColor.GREEN + "You received a Level " + level + " Claim Block");
                    } else {
                        player.sendMessage(ChatColor.RED + "Error creating claim block");
                    }
                    return true;
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
                    e.printStackTrace();
                    return true;
                }
            } else if (slot >= 28 && slot <= 32) {
                try {
                    // Defense blocks
                    int level = slot - 27;
                    ItemStack defenseBlock = plugin.getCraftingManager().createDefenseBlock(level);
                    if (defenseBlock != null) {
                        player.getInventory().addItem(defenseBlock);
                        player.sendMessage(ChatColor.GREEN + "You received a Level " + level + " Defense Block");
                    } else {
                        player.sendMessage(ChatColor.RED + "Error creating defense block");
                    }
                    return true;
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
                    e.printStackTrace();
                    return true;
                }
            } else if (slot == 35) { // Updated slot for back button
                // Back to admin menu
                openAdminGUI(player);
                return true;
            }
        }

        // Handle points management clicks
        else if (guiType.equals("points")) {
            Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());

            if (clan != null) {
                try {
                    if (slot == 19) {
                        // Add 100 points
                        clan.addPoints(100);
                        player.sendMessage(ChatColor.GREEN + "Added 100 points to your clan. New total: " + clan.getPoints());
                        openPointsGUI(player);
                        return true;
                    } else if (slot == 20) {
                        // Add 1000 points
                        clan.addPoints(1000);
                        player.sendMessage(ChatColor.GREEN + "Added 1,000 points to your clan. New total: " + clan.getPoints());
                        openPointsGUI(player);
                        return true;
                    } else if (slot == 21) {
                        // Add 10000 points
                        clan.addPoints(10000);
                        player.sendMessage(ChatColor.GREEN + "Added 10,000 points to your clan. New total: " + clan.getPoints());
                        openPointsGUI(player);
                        return true;
                    } else if (slot == 22) {
                        // Add 100000 points
                        clan.addPoints(100000);
                        player.sendMessage(ChatColor.GREEN + "Added 100,000 points to your clan. New total: " + clan.getPoints());
                        openPointsGUI(player);
                        return true;
                    } else if (slot == 23) {
                        // Remove 100 points
                        if (clan.getPoints() >= 100) {
                            clan.removePoints(100);
                            player.sendMessage(ChatColor.GREEN + "Removed 100 points from your clan. New total: " + clan.getPoints());
                        } else {
                            player.sendMessage(ChatColor.RED + "Your clan doesn't have enough points. Current: " + clan.getPoints());
                        }
                        openPointsGUI(player);
                        return true;
                    } else if (slot == 24) {
                        // Remove 1000 points
                        if (clan.getPoints() >= 1000) {
                            clan.removePoints(1000);
                            player.sendMessage(ChatColor.GREEN + "Removed 1,000 points from your clan. New total: " + clan.getPoints());
                        } else {
                            player.sendMessage(ChatColor.RED + "Your clan doesn't have enough points. Current: " + clan.getPoints());
                        }
                        openPointsGUI(player);
                        return true;
                    } else if (slot == 25) {
                        // Remove 10000 points
                        if (clan.getPoints() >= 10000) {
                            clan.removePoints(10000);
                            player.sendMessage(ChatColor.GREEN + "Removed 10,000 points from your clan. New total: " + clan.getPoints());
                        } else {
                            player.sendMessage(ChatColor.RED + "Your clan doesn't have enough points. Current: " + clan.getPoints());
                        }
                        openPointsGUI(player);
                        return true;
                    }
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Error managing points: " + e.getMessage());
                    e.printStackTrace();
                    return true;
                }
            } else {
                player.sendMessage(ChatColor.RED + "You are not in a clan. Create or join a clan first.");
                return true;
            }

            if (slot == 31) {
                // Back to admin menu
                openAdminGUI(player);
                return true;
            }
        }

        // Cancel all clicks in admin GUIs
        return true;
    }

    /**
     * Remove a player from the open GUIs map
     * @param player Player to remove
     */
    public void removePlayer(Player player) {
        openAdminGUIs.remove(player);
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
