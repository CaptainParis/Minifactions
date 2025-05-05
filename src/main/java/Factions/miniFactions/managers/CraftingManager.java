package Factions.miniFactions.managers;

import Factions.miniFactions.MiniFactions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class CraftingManager {

    private final MiniFactions plugin;
    private final List<NamespacedKey> registeredRecipes = new ArrayList<>();

    // Materials for special blocks
    // Materials for special blocks - using terracotta colors for defense blocks to visually indicate tier
    private static final Material DEFENSE_BLOCK_MATERIAL = Material.RED_TERRACOTTA; // Default tier 1 material
    private static final Material CLAIM_BLOCK_MATERIAL = Material.EMERALD_BLOCK;
    private static final Material EXPLOSIVE_MATERIAL = Material.STONE_BUTTON; // Changed from TNT to button
    private static final Material CLAN_DOOR_MATERIAL = Material.IRON_TRAPDOOR;
    private static final Material CORE_BLOCK_MATERIAL = Material.BEACON;

    public CraftingManager(MiniFactions plugin) {
        this.plugin = plugin;
    }

    /**
     * Register all custom recipes
     */
    public void registerRecipes() {
        // Core blocks are no longer craftable, they are given to clan leaders automatically
        try {
            registerDefenseBlockRecipes();
            registerClaimBlockRecipes();
            registerExplosiveRecipes();
            registerClanDoorRecipes();
            plugin.getLogger().info("Successfully registered all custom recipes");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to register recipes: " + e.getMessage(), e);
        }
    }

    /**
     * Register core block recipes - No longer used as core blocks are given to clan leaders automatically
     * Kept for reference only
     */
    /*
    private void registerCoreBlockRecipes() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        int maxLevel = config.getInt("core.max-level", 20);

        for (int level = 1; level <= maxLevel; level++) {
            ItemStack coreBlock = createCoreBlock(level);

            NamespacedKey key = new NamespacedKey(plugin, "core_block_level_" + level);
            ShapedRecipe recipe = new ShapedRecipe(key, coreBlock);

            // Get recipe pattern from config
            List<String> patternList = config.getStringList("core.levels." + level + ".recipe-pattern");
            if (patternList.size() >= 3) {
                recipe.shape(patternList.get(0), patternList.get(1), patternList.get(2));

                // Get recipe key mappings
                ConfigurationSection keySection = config.getConfigurationSection("core.levels." + level + ".recipe-key");
                if (keySection != null) {
                    for (String keyChar : keySection.getKeys(false)) {
                        if (keyChar.length() == 1) {
                            String materialName = keySection.getString(keyChar);
                            Material material = Material.getMaterial(materialName);
                            if (material != null) {
                                recipe.setIngredient(keyChar.charAt(0), material);
                            } else {
                                plugin.getLogger().warning("Invalid material in core recipe: " + materialName);
                            }
                        }
                    }
                } else {
                    // Fallback to default recipe
                    recipe.setIngredient('D', Material.DIAMOND);
                    recipe.setIngredient('O', Material.OBSIDIAN);
                    recipe.setIngredient('B', Material.BEACON);
                }

                // Register recipe
                Bukkit.addRecipe(recipe);
                registeredRecipes.add(key);
            } else {
                plugin.getLogger().warning("Invalid recipe pattern for core level " + level);
            }
        }
    }
    */

    /**
     * Register defense block recipes
     */
    private void registerDefenseBlockRecipes() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        // Get max tier from config, default to 5 if not specified
        int maxTier = config.getInt("defense.max-tier", 5);

        for (int tier = 1; tier <= maxTier; tier++) {
            ItemStack defenseBlock = createDefenseBlock(tier);

            NamespacedKey key = new NamespacedKey(plugin, "defense_block_tier_" + tier);
            ShapedRecipe recipe = new ShapedRecipe(key, defenseBlock);

            // Get recipe pattern from config
            List<String> patternList = config.getStringList("defense.tiers." + tier + ".recipe-pattern");
            if (patternList.size() >= 3) {
                recipe.shape(patternList.get(0), patternList.get(1), patternList.get(2));

                // Get recipe key mappings
                ConfigurationSection keySection = config.getConfigurationSection("defense.tiers." + tier + ".recipe-key");
                if (keySection != null) {
                    for (String keyChar : keySection.getKeys(false)) {
                        if (keyChar.length() == 1) {
                            String materialName = keySection.getString(keyChar);
                            Material material = Material.getMaterial(materialName);
                            if (material != null) {
                                recipe.setIngredient(keyChar.charAt(0), material);
                            } else {
                                plugin.getLogger().warning("Invalid material in defense recipe: " + materialName);
                            }
                        }
                    }
                } else {
                    // Fallback to default recipe if not defined
                    recipe.setIngredient('O', Material.OBSIDIAN);

                    if (tier <= 3) {
                        recipe.setIngredient('I', Material.IRON_BLOCK);
                    } else if (tier <= 6) {
                        recipe.setIngredient('G', Material.GOLD_BLOCK);
                    } else {
                        recipe.setIngredient('D', Material.DIAMOND_BLOCK);
                    }
                }

                // Register recipe
                Bukkit.addRecipe(recipe);
                registeredRecipes.add(key);
            } else {
                // Try old recipe format
                List<String> ingredients = config.getStringList("defense.tiers." + tier + ".recipe");
                if (!ingredients.isEmpty()) {
                    // Use old recipe format
                    recipe.shape("OOO", "ODO", "OOO");

                    // Use first ingredient for 'O' and second for 'D'
                    if (ingredients.size() >= 1) {
                        String[] parts = ingredients.get(0).split(":");
                        Material mat = Material.getMaterial(parts[0]);
                        if (mat != null) {
                            recipe.setIngredient('O', mat);
                        }
                    }

                    if (ingredients.size() >= 2) {
                        String[] parts = ingredients.get(1).split(":");
                        Material mat = Material.getMaterial(parts[0]);
                        if (mat != null) {
                            recipe.setIngredient('D', mat);
                        }
                    }

                    // Register recipe
                    Bukkit.addRecipe(recipe);
                    registeredRecipes.add(key);
                } else {
                    plugin.getLogger().warning("Invalid recipe pattern for defense tier " + tier);
                }
            }
        }
    }

    /**
     * Register claim block recipes
     * Only level 1 claim blocks are craftable, higher levels use GUI upgrade
     */
    private void registerClaimBlockRecipes() {
        FileConfiguration config = plugin.getConfigManager().getConfig();

        // Only register level 1 claim block recipe
        int level = 1;
        ItemStack claimBlock = createClaimBlock(level);

        NamespacedKey key = new NamespacedKey(plugin, "claim_block_level_" + level);
        ShapedRecipe recipe = new ShapedRecipe(key, claimBlock);

        plugin.getLogger().info("Registering claim block recipe for level " + level);

        // Get recipe pattern from config
        List<String> patternList = config.getStringList("claim.levels." + level + ".recipe-pattern");
        if (patternList.size() >= 3) {
            recipe.shape(patternList.get(0), patternList.get(1), patternList.get(2));

            // Get recipe key mappings
            ConfigurationSection keySection = config.getConfigurationSection("claim.levels." + level + ".recipe-key");
            if (keySection != null) {
                for (String keyChar : keySection.getKeys(false)) {
                    if (keyChar.length() == 1) {
                        String materialName = keySection.getString(keyChar);
                        Material material = Material.getMaterial(materialName);
                        if (material != null) {
                            recipe.setIngredient(keyChar.charAt(0), material);
                        } else {
                            plugin.getLogger().warning("Invalid material in claim recipe: " + materialName);
                        }
                    }
                }
            } else {
                // Fallback to default recipe
                recipe.setIngredient('E', Material.EMERALD);
                recipe.setIngredient('R', Material.REDSTONE_BLOCK);
            }

            // Register recipe
            Bukkit.addRecipe(recipe);
            registeredRecipes.add(key);
            plugin.getLogger().info("Registered level 1 claim block recipe");
        } else {
            plugin.getLogger().warning("Invalid recipe pattern for claim level 1");
        }
    }

    /**
     * Register explosive recipes
     */
    private void registerExplosiveRecipes() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        ConfigurationSection explosives = config.getConfigurationSection("raiding.explosives");

        if (explosives == null) {
            plugin.getLogger().warning("No explosives configuration found in config.yml");
            return;
        }

        plugin.getLogger().info("Registering explosive recipes");

        for (String tierKey : explosives.getKeys(false)) {
            try {
                int tier = Integer.parseInt(tierKey);
                ItemStack explosive = createExplosive(tier);

                NamespacedKey key = new NamespacedKey(plugin, "explosive_tier_" + tier);
                ShapedRecipe recipe = new ShapedRecipe(key, explosive);

                // Get recipe pattern from config
                List<String> patternList = config.getStringList("raiding.explosives." + tier + ".recipe-pattern");
                if (patternList.size() >= 3) {
                    recipe.shape(patternList.get(0), patternList.get(1), patternList.get(2));

                    // Get recipe key mappings
                    ConfigurationSection keySection = config.getConfigurationSection("raiding.explosives." + tier + ".recipe-key");
                    if (keySection != null) {
                        for (String keyChar : keySection.getKeys(false)) {
                            if (keyChar.length() == 1) {
                                String materialName = keySection.getString(keyChar);
                                Material material = Material.getMaterial(materialName);
                                if (material != null) {
                                    recipe.setIngredient(keyChar.charAt(0), material);
                                } else {
                                    plugin.getLogger().warning("Invalid material in explosive recipe: " + materialName);
                                }
                            }
                        }
                    } else {
                        // Fallback to default recipe
                        recipe.setIngredient('G', Material.GUNPOWDER);
                        recipe.setIngredient('B', Material.STONE_BUTTON);
                        recipe.setIngredient('R', Material.REDSTONE_BLOCK);
                    }

                    // Register recipe
                    Bukkit.addRecipe(recipe);
                    registeredRecipes.add(key);
                } else {
                    // Try old recipe format
                    List<String> ingredients = config.getStringList("raiding.explosives." + tier + ".recipe");
                    if (!ingredients.isEmpty()) {
                        // Use old recipe format
                        recipe.shape("GBG", "BRB", "GBG");

                        // Use first ingredient for 'G' and second for 'R'
                        if (ingredients.size() >= 1) {
                            String[] parts = ingredients.get(0).split(":");
                            Material mat = Material.getMaterial(parts[0]);
                            if (mat != null) {
                                recipe.setIngredient('G', mat);
                                recipe.setIngredient('B', Material.STONE_BUTTON);
                            }
                        }

                        if (ingredients.size() >= 2) {
                            String[] parts = ingredients.get(1).split(":");
                            Material mat = Material.getMaterial(parts[0]);
                            if (mat != null) {
                                recipe.setIngredient('R', mat);
                            }
                        }

                        // Register recipe
                        Bukkit.addRecipe(recipe);
                        registeredRecipes.add(key);
                    } else {
                        plugin.getLogger().warning("Invalid recipe pattern for explosive tier " + tier);
                    }
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid tier number in explosives config: " + tierKey);
            }
        }
    }

    /**
     * Register clan door recipes
     */
    private void registerClanDoorRecipes() {
        ItemStack clanDoor = createClanDoor();

        NamespacedKey key = new NamespacedKey(plugin, "clan_door");
        ShapedRecipe recipe = new ShapedRecipe(key, clanDoor);

        // Recipe pattern for trapdoor
        recipe.shape("III", "IRI", "   ");

        // Set ingredients
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('R', Material.REDSTONE);

        // Register recipe
        Bukkit.addRecipe(recipe);
        registeredRecipes.add(key);
    }

    /**
     * Create a defense block item
     * @param tier Block tier
     * @return ItemStack
     */
    public ItemStack createDefenseBlock(int tier) {
        // Get the appropriate material for this tier
        Material material = CraftingManager.getTierMaterial(tier);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            plugin.getLogger().warning("Failed to get ItemMeta for defense block tier " + tier);
            return item;
        }

        FileConfiguration config = plugin.getConfigManager().getConfig();
        int cost = config.getInt("defense.tiers." + tier + ".cost", 100 * (int) Math.pow(2, tier - 1));
        int decayTime = config.getInt("defense.tiers." + tier + ".decay-time", 24);

        meta.setDisplayName(ChatColor.AQUA + "Defense Block (Tier " + tier + ")");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "A special block that can only be broken by explosives.");
        lore.add(ChatColor.GRAY + "Tier: " + tier);
        lore.add(ChatColor.GRAY + "Cost: " + cost + " points");
        lore.add(ChatColor.GRAY + "Decay time: " + decayTime + " hours");
        lore.add(ChatColor.GRAY + "Requires a Tier " + tier + " or higher explosive to damage.");
        meta.setLore(lore);

        // Store tier in item
        meta.setCustomModelData(tier);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Get the material to use for a specific defense block tier
     * @param tier The tier level
     * @return The material to use
     */
    public static Material getTierMaterial(int tier) {
        switch (tier) {
            case 5:
                return Material.BLACK_TERRACOTTA; // Highest tier
            case 4:
                return Material.GRAY_TERRACOTTA;
            case 3:
                return Material.BROWN_TERRACOTTA;
            case 2:
                return Material.LIGHT_GRAY_TERRACOTTA;
            case 1:
            default:
                return Material.WHITE_TERRACOTTA; // Lowest tier
        }
    }

    /**
     * Create a claim block item
     * @param level Block level
     * @return ItemStack
     */
    public ItemStack createClaimBlock(int level) {
        ItemStack item = new ItemStack(CLAIM_BLOCK_MATERIAL);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            plugin.getLogger().warning("Failed to get ItemMeta for claim block level " + level);
            return item;
        }

        FileConfiguration config = plugin.getConfigManager().getConfig();
        int pointsPerDay = config.getInt("claim.levels." + level + ".points-per-day", 100);

        meta.setDisplayName(ChatColor.GREEN + "Claim Block (Level " + level + ")");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "A special block that generates points over time.");
        lore.add(ChatColor.GRAY + "Level: " + level);
        lore.add(ChatColor.GRAY + "Points per day: " + pointsPerDay);
        lore.add(ChatColor.GRAY + "Place within your clan's area of influence.");
        meta.setLore(lore);

        // Store level in item
        meta.setCustomModelData(level);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create an explosive item
     * @param tier Explosive tier
     * @return ItemStack
     */
    public ItemStack createExplosive(int tier) {
        ItemStack item = new ItemStack(EXPLOSIVE_MATERIAL);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            plugin.getLogger().warning("Failed to get ItemMeta for explosive tier " + tier);
            return item;
        }

        FileConfiguration config = plugin.getConfigManager().getConfig();
        int fuseTime = config.getInt("raiding.explosives." + tier + ".fuse-time", 5);

        meta.setDisplayName(ChatColor.RED + "Explosive (Tier " + tier + ")");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "A special explosive that can damage defense blocks.");
        lore.add(ChatColor.GRAY + "Tier: " + tier);
        lore.add(ChatColor.GRAY + "Fuse time: " + fuseTime + " seconds");
        lore.add(ChatColor.GRAY + "Can damage Tier " + tier + " or lower defense blocks.");
        meta.setLore(lore);

        // Store tier in item
        meta.setCustomModelData(tier);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create a core block item
     * @param level Block level
     * @return ItemStack
     */
    public ItemStack createCoreBlock(int level) {
        ItemStack item = new ItemStack(CORE_BLOCK_MATERIAL);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            plugin.getLogger().warning("Failed to get ItemMeta for core block level " + level);
            return item;
        }

        FileConfiguration config = plugin.getConfigManager().getConfig();
        int area = config.getInt("core.levels." + level + ".area", 10);
        int defenseSlots = config.getInt("core.levels." + level + ".defense-slots", 5);
        int claimSlots = config.getInt("core.levels." + level + ".claim-slots", 2);
        int doorSlots = config.getInt("core.levels." + level + ".door-slots", 1);
        int memberSlots = config.getInt("core.levels." + level + ".member-slots", 10);

        meta.setDisplayName(ChatColor.GOLD + "Core Block (Level " + level + ")");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "The central element of your clan's territory.");
        lore.add(ChatColor.GRAY + "Level: " + level);
        lore.add(ChatColor.GRAY + "Area: " + area + " blocks radius");
        lore.add(ChatColor.GRAY + "Defense Slots: " + defenseSlots);
        lore.add(ChatColor.GRAY + "Claim Slots: " + claimSlots);
        lore.add(ChatColor.GRAY + "Door Slots: " + doorSlots);
        lore.add(ChatColor.GRAY + "Member Slots: " + memberSlots);
        meta.setLore(lore);

        // Store level in item
        meta.setCustomModelData(level);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create a clan door item
     * @return ItemStack
     */
    public ItemStack createClanDoor() {
        ItemStack item = new ItemStack(CLAN_DOOR_MATERIAL);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            plugin.getLogger().warning("Failed to get ItemMeta for clan door");
            return item;
        }

        meta.setDisplayName(ChatColor.GOLD + "Clan Trapdoor");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "A special trapdoor that can only be opened by clan members.");
        lore.add(ChatColor.GRAY + "Place within your clan's area of influence.");
        lore.add(ChatColor.GRAY + "Right-click to open or close.");
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Get the defense block material
     * @return Defense block material
     */
    public static Material getDefenseBlockMaterial() {
        return DEFENSE_BLOCK_MATERIAL;
    }

    /**
     * Get the claim block material
     * @return Claim block material
     */
    public static Material getClaimBlockMaterial() {
        return CLAIM_BLOCK_MATERIAL;
    }

    /**
     * Get the explosive material
     * @return Explosive material
     */
    public static Material getExplosiveMaterial() {
        return EXPLOSIVE_MATERIAL;
    }

    /**
     * Get the clan door material
     * @return Clan door material
     */
    public static Material getClanDoorMaterial() {
        return CLAN_DOOR_MATERIAL;
    }

    /**
     * Unregister all recipes
     */
    public void unregisterRecipes() {
        try {
            for (NamespacedKey key : registeredRecipes) {
                Bukkit.removeRecipe(key);
            }
            registeredRecipes.clear();
            plugin.getLogger().info("Successfully unregistered all custom recipes");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to unregister recipes: " + e.getMessage(), e);
        }
    }
}
