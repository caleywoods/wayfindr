package net.dfnkt.wayfindr

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import org.slf4j.LoggerFactory

object Wayfindr : ModInitializer {
	const val MOD_ID = "wayfindr";
	private val logger = LoggerFactory.getLogger(MOD_ID);

	override fun onInitialize() {
		logger.info("Thanks for using the Wayfindr mod. Enjoy.")
		
		// Initialize networking
		WayfindrNetworking.initialize()
		
		// Register server lifecycle events for server-side waypoint management
		registerServerEvents()
		
		// Register commands
		WayfindrCommands.register()
	}
	
	/**
	 * Registers server lifecycle events for server-side waypoint management.
	 */
	private fun registerServerEvents() {
		// Initialize server components when server starts
		ServerLifecycleEvents.SERVER_STARTING.register { server ->
			logger.info("Initializing server-side waypoint management")
			ServerWaypointManager.initialize(server)
		}
		
		// Save waypoints when server stops (if needed, though our implementation saves on each change)
		ServerLifecycleEvents.SERVER_STOPPING.register { server ->
			logger.info("Server stopping, ensuring waypoints are saved")
			// No explicit action needed as we save on each change
		}
	}
}