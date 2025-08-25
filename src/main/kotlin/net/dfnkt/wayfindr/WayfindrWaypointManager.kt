package net.dfnkt.wayfindr

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.minecraft.util.math.Vec3d
import org.slf4j.LoggerFactory

/**
 * Manages waypoints for the Wayfindr mod.
 * 
 * This singleton is responsible for storing, adding, removing, and manipulating waypoints.
 * It also handles persistence by interfacing with the [WayfindrSaveFileHandler].
 */
object WaypointManager {
    private val logger = LoggerFactory.getLogger("wayfindr")
    private val json = Json { prettyPrint = true }
    private val saveHandler = WayfindrSaveFileHandler
    
    /**
     * List of all waypoints currently loaded in the game.
     */
    var waypoints = mutableListOf<Waypoint>()
        private set
    
    /**
     * Initializes waypoints by loading them from the save file.
     */
    fun initializeWaypoints() {
        waypoints = saveHandler.loadWaypoints().toMutableList()
        logger.info("Loaded ${waypoints.size} waypoints")
    }
    
    /**
     * Adds a new waypoint with the given parameters.
     * 
     * @param name The name of the waypoint
     * @param position The 3D position of the waypoint
     * @param color The color of the waypoint in RGB format (default: red)
     * @param dimension The dimension of the waypoint (default: "minecraft:overworld")
     * @param visible Whether the waypoint should be visible (default: true)
     * @return The newly created waypoint
     */
    fun addWaypoint(name: String, position: Vec3d, color: Int = 0xFF0000, dimension: String = "minecraft:overworld", visible: Boolean = true): Waypoint {
        val waypoint = Waypoint(name, SerializableVec3d(position.x, position.y, position.z), color, dimension, visible)
        waypoints.add(waypoint)
        
        val waypointJson = json.encodeToString(waypoint)
        saveHandler.saveWaypoint(waypointJson)
        
        return waypoint
    }
    
    /**
     * Removes a waypoint with the given name.
     * 
     * @param name The name of the waypoint to remove
     * @return True if the waypoint was found and removed, false otherwise
     */
    fun removeWaypoint(name: String): Boolean {
        val waypoint = waypoints.find { it.name == name } ?: return false
        waypoints.remove(waypoint)
        saveHandler.saveAllWaypoints(waypoints)
        return true
    }
    
    /**
     * Renames a waypoint.
     * 
     * @param oldName The current name of the waypoint
     * @param newName The new name for the waypoint
     * @return True if the waypoint was found and renamed, false otherwise
     */
    fun renameWaypoint(oldName: String, newName: String): Boolean {
        val waypoint = waypoints.find { it.name == oldName } ?: return false
        waypoint.name = newName
        saveHandler.saveAllWaypoints(waypoints)
        return true
    }
    
    /**
     * Toggles the visibility of a waypoint.
     * 
     * @param name The name of the waypoint to toggle
     * @return True if the waypoint was found and its visibility toggled, false otherwise
     */
    fun toggleWaypointVisibility(name: String): Boolean {
        val waypoint = waypoints.find { it.name == name } ?: return false
        waypoint.visible = !waypoint.visible
        saveHandler.saveAllWaypoints(waypoints)
        return true
    }
    
    /**
     * Changes the color of a waypoint.
     * 
     * @param name The name of the waypoint
     * @param color The new color in RGB format
     * @return True if the waypoint was found and its color changed, false otherwise
     */
    fun changeWaypointColor(name: String, color: Int): Boolean {
        val waypoint = waypoints.find { it.name == name } ?: return false
        waypoint.color = color
        saveHandler.saveAllWaypoints(waypoints)
        return true
    }
    
    /**
     * Gets a waypoint by its name.
     * 
     * @param name The name of the waypoint to retrieve
     * @return The waypoint if found, null otherwise
     */
    fun getWaypoint(name: String): Waypoint? {
        return waypoints.find { it.name == name }
    }
    
    /**
     * Represents a waypoint in the game world.
     * 
     * @property name The name of the waypoint
     * @property position The 3D position of the waypoint
     * @property color The color of the waypoint in RGB format
     * @property dimension The dimension of the waypoint
     * @property visible Whether the waypoint is currently visible
     */
    @Serializable
    data class Waypoint(
        var name: String,
        val position: SerializableVec3d,
        var color: Int,
        var dimension: String,
        var visible: Boolean
    ) {
        /**
         * Gets the position as a Minecraft Vec3d.
         *
         * @return A Vec3d representation of this waypoint's position
         */
        fun getPosition(): Vec3d {
            return position.toVec3d()
        }
    }
    
    /**
     * Represents a 3D position for a waypoint.
     * 
     * @property x The x-coordinate
     * @property y The y-coordinate
     * @property z The z-coordinate
     */
    @Serializable
    data class SerializableVec3d(val x: Double, val y: Double, val z: Double) {
        /**
         * Converts this position to a Minecraft Vec3d.
         * 
         * @return A Vec3d representation of this position
         */
        fun toVec3d(): Vec3d {
            return Vec3d(x, y, z)
        }
    }
}