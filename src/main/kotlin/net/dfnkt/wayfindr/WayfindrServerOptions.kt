package net.dfnkt.wayfindr

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.util.UUID

/**
 * On-disk shape of the optional server options file.
 *
 * @property sharingEnabled When false, the server rejects all waypoint-share requests.
 * @property shareDenylist Players not allowed to share waypoints. Each entry may be a
 *   player UUID string or a player name (matched case-insensitively) for the server
 *   owner's convenience.
 */
@Serializable
data class WayfindrServerOptionsData(
    val sharingEnabled: Boolean = true,
    val shareDenylist: List<String> = emptyList()
)

/**
 * Server-owner controls for waypoint sharing, read from an optional JSON file in the
 * server world root (`wayfindr_server_options.json`).
 *
 * The file is intentionally never created by the mod: if it is absent (or unreadable),
 * the mod runs with permissive defaults (sharing enabled, empty denylist). Owners opt in
 * by creating the file themselves. This keeps a stock install behaving exactly as before.
 */
object WayfindrServerOptions {
    private val logger = LoggerFactory.getLogger("wayfindr")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private const val OPTIONS_FILE = "wayfindr_server_options.json"

    @Volatile
    private var options = WayfindrServerOptionsData()

    /**
     * Loads the options file if it exists. Called on server start. Never writes the file.
     */
    fun initialize(server: MinecraftServer) {
        val file = getFile(server)
        if (!file.exists()) {
            options = WayfindrServerOptionsData()
            logger.info("No server options file at ${file.absolutePath}; using defaults (sharing enabled, no denylist). The mod does not create this file.")
            return
        }

        options = try {
            val content = Files.readString(file.toPath())
            if (content.isBlank()) {
                logger.info("Server options file is empty; using defaults")
                WayfindrServerOptionsData()
            } else {
                json.decodeFromString<WayfindrServerOptionsData>(content)
            }
        } catch (e: Exception) {
            logger.error("Failed to read ${file.name}; using defaults", e)
            WayfindrServerOptionsData()
        }

        logger.info("Loaded Wayfindr server options: sharingEnabled=${options.sharingEnabled}, denylist=${options.shareDenylist.size} entr${if (options.shareDenylist.size == 1) "y" else "ies"}")
    }

    /** Whether players are allowed to share waypoints at all. */
    fun isSharingEnabled(): Boolean = options.sharingEnabled

    /**
     * Whether the given player is barred from sharing. Matches denylist entries against
     * the player's UUID or name, case-insensitively.
     */
    fun isSharingDenied(uuid: UUID, name: String): Boolean {
        if (options.shareDenylist.isEmpty()) return false
        val uuidStr = uuid.toString()
        return options.shareDenylist.any { it.equals(uuidStr, ignoreCase = true) || it.equals(name, ignoreCase = true) }
    }

    private fun getFile(server: MinecraftServer): File {
        val worldDirectory = server.getWorldPath(LevelResource.ROOT).toFile()
        return File(worldDirectory, OPTIONS_FILE)
    }
}
