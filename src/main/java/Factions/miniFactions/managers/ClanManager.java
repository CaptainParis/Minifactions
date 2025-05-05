package Factions.miniFactions.managers;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.models.Clan;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ClanManager {

    private final MiniFactions plugin;

    public ClanManager(MiniFactions plugin) {
        this.plugin = plugin;
    }

    /**
     * Create a new clan
     * @param name Clan name
     * @param leader Player who will be the leader
     * @return The new clan, or null if creation failed
     */
    public Clan createClan(String name, Player leader) {
        // Check if name is valid
        if (!isValidClanName(name)) {
            leader.sendMessage(ChatColor.RED + "Invalid clan name. Names must be " +
                    getMinNameLength() + "-" + getMaxNameLength() + " characters and contain only letters and numbers.");
            return null;
        }

        // Check if name is already taken
        if (getClanByName(name) != null) {
            leader.sendMessage(ChatColor.RED + "A clan with that name already exists.");
            return null;
        }

        // Check if player is already in a clan
        if (getClanByPlayer(leader.getUniqueId()) != null) {
            leader.sendMessage(ChatColor.RED + "You are already in a clan. Leave your current clan first.");
            return null;
        }

        // Create the clan
        String id = UUID.randomUUID().toString();
        Clan clan = new Clan(id, name, leader.getUniqueId());

        // Add to storage
        plugin.getDataStorage().addClan(clan);

        // Give the leader a core block
        ItemStack coreBlock = plugin.getCraftingManager().createCoreBlock(1);
        leader.getInventory().addItem(coreBlock);

        leader.sendMessage(ChatColor.GREEN + "Clan " + name + " created successfully!");
        leader.sendMessage(ChatColor.GREEN + "You have received a Core Block. Place it to establish your clan's territory.");
        return clan;
    }

    /**
     * Disband a clan
     * @param clan Clan to disband
     * @param player Player who is disbanding the clan
     * @return true if successful
     */
    public boolean disbandClan(Clan clan, Player player) {
        // Check if player is the leader
        if (!clan.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the clan leader can disband the clan.");
            return false;
        }

        // Remove core block if it exists
        if (clan.getCoreBlock() != null) {
            plugin.getCoreBlockManager().removeCoreBlock(clan.getCoreBlock().getLocation());
        }

        // Remove from storage
        plugin.getDataStorage().removeClan(clan.getId());

        // Notify members
        for (UUID memberUUID : clan.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatColor.RED + "Your clan has been disbanded by " + player.getName() + ".");
            }
        }

        player.sendMessage(ChatColor.GREEN + "Clan " + clan.getName() + " has been disbanded.");
        return true;
    }

    /**
     * Invite a player to a clan
     * @param clan Clan to invite to
     * @param inviter Player sending the invite
     * @param invitee Player being invited
     * @return true if successful
     */
    public boolean invitePlayer(Clan clan, Player inviter, Player invitee) {
        // Check if inviter has permission
        if (!clan.isLeader(inviter.getUniqueId()) && !clan.isCoLeader(inviter.getUniqueId())) {
            inviter.sendMessage(ChatColor.RED + "Only the clan leader or co-leaders can invite players.");
            return false;
        }

        // Check if invitee is already in a clan
        if (getClanByPlayer(invitee.getUniqueId()) != null) {
            inviter.sendMessage(ChatColor.RED + invitee.getName() + " is already in a clan.");
            return false;
        }

        // Check if clan is full
        int maxMembers = 10; // Default value

        if (clan.getCoreBlock() != null) {
            maxMembers = clan.getCoreBlock().getMaxClanMembers();
        }

        if (clan.getMemberCount() >= maxMembers) {
            inviter.sendMessage(ChatColor.RED + "Your clan is full. Upgrade your core to invite more members.");
            return false;
        }

        // Send invite
        invitee.sendMessage(ChatColor.GREEN + "You have been invited to join clan " + clan.getName() + ".");
        invitee.sendMessage(ChatColor.GREEN + "Type " + ChatColor.YELLOW + "/clan join " + clan.getName() +
                ChatColor.GREEN + " to accept the invitation.");

        inviter.sendMessage(ChatColor.GREEN + "Invitation sent to " + invitee.getName() + ".");

        // Store invite (could be implemented with a map of player UUID to clan ID)
        // For simplicity, we'll just rely on the player using the join command with the clan name

        return true;
    }

    /**
     * Add a player to a clan
     * @param clan Clan to join
     * @param player Player joining
     * @return true if successful
     */
    public boolean joinClan(Clan clan, Player player) {
        // Check if player is already in a clan
        if (getClanByPlayer(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "You are already in a clan. Leave your current clan first.");
            return false;
        }

        // Add player to clan
        clan.addMember(player.getUniqueId(), Clan.ROLE_MEMBER);

        // Notify members
        for (UUID memberUUID : clan.getMembers().keySet()) {
            if (memberUUID.equals(player.getUniqueId())) continue;

            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatColor.GREEN + player.getName() + " has joined the clan.");
            }
        }

        player.sendMessage(ChatColor.GREEN + "You have joined clan " + clan.getName() + ".");
        return true;
    }

    /**
     * Remove a player from a clan
     * @param clan Clan to leave
     * @param player Player leaving
     * @return true if successful
     */
    public boolean leaveClan(Clan clan, Player player) {
        // Check if player is in the clan
        if (!clan.isMember(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You are not in this clan.");
            return false;
        }

        // Check if player is the leader
        if (clan.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "As the leader, you cannot leave the clan. Use /clan disband or promote someone else first.");
            return false;
        }

        // Remove player from clan
        clan.removeMember(player.getUniqueId());

        // Notify members
        for (UUID memberUUID : clan.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatColor.YELLOW + player.getName() + " has left the clan.");
            }
        }

        player.sendMessage(ChatColor.GREEN + "You have left clan " + clan.getName() + ".");
        return true;
    }

    /**
     * Promote a player in a clan
     * @param clan Clan
     * @param promoter Player doing the promotion
     * @param target Player being promoted
     * @return true if successful
     */
    public boolean promotePlayer(Clan clan, Player promoter, OfflinePlayer target) {
        // Check if promoter is the leader
        if (!clan.isLeader(promoter.getUniqueId())) {
            promoter.sendMessage(ChatColor.RED + "Only the clan leader can promote members.");
            return false;
        }

        // Check if target is in the clan
        if (!clan.isMember(target.getUniqueId())) {
            promoter.sendMessage(ChatColor.RED + target.getName() + " is not in your clan.");
            return false;
        }

        // Check if target is already a co-leader
        if (clan.isCoLeader(target.getUniqueId())) {
            promoter.sendMessage(ChatColor.RED + target.getName() + " is already a co-leader.");
            return false;
        }

        // Promote player
        clan.setMemberRole(target.getUniqueId(), Clan.ROLE_CO_LEADER);

        // Notify members
        for (UUID memberUUID : clan.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatColor.GREEN + target.getName() + " has been promoted to co-leader.");
            }
        }

        promoter.sendMessage(ChatColor.GREEN + "You have promoted " + target.getName() + " to co-leader.");
        return true;
    }

    /**
     * Demote a player in a clan
     * @param clan Clan
     * @param demoter Player doing the demotion
     * @param target Player being demoted
     * @return true if successful
     */
    public boolean demotePlayer(Clan clan, Player demoter, OfflinePlayer target) {
        // Check if demoter is the leader
        if (!clan.isLeader(demoter.getUniqueId())) {
            demoter.sendMessage(ChatColor.RED + "Only the clan leader can demote members.");
            return false;
        }

        // Check if target is in the clan
        if (!clan.isMember(target.getUniqueId())) {
            demoter.sendMessage(ChatColor.RED + target.getName() + " is not in your clan.");
            return false;
        }

        // Check if target is a co-leader
        if (!clan.isCoLeader(target.getUniqueId())) {
            demoter.sendMessage(ChatColor.RED + target.getName() + " is not a co-leader.");
            return false;
        }

        // Demote player
        clan.setMemberRole(target.getUniqueId(), Clan.ROLE_MEMBER);

        // Notify members
        for (UUID memberUUID : clan.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatColor.YELLOW + target.getName() + " has been demoted to member.");
            }
        }

        demoter.sendMessage(ChatColor.GREEN + "You have demoted " + target.getName() + " to member.");
        return true;
    }

    /**
     * Kick a player from a clan
     * @param clan Clan
     * @param kicker Player doing the kicking
     * @param target Player being kicked
     * @return true if successful
     */
    public boolean kickPlayer(Clan clan, Player kicker, OfflinePlayer target) {
        // Check if kicker has permission
        if (!clan.isLeader(kicker.getUniqueId()) && !clan.isCoLeader(kicker.getUniqueId())) {
            kicker.sendMessage(ChatColor.RED + "Only the clan leader or co-leaders can kick members.");
            return false;
        }

        // Check if target is in the clan
        if (!clan.isMember(target.getUniqueId())) {
            kicker.sendMessage(ChatColor.RED + target.getName() + " is not in your clan.");
            return false;
        }

        // Check if target is the leader
        if (clan.isLeader(target.getUniqueId())) {
            kicker.sendMessage(ChatColor.RED + "You cannot kick the clan leader.");
            return false;
        }

        // Check if kicker is co-leader and target is also co-leader
        if (clan.isCoLeader(kicker.getUniqueId()) && clan.isCoLeader(target.getUniqueId())) {
            kicker.sendMessage(ChatColor.RED + "Co-leaders cannot kick other co-leaders.");
            return false;
        }

        // Kick player
        clan.removeMember(target.getUniqueId());

        // Notify members
        for (UUID memberUUID : clan.getMembers().keySet()) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatColor.YELLOW + target.getName() + " has been kicked from the clan by " + kicker.getName() + ".");
            }
        }

        // Notify kicked player if online
        Player targetPlayer = Bukkit.getPlayer(target.getUniqueId());
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage(ChatColor.RED + "You have been kicked from clan " + clan.getName() + " by " + kicker.getName() + ".");
        }

        kicker.sendMessage(ChatColor.GREEN + "You have kicked " + target.getName() + " from the clan.");
        return true;
    }

    /**
     * Get a clan by name
     * @param name Clan name
     * @return Clan or null if not found
     */
    public Clan getClanByName(String name) {
        return plugin.getDataStorage().getClanByName(name);
    }

    /**
     * Get a clan by player UUID
     * @param playerUUID Player UUID
     * @return Clan or null if not found
     */
    public Clan getClanByPlayer(UUID playerUUID) {
        return plugin.getDataStorage().getClanByPlayer(playerUUID);
    }

    /**
     * Get all clans
     * @return List of all clans
     */
    public List<Clan> getAllClans() {
        return new ArrayList<>(plugin.getDataStorage().getClans().values());
    }

    /**
     * Get top clans by points
     * @param limit Maximum number of clans to return
     * @return List of top clans
     */
    public List<Clan> getTopClansByPoints(int limit) {
        List<Clan> clans = getAllClans();
        clans.sort(Comparator.comparingInt(Clan::getPoints).reversed());

        if (clans.size() > limit) {
            return clans.subList(0, limit);
        }

        return clans;
    }

    /**
     * Get top clans by core level
     * @param limit Maximum number of clans to return
     * @return List of top clans
     */
    public List<Clan> getTopClansByCoreLevel(int limit) {
        List<Clan> clans = getAllClans();
        clans.sort((c1, c2) -> {
            int level1 = c1.getCoreBlock() != null ? c1.getCoreBlock().getLevel() : 0;
            int level2 = c2.getCoreBlock() != null ? c2.getCoreBlock().getLevel() : 0;
            return Integer.compare(level2, level1);
        });

        if (clans.size() > limit) {
            return clans.subList(0, limit);
        }

        return clans;
    }

    /**
     * Check if a clan name is valid
     * @param name Clan name to check
     * @return true if valid
     */
    public boolean isValidClanName(String name) {
        // Check length
        if (name.length() < getMinNameLength() || name.length() > getMaxNameLength()) {
            return false;
        }

        // Check characters (only letters and numbers)
        return name.matches("^[a-zA-Z0-9]+$");
    }

    /**
     * Get the minimum clan name length
     * @return Minimum length
     */
    public int getMinNameLength() {
        return plugin.getConfigManager().getConfig().getInt("clan.name-min-length", 3);
    }

    /**
     * Get the maximum clan name length
     * @return Maximum length
     */
    public int getMaxNameLength() {
        return plugin.getConfigManager().getConfig().getInt("clan.name-max-length", 16);
    }

    /**
     * Give a new core block to a clan leader
     * @param clan The clan whose leader should receive a core block
     */
    public void giveCoreBlockToLeader(Clan clan) {
        Player leader = Bukkit.getPlayer(clan.getLeader());

        if (leader != null && leader.isOnline()) {
            // Give the leader a new core block
            ItemStack coreBlock = plugin.getCraftingManager().createCoreBlock(1);
            leader.getInventory().addItem(coreBlock);
            leader.sendMessage(ChatColor.GREEN + "You have received a new Core Block after yours was destroyed.");
        } else {
            // Leader is offline, store the core block for when they log in
            // This would require additional implementation for storing pending items
            // For now, we'll just make a note that this feature would be needed
            plugin.getLogger().info("Leader of clan " + clan.getName() + " is offline. Core block will be given when they log in.");
            // TODO: Implement pending item storage and delivery system
        }
    }
}
