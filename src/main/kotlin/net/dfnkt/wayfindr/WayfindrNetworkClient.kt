package net.dfnkt.wayfindr

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Handles client-side network communication for waypoint synchronization.
 */
object WayfindrNetworkClient {
    private val logger = LoggerFactory.getLogger("wayfindr")
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * Initializes client-side network handlers for waypoint synchronization.
     */
    fun initialize() {
        logger.info("Initializing client-side network handlers for waypoint synchronization")
        
        // Handler for full waypoint sync (received when joining a server)
        ClientPlayNetworking.registerGlobalReceiver(WayfindrNetworking.WaypointSyncPayload.ID) { payload, context ->
            val jsonData = payload.data
            
            context.client().execute {
                try {
                    logger.info("Received waypoint sync data: $jsonData")
                    val serverWaypoints = json.decodeFromString<List<WaypointManager.Waypoint>>(jsonData)
                    logger.info("Received waypoint sync with ${serverWaypoints.size} waypoints")
                    
                    // Get current local waypoints
                    val localWaypoints = WaypointManager.waypoints.filter { !it.isShared }
                    
                    // Combine local non-shared waypoints with server waypoints
                    val combinedWaypoints = localWaypoints + serverWaypoints
                    
                    // Use replaceAllWaypoints with the combined list
                    WaypointManager.replaceAllWaypoints(combinedWaypoints)
                    
                    logger.info("Synchronized ${serverWaypoints.size} shared waypoints from server while preserving ${localWaypoints.size} local waypoints")
                } catch (e: Exception) {
                    logger.error("Error processing waypoint sync", e)
                }
            }
        }
        
        // Handler for adding a new waypoint
        ClientPlayNetworking.registerGlobalReceiver(WayfindrNetworking.WaypointAddPayload.ID) { payload, context ->
            val jsonData = payload.data
            
            context.client().execute {
                try {
                    logger.info("Received waypoint add data: $jsonData")
                    val waypoint = json.decodeFromString<WaypointManager.Waypoint>(jsonData)
                    logger.info("Adding waypoint from server: ${waypoint.name} (ID: ${waypoint.id}, Owner: ${waypoint.owner})")
                    
                    // Use addWaypoint method instead of directly modifying the list
                    WaypointManager.addWaypoint(waypoint)
                    
                    logger.info("Added waypoint from server: ${waypoint.name}")
                } catch (e: Exception) {
                    logger.error("Error processing waypoint add", e)
                }
            }
        }
        
        // Handler for updating an existing waypoint
        ClientPlayNetworking.registerGlobalReceiver(WayfindrNetworking.WaypointUpdatePayload.ID) { payload, context ->
            val jsonData = payload.data
            
            context.client().execute {
                try {
                    logger.info("Received waypoint update data: $jsonData")
                    val updatedWaypoint = json.decodeFromString<WaypointManager.Waypoint>(jsonData)
                    
                    // Use updateWaypoint method instead of directly modifying the list
                    val success = WaypointManager.updateWaypoint(updatedWaypoint)
                    
                    if (success) {
                        logger.info("Updated waypoint from server: ${updatedWaypoint.name} (ID: ${updatedWaypoint.id}, Owner: ${updatedWaypoint.owner})")
                    } else {
                        logger.warn("Received update for unknown waypoint ID: ${updatedWaypoint.id}")
                    }
                } catch (e: Exception) {
                    logger.error("Error processing waypoint update", e)
                }
            }
        }
        
        // Handler for deleting a waypoint
        ClientPlayNetworking.registerGlobalReceiver(WayfindrNetworking.WaypointDeletePayload.ID) { payload, context ->
            val waypointId = UUID.fromString(payload.waypointId)
            
            context.client().execute {
                try {
                    logger.info("Received waypoint delete for ID: $waypointId")
                    
                    // Use removeWaypoint method instead of directly modifying the list
                    val success = WaypointManager.removeWaypoint(waypointId)
                    
                    if (success) {
                        logger.info("Removed waypoint with ID: $waypointId")
                    } else {
                        logger.warn("Received delete for unknown waypoint ID: $waypointId")
                    }
                } catch (e: Exception) {
                    logger.error("Error processing waypoint delete", e)
                }
            }
        }
        
        logger.info("Registered waypoint network handlers")
    }
    
    /**
     * Sends a waypoint to the server to be shared with all players.
     * 
     * @param waypoint The waypoint to share
     * @return True if the waypoint was sent successfully, false otherwise
     */
    fun sendWaypointToServer(waypoint: WaypointManager.Waypoint): Boolean {
        try {
            logger.info("Sending waypoint to server: ${waypoint.name} (ID: ${waypoint.id})")
            val jsonData = json.encodeToString(waypoint)
            val payload = WayfindrNetworking.WaypointAddPayload(jsonData)
            ClientPlayNetworking.send(payload)
            logger.info("Sent waypoint to server: ${waypoint.name}")
            return true
        } catch (e: Exception) {
            logger.error("Error sending waypoint to server", e)
            return false
        }
    }
    
    /**
     * Sends a waypoint update to the server.
     * 
     * @param waypoint The updated waypoint
     * @return True if the update was sent successfully, false otherwise
     */
    fun sendWaypointUpdateToServer(waypoint: WaypointManager.Waypoint): Boolean {
        try {
            logger.info("Sending waypoint update to server: ${waypoint.name} (ID: ${waypoint.id})")
            val jsonData = json.encodeToString(waypoint)
            val payload = WayfindrNetworking.WaypointUpdatePayload(jsonData)
            ClientPlayNetworking.send(payload)
            logger.info("Sent waypoint update to server: ${waypoint.name}")
            return true
        } catch (e: Exception) {
            logger.error("Error sending waypoint update to server", e)
            return false
        }
    }
    
    /**
     * Sends a waypoint deletion request to the server.
     * 
     * @param waypointId The ID of the waypoint to delete
     * @return True if the deletion request was sent successfully, false otherwise
     */
    fun sendWaypointDeleteToServer(waypointId: UUID): Boolean {
        try {
            logger.info("Sending waypoint delete to server for ID: $waypointId")
            val payload = WayfindrNetworking.WaypointDeletePayload(waypointId.toString())
            ClientPlayNetworking.send(payload)
            logger.info("Sent waypoint delete to server for ID: $waypointId")
            return true
        } catch (e: Exception) {
            logger.error("Error sending waypoint delete to server", e)
            return false
        }
    }
}
