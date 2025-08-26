package net.dfnkt.wayfindr

import kotlinx.serialization.json.Json
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.MinecraftServer
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages server-side waypoints that are shared among all players.
 * This is the source of truth for shared waypoints.
 */
object ServerWaypointManager {
    private val logger = LoggerFactory.getLogger("wayfindr")
    private val json = Json { prettyPrint = true }
    
    // Map of waypoint ID to waypoint
    private val sharedWaypoints = ConcurrentHashMap<UUID, WaypointManager.Waypoint>()
    
    // Server instance, set during initialization
    private var server: MinecraftServer? = null
    
    /**
     * Initializes the server waypoint manager.
     * Should be called during server initialization.
     * 
     * @param minecraftServer The server instance
     */
    fun initialize(minecraftServer: MinecraftServer) {
        server = minecraftServer
        
        // Initialize the save handler
        ServerWaypointSaveHandler.initialize(minecraftServer)
        
        // Initialize the network server
        WayfindrNetworkServer.initialize(minecraftServer)
        
        // Load waypoints from storage
        loadWaypoints()
        
        // Register player join event to send waypoints to new players
        registerPlayerJoinHandler()
        
        logger.info("Server waypoint manager initialized with ${sharedWaypoints.size} waypoints")
    }
    
    /**
     * Loads all shared waypoints from disk.
     */
    private fun loadWaypoints() {
        val loadedWaypoints = ServerWaypointSaveHandler.loadWaypoints()
        
        // Clear existing waypoints and add loaded ones
        sharedWaypoints.clear()
        loadedWaypoints.forEach { waypoint ->
            sharedWaypoints[waypoint.id] = waypoint
        }
        
        logger.info("Loaded ${loadedWaypoints.size} shared waypoints")
    }
    
    /**
     * Saves all shared waypoints to disk.
     */
    private fun saveWaypoints() {
        val success = ServerWaypointSaveHandler.saveWaypoints(sharedWaypoints.values)
        if (success) {
            logger.info("Saved ${sharedWaypoints.size} shared waypoints")
        } else {
            logger.error("Failed to save shared waypoints")
        }
    }
    
    /**
     * Registers event handlers for player join events.
     * This sends the full waypoint list to players when they join.
     */
    private fun registerPlayerJoinHandler() {
        ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
            // Send full waypoint sync to the joining player
            WayfindrNetworkServer.sendWaypointSync(handler.player)
            logger.info("Player ${handler.player.name.string} joined, sending waypoint sync")
        }
    }
    
    /**
     * Adds a new shared waypoint.
     * 
     * @param waypoint The waypoint to add
     * @return The added waypoint
     */
    fun addWaypoint(waypoint: WaypointManager.Waypoint): WaypointManager.Waypoint {
        // Ensure the waypoint is marked as shared
        waypoint.isShared = true
        
        // Add to the map
        sharedWaypoints[waypoint.id] = waypoint
        
        // Save to disk
        saveWaypoints()
        
        logger.info("Added shared waypoint '${waypoint.name}' to server")
        return waypoint
    }
    
    /**
     * Updates an existing shared waypoint.
     * 
     * @param waypoint The waypoint to update
     * @return True if the waypoint was found and updated, false otherwise
     */
    fun updateWaypoint(waypoint: WaypointManager.Waypoint): Boolean {
        if (!sharedWaypoints.containsKey(waypoint.id)) {
            return false
        }
        
        // Update the waypoint
        sharedWaypoints[waypoint.id] = waypoint
        
        // Save to disk
        saveWaypoints()
        
        logger.info("Updated shared waypoint '${waypoint.name}' on server")
        return true
    }
    
    /**
     * Removes a shared waypoint by its ID.
     * 
     * @param id The ID of the waypoint to remove
     * @return True if the waypoint was found and removed, false otherwise
     */
    fun removeWaypoint(id: UUID): Boolean {
        val waypoint = sharedWaypoints.remove(id) ?: return false
        
        // Save to disk
        saveWaypoints()
        
        logger.info("Removed shared waypoint '${waypoint.name}' from server")
        return true
    }
    
    /**
     * Gets all shared waypoints.
     * 
     * @return List of all shared waypoints
     */
    fun getAllWaypoints(): List<WaypointManager.Waypoint> {
        return sharedWaypoints.values.toList()
    }
    
    /**
     * Gets a shared waypoint by its ID.
     * 
     * @param id The ID of the waypoint to get
     * @return The waypoint if found, null otherwise
     */
    fun getWaypoint(id: UUID): WaypointManager.Waypoint? {
        return sharedWaypoints[id]
    }
}
