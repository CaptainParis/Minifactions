package Factions.miniFactions.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Utility class for text animation effects
 */
public class TextAnimationUtil {

    /**
     * Creates a text display with a typed-in animation effect
     * 
     * @param plugin The plugin instance
     * @param location The location to place the text display
     * @param fullText The complete text to display
     * @param typingSpeed The delay between characters in ticks (20 ticks = 1 second)
     * @param onComplete Consumer to receive the completed TextDisplay entity
     * @return UUID of the created text display entity
     */
    public static UUID createTypedTextDisplay(Plugin plugin, Location location, String fullText, 
                                             long typingSpeed, Consumer<TextDisplay> onComplete) {
        // Create the text display entity
        World world = location.getWorld();
        TextDisplay textDisplay = (TextDisplay) world.spawnEntity(location, EntityType.TEXT_DISPLAY);
        
        // Apply bold and shadow formatting
        String formattedText = ChatColor.BOLD + "";
        
        // Configure the text display
        textDisplay.setBillboard(Display.Billboard.CENTER); // Always face the player
        textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER); // Center-align the text
        textDisplay.setBackgroundColor(null); // Transparent background
        textDisplay.setSeeThrough(true); // Text can be seen through blocks
        textDisplay.setShadowed(true); // Add shadow to text for better visibility
        
        // Set the transformation (scale to make it more visible)
        textDisplay.setTransformation(new Transformation(
                new org.joml.Vector3f(0, 0, 0), // Translation
                new org.joml.AxisAngle4f(0, 0, 0, 0), // Left rotation
                new org.joml.Vector3f(1.0f, 1.0f, 1.0f), // Scale
                new org.joml.AxisAngle4f(0, 0, 0, 0)  // Right rotation
        ));
        
        // Make it persistent so it doesn't despawn
        textDisplay.setPersistent(true);
        
        // Start with empty text
        textDisplay.setText("");
        
        // Start the typing animation
        animateTypedText(plugin, textDisplay, fullText, 0, typingSpeed, onComplete);
        
        return textDisplay.getUniqueId();
    }
    
    /**
     * Recursive method to animate the typed text effect
     * 
     * @param plugin The plugin instance
     * @param textDisplay The text display entity
     * @param fullText The complete text to display
     * @param currentIndex Current character index
     * @param typingSpeed The delay between characters in ticks
     * @param onComplete Consumer to receive the completed TextDisplay entity
     */
    private static void animateTypedText(Plugin plugin, TextDisplay textDisplay, String fullText, 
                                        int currentIndex, long typingSpeed, Consumer<TextDisplay> onComplete) {
        // If we've reached the end of the text, call the completion handler
        if (currentIndex >= fullText.length()) {
            if (onComplete != null) {
                onComplete.accept(textDisplay);
            }
            return;
        }
        
        // Get the substring up to the current index
        String currentText = fullText.substring(0, currentIndex + 1);
        
        // Apply bold formatting
        currentText = ChatColor.BOLD + currentText;
        
        // Update the text display
        textDisplay.setText(currentText);
        
        // Schedule the next character
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            animateTypedText(plugin, textDisplay, fullText, currentIndex + 1, typingSpeed, onComplete);
        }, typingSpeed);
    }
}
