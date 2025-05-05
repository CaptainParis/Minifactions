package Factions.miniFactions.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Clan {

    public static final String ROLE_LEADER = "LEADER";
    public static final String ROLE_CO_LEADER = "CO_LEADER";
    public static final String ROLE_MEMBER = "MEMBER";
    
    private final String id;
    private String name;
    private final UUID leader;
    private final Map<UUID, String> members = new HashMap<>();
    private int points;
    private CoreBlock coreBlock;
    private final Set<ClaimBlock> claimBlocks = new HashSet<>();
    private final Set<DefenseBlock> defenseBlocks = new HashSet<>();
    private final Set<ClanDoor> clanDoors = new HashSet<>();
    
    /**
     * Create a new clan
     * @param id Unique clan ID
     * @param name Clan name
     * @param leader UUID of the clan leader
     */
    public Clan(String id, String name, UUID leader) {
        this.id = id;
        this.name = name;
        this.leader = leader;
        this.members.put(leader, ROLE_LEADER);
        this.points = 0;
    }
    
    /**
     * Get the clan ID
     * @return Clan ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the clan name
     * @return Clan name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the clan name
     * @param name New clan name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Get the clan leader UUID
     * @return Leader UUID
     */
    public UUID getLeader() {
        return leader;
    }
    
    /**
     * Get all clan members
     * @return Map of member UUID to role
     */
    public Map<UUID, String> getMembers() {
        return members;
    }
    
    /**
     * Add a member to the clan
     * @param playerUUID Player UUID
     * @param role Member role
     */
    public void addMember(UUID playerUUID, String role) {
        members.put(playerUUID, role);
    }
    
    /**
     * Remove a member from the clan
     * @param playerUUID Player UUID
     */
    public void removeMember(UUID playerUUID) {
        members.remove(playerUUID);
    }
    
    /**
     * Check if a player is a member of the clan
     * @param playerUUID Player UUID
     * @return true if the player is a member
     */
    public boolean isMember(UUID playerUUID) {
        return members.containsKey(playerUUID);
    }
    
    /**
     * Get a member's role
     * @param playerUUID Player UUID
     * @return Member role or null if not a member
     */
    public String getMemberRole(UUID playerUUID) {
        return members.get(playerUUID);
    }
    
    /**
     * Set a member's role
     * @param playerUUID Player UUID
     * @param role New role
     */
    public void setMemberRole(UUID playerUUID, String role) {
        if (members.containsKey(playerUUID)) {
            members.put(playerUUID, role);
        }
    }
    
    /**
     * Check if a player is the leader
     * @param playerUUID Player UUID
     * @return true if the player is the leader
     */
    public boolean isLeader(UUID playerUUID) {
        return leader.equals(playerUUID);
    }
    
    /**
     * Check if a player is a co-leader
     * @param playerUUID Player UUID
     * @return true if the player is a co-leader
     */
    public boolean isCoLeader(UUID playerUUID) {
        return ROLE_CO_LEADER.equals(members.get(playerUUID));
    }
    
    /**
     * Get the clan's points
     * @return Points
     */
    public int getPoints() {
        return points;
    }
    
    /**
     * Set the clan's points
     * @param points New points
     */
    public void setPoints(int points) {
        this.points = points;
    }
    
    /**
     * Add points to the clan
     * @param amount Amount to add
     */
    public void addPoints(int amount) {
        this.points += amount;
    }
    
    /**
     * Remove points from the clan
     * @param amount Amount to remove
     * @return true if successful, false if not enough points
     */
    public boolean removePoints(int amount) {
        if (points >= amount) {
            points -= amount;
            return true;
        }
        return false;
    }
    
    /**
     * Get the clan's core block
     * @return CoreBlock or null if not set
     */
    public CoreBlock getCoreBlock() {
        return coreBlock;
    }
    
    /**
     * Set the clan's core block
     * @param coreBlock CoreBlock
     */
    public void setCoreBlock(CoreBlock coreBlock) {
        this.coreBlock = coreBlock;
    }
    
    /**
     * Get all claim blocks
     * @return Set of ClaimBlock
     */
    public Set<ClaimBlock> getClaimBlocks() {
        return claimBlocks;
    }
    
    /**
     * Add a claim block
     * @param claimBlock ClaimBlock to add
     */
    public void addClaimBlock(ClaimBlock claimBlock) {
        claimBlocks.add(claimBlock);
    }
    
    /**
     * Remove a claim block
     * @param claimBlock ClaimBlock to remove
     */
    public void removeClaimBlock(ClaimBlock claimBlock) {
        claimBlocks.remove(claimBlock);
    }
    
    /**
     * Get all defense blocks
     * @return Set of DefenseBlock
     */
    public Set<DefenseBlock> getDefenseBlocks() {
        return defenseBlocks;
    }
    
    /**
     * Add a defense block
     * @param defenseBlock DefenseBlock to add
     */
    public void addDefenseBlock(DefenseBlock defenseBlock) {
        defenseBlocks.add(defenseBlock);
    }
    
    /**
     * Remove a defense block
     * @param defenseBlock DefenseBlock to remove
     */
    public void removeDefenseBlock(DefenseBlock defenseBlock) {
        defenseBlocks.remove(defenseBlock);
    }
    
    /**
     * Get all clan doors
     * @return Set of ClanDoor
     */
    public Set<ClanDoor> getClanDoors() {
        return clanDoors;
    }
    
    /**
     * Add a clan door
     * @param clanDoor ClanDoor to add
     */
    public void addClanDoor(ClanDoor clanDoor) {
        clanDoors.add(clanDoor);
    }
    
    /**
     * Remove a clan door
     * @param clanDoor ClanDoor to remove
     */
    public void removeClanDoor(ClanDoor clanDoor) {
        clanDoors.remove(clanDoor);
    }
    
    /**
     * Get the number of members
     * @return Member count
     */
    public int getMemberCount() {
        return members.size();
    }
    
    /**
     * Get the number of claim blocks
     * @return Claim block count
     */
    public int getClaimBlockCount() {
        return claimBlocks.size();
    }
    
    /**
     * Get the number of defense blocks
     * @return Defense block count
     */
    public int getDefenseBlockCount() {
        return defenseBlocks.size();
    }
    
    /**
     * Get the number of clan doors
     * @return Clan door count
     */
    public int getClanDoorCount() {
        return clanDoors.size();
    }
}
