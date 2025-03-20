package net.dfnkt.wayfindr

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import org.slf4j.LoggerFactory
import net.minecraft.server.command.CommandManager.literal;
import net.minecraft.text.Text

object Wayfindr : ModInitializer {
	const val MOD_ID = "wayfindr";
	private val logger = LoggerFactory.getLogger(MOD_ID);

	override fun onInitialize() {
		logger.info("Thanks for using the Wayfindr mod. Enjoy.")

		// @TODO: This should probably be moved to a Commands class for the mod but for now it can live here
		CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
			dispatcher.register(
				literal("foo")
					.executes {context ->
						context.source.sendFeedback({ Text.literal("Called /foo with no args")}, false)
						1;
					}
			)
		}
	}
}