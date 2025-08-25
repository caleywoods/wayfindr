package net.dfnkt.wayfindr

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object Wayfindr : ModInitializer {
	const val MOD_ID = "wayfindr";
	private val logger = LoggerFactory.getLogger(MOD_ID);

	override fun onInitialize() {
		logger.info("Thanks for using the Wayfindr mod. Enjoy.")

		WaypointManager.initializeWaypoints()
		
		// Register commands
		WayfindrCommands.register()
	}
}