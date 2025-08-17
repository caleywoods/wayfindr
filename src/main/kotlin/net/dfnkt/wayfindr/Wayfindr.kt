package net.dfnkt.wayfindr

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import org.slf4j.LoggerFactory
import net.minecraft.text.Text
import net.minecraft.util.math.Vec3d

object Wayfindr : ModInitializer {
	const val MOD_ID = "wayfindr";
	private val logger = LoggerFactory.getLogger(MOD_ID);

	override fun onInitialize() {
		logger.info("Thanks for using the Wayfindr mod. Enjoy.")

		WaypointManager.initializeWaypoints()

		// @TODO: These should probably be moved to a Commands class for the mod but for now it can live here
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
										val name = StringArgumentType.getString(context, "name");

										WaypointManager.removeWaypoint(name)
										context.source.sendFeedback(
											{ Text.literal("Removed waypoint '$name'") },
											false
										)
										return@executes 1;
									}
							)
					)
			)
		}


	}

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
		WaypointManager.addWaypoint(name, position, finalColor)

		// Feedback to player with placement mode info
		val placementMode = if (usePlayerPosition) "at your location" else "at crosshair target"
		context.source.sendFeedback({
			Text.literal("Added waypoint '$name' $placementMode at ${position.x.toInt()}, ${position.y.toInt()}, ${position.z.toInt()}")
		}, false)

		return 1
	}

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