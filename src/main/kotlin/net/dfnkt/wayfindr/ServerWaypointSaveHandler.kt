package net.dfnkt.wayfindr

import kotlinx.serialization.json.Json
import net.minecraft.server.MinecraftServer
import net.minecraft.util.WorldSavePath
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files

/**
 * Handles saving and loading shared waypoints on the server side.
 * 
 * This class is responsible for persisting shared waypoints to disk
 * and loading them when the server starts.
 */
object ServerWaypointSaveHandler {
    private val logger = LoggerFactory.getLogger("wayfindr")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    
    // File name for shared waypoints
    private const val SHARED_WAYPOINTS_FILE = "wayfindr_shared_waypoints.json"
    
    // Server instance, set during initialization
    private var server: MinecraftServer? = null
    
    /**
     * Initializes the save handler with the server instance.
     * 
     * @param minecraftServer The server instance
     */
    fun initialize(minecraftServer: MinecraftServer) {
        server = minecraftServer
        logger.info("Server waypoint save handler initialized")
    }
    
    /**
     * Loads all shared waypoints from disk.
     * 
     * @return List of loaded waypoints, or empty list if none found
     */
    fun loadWaypoints(): List<WaypointManager.Waypoint> {
        val server = server ?: run {
            logger.error("Cannot load waypoints: Server not initialized")
            return emptyList()
        }
        
        val saveFile = getSaveFile(server)
        if (!saveFile.exists()) {
            logger.info("No shared waypoints file found, starting with empty list")
            return emptyList()
        }
        
        return try {
            val content = Files.readString(saveFile.toPath())
            val waypoints = json.decodeFromString<List<WaypointManager.Waypoint>>(content)
            logger.info("Loaded ${waypoints.size} shared waypoints from disk")
            waypoints
        } catch (e: Exception) {
            logger.error("Failed to load shared waypoints", e)
            emptyList()
        }
    }
    
    /**
     * Saves all shared waypoints to disk.
     * 
     * @param waypoints The waypoints to save
     * @return True if successful, false otherwise
     */
    fun saveWaypoints(waypoints: Collection<WaypointManager.Waypoint>): Boolean {
        val server = server ?: run {
            logger.error("Cannot save waypoints: Server not initialized")
            return false
        }
        
        val saveFile = getSaveFile(server)
        
        // Create parent directories if they don't exist
        saveFile.parentFile?.mkdirs()
        
        return try {
            val content = json.encodeToString(waypoints.toList())
            Files.writeString(saveFile.toPath(), content)
            logger.info("Saved ${waypoints.size} shared waypoints to disk")
            true
        } catch (e: Exception) {
            logger.error("Failed to save shared waypoints", e)
            false
        }
    }
    
    /**
     * Gets the save file for shared waypoints.
     * 
     * @param server The server instance
     * @return The save file
     */
    private fun getSaveFile(server: MinecraftServer): File {
        // Store in the server's world directory
        val worldDirectory = server.getSavePath(WorldSavePath.ROOT).toFile()
        return File(worldDirectory, SHARED_WAYPOINTS_FILE)
    }
}
