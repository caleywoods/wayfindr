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
            if (!modDir.exists() && !modDir.mkdirs()) {
                logger.info("Failed to create directory: ${modDir.absolutePath}")
                return
            }

            val newWaypoint = json.decodeFromString<WaypointManager.Waypoint>(waypointJson)

            val waypoints = if (waypointFile.exists()) {
                try {
                    json.decodeFromString<List<WaypointManager.Waypoint>>(waypointFile.readText())
                } catch (e: Exception) {
                    logger.info("Error reading existing waypoints, creating new file: ${e.message}")
                    listOf()
                }
            } else {
                listOf()
            }

            val updatedWaypoints = waypoints + newWaypoint

            waypointFile.writeText(json.encodeToString(updatedWaypoints))

            logger.info("Saved waypoint to ${waypointFile.absolutePath}")
        } catch (e: Exception) {
            logger.info("Error writing waypoint: ${e.message}")
        }
    }

    fun loadWaypoints(): List<WaypointManager.Waypoint> {
        return try {
            if (waypointFile.exists()) {
                val jsonContent = waypointFile.readText()
                if (jsonContent.isNotBlank()) {
                    json.decodeFromString<List<WaypointManager.Waypoint>>(jsonContent)
                } else {
                    logger.info("Waypoint file is empty")
                    listOf()
                }
            } else {
                logger.info("No waypoint file found at ${waypointFile.absolutePath}")
                listOf()
            }
        } catch (e: Exception) {
            logger.info("Error loading waypoints: ${e.message}")
            listOf()
        }
    }

    fun saveAllWaypoints(waypoints: List<WaypointManager.Waypoint>) {
        try {
            if (!modDir.exists() && !modDir.mkdirs()) {
                logger.info("Failed to create directory: ${modDir.absolutePath}")
                return
            }

            waypointFile.writeText(json.encodeToString(waypoints))
            logger.info("Saved ${waypoints.size} waypoints to ${waypointFile.absolutePath}")
        } catch (e: Exception) {
            logger.info("Error saving waypoints: ${e.message}")
        }
    }
}