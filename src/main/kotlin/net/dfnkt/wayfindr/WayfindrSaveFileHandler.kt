package net.dfnkt.wayfindr

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles saving and loading waypoints to/from persistent storage.
 * 
 * This singleton manages file I/O operations for waypoints, including:
 * - Saving individual waypoints
 * - Loading all waypoints
 * - Saving all waypoints at once
 * 
 * It implements a caching mechanism to reduce disk I/O operations.
 */
object WayfindrSaveFileHandler {
    private val logger = LoggerFactory.getLogger("wayfindr")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    
    // Cache of loaded waypoints to avoid reading from disk repeatedly
    private val waypointCache = ConcurrentHashMap<String, List<WaypointManager.Waypoint>>()
    
    private val modDir = File(System.getProperty("user.dir") + "/wayfindr")
    private val waypointFile = File(modDir, "waypoints.json")

    /**
     * Saves a single waypoint to the waypoints file.
     * 
     * This method:
     * 1. Ensures the mod directory exists
     * 2. Deserializes the waypoint from JSON
     * 3. Loads existing waypoints (from cache if available)
     * 4. Adds the new waypoint to the list
     * 5. Updates the cache and writes to disk
     * 
     * @param waypointJson JSON string representation of the waypoint to save
     */
    fun saveWaypoint(waypointJson: String) {
        try {
            if (!modDir.exists() && !modDir.mkdirs()) {
                logger.error("Failed to create directory: ${modDir.absolutePath}")
                return
            }

            val newWaypoint = json.decodeFromString<WaypointManager.Waypoint>(waypointJson)
            
            // Use cached waypoints if available, otherwise read from file
            val waypoints = waypointCache[waypointFile.absolutePath] ?: if (waypointFile.exists()) {
                try {
                    json.decodeFromString<List<WaypointManager.Waypoint>>(waypointFile.readText())
                } catch (e: Exception) {
                    logger.error("Error reading existing waypoints, creating new file", e)
                    listOf()
                }
            } else {
                listOf()
            }

            val updatedWaypoints = waypoints + newWaypoint
            
            // Update the cache
            waypointCache[waypointFile.absolutePath] = updatedWaypoints
            
            // Write to file
            waypointFile.writeText(json.encodeToString(updatedWaypoints))

            logger.info("Saved waypoint to ${waypointFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to save waypoint", e)
        }
    }

    /**
     * Loads all waypoints from the waypoints file.
     * 
     * This method:
     * 1. Checks if waypoints are available in the cache
     * 2. If not, reads from the waypoints file
     * 3. Updates the cache with loaded waypoints
     * 
     * @return List of waypoints, or an empty list if none are found or an error occurs
     */
    fun loadWaypoints(): List<WaypointManager.Waypoint> {
        return try {
            // Check if we have cached waypoints
            waypointCache[waypointFile.absolutePath]?.let { 
                return it 
            }
            
            if (waypointFile.exists()) {
                val jsonContent = waypointFile.readText()
                if (jsonContent.isNotBlank()) {
                    val waypoints = json.decodeFromString<List<WaypointManager.Waypoint>>(jsonContent)
                    // Update cache
                    waypointCache[waypointFile.absolutePath] = waypoints
                    waypoints
                } else {
                    logger.info("Waypoint file is empty")
                    listOf()
                }
            } else {
                logger.info("No waypoint file found at ${waypointFile.absolutePath}")
                listOf()
            }
        } catch (e: Exception) {
            logger.error("Error loading waypoints", e)
            listOf()
        }
    }

    /**
     * Saves all waypoints to the waypoints file.
     * 
     * This method:
     * 1. Ensures the mod directory exists
     * 2. Updates the cache with the provided waypoints
     * 3. Writes all waypoints to disk
     * 
     * @param waypoints List of waypoints to save
     */
    fun saveAllWaypoints(waypoints: List<WaypointManager.Waypoint>) {
        try {
            if (!modDir.exists() && !modDir.mkdirs()) {
                logger.error("Failed to create directory: ${modDir.absolutePath}")
                return
            }
            
            // Update cache
            waypointCache[waypointFile.absolutePath] = waypoints
            
            waypointFile.writeText(json.encodeToString(waypoints))
            logger.info("Saved ${waypoints.size} waypoints to ${waypointFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to save waypoints", e)
        }
    }
    
    /**
     * Clears the waypoint cache.
     * 
     * This should be called when waypoints are deleted or modified outside
     * of the normal save methods to ensure cache consistency.
     */
    fun clearCache() {
        waypointCache.clear()
    }
}