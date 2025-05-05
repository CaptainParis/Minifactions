package Factions.miniFactions.managers;

import Factions.miniFactions.MiniFactions;
import Factions.miniFactions.models.Clan;
import Factions.miniFactions.models.CoreBlock;
import Factions.miniFactions.utils.TextAnimationUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages visual effects for core blocks including text displays and particles
 */
public class CoreBlockVisualManager {

    private final MiniFactions plugin;
    private final Map<Location, UUID> textDisplays = new ConcurrentHashMap<>();
    private BukkitTask particleTask;

    /**
     * Create a new core block visual manager
     * @param plugin MiniFactions plugin
     */
    public CoreBlockVisualManager(MiniFactions plugin) {
        this.plugin = plugin;
        startParticleTask();
    }

    /**
     * Create or update a text display for a core block
     * @param coreBlock Core block to create display for
     */
    public void createOrUpdateTextDisplay(CoreBlock coreBlock) {
        if (coreBlock == null || coreBlock.getLocation() == null) {
            return;
        }

        Location blockLoc = coreBlock.getLocation();
        Clan clan = coreBlock.getClan();

        // Remove existing text display if any
        removeTextDisplay(blockLoc);

        // Position the text display 1.5 blocks above the core block
        Location displayLoc = blockLoc.clone().add(0.5, 1.5, 0.5);

        // Prepare the text (clan name and level)
        String displayText = ChatColor.GOLD + clan.getName() + "\n" +
                             ChatColor.YELLOW + "Level " + coreBlock.getLevel();

        // Create animated text display with typing effect
        UUID displayId = TextAnimationUtil.createTypedTextDisplay(
            plugin,
            displayLoc,
            displayText,
            2L, // 2 ticks between characters for typing speed
            textDisplay -> {
                // This runs when the animation is complete
                plugin.getLogger().info("Completed text animation for clan " + clan.getName() + " at " + blockLoc);
            }
        );

        // Store the text display entity ID
        textDisplays.put(blockLoc, displayId);

        plugin.getLogger().info("Created text display for clan " + clan.getName() + " at " + blockLoc);
    }

    /**
     * Remove a text display for a core block
     * @param location Core block location
     */
    public void removeTextDisplay(Location location) {
        UUID displayId = textDisplays.remove(location);
        if (displayId != null) {
            // Find and remove the entity
            World world = location.getWorld();
            world.getEntities().stream()
                    .filter(entity -> entity.getUniqueId().equals(displayId) && entity instanceof TextDisplay)
                    .forEach(entity -> entity.remove());

            plugin.getLogger().info("Removed text display at " + location);
        }
    }

    /**
     * Start the particle task for all core blocks
     */
    private void startParticleTask() {
        // Run task every 5 ticks (0.25 seconds) for smooth particle effects
        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                // Get all core blocks
                Map<Location, CoreBlock> coreBlocks = plugin.getDataStorage().getCoreBlocks();
                if (coreBlocks == null || coreBlocks.isEmpty()) {
                    return;
                }

                // Show particles for each core block
                for (CoreBlock coreBlock : coreBlocks.values()) {
                    if (coreBlock == null || coreBlock.getLocation() == null) {
                        continue;
                    }

                    // Get the block location
                    Location blockLoc = coreBlock.getLocation();

                    // Create a location for particles (centered on the block)
                    Location particleLoc = blockLoc.clone().add(0.5, 1.0, 0.5);

                    // Spawn skulk soul particles
                    blockLoc.getWorld().spawnParticle(
                            Particle.SCULK_SOUL,
                            particleLoc,
                            2,  // Amount of particles
                            0.3, 0.3, 0.3,  // Spread (x, y, z)
                            0.02  // Speed
                    );

                    // Also create a small spiral effect
                    double angle = (System.currentTimeMillis() % 2000) / 2000.0 * Math.PI * 2;
                    double radius = 0.7;
                    double x = Math.sin(angle) * radius;
                    double z = Math.cos(angle) * radius;

                    Location spiralLoc = blockLoc.clone().add(0.5 + x, 0.5, 0.5 + z);
                    blockLoc.getWorld().spawnParticle(
                            Particle.SCULK_SOUL,
                            spiralLoc,
                            1,  // Just one particle per position
                            0.05, 0.05, 0.05,  // Small spread
                            0.01  // Speed
                    );
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error in core block particle task: " + e.getMessage());
                e.printStackTrace();
            }
        }, 5L, 5L);
    }

    /**
     * Create or update text displays for all core blocks
     */
    public void updateAllTextDisplays() {
        Map<Location, CoreBlock> coreBlocks = plugin.getDataStorage().getCoreBlocks();
        if (coreBlocks == null || coreBlocks.isEmpty()) {
            return;
        }

        for (CoreBlock coreBlock : coreBlocks.values()) {
            createOrUpdateTextDisplay(coreBlock);
        }
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        // Cancel particle task
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }

        // Remove all text displays
        for (Location location : textDisplays.keySet()) {
            removeTextDisplay(location);
        }

        textDisplays.clear();
    }
}
