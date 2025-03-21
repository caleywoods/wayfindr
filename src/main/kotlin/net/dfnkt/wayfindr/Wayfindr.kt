package net.dfnkt.wayfindr

import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.argument
import org.slf4j.LoggerFactory
import net.minecraft.server.command.CommandManager.literal;
import net.minecraft.text.Text

object Wayfindr : ModInitializer {
    const val MOD_ID = "wayfindr";
    private val logger = LoggerFactory.getLogger(MOD_ID);

    override fun onInitialize() {
        logger.info("Thanks for using the Wayfindr mod. Enjoy.")

        // @TODO: These should probably be moved to a Commands class for the mod but for now it can live here
		CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
			dispatcher.register(
				// @TODO: This should write a new entry to our waypoints JSON file
				literal("waypoint")
					.then(
						literal("add")
							.then(
								argument("name", StringArgumentType.string())
									.executes { context ->
										val name = StringArgumentType.getString(context, "name");
										// @TODO: These strings should be defined in the lang folder so we have localization
										context.source.sendFeedback(
											{ Text.literal("Wayfindr: waypoint \"$name\" was added") },
											false
										)
										1;
									}
							),
					)
					.then(
						literal("delete")
						.then(
							argument("name", StringArgumentType.string())
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