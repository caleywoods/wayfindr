package net.dfnkt.wayfindr

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.lwjgl.glfw.GLFW
import java.io.File
import org.slf4j.LoggerFactory

/**
 * How the waypoint manager list is ordered.
 * - [NAME_ASC] / [NAME_DESC]: alphabetical by name.
 * - [DISTANCE]: closest first, for waypoints in the player's current dimension;
 *   waypoints in other dimensions (no meaningful distance) sort after those, by name.
 */
@Serializable
enum class WaypointSortMode {
    NAME_ASC,
    NAME_DESC,
    DISTANCE
}

@Serializable
data class WayfindrConfig(
    val maxRenderDistance: Double = 200.0,
    val maxRaycastDistance: Double = 200.0,
    val openMenuKey: Int = GLFW.GLFW_KEY_M,
    val quickAddKey: Int = GLFW.GLFW_KEY_N,
    val createDeathWaypoint: Boolean = false,
    val enableTeleport: Boolean = true,
    val sortMode: WaypointSortMode = WaypointSortMode.NAME_ASC,
    val showTypeLabels: Boolean = false
) {
    companion object {
        private val minecraftDir = File(System.getProperty("user.home"), ".minecraft")
        private val modDir = File(minecraftDir, "config/wayfindr")
        private val configFile = File(modDir, "config.json")
        private val MOD_ID = "wayfindr"
        private val logger = LoggerFactory.getLogger(MOD_ID)
        // encodeDefaults so every field is written to disk, even when equal to its
        // default. Keeps the saved config explicit and unambiguous (e.g. an off toggle
        // is persisted as `false` rather than omitted). ignoreUnknownKeys so older/newer
        // config files still load.
        private val json = Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
        
        private var instance = WayfindrConfig()
        
        fun get(): WayfindrConfig {
            return instance
        }
        
        fun load() {
            try {
                if (configFile.exists()) {
                    val jsonContent = configFile.readText()
                    if (jsonContent.isNotBlank()) {
                        instance = json.decodeFromString(jsonContent)
                        logger.info("Loaded configuration from ${configFile.absolutePath}")
                    } else {
                        logger.info("Config file is empty, using defaults")
                        save()
                    }
                } else {
                    logger.info("No config file found at ${configFile.absolutePath}, creating default")
                    save()
                }
            } catch (e: Exception) {
                logger.error("Error loading config: ${e.message}")
                save()
            }
        }
        
        fun save() {
            try {
                if (!modDir.exists() && !modDir.mkdirs()) {
                    logger.error("Failed to create directory: ${modDir.absolutePath}")
                    return
                }

                configFile.writeText(json.encodeToString(instance))
                logger.info("Saved configuration to ${configFile.absolutePath}")
            } catch (e: Exception) {
                logger.error("Error saving config to ${configFile.absolutePath}", e)
            }
        }

        fun update(newConfig: WayfindrConfig) {
            instance = newConfig
            save()
        }
    }
}
