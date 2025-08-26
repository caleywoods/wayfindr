package net.dfnkt.wayfindr

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Handles server-side network communication for waypoint synchronization.
 */
object WayfindrNetworkServer {
    private val logger = LoggerFactory.getLogger("wayfindr")
    private val json = Json { prettyPrint = true }
    
    // Server instance, set during initialization
    private var server: MinecraftServer? = null
    
    /**
     * Initializes server-side network handlers for waypoint synchronization.
     * 
     * @param minecraftServer The server instance
     */
    fun initialize(minecraftServer: MinecraftServer) {
        server = minecraftServer
        
        // Register packet handlers for client requests
        registerNetworkHandlers()
        
        logger.info("Server-side waypoint network handlers initialized")
    }
    
    /**
     * Registers network handlers for processing client requests.
     */
    private fun registerNetworkHandlers() {
        logger.info("Registering server-side network handlers for waypoint synchronization")
        
        // Handler for adding a new waypoint from a client
        ServerPlayNetworking.registerGlobalReceiver(WayfindrNetworking.WaypointAddPayload.ID) { payload, context ->
            val jsonData = payload.data
            logger.info("Received waypoint add request from ${context.player().name.string}: $jsonData")
            
            context.server().execute {
                try {
                    val receivedWaypoint = json.decodeFromString<WaypointManager.Waypoint>(jsonData)
                    logger.info("Decoded waypoint: ${receivedWaypoint.name} (ID: ${receivedWaypoint.id})")
                    
                    // Create a new waypoint with proper ownership and shared status
                    val waypoint = receivedWaypoint.copy(
                        owner = context.player().uuid,
                        isShared = true
                    )
                    
                    // Add to server's waypoint manager
                    logger.info("Adding waypoint to ServerWaypointManager: ${waypoint.name} (ID: ${waypoint.id}, Owner: ${waypoint.owner})")
                    ServerWaypointManager.addWaypoint(waypoint)
                    
                    // Broadcast to all players (including the sender for confirmation)
                    logger.info("Broadcasting waypoint add to all players: ${waypoint.name}")
                    broadcastWaypointAdd(waypoint)
                    
                    logger.info("Player ${context.player().name.string} added shared waypoint: ${waypoint.name}")
                } catch (e: Exception) {
                    logger.error("Error processing waypoint add request from ${context.player().name.string}", e)
                }
            }
        }
        
        // Handler for updating an existing waypoint from a client
        ServerPlayNetworking.registerGlobalReceiver(WayfindrNetworking.WaypointUpdatePayload.ID) { payload, context ->
            val jsonData = payload.data
            logger.info("Received waypoint update request from ${context.player().name.string}: $jsonData")
            
            context.server().execute {
                try {
                    val waypoint = json.decodeFromString<WaypointManager.Waypoint>(jsonData)
                    logger.info("Decoded waypoint update: ${waypoint.name} (ID: ${waypoint.id})")
                    
                    // Check if player is allowed to update this waypoint
                    // For now, simple check: player must be the owner or an op
                    val existingWaypoint = ServerWaypointManager.getWaypoint(waypoint.id)
                    if (existingWaypoint != null) {
                        logger.info("Found existing waypoint: ${existingWaypoint.name} (Owner: ${existingWaypoint.owner})")
                        
                        if (existingWaypoint.owner == context.player().uuid || context.player().hasPermissionLevel(2)) {
                            // Update the waypoint
                            if (ServerWaypointManager.updateWaypoint(waypoint)) {
                                // Broadcast to all players
                                logger.info("Broadcasting waypoint update to all players: ${waypoint.name}")
                                broadcastWaypointUpdate(waypoint)
                                
                                logger.info("Player ${context.player().name.string} updated shared waypoint: ${waypoint.name}")
                            } else {
                                logger.warn("Failed to update waypoint in ServerWaypointManager: ${waypoint.name}")
                            }
                        } else {
                            logger.warn("Player ${context.player().name.string} attempted to update waypoint without permission")
                        }
                    } else {
                        logger.warn("Waypoint not found for update: ${waypoint.id}")
                    }
                } catch (e: Exception) {
                    logger.error("Error processing waypoint update request from ${context.player().name.string}", e)
                }
            }
        }
        
        // Handler for deleting a waypoint from a client
        ServerPlayNetworking.registerGlobalReceiver(WayfindrNetworking.WaypointDeletePayload.ID) { payload, context ->
            val waypointId = UUID.fromString(payload.waypointId)
            logger.info("Received waypoint delete request from ${context.player().name.string} for ID: $waypointId")
            
            context.server().execute {
                try {
                    // Check if player is allowed to delete this waypoint
                    val waypoint = ServerWaypointManager.getWaypoint(waypointId)
                    if (waypoint != null) {
                        logger.info("Found waypoint to delete: ${waypoint.name} (Owner: ${waypoint.owner})")
                        
                        if (waypoint.owner == context.player().uuid || context.player().hasPermissionLevel(2)) {
                            // Remove the waypoint
                            if (ServerWaypointManager.removeWaypoint(waypointId)) {
                                // Broadcast to all players
                                logger.info("Broadcasting waypoint delete to all players: ${waypoint.name}")
                                broadcastWaypointDelete(waypointId)
                                
                                logger.info("Player ${context.player().name.string} deleted shared waypoint: ${waypoint.name}")
                            } else {
                                logger.warn("Failed to delete waypoint in ServerWaypointManager: ${waypoint.name}")
                            }
                        } else {
                            logger.warn("Player ${context.player().name.string} attempted to delete waypoint without permission")
                        }
                    } else {
                        logger.warn("Waypoint not found for deletion: $waypointId")
                    }
                } catch (e: Exception) {
                    logger.error("Error processing waypoint delete request from ${context.player().name.string}", e)
                }
            }
        }
        
        logger.info("Server-side network handlers registered successfully")
    }
    
    /**
     * Sends a full waypoint sync to a specific player.
     * Used when a player joins the server.
     * 
     * @param player The player to send waypoints to
     */
    fun sendWaypointSync(player: ServerPlayerEntity) {
        val waypoints = ServerWaypointManager.getAllWaypoints()
        logger.info("Preparing to send waypoint sync to ${player.name.string} with ${waypoints.size} waypoints")
        
        val jsonData = json.encodeToString(waypoints)
        logger.debug("Waypoint sync data: $jsonData")
        
        val payload = WayfindrNetworking.WaypointSyncPayload(jsonData)
        ServerPlayNetworking.send(player, payload)
        logger.info("Sent waypoint sync to player ${player.name.string} with ${waypoints.size} waypoints")
    }
    
    /**
     * Broadcasts a waypoint addition to all connected players.
     * 
     * @param waypoint The waypoint that was added
     */
    fun broadcastWaypointAdd(waypoint: WaypointManager.Waypoint) {
        val server = server ?: return
        val jsonData = json.encodeToString(waypoint)
        logger.debug("Broadcasting waypoint add data: $jsonData")
        
        val payload = WayfindrNetworking.WaypointAddPayload(jsonData)
        
        var playerCount = 0
        for (player in server.playerManager.playerList) {
            ServerPlayNetworking.send(player, payload)
            playerCount++
        }
        logger.info("Broadcast waypoint add to $playerCount players: ${waypoint.name}")
    }
    
    /**
     * Broadcasts a waypoint update to all connected players.
     * 
     * @param waypoint The waypoint that was updated
     */
    fun broadcastWaypointUpdate(waypoint: WaypointManager.Waypoint) {
        val server = server ?: return
        val jsonData = json.encodeToString(waypoint)
        logger.debug("Broadcasting waypoint update data: $jsonData")
        
        val payload = WayfindrNetworking.WaypointUpdatePayload(jsonData)
        
        var playerCount = 0
        for (player in server.playerManager.playerList) {
            ServerPlayNetworking.send(player, payload)
            playerCount++
        }
        logger.info("Broadcast waypoint update to $playerCount players: ${waypoint.name}")
    }
    
    /**
     * Broadcasts a waypoint deletion to all connected players.
     * 
     * @param waypointId The ID of the waypoint that was deleted
     */
    fun broadcastWaypointDelete(waypointId: UUID) {
        val server = server ?: return
        logger.debug("Broadcasting waypoint delete for ID: $waypointId")
        
        val payload = WayfindrNetworking.WaypointDeletePayload(waypointId.toString())
        
        var playerCount = 0
        for (player in server.playerManager.playerList) {
            ServerPlayNetworking.send(player, payload)
            playerCount++
        }
        logger.info("Broadcast waypoint delete to $playerCount players for ID: $waypointId")
    }
}
