package net.dfnkt.wayfindr

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager
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
										val player = context.source.player
											?: return@executes 0
										val name = StringArgumentType.getString(context, "name")
										val position = Vec3d(player.x, player.y + 10, player.z)

										val waypoint = WaypointManager.addWaypoint(name, position)
										// @TODO: These strings should be defined in the lang folder so we have localization
										context.source.sendFeedback(
											{ Text.literal("Added waypoint '$name' at ${position.x.toInt()}, ${position.y.toInt()}, ${position.z.toInt()}") },
											false
										)
										return@executes 1
									}
							),
					)
					.then(
						CommandManager.literal("delete")
						.then(
							CommandManager.argument("name", StringArgumentType.string())
								.executes { context ->
									val name = StringArgumentType.getString(context, "name");
									// @TODO: These strings should be defined in the lang folder so we have localization
									context.source.sendFeedback(
										{ Text.literal("Wayfindr: waypoint \"$name\" was deleted") },
										false
									)
									1;
								}
						)
					)
            )
        }
    }
}