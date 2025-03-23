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

		// @TODO: These should probably be moved to a Commands class for the mod but for now it can live here
		CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
			dispatcher.register(
				// @TODO: This should write a new entry to our waypoints JSON file
				CommandManager.literal("waypoint")
					.then(
						CommandManager.literal("add")
							.then(
								CommandManager.argument("name", StringArgumentType.string())
									.executes { context ->

										handleAddWaypoint(context, null)
									}
									.then(
										CommandManager.argument("color", StringArgumentType.word())
											.executes { context ->
												val colorArg = StringArgumentType.getString(context, "color")
												val color = parseColor(colorArg)

												handleAddWaypoint(context, color)
											}
									)
							),
					)
					.then(
						CommandManager.literal("delete")
							.then(
								CommandManager.argument("name", StringArgumentType.string())
									.executes { context ->
										val name = StringArgumentType.getString(context, "name");

										WaypointManager.removeWaypoint(name)
										// @TODO: These strings should be defined in the lang folder so we have localization
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

	private fun handleAddWaypoint(context: CommandContext<ServerCommandSource>, colorInt: Int?): Int {
		val player = context.source.player
			?: return 0 // Exit early if no player

		val name = StringArgumentType.getString(context, "name")
		val position = Vec3d(player.x, player.y, player.z)

		// Use provided color or default
		val finalColor = colorInt ?: 0xFF0000 // Default red

		// Add waypoint to the manager
		val waypoint = WaypointManager.addWaypoint(name, position, finalColor)

		// Feedback to player
		context.source.sendFeedback({
			Text.literal("Added waypoint '$name' at ${position.x.toInt()}, ${position.y.toInt()}, ${position.z.toInt()}")
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
				// Try to parse hex color code if it starts with #
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