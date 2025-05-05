package Factions.miniFactions.commands;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.models.Clan;
import Factions.miniFactions.models.CoreBlock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ClanCommandManager implements CommandExecutor, TabCompleter {

    private final MiniFactions plugin;
    
    public ClanCommandManager(MiniFactions plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Register all commands
     */
    public void registerCommands() {
        plugin.getCommand("clan").setExecutor(this);
        plugin.getCommand("clan").setTabCompleter(this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use clan commands.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                handleCreateCommand(player, args);
                break;
            case "disband":
                handleDisbandCommand(player);
                break;
            case "invite":
                handleInviteCommand(player, args);
                break;
            case "join":
                handleJoinCommand(player, args);
                break;
            case "leave":
                handleLeaveCommand(player);
                break;
            case "promote":
                handlePromoteCommand(player, args);
                break;
            case "demote":
                handleDemoteCommand(player, args);
                break;
            case "kick":
                handleKickCommand(player, args);
                break;
            case "view":
                handleViewCommand(player);
                break;
            case "list":
                handleListCommand(player);
                break;
            case "members":
                handleMembersCommand(player);
                break;
            case "top":
                handleTopCommand(player, args);
                break;
            case "help":
                sendHelpMessage(player);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown command. Type /clan help for a list of commands.");
                break;
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String[] subCommands = {"create", "disband", "invite", "join", "leave", "promote", "demote", "kick", "view", "list", "members", "top", "help"};
            String input = args[0].toLowerCase();
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            String input = args[1].toLowerCase();
            
            switch (subCommand) {
                case "invite":
                case "promote":
                case "demote":
                case "kick":
                    // Complete with online player names
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().toLowerCase().startsWith(input)) {
                            completions.add(player.getName());
                        }
                    }
                    break;
                case "join":
                    // Complete with clan names
                    for (Clan clan : plugin.getDataStorage().getClans().values()) {
                        if (clan.getName().toLowerCase().startsWith(input)) {
                            completions.add(clan.getName());
                        }
                    }
                    break;
                case "top":
                    // Complete with top types
                    String[] topTypes = {"points", "level"};
                    for (String type : topTypes) {
                        if (type.startsWith(input)) {
                            completions.add(type);
                        }
                    }
                    break;
            }
        }
        
        return completions;
    }
    
    /**
     * Handle the create command
     * @param player Player executing the command
     * @param args Command arguments
     */
    private void handleCreateCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan create <name>");
            return;
        }
        
        String name = args[1];
        plugin.getClanManager().createClan(name, player);
    }
    
    /**
     * Handle the disband command
     * @param player Player executing the command
     */
    private void handleDisbandCommand(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan.");
            return;
        }
        
        plugin.getClanManager().disbandClan(clan, player);
    }
    
    /**
     * Handle the invite command
     * @param player Player executing the command
     * @param args Command arguments
     */
    private void handleInviteCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan invite <player>");
            return;
        }
        
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan.");
            return;
        }
        
        Player invitee = Bukkit.getPlayer(args[1]);
        
        if (invitee == null || !invitee.isOnline()) {
            player.sendMessage(ChatColor.RED + "Player not found or not online.");
            return;
        }
        
        plugin.getClanManager().invitePlayer(clan, player, invitee);
    }
    
    /**
     * Handle the join command
     * @param player Player executing the command
     * @param args Command arguments
     */
    private void handleJoinCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan join <name>");
            return;
        }
        
        String name = args[1];
        Clan clan = plugin.getClanManager().getClanByName(name);
        
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "Clan not found.");
            return;
        }
        
        plugin.getClanManager().joinClan(clan, player);
    }
    
    /**
     * Handle the leave command
     * @param player Player executing the command
     */
    private void handleLeaveCommand(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan.");
            return;
        }
        
        plugin.getClanManager().leaveClan(clan, player);
    }
    
    /**
     * Handle the promote command
     * @param player Player executing the command
     * @param args Command arguments
     */
    private void handlePromoteCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan promote <player>");
            return;
        }
        
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan.");
            return;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        
        plugin.getClanManager().promotePlayer(clan, player, target);
    }
    
    /**
     * Handle the demote command
     * @param player Player executing the command
     * @param args Command arguments
     */
    private void handleDemoteCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan demote <player>");
            return;
        }
        
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan.");
            return;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        
        plugin.getClanManager().demotePlayer(clan, player, target);
    }
    
    /**
     * Handle the kick command
     * @param player Player executing the command
     * @param args Command arguments
     */
    private void handleKickCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan kick <player>");
            return;
        }
        
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan.");
            return;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        
        plugin.getClanManager().kickPlayer(clan, player, target);
    }
    
    /**
     * Handle the view command
     * @param player Player executing the command
     */
    private void handleViewCommand(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan.");
            return;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Clan: " + clan.getName() + " ===");
        player.sendMessage(ChatColor.YELLOW + "Points: " + ChatColor.WHITE + clan.getPoints());
        player.sendMessage(ChatColor.YELLOW + "Members: " + ChatColor.WHITE + clan.getMemberCount());
        
        CoreBlock coreBlock = clan.getCoreBlock();
        if (coreBlock != null) {
            player.sendMessage(ChatColor.YELLOW + "Core Level: " + ChatColor.WHITE + coreBlock.getLevel());
            player.sendMessage(ChatColor.YELLOW + "Buildable Area: " + ChatColor.WHITE + coreBlock.getBuildableArea() + " blocks");
            player.sendMessage(ChatColor.YELLOW + "Defense Blocks: " + ChatColor.WHITE + clan.getDefenseBlockCount() + "/" + coreBlock.getMaxDefenseBlocks());
            player.sendMessage(ChatColor.YELLOW + "Claim Blocks: " + ChatColor.WHITE + clan.getClaimBlockCount() + "/" + coreBlock.getMaxClaimBlocks());
            player.sendMessage(ChatColor.YELLOW + "Clan Doors: " + ChatColor.WHITE + clan.getClanDoorCount() + "/" + coreBlock.getMaxClanDoors());
            
            int pointsPerDay = 0;
            for (int i = 0; i < clan.getClaimBlockCount() && i < coreBlock.getMaxClaimBlocks(); i++) {
                pointsPerDay += 100; // Simplified calculation
            }
            player.sendMessage(ChatColor.YELLOW + "Points Generation: " + ChatColor.WHITE + pointsPerDay + " points/day");
            
            if (coreBlock.isUpkeepDue()) {
                player.sendMessage(ChatColor.RED + "Upkeep: " + ChatColor.WHITE + "Due now! Cost: " + coreBlock.getUpkeepCost() + " points");
            } else {
                player.sendMessage(ChatColor.YELLOW + "Upkeep: " + ChatColor.WHITE + coreBlock.getUpkeepCost() + " points");
                player.sendMessage(ChatColor.YELLOW + "Days of Upkeep: " + ChatColor.WHITE + coreBlock.getDaysOfUpkeep());
            }
        } else {
            player.sendMessage(ChatColor.RED + "No core block found. Place one to access more features.");
        }
    }
    
    /**
     * Handle the list command
     * @param player Player executing the command
     */
    private void handleListCommand(Player player) {
        List<Clan> clans = plugin.getClanManager().getAllClans();
        
        if (clans.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No clans found.");
            return;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Clans ===");
        
        for (Clan clan : clans) {
            String coreLevel = clan.getCoreBlock() != null ? String.valueOf(clan.getCoreBlock().getLevel()) : "N/A";
            player.sendMessage(ChatColor.YELLOW + clan.getName() + ChatColor.WHITE + " - Points: " + clan.getPoints() + 
                    ", Members: " + clan.getMemberCount() + ", Core Level: " + coreLevel);
        }
    }
    
    /**
     * Handle the members command
     * @param player Player executing the command
     */
    private void handleMembersCommand(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan.");
            return;
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Clan Members ===");
        
        for (UUID memberUUID : clan.getMembers().keySet()) {
            String role = clan.getMemberRole(memberUUID);
            String name = Bukkit.getOfflinePlayer(memberUUID).getName();
            
            ChatColor roleColor;
            switch (role) {
                case Clan.ROLE_LEADER:
                    roleColor = ChatColor.GOLD;
                    break;
                case Clan.ROLE_CO_LEADER:
                    roleColor = ChatColor.YELLOW;
                    break;
                default:
                    roleColor = ChatColor.WHITE;
                    break;
            }
            
            player.sendMessage(roleColor + name + ChatColor.WHITE + " - " + role);
        }
    }
    
    /**
     * Handle the top command
     * @param player Player executing the command
     * @param args Command arguments
     */
    private void handleTopCommand(Player player, String[] args) {
        String type = "points";
        if (args.length > 1) {
            type = args[1].toLowerCase();
        }
        
        List<Clan> topClans;
        if (type.equals("level")) {
            topClans = plugin.getClanManager().getTopClansByCoreLevel(10);
            player.sendMessage(ChatColor.GOLD + "=== Top Clans by Core Level ===");
        } else {
            topClans = plugin.getClanManager().getTopClansByPoints(10);
            player.sendMessage(ChatColor.GOLD + "=== Top Clans by Points ===");
        }
        
        if (topClans.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No clans found.");
            return;
        }
        
        int rank = 1;
        for (Clan clan : topClans) {
            if (type.equals("level")) {
                int level = clan.getCoreBlock() != null ? clan.getCoreBlock().getLevel() : 0;
                player.sendMessage(ChatColor.YELLOW + "#" + rank + ": " + clan.getName() + ChatColor.WHITE + 
                        " - Level: " + level + ", Members: " + clan.getMemberCount());
            } else {
                player.sendMessage(ChatColor.YELLOW + "#" + rank + ": " + clan.getName() + ChatColor.WHITE + 
                        " - Points: " + clan.getPoints() + ", Members: " + clan.getMemberCount());
            }
            rank++;
        }
    }
    
    /**
     * Send help message to player
     * @param player Player to send message to
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== MiniFactions Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/clan create <name>" + ChatColor.WHITE + " - Create a new clan");
        player.sendMessage(ChatColor.YELLOW + "/clan disband" + ChatColor.WHITE + " - Disband your clan (leader only)");
        player.sendMessage(ChatColor.YELLOW + "/clan invite <player>" + ChatColor.WHITE + " - Invite a player to your clan");
        player.sendMessage(ChatColor.YELLOW + "/clan join <name>" + ChatColor.WHITE + " - Join a clan");
        player.sendMessage(ChatColor.YELLOW + "/clan leave" + ChatColor.WHITE + " - Leave your clan");
        player.sendMessage(ChatColor.YELLOW + "/clan promote <player>" + ChatColor.WHITE + " - Promote a member to co-leader");
        player.sendMessage(ChatColor.YELLOW + "/clan demote <player>" + ChatColor.WHITE + " - Demote a co-leader to member");
        player.sendMessage(ChatColor.YELLOW + "/clan kick <player>" + ChatColor.WHITE + " - Kick a member from your clan");
        player.sendMessage(ChatColor.YELLOW + "/clan view" + ChatColor.WHITE + " - View your clan's stats");
        player.sendMessage(ChatColor.YELLOW + "/clan list" + ChatColor.WHITE + " - List all clans");
        player.sendMessage(ChatColor.YELLOW + "/clan members" + ChatColor.WHITE + " - List your clan's members");
        player.sendMessage(ChatColor.YELLOW + "/clan top [points|level]" + ChatColor.WHITE + " - View top clans");
    }
}
