package Factions.miniFactions.models;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;

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
     * @throws IllegalArgumentException if any parameter is null or invalid
     */
    public Clan(String id, String name, UUID leader) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Clan ID cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Clan name cannot be null or empty");
        }
        if (leader == null) {
            throw new IllegalArgumentException("Clan leader cannot be null");
        }

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
     * @throws IllegalArgumentException if name is null or empty
     */
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Clan name cannot be null or empty");
        }
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
     * @return Unmodifiable map of member UUID to role
     */
    public Map<UUID, String> getMembers() {
        return Collections.unmodifiableMap(members);
    }

    /**
     * Add a member to the clan
     * @param playerUUID Player UUID
     * @param role Member role
     * @throws IllegalArgumentException if playerUUID is null or role is invalid
     */
    public void addMember(UUID playerUUID, String role) {
        if (playerUUID == null) {
            throw new IllegalArgumentException("Player UUID cannot be null");
        }
        if (role == null || (!role.equals(ROLE_LEADER) && !role.equals(ROLE_CO_LEADER) && !role.equals(ROLE_MEMBER))) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        members.put(playerUUID, role);

        // Log the addition for debugging
        Bukkit.getLogger().info("Added player " + playerUUID + " to clan " + name + " with role " + role);
    }

    /**
     * Remove a member from the clan
     * @param playerUUID Player UUID
     * @return true if the member was removed, false if they weren't in the clan
     * @throws IllegalArgumentException if playerUUID is null
     */
    public boolean removeMember(UUID playerUUID) {
        if (playerUUID == null) {
            throw new IllegalArgumentException("Player UUID cannot be null");
        }

        // Prevent removing the leader
        if (isLeader(playerUUID)) {
            throw new IllegalArgumentException("Cannot remove the clan leader");
        }

        boolean removed = members.remove(playerUUID) != null;
        if (removed) {
            // Log the removal for debugging
            Bukkit.getLogger().info("Removed player " + playerUUID + " from clan " + name);
        }
        return removed;
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
     * @return true if the role was changed, false if the player is not a member
     * @throws IllegalArgumentException if playerUUID is null, role is invalid, or trying to change the leader's role
     */
    public boolean setMemberRole(UUID playerUUID, String role) {
        if (playerUUID == null) {
            throw new IllegalArgumentException("Player UUID cannot be null");
        }
        if (role == null || (!role.equals(ROLE_LEADER) && !role.equals(ROLE_CO_LEADER) && !role.equals(ROLE_MEMBER))) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }

        // Prevent changing the leader's role
        if (isLeader(playerUUID) && !role.equals(ROLE_LEADER)) {
            throw new IllegalArgumentException("Cannot change the leader's role");
        }

        // Prevent setting another player as leader
        if (role.equals(ROLE_LEADER) && !isLeader(playerUUID)) {
            throw new IllegalArgumentException("Cannot set another player as leader");
        }

        if (members.containsKey(playerUUID)) {
            members.put(playerUUID, role);
            return true;
        }
        return false;
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
     * @throws IllegalArgumentException if points is negative
     */
    public void setPoints(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("Points cannot be negative");
        }
        this.points = points;
    }

    /**
     * Add points to the clan
     * @param amount Amount to add
     * @throws IllegalArgumentException if amount is negative
     */
    public void addPoints(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot add negative points");
        }
        this.points += amount;
    }

    /**
     * Remove points from the clan
     * @param amount Amount to remove
     * @return true if successful, false if not enough points
     * @throws IllegalArgumentException if amount is negative
     */
    public boolean removePoints(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Cannot remove negative points");
        }
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
     * @param coreBlock CoreBlock (can be null to remove the core block)
     */
    public void setCoreBlock(CoreBlock coreBlock) {
        this.coreBlock = coreBlock;
    }

    /**
     * Get all claim blocks
     * @return Unmodifiable set of ClaimBlock
     */
    public Set<ClaimBlock> getClaimBlocks() {
        return Collections.unmodifiableSet(claimBlocks);
    }

    /**
     * Add a claim block
     * @param claimBlock ClaimBlock to add
     * @throws IllegalArgumentException if claimBlock is null
     */
    public void addClaimBlock(ClaimBlock claimBlock) {
        if (claimBlock == null) {
            throw new IllegalArgumentException("Claim block cannot be null");
        }
        claimBlocks.add(claimBlock);
    }

    /**
     * Remove a claim block
     * @param claimBlock ClaimBlock to remove
     * @return true if the claim block was removed, false if it wasn't in the clan
     * @throws IllegalArgumentException if claimBlock is null
     */
    public boolean removeClaimBlock(ClaimBlock claimBlock) {
        if (claimBlock == null) {
            throw new IllegalArgumentException("Claim block cannot be null");
        }
        return claimBlocks.remove(claimBlock);
    }

    /**
     * Get all defense blocks
     * @return Unmodifiable set of DefenseBlock
     */
    public Set<DefenseBlock> getDefenseBlocks() {
        return Collections.unmodifiableSet(defenseBlocks);
    }

    /**
     * Add a defense block
     * @param defenseBlock DefenseBlock to add
     * @throws IllegalArgumentException if defenseBlock is null
     */
    public void addDefenseBlock(DefenseBlock defenseBlock) {
        if (defenseBlock == null) {
            throw new IllegalArgumentException("Defense block cannot be null");
        }
        defenseBlocks.add(defenseBlock);
    }

    /**
     * Remove a defense block
     * @param defenseBlock DefenseBlock to remove
     * @return true if the defense block was removed, false if it wasn't in the clan
     * @throws IllegalArgumentException if defenseBlock is null
     */
    public boolean removeDefenseBlock(DefenseBlock defenseBlock) {
        if (defenseBlock == null) {
            throw new IllegalArgumentException("Defense block cannot be null");
        }
        return defenseBlocks.remove(defenseBlock);
    }

    /**
     * Get all clan doors
     * @return Unmodifiable set of ClanDoor
     */
    public Set<ClanDoor> getClanDoors() {
        return Collections.unmodifiableSet(clanDoors);
    }

    /**
     * Add a clan door
     * @param clanDoor ClanDoor to add
     * @throws IllegalArgumentException if clanDoor is null
     */
    public void addClanDoor(ClanDoor clanDoor) {
        if (clanDoor == null) {
            throw new IllegalArgumentException("Clan door cannot be null");
        }
        clanDoors.add(clanDoor);
    }

    /**
     * Remove a clan door
     * @param clanDoor ClanDoor to remove
     * @return true if the clan door was removed, false if it wasn't in the clan
     * @throws IllegalArgumentException if clanDoor is null
     */
    public boolean removeClanDoor(ClanDoor clanDoor) {
        if (clanDoor == null) {
            throw new IllegalArgumentException("Clan door cannot be null");
        }
        return clanDoors.remove(clanDoor);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Clan clan = (Clan) o;
        return Objects.equals(id, clan.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Clan{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", leader=" + leader +
                ", members=" + members.size() +
                ", points=" + points +
                ", coreBlock=" + (coreBlock != null) +
                ", claimBlocks=" + claimBlocks.size() +
                ", defenseBlocks=" + defenseBlocks.size() +
                ", clanDoors=" + clanDoors.size() +
                '}';
    }
}