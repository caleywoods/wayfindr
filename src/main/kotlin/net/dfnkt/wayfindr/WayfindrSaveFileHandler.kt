package net.dfnkt.wayfindr

import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import net.minecraft.client.MinecraftClient

/**
 * Handles saving and loading waypoints to/from persistent storage.
 */
object WayfindrSaveFileHandler {
    private val logger = LoggerFactory.getLogger("wayfindr")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    
    // Replace the simple cache with our new implementation
    private val waypointCache = WaypointCache()
    
    private val modDir = File(System.getProperty("user.dir") + "/wayfindr")
    private val worldsDir = File(modDir, "worlds")
    
    /**
     * A simple cache implementation for waypoints with size limits and expiration.
     */
    private class WaypointCache {
        private val maxCacheSize = 10 // Limit number of worlds cached
        private val cacheExpirationMs = 10 * 60 * 1000
        
        private data class CacheEntry(
            val waypoints: List<WaypointManager.Waypoint>,
            val timestamp: Long = System.currentTimeMillis()
        )
        
        private val cache = LinkedHashMap<String, CacheEntry>(16, 0.75f, true) // Access-order
        
        fun get(key: String): List<WaypointManager.Waypoint>? {
            val entry = cache[key] ?: return null
            
            if (System.currentTimeMillis() - entry.timestamp > cacheExpirationMs) {
                cache.remove(key)
                return null
            }
            
            return entry.waypoints
        }
        
        fun put(key: String, waypoints: List<WaypointManager.Waypoint>) {
            cache[key] = CacheEntry(waypoints)
            
            while (cache.size > maxCacheSize) {
                val oldestKey = cache.keys.first()
                cache.remove(oldestKey)
                logger.debug("Cache eviction: removed waypoints for $oldestKey")
            }
        }
        
        fun clear() {
            cache.clear()
        }
        
        fun invalidate(key: String) {
            cache.remove(key)
        }
    }
    
    /**
     * Ensures that the necessary directories for waypoint storage exist.
     * 
     * @return True if directories exist or were successfully created, false otherwise
     */
    private fun ensureDirectoriesExist(): Boolean {
        if (!modDir.exists() && !modDir.mkdirs()) {
            logger.error("Failed to create directory: ${modDir.absolutePath}")
            return false
        }
        
        if (!worldsDir.exists() && !worldsDir.mkdirs()) {
            logger.error("Failed to create worlds directory: ${worldsDir.absolutePath}")
            return false
        }
        
        return true
    }

    /**
     * Gets the current world name from the Minecraft client.
     * For singleplayer, this is the level name.
     * For multiplayer, this is the server address.
     *
     * @return The current world name or "default" if it cannot be determined
     */
    fun getCurrentWorldName(): String {
        val client = MinecraftClient.getInstance()
        
        return when {
            client.server != null -> {
                client.server?.saveProperties?.levelName ?: "default"
            }
            client.networkHandler != null -> {
                val serverInfo = client.currentServerEntry
                serverInfo?.address ?: "default"
            }
            else -> "default"
        }
    }
    
    /**
     * Gets the waypoint file for the current world.
     *
     * @return File object pointing to the world-specific waypoint file
     */
    fun getWorldWaypointFile(): File {
        val worldName = getCurrentWorldName()
        val worldDir = File(worldsDir, sanitizeFileName(worldName))
        
        if (!worldDir.exists() && !worldDir.mkdirs()) {
            logger.error("Failed to create world directory: ${worldDir.absolutePath}")
        }
        
        return File(worldDir, "waypoints.json")
    }
    
    /**
     * Sanitizes a filename to ensure it's valid across different file systems.
     *
     * @param name The raw filename to sanitize
     * @return A sanitized filename
     */
    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    /**
     * Loads all waypoints from the current world's waypoint file.
     * 
     * This method:
     * 1. Checks if waypoints are available in the cache
     * 2. If not, reads from the world-specific waypoints file
     * 3. Updates the cache with loaded waypoints
     * 
     * @return List of waypoints, or an empty list if none are found or an error occurs
     */
    fun loadWaypoints(): List<WaypointManager.Waypoint> {
        return try {
            val waypointFile = getWorldWaypointFile()
            
            // Try to get waypoints from cache first
            waypointCache.get(waypointFile.absolutePath)?.let { 
                logger.debug("Cache hit: loaded waypoints from cache for ${getCurrentWorldName()}")
                return it 
            }
            
            if (waypointFile.exists()) {
                val jsonContent = waypointFile.readText()
                if (jsonContent.isNotBlank()) {
                    val waypoints = json.decodeFromString<List<WaypointManager.Waypoint>>(jsonContent)
                    // Update cache with loaded waypoints
                    waypointCache.put(waypointFile.absolutePath, waypoints)
                    logger.debug("Cache miss: loaded waypoints from disk for ${getCurrentWorldName()}")
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
     * Saves a single waypoint to the world-specific waypoints file.
     * 
     * This method:
     * 1. Ensures the world directory exists
     * 2. Deserializes the waypoint from JSON
     * 3. Loads existing waypoints (from cache if available)
     * 4. Adds the new waypoint to the list
     * 5. Updates the cache and writes to disk
     * 
     * @param waypointJson JSON string representation of the waypoint to save
     */
    fun saveWaypoint(waypointJson: String) {
        try {
            if (!ensureDirectoriesExist()) {
                return
            }
            
            val waypointFile = getWorldWaypointFile()
            val newWaypoint = json.decodeFromString<WaypointManager.Waypoint>(waypointJson)
            
            // Get existing waypoints (from cache or disk)
            val waypoints = waypointCache.get(waypointFile.absolutePath) ?: if (waypointFile.exists()) {
                try {
                    json.decodeFromString<List<WaypointManager.Waypoint>>(waypointFile.readText())
                } catch (e: Exception) {
                    logger.error("Error reading existing waypoints, creating new file", e)
                    listOf()
                }
            } else {
                listOf()
            }

            val existingIndex = waypoints.indexOfFirst { it.id == newWaypoint.id }
            val updatedWaypoints = if (existingIndex >= 0) {
                logger.warn("Duplicate waypoint ID detected on save; replacing existing entry: ${newWaypoint.id}")
                waypoints.toMutableList().also { it[existingIndex] = newWaypoint }
            } else {
                waypoints + newWaypoint
            }
            
            // Update cache with new waypoint list
            waypointCache.put(waypointFile.absolutePath, updatedWaypoints)
            
            // Write to file
            waypointFile.writeText(json.encodeToString(updatedWaypoints))

            logger.info("Saved waypoint to ${waypointFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to save waypoint", e)
        }
    }

    /**
     * Saves all waypoints to the current world's waypoint file.
     * 
     * This method:
     * 1. Ensures the world directory exists
     * 2. Updates the cache with the provided waypoints
     * 3. Writes all waypoints to disk
     * 
     * @param waypoints List of waypoints to save
     */
    fun saveAllWaypoints(waypoints: List<WaypointManager.Waypoint>) {
        try {
            if (!ensureDirectoriesExist()) {
                return
            }
            
            val waypointFile = getWorldWaypointFile()
            
            // Update cache with new waypoint list
            waypointCache.put(waypointFile.absolutePath, waypoints)
            
            waypointFile.writeText(json.encodeToString(waypoints))
            logger.info("Saved ${waypoints.size} waypoints to ${waypointFile.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to save waypoints", e)
        }
    }
    
    /**
     * Clears the waypoint cache.
     */
    fun clearCache() {
        waypointCache.clear()
        logger.debug("Waypoint cache cleared")
    }
    
    /**
     * Invalidates a specific world's waypoints in the cache.
     * 
     * @param worldName The name of the world to invalidate
     */
    fun invalidateCache(worldName: String) {
        val worldDir = File(worldsDir, sanitizeFileName(worldName))
        val waypointFile = File(worldDir, "waypoints.json")
        waypointCache.invalidate(waypointFile.absolutePath)
        logger.debug("Cache invalidated for world: $worldName")
    }
}