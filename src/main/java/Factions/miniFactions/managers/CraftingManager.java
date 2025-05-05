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

public class CraftingManager {

    private final MiniFactions plugin;
    private final List<NamespacedKey> registeredRecipes = new ArrayList<>();

    // Materials for special blocks
    private static final Material DEFENSE_BLOCK_MATERIAL = Material.OBSIDIAN;
    private static final Material CLAIM_BLOCK_MATERIAL = Material.EMERALD_BLOCK;
    private static final Material EXPLOSIVE_MATERIAL = Material.STONE_BUTTON; // Changed from TNT to button
    private static final Material CLAN_DOOR_MATERIAL = Material.IRON_DOOR;
    private static final Material CORE_BLOCK_MATERIAL = Material.BEACON;

    public CraftingManager(MiniFactions plugin) {
        this.plugin = plugin;
    }

    /**
     * Register all custom recipes
     */
    public void registerRecipes() {
        // Core blocks are no longer craftable, they are given to clan leaders automatically
        registerDefenseBlockRecipes();
        registerClaimBlockRecipes();
        registerExplosiveRecipes();
        registerClanDoorRecipes();
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
        int maxTier = config.getInt("defense.max-tier", 10);

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
     */
    private void registerClaimBlockRecipes() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        int maxLevel = config.getInt("claim.max-upgrade-level", 5);

        for (int level = 1; level <= maxLevel; level++) {
            ItemStack claimBlock = createClaimBlock(level);

            NamespacedKey key = new NamespacedKey(plugin, "claim_block_level_" + level);
            ShapedRecipe recipe = new ShapedRecipe(key, claimBlock);

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
            } else {
                // Try old recipe format
                List<String> ingredients = config.getStringList("claim.levels." + level + ".recipe");
                if (!ingredients.isEmpty()) {
                    // Use old recipe format
                    recipe.shape("EEE", "ERE", "EEE");

                    // Use first ingredient for 'E' and second for 'R'
                    if (ingredients.size() >= 1) {
                        String[] parts = ingredients.get(0).split(":");
                        Material mat = Material.getMaterial(parts[0]);
                        if (mat != null) {
                            recipe.setIngredient('E', mat);
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
                    plugin.getLogger().warning("Invalid recipe pattern for claim level " + level);
                }
            }
        }
    }

    /**
     * Register explosive recipes
     */
    private void registerExplosiveRecipes() {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        ConfigurationSection explosives = config.getConfigurationSection("raiding.explosives");

        if (explosives == null) {
            return;
        }

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
                        recipe.setIngredient('T', Material.TNT);
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
                        recipe.shape("GTG", "TRT", "GTG");

                        // Use first ingredient for 'G' and second for 'R'
                        if (ingredients.size() >= 1) {
                            String[] parts = ingredients.get(0).split(":");
                            Material mat = Material.getMaterial(parts[0]);
                            if (mat != null) {
                                recipe.setIngredient('G', mat);
                                recipe.setIngredient('T', Material.TNT);
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

        // Recipe pattern
        recipe.shape("II ", "IRI", "II ");

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
        ItemStack item = new ItemStack(DEFENSE_BLOCK_MATERIAL);
        ItemMeta meta = item.getItemMeta();

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
     * Create a claim block item
     * @param level Block level
     * @return ItemStack
     */
    public ItemStack createClaimBlock(int level) {
        ItemStack item = new ItemStack(CLAIM_BLOCK_MATERIAL);
        ItemMeta meta = item.getItemMeta();

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

        meta.setDisplayName(ChatColor.GOLD + "Clan Door");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "A special door that can only be opened by clan members.");
        lore.add(ChatColor.GRAY + "Place within your clan's area of influence.");
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
        for (NamespacedKey key : registeredRecipes) {
            Bukkit.removeRecipe(key);
        }
        registeredRecipes.clear();
    }
}
