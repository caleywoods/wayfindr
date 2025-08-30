package net.dfnkt.wayfindr

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d

/**
 * Handles registration and execution of Wayfindr mod commands.
 * 
 * This class provides in-game commands for waypoint management, including:
 * - Adding waypoints at crosshair target location
 * - Adding waypoints at player's current position
 * - Deleting waypoints by name
 */
object WayfindrCommands {
    /**
     * Registers all Wayfindr commands with the Minecraft command system.
     * 
     * Command structure:
     * - /waypoint add <n> [color] - Add waypoint at crosshair target
     * - /waypoint addhere <n> [color] - Add waypoint at player position
     * - /waypoint delete <n> - Delete waypoint by name
     */
    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                CommandManager.literal("waypoint")
                    .then(
                        CommandManager.literal("add")
                            .then(
                                CommandManager.argument("name", StringArgumentType.string())
                                    .executes { context ->
                                        handleAddWaypoint(context, null, false)
                                    }
                                    .then(
                                        CommandManager.argument("color", StringArgumentType.word())
                                            .executes { context ->
                                                val colorArg = StringArgumentType.getString(context, "color")
                                                val color = parseColor(colorArg)
                                                handleAddWaypoint(context, color, false)
                                            }
                                    )
                            )
                    )
                    .then(
                        CommandManager.literal("addhere")
                            .then(
                                CommandManager.argument("name", StringArgumentType.string())
                                    .executes { context ->
                                        handleAddWaypoint(context, null, true)
                                    }
                                    .then(
                                        CommandManager.argument("color", StringArgumentType.word())
                                            .executes { context ->
                                                val colorArg = StringArgumentType.getString(context, "color")
                                                val color = parseColor(colorArg)
                                                handleAddWaypoint(context, color, true)
                                            }
                                    )
                            )
                    )
                    .then(
                        CommandManager.literal("delete")
                            .then(
                                CommandManager.argument("name", StringArgumentType.string())
                                    .executes { context ->
                                        val name = StringArgumentType.getString(context, "name")
                                        
                                        // Find waypoint by name first, then remove by UUID
                                        val waypoint = WaypointManager.getWaypointByName(name)
                                        if (waypoint != null) {
                                            WaypointManager.removeWaypoint(waypoint.id)
                                            context.source.sendFeedback(
                                                { Text.literal("Removed waypoint '$name'") },
                                                false
                                            )
                                        } else {
                                            context.source.sendFeedback(
                                                { Text.literal("No waypoint found with name '$name'") },
                                                false
                                            )
                                        }
                                        return@executes 1
                                    }
                            )
                    )
            )
        }
    }

    /**
     * Handles the logic for adding a waypoint via command.
     * 
     * @param context The command context containing source and arguments
     * @param colorInt The color for the waypoint, or null to use default
     * @param usePlayerPosition If true, use player's position; otherwise use crosshair target
     * @return Command success value (1 for success, 0 for failure)
     */
    private fun handleAddWaypoint(context: CommandContext<ServerCommandSource>, colorInt: Int?, usePlayerPosition: Boolean): Int {
        val player = context.source.player
            ?: return 0 // Exit early if no player

        val name = StringArgumentType.getString(context, "name")
        
        // Determine position based on placement mode
        val position = if (usePlayerPosition) {
            // Place at player's exact position
            Vec3d(player.x, player.y, player.z)
        } else {
            // Place at crosshair target location
            WayfindrRaycast.getRaycastPosition(player)
        }

        // Use provided color or default
        val finalColor = colorInt ?: 0xFF0000 // Default red

        // Add waypoint to the manager
        val waypoint = WaypointManager.addWaypoint(name, position, finalColor)

        // Feedback to player with placement mode info
        val placementMode = if (usePlayerPosition) "at your location" else "at crosshair target"
        context.source.sendFeedback({
            Text.literal("Added waypoint '$name' $placementMode at ${position.x.toInt()}, ${position.y.toInt()}, ${position.z.toInt()}")
        }, false)

        return 1
    }

    /**
     * Parses a color string into an RGB integer value.
     * 
     * Supports:
     * - Named colors (red, green, blue, etc.)
     * - Hex colors (#RRGGBB)
     * 
     * @param colorArg The color string to parse
     * @return The RGB integer value of the color
     */
    private fun parseColor(colorArg: String): Int {
        return when (colorArg.lowercase()) {
            "red" -> 0xFF0000
            "green" -> 0x00FF00
            "blue" -> 0x0000FF
            "yellow" -> 0xFFFF00
            "purple" -> 0x800080
            "orange" -> 0xFFA500
            "white" -> 0xFFFFFF
            "black" -> 0x000000
            else -> {
                if (colorArg.startsWith("#")) {
                    try {
                        return colorArg.substring(1).toInt(16)
                    } catch (e: NumberFormatException) {
                        // Invalid hex color
                    }
                }
                0xFF0000 // Default to red if color not recognized
            }
        }
    }
}
