package Factions.miniFactions.commands;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.models.Clan;
import Factions.miniFactions.models.ClaimBlock;
import Factions.miniFactions.models.ClanDoor;
import Factions.miniFactions.models.CoreBlock;
import Factions.miniFactions.models.DefenseBlock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin commands for MiniFactions
 */
public class AdminCommands implements CommandExecutor, TabCompleter {

    private final MiniFactions plugin;

    public AdminCommands(MiniFactions plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("minifactions.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "points":
                handlePointsCommand(sender, args);
                break;
            case "level":
                handleLevelCommand(sender, args);
                break;
            case "block":
                handleBlockCommand(sender, args);
                break;
            case "explosive":
                handleExplosiveCommand(sender, args);
                break;
            case "upkeep":
                handleUpkeepCommand(sender, args);
                break;
            case "door":
                handleDoorCommand(sender, args);
                break;
            case "help":
                sendHelpMessage(sender);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown command. Type /admin help for a list of commands.");
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("minifactions.admin")) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String[] subCommands = {"points", "level", "block", "explosive", "upkeep", "door", "help"};
            String input = args[0].toLowerCase();

            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String input = args[1].toLowerCase();

            if (subCommand.equals("points")) {
                String[] options = {"add", "remove", "set"};
                for (String option : options) {
                    if (option.startsWith(input)) {
                        completions.add(option);
                    }
                }
            } else if (subCommand.equals("level")) {
                String[] options = {"set", "add"};
                for (String option : options) {
                    if (option.startsWith(input)) {
                        completions.add(option);
                    }
                }
            } else if (subCommand.equals("block")) {
                String[] options = {"core", "claim", "defense"};
                for (String option : options) {
                    if (option.startsWith(input)) {
                        completions.add(option);
                    }
                }
            } else if (subCommand.equals("explosive")) {
                String[] options = {"give"};
                for (String option : options) {
                    if (option.startsWith(input)) {
                        completions.add(option);
                    }
                }
            } else if (subCommand.equals("upkeep")) {
                String[] options = {"check", "reset", "exempt"};
                for (String option : options) {
                    if (option.startsWith(input)) {
                        completions.add(option);
                    }
                }
            } else if (subCommand.equals("door")) {
                String[] options = {"give", "upgrade", "list"};
                for (String option : options) {
                    if (option.startsWith(input)) {
                        completions.add(option);
                    }
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            String input = args[2].toLowerCase();

            if (subCommand.equals("points") || subCommand.equals("level")) {
                // Suggest player names
                return getOnlinePlayerNames(input);
            } else if (subCommand.equals("block")) {
                if (action.equals("core") || action.equals("claim") || action.equals("defense")) {
                    // Suggest levels 1-5
                    List<String> levels = Arrays.asList("1", "2", "3", "4", "5");
                    return levels.stream()
                            .filter(level -> level.startsWith(input))
                            .collect(Collectors.toList());
                }
            } else if (subCommand.equals("explosive")) {
                if (action.equals("give")) {
                    // Suggest tiers 1-5
                    List<String> tiers = Arrays.asList("1", "2", "3", "4", "5");
                    return tiers.stream()
                            .filter(tier -> tier.startsWith(input))
                            .collect(Collectors.toList());
                }
            }
        } else if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            String action = args[1].toLowerCase();

            if (subCommand.equals("points")) {
                if (action.equals("add") || action.equals("remove") || action.equals("set")) {
                    // Suggest amounts
                    List<String> amounts = Arrays.asList("100", "1000", "10000", "100000");
                    String input = args[3].toLowerCase();
                    return amounts.stream()
                            .filter(amount -> amount.startsWith(input))
                            .collect(Collectors.toList());
                }
            } else if (subCommand.equals("level")) {
                if (action.equals("set") || action.equals("add")) {
                    // Suggest levels 1-20
                    List<String> levels = new ArrayList<>();
                    for (int i = 1; i <= 20; i++) {
                        levels.add(String.valueOf(i));
                    }
                    String input = args[3].toLowerCase();
                    return levels.stream()
                            .filter(level -> level.startsWith(input))
                            .collect(Collectors.toList());
                }
            } else if (subCommand.equals("block") || subCommand.equals("explosive")) {
                // Suggest player names
                String input = args[3].toLowerCase();
                return getOnlinePlayerNames(input);
            }
        }

        return completions;
    }

    /**
     * Handle the points command
     * @param sender Command sender
     * @param args Command arguments
     */
    private void handlePointsCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /admin points <add|remove|set> <player> <amount>");
            return;
        }

        String action = args[1].toLowerCase();
        String playerName = args[2];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(target.getUniqueId());
        if (clan == null) {
            sender.sendMessage(ChatColor.RED + playerName + " is not in a clan.");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
            if (amount < 0) {
                sender.sendMessage(ChatColor.RED + "Amount must be a positive number.");
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[3]);
            return;
        }

        int oldPoints = clan.getPoints();

        switch (action) {
            case "add":
                clan.addPoints(amount);
                sender.sendMessage(ChatColor.GREEN + "Added " + amount + " points to " + clan.getName() +
                        ". New total: " + clan.getPoints());
                target.sendMessage(ChatColor.GREEN + "An admin added " + amount + " points to your clan. New total: " +
                        clan.getPoints());
                break;
            case "remove":
                if (clan.getPoints() < amount) {
                    sender.sendMessage(ChatColor.RED + "Clan only has " + clan.getPoints() + " points.");
                    return;
                }
                clan.removePoints(amount);
                sender.sendMessage(ChatColor.GREEN + "Removed " + amount + " points from " + clan.getName() +
                        ". New total: " + clan.getPoints());
                target.sendMessage(ChatColor.GREEN + "An admin removed " + amount + " points from your clan. New total: " +
                        clan.getPoints());
                break;
            case "set":
                clan.setPoints(amount);
                sender.sendMessage(ChatColor.GREEN + "Set " + clan.getName() + "'s points to " + amount +
                        ". Previous: " + oldPoints);
                target.sendMessage(ChatColor.GREEN + "An admin set your clan's points to " + amount +
                        ". Previous: " + oldPoints);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid action. Use add, remove, or set.");
                break;
        }
    }

    /**
     * Handle the level command
     * @param sender Command sender
     * @param args Command arguments
     */
    private void handleLevelCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /admin level <set|add> <player> <level>");
            return;
        }

        String action = args[1].toLowerCase();
        String playerName = args[2];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(target.getUniqueId());
        if (clan == null) {
            sender.sendMessage(ChatColor.RED + playerName + " is not in a clan.");
            return;
        }

        CoreBlock coreBlock = clan.getCoreBlock();
        if (coreBlock == null) {
            sender.sendMessage(ChatColor.RED + clan.getName() + " doesn't have a core block.");
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[3]);
            if (level < 1) {
                sender.sendMessage(ChatColor.RED + "Level must be at least 1.");
                return;
            }

            int maxLevel = plugin.getConfigManager().getConfig().getInt("core.max-level", 20);
            if (level > maxLevel) {
                sender.sendMessage(ChatColor.RED + "Maximum level is " + maxLevel + ".");
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid level: " + args[3]);
            return;
        }

        int oldLevel = coreBlock.getLevel();

        switch (action) {
            case "set":
                coreBlock.setLevel(level);
                sender.sendMessage(ChatColor.GREEN + "Set " + clan.getName() + "'s core block level to " + level +
                        ". Previous: " + oldLevel);
                target.sendMessage(ChatColor.GREEN + "An admin set your clan's core block level to " + level +
                        ". Previous: " + oldLevel);
                break;
            case "add":
                int newLevel = Math.min(oldLevel + level, plugin.getConfigManager().getConfig().getInt("core.max-level", 20));
                coreBlock.setLevel(newLevel);
                sender.sendMessage(ChatColor.GREEN + "Added " + level + " levels to " + clan.getName() +
                        "'s core block. New level: " + newLevel);
                target.sendMessage(ChatColor.GREEN + "An admin added " + level + " levels to your clan's core block. New level: " +
                        newLevel);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid action. Use set or add.");
                break;
        }
    }

    /**
     * Handle the block command
     * @param sender Command sender
     * @param args Command arguments
     */
    private void handleBlockCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /admin block <core|claim|defense> <level> <player>");
            return;
        }

        String blockType = args[1].toLowerCase();
        int level;
        try {
            level = Integer.parseInt(args[2]);
            if (level < 1 || level > 5) {
                sender.sendMessage(ChatColor.RED + "Level must be between 1 and 5.");
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid level: " + args[2]);
            return;
        }

        String playerName = args[3];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return;
        }

        ItemStack block = null;
        String blockName = "";

        switch (blockType) {
            case "core":
                block = plugin.getCraftingManager().createCoreBlock(level);
                blockName = "Core Block (Level " + level + ")";
                break;
            case "claim":
                block = plugin.getCraftingManager().createClaimBlock(level);
                blockName = "Claim Block (Level " + level + ")";
                break;
            case "defense":
                block = plugin.getCraftingManager().createDefenseBlock(level);
                blockName = "Defense Block (Tier " + level + ")";
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid block type. Use core, claim, or defense.");
                return;
        }

        if (block != null) {
            target.getInventory().addItem(block);
            sender.sendMessage(ChatColor.GREEN + "Gave " + blockName + " to " + target.getName() + ".");
            target.sendMessage(ChatColor.GREEN + "You received a " + blockName + " from an admin.");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to create block.");
        }
    }

    /**
     * Handle the explosive command
     * @param sender Command sender
     * @param args Command arguments
     */
    private void handleExplosiveCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /admin explosive give <tier> <player>");
            return;
        }

        if (!args[1].equalsIgnoreCase("give")) {
            sender.sendMessage(ChatColor.RED + "Usage: /admin explosive give <tier> <player>");
            return;
        }

        int tier;
        try {
            tier = Integer.parseInt(args[2]);
            if (tier < 1 || tier > 5) {
                sender.sendMessage(ChatColor.RED + "Tier must be between 1 and 5.");
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid tier: " + args[2]);
            return;
        }

        String playerName = args[3];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return;
        }

        ItemStack explosive = plugin.getCraftingManager().createExplosive(tier);
        if (explosive != null) {
            target.getInventory().addItem(explosive);
            sender.sendMessage(ChatColor.GREEN + "Gave Explosive (Tier " + tier + ") to " + target.getName() + ".");
            target.sendMessage(ChatColor.GREEN + "You received an Explosive (Tier " + tier + ") from an admin.");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to create explosive.");
        }
    }

    /**
     * Handle the upkeep command
     * @param sender Command sender
     * @param args Command arguments
     */
    private void handleUpkeepCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /admin upkeep <check|reset|exempt> <player> [true/false] [duration]");
            return;
        }

        String action = args[1].toLowerCase();

        if (args.length < 3 && !action.equals("help")) {
            sender.sendMessage(ChatColor.RED + "Usage: /admin upkeep <check|reset|exempt> <player> [true/false] [duration]");
            return;
        }

        if (action.equals("help")) {
            sender.sendMessage(ChatColor.GOLD + "=== Upkeep Admin Commands ===");
            sender.sendMessage(ChatColor.YELLOW + "/admin upkeep check <player>" + ChatColor.WHITE + " - Check upkeep status for a player's clan");
            sender.sendMessage(ChatColor.YELLOW + "/admin upkeep reset <player>" + ChatColor.WHITE + " - Reset upkeep timers for a player's clan");
            sender.sendMessage(ChatColor.YELLOW + "/admin upkeep exempt <player> [true/false] [duration]" + ChatColor.WHITE + " - Exempt a clan from upkeep");
            return;
        }

        String playerName = args[2];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(target.getUniqueId());
        if (clan == null) {
            sender.sendMessage(ChatColor.RED + playerName + " is not in a clan.");
            return;
        }

        switch (action) {
            case "check":
                checkClanUpkeep(sender, clan);
                break;

            case "reset":
                resetClanUpkeep(sender, clan);
                break;

            case "exempt":
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /admin upkeep exempt <player> <true/false> [duration]");
                    return;
                }

                boolean exempt = Boolean.parseBoolean(args[3]);
                long duration = 0; // 0 means permanent

                if (args.length >= 5) {
                    try {
                        duration = Long.parseLong(args[4]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid duration: " + args[4]);
                        return;
                    }
                }

                exemptClanFromUpkeep(sender, clan, exempt, duration);
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Invalid action. Use check, reset, or exempt.");
                break;
        }
    }

    /**
     * Check a clan's upkeep status
     * @param sender Command sender
     * @param clan The clan to check
     */
    private void checkClanUpkeep(CommandSender sender, Clan clan) {
        CoreBlock coreBlock = clan.getCoreBlock();

        if (coreBlock == null) {
            sender.sendMessage(ChatColor.RED + clan.getName() + " doesn't have a core block.");
            return;
        }

        boolean isExempt = plugin.getUpkeepManager().isExempt(clan.getLeader());

        sender.sendMessage(ChatColor.GOLD + "=== Upkeep Status for " + clan.getName() + " ===");
        sender.sendMessage(ChatColor.YELLOW + "Exempt from upkeep: " + ChatColor.WHITE + isExempt);

        if (!isExempt) {
            boolean upkeepDue = coreBlock.isUpkeepDue();
            int upkeepCost = coreBlock.getUpkeepCost();
            int daysOfUpkeep = coreBlock.getDaysOfUpkeep();

            sender.sendMessage(ChatColor.YELLOW + "Upkeep due: " + ChatColor.WHITE + upkeepDue);
            sender.sendMessage(ChatColor.YELLOW + "Upkeep cost: " + ChatColor.WHITE + upkeepCost + " points");
            sender.sendMessage(ChatColor.YELLOW + "Days of upkeep: " + ChatColor.WHITE + daysOfUpkeep);

            // Calculate time until next upkeep
            FileConfiguration config = plugin.getConfigManager().getConfig();
            int paymentInterval = config.getInt("core.upkeep.payment-interval", 24);
            long intervalMillis = paymentInterval * 60 * 60 * 1000L; // Convert hours to milliseconds
            long timeUntilUpkeep = (coreBlock.getLastUpkeepTime() + intervalMillis) - System.currentTimeMillis();

            if (timeUntilUpkeep > 0) {
                long hoursRemaining = timeUntilUpkeep / (60 * 60 * 1000L);
                sender.sendMessage(ChatColor.YELLOW + "Next upkeep due in: " + ChatColor.WHITE + hoursRemaining + " hours");
            } else {
                sender.sendMessage(ChatColor.RED + "Upkeep is overdue!");
            }
        }
    }

    /**
     * Reset a clan's upkeep timers
     * @param sender Command sender
     * @param clan The clan to reset
     */
    private void resetClanUpkeep(CommandSender sender, Clan clan) {
        CoreBlock coreBlock = clan.getCoreBlock();

        if (coreBlock == null) {
            sender.sendMessage(ChatColor.RED + clan.getName() + " doesn't have a core block.");
            return;
        }

        coreBlock.updateUpkeepTime();

        // Update all claim blocks
        for (ClaimBlock claimBlock : clan.getClaimBlocks()) {
            claimBlock.updatePointGenerationTime();
        }

        // Update all defense blocks
        for (DefenseBlock defenseBlock : clan.getDefenseBlocks()) {
            defenseBlock.updatePlacementTime();
        }

        // Update all clan doors
        for (ClanDoor clanDoor : clan.getClanDoors()) {
            clanDoor.updateUpkeepTime();
        }

        sender.sendMessage(ChatColor.GREEN + "Reset all upkeep timers for clan " + clan.getName() + ".");

        // Notify clan members
        for (UUID memberUUID : clan.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatColor.GREEN + "An admin has reset your clan's upkeep timers.");
            }
        }
    }

    /**
     * Exempt a clan from upkeep
     * @param sender Command sender
     * @param clan The clan to exempt
     * @param exempt Whether to exempt or not
     * @param duration Duration in hours (0 for permanent)
     */
    private void exemptClanFromUpkeep(CommandSender sender, Clan clan, boolean exempt, long duration) {
        plugin.getUpkeepManager().setExempt(clan.getLeader(), exempt, duration);

        if (exempt) {
            String durationStr = duration > 0 ? duration + " hours" : "permanently";
            sender.sendMessage(ChatColor.GREEN + "Exempted clan " + clan.getName() + " from upkeep " + durationStr + ".");

            // Notify clan members
            for (UUID memberUUID : clan.getMembers().keySet()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && member.isOnline()) {
                    member.sendMessage(ChatColor.GREEN + "An admin has exempted your clan from upkeep " + durationStr + ".");
                }
            }
        } else {
            sender.sendMessage(ChatColor.GREEN + "Removed upkeep exemption from clan " + clan.getName() + ".");

            // Notify clan members
            for (UUID memberUUID : clan.getMembers().keySet()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && member.isOnline()) {
                    member.sendMessage(ChatColor.GREEN + "An admin has removed your clan's upkeep exemption.");
                }
            }
        }
    }

    /**
     * Handle the door command
     * @param sender Command sender
     * @param args Command arguments
     */
    private void handleDoorCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /admin door <give|upgrade|list> <player> [tier]");
            return;
        }

        String action = args[1].toLowerCase();

        if (args.length < 3 && !action.equals("help")) {
            sender.sendMessage(ChatColor.RED + "Usage: /admin door <give|upgrade|list> <player> [tier]");
            return;
        }

        if (action.equals("help")) {
            sender.sendMessage(ChatColor.GOLD + "=== Door Admin Commands ===");
            sender.sendMessage(ChatColor.YELLOW + "/admin door give <player> [tier]" + ChatColor.WHITE + " - Give a clan door to a player");
            sender.sendMessage(ChatColor.YELLOW + "/admin door upgrade <player> <tier>" + ChatColor.WHITE + " - Upgrade a player's clan doors");
            sender.sendMessage(ChatColor.YELLOW + "/admin door list <player>" + ChatColor.WHITE + " - List all doors owned by a player's clan");
            return;
        }

        String playerName = args[2];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return;
        }

        Clan clan = plugin.getClanManager().getClanByPlayer(target.getUniqueId());
        if (clan == null) {
            sender.sendMessage(ChatColor.RED + playerName + " is not in a clan.");
            return;
        }

        switch (action) {
            case "give":
                int tier = 1;
                if (args.length >= 4) {
                    try {
                        tier = Integer.parseInt(args[3]);
                        if (tier < 1) {
                            sender.sendMessage(ChatColor.RED + "Tier must be at least 1.");
                            return;
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid tier: " + args[3]);
                        return;
                    }
                }

                giveClanDoor(sender, target, tier);
                break;

            case "upgrade":
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /admin door upgrade <player> <tier>");
                    return;
                }

                int upgradeTier;
                try {
                    upgradeTier = Integer.parseInt(args[3]);
                    if (upgradeTier < 1) {
                        sender.sendMessage(ChatColor.RED + "Tier must be at least 1.");
                        return;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid tier: " + args[3]);
                    return;
                }

                upgradeClanDoors(sender, clan, upgradeTier);
                break;

            case "list":
                listClanDoors(sender, clan);
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Invalid action. Use give, upgrade, or list.");
                break;
        }
    }

    /**
     * Give a clan door to a player
     * @param sender Command sender
     * @param player Player to give the door to
     * @param tier Door tier
     */
    private void giveClanDoor(CommandSender sender, Player player, int tier) {
        ItemStack door = plugin.getCraftingManager().createClanDoor();
        ItemMeta meta = door.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Clan Door (Tier " + tier + ")");
            meta.setCustomModelData(tier);
            door.setItemMeta(meta);
        }

        player.getInventory().addItem(door);
        sender.sendMessage(ChatColor.GREEN + "Gave Clan Door (Tier " + tier + ") to " + player.getName() + ".");
        player.sendMessage(ChatColor.GREEN + "You received a Clan Door (Tier " + tier + ") from an admin.");
    }

    /**
     * Upgrade all clan doors to a specific tier
     * @param sender Command sender
     * @param clan Clan whose doors to upgrade
     * @param tier New tier
     */
    private void upgradeClanDoors(CommandSender sender, Clan clan, int tier) {
        Set<ClanDoor> doors = clan.getClanDoors();

        if (doors.isEmpty()) {
            sender.sendMessage(ChatColor.RED + clan.getName() + " doesn't have any clan doors.");
            return;
        }

        int upgradedCount = 0;
        for (ClanDoor door : doors) {
            if (door.getTier() < tier) {
                door.setTier(tier);
                upgradedCount++;
            }
        }

        if (upgradedCount > 0) {
            sender.sendMessage(ChatColor.GREEN + "Upgraded " + upgradedCount + " clan doors to tier " + tier + ".");

            // Notify clan members
            for (UUID memberUUID : clan.getMembers().keySet()) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && member.isOnline()) {
                    member.sendMessage(ChatColor.GREEN + "An admin has upgraded " + upgradedCount + " of your clan's doors to tier " + tier + ".");
                }
            }
        } else {
            sender.sendMessage(ChatColor.YELLOW + "No doors were upgraded. All doors are already at tier " + tier + " or higher.");
        }
    }

    /**
     * List all clan doors
     * @param sender Command sender
     * @param clan Clan whose doors to list
     */
    private void listClanDoors(CommandSender sender, Clan clan) {
        Set<ClanDoor> doors = clan.getClanDoors();

        if (doors.isEmpty()) {
            sender.sendMessage(ChatColor.RED + clan.getName() + " doesn't have any clan doors.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Clan Doors for " + clan.getName() + " (" + doors.size() + ") ===");

        int i = 1;
        for (ClanDoor door : doors) {
            Location loc = door.getLocation();
            sender.sendMessage(ChatColor.YELLOW + String.valueOf(i) + ". " + ChatColor.WHITE + "Tier " + door.getTier() + " at " +
                    loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")");
            i++;
        }
    }

    /**
     * Send help message to sender
     * @param sender Command sender
     */
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== MiniFactions Admin Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/admin points add <player> <amount>" + ChatColor.WHITE + " - Add points to a player's clan");
        sender.sendMessage(ChatColor.YELLOW + "/admin points remove <player> <amount>" + ChatColor.WHITE + " - Remove points from a player's clan");
        sender.sendMessage(ChatColor.YELLOW + "/admin points set <player> <amount>" + ChatColor.WHITE + " - Set a player's clan points");
        sender.sendMessage(ChatColor.YELLOW + "/admin level set <player> <level>" + ChatColor.WHITE + " - Set a player's clan core block level");
        sender.sendMessage(ChatColor.YELLOW + "/admin level add <player> <amount>" + ChatColor.WHITE + " - Add levels to a player's clan core block");
        sender.sendMessage(ChatColor.YELLOW + "/admin block core <level> <player>" + ChatColor.WHITE + " - Give a core block to a player");
        sender.sendMessage(ChatColor.YELLOW + "/admin block claim <level> <player>" + ChatColor.WHITE + " - Give a claim block to a player");
        sender.sendMessage(ChatColor.YELLOW + "/admin block defense <level> <player>" + ChatColor.WHITE + " - Give a defense block to a player");
        sender.sendMessage(ChatColor.YELLOW + "/admin explosive give <tier> <player>" + ChatColor.WHITE + " - Give an explosive to a player");
        sender.sendMessage(ChatColor.YELLOW + "/admin upkeep check <player>" + ChatColor.WHITE + " - Check upkeep status for a player's clan");
        sender.sendMessage(ChatColor.YELLOW + "/admin upkeep reset <player>" + ChatColor.WHITE + " - Reset upkeep timers for a player's clan");
        sender.sendMessage(ChatColor.YELLOW + "/admin upkeep exempt <player> <true/false> [duration]" + ChatColor.WHITE + " - Exempt a clan from upkeep");
        sender.sendMessage(ChatColor.YELLOW + "/admin door give <player> [tier]" + ChatColor.WHITE + " - Give a clan door to a player");
        sender.sendMessage(ChatColor.YELLOW + "/admin door upgrade <player> <tier>" + ChatColor.WHITE + " - Upgrade a player's clan doors");
        sender.sendMessage(ChatColor.YELLOW + "/admin door list <player>" + ChatColor.WHITE + " - List all doors owned by a player's clan");
    }

    /**
     * Get online player names that start with the given input
     * @param input Input to filter by
     * @return List of matching player names
     */
    private List<String> getOnlinePlayerNames(String input) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
