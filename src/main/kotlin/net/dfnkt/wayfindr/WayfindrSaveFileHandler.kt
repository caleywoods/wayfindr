package net.dfnkt.wayfindr

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import org.slf4j.LoggerFactory

class WayfindrSaveFileHandler {
    private val minecraftDir = File(System.getProperty("user.home"), ".minecraft")
    private val modDir = File(minecraftDir, "config/wayfindr")
    private val waypointFile = File(modDir, "waypoints.json")
    private val MOD_ID = "wayfindr"
    private val logger = LoggerFactory.getLogger(MOD_ID)
    private val json = Json { prettyPrint = true }

    fun saveWaypoint(waypointJson: String) {
        try {
            // Create directory if it doesn't exist
            if (!modDir.exists() && !modDir.mkdirs()) {
                logger.info("Failed to create directory: ${modDir.absolutePath}")
                return
            }

            // Parse the new waypoint
            val newWaypoint = json.decodeFromString<WaypointManager.Waypoint>(waypointJson)

            // Load existing waypoints or create empty list
            val waypoints = if (waypointFile.exists()) {
                try {
                    json.decodeFromString<List<WaypointManager.Waypoint>>(waypointFile.readText())
                } catch (e: Exception) {
                    logger.info("Error reading existing waypoints, creating new file: ${e.message}")
                    listOf() // Return empty list if file is corrupted
                }
            } else {
                listOf() // Return empty list if file doesn't exist
            }

            // Create a new list with all waypoints including the new one
            val updatedWaypoints = waypoints + newWaypoint

            // Save the complete list back to the file
            waypointFile.writeText(json.encodeToString(updatedWaypoints))

            logger.info("Saved waypoint to ${waypointFile.absolutePath}")
        } catch (e: Exception) {
            logger.info("Error writing waypoint: ${e.message}")
        }
    }
}