package net.dfnkt.wayfindr

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.minecraft.util.math.Vec3d
import org.slf4j.LoggerFactory
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import java.util.UUID
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

/**
 * Manages waypoints for the Wayfindr mod.
 * 
 * This singleton is responsible for storing, adding, removing, and manipulating waypoints.
 * It also handles persistence by interfacing with the [WayfindrSaveFileHandler].
 */
object WaypointManager {
    private val logger = LoggerFactory.getLogger("wayfindr")
    
    // Create a serializers module with the UUID serializer
    private val waypointSerializers = SerializersModule {
        contextual(UUID::class, UUIDSerializer)
    }
    
    // Initialize Json with the serializers module
    private val json = Json { 
        prettyPrint = true 
        serializersModule = waypointSerializers
        ignoreUnknownKeys = true
    }
    
    private val saveHandler = WayfindrSaveFileHandler
    
    /**
     * Deadzone threshold in blocks for each axis.
     * When the player is within this distance of the waypoint in all three dimensions,
     * they are considered to have reached the waypoint.
     */
    const val DEADZONE_THRESHOLD = 3.0
    
    /**
     * List of all waypoints currently loaded in the game.
     */
    var waypoints = mutableListOf<Waypoint>()
        private set
    
    /**
     * The currently active navigation waypoint, if any.
     */
    private var navigationTarget: Waypoint? = null
    
    /**
     * Stores the scroll position in the waypoint list UI.
     */
    private var waypointListScrollPosition = 0
    
    /**
     * The current world name.
     */
    private var currentWorldName: String = "default"
    
    /**
     * Initializes waypoints by loading them from the save file.
     * Also registers world change listeners.
     */
    fun initializeWaypoints() {
        // Register world change listener
        ClientLifecycleEvents.CLIENT_STARTED.register { client ->
            // Initial load
            loadWaypointsForCurrentWorld()
        }
        
        // Load waypoints for the current world
        loadWaypointsForCurrentWorld()
    }
    
    /**
     * Loads waypoints for the current world.
     * This should be called when the player changes worlds.
     */
    fun loadWaypointsForCurrentWorld() {
        val newWorldName = saveHandler.getCurrentWorldName()
        
        // Only reload if the world has changed
        if (currentWorldName != newWorldName) {
            logger.info("World changed from '$currentWorldName' to '$newWorldName', reloading waypoints")
            currentWorldName = newWorldName
            
            // Clear navigation target when changing worlds
            navigationTarget = null
        }
        
        waypoints = saveHandler.loadWaypoints().toMutableList()
        logger.info("Loaded ${waypoints.size} waypoints for world '$currentWorldName'")
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
     * Adds a pre-constructed waypoint to the waypoint list.
     * 
     * @param waypoint The waypoint object to add
     * @return The added waypoint
     */
    fun addWaypoint(waypoint: Waypoint): Waypoint {
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
     * Removes a waypoint with the given UUID.
     * 
     * @param id The UUID of the waypoint to remove
     * @return True if the waypoint was found and removed, false otherwise
     */
    fun removeWaypoint(id: UUID): Boolean {
        val waypoint = waypoints.find { it.id == id } ?: return false
        waypoints.remove(waypoint)
        saveHandler.saveAllWaypoints(waypoints)
        return true
    }
    
    /**
     * Removes a waypoint at the specified index.
     * 
     * @param index The index of the waypoint to remove
     * @return True if the index was valid and the waypoint was removed, false otherwise
     */
    fun removeWaypoint(index: Int): Boolean {
        if (index < 0 || index >= waypoints.size) return false
        waypoints.removeAt(index)
        saveHandler.saveAllWaypoints(waypoints)
        return true
    }
    
    /**
     * Updates an existing waypoint with new data.
     * 
     * @param waypoint The waypoint with updated data
     * @return True if the waypoint was found and updated, false otherwise
     */
    fun updateWaypoint(waypoint: Waypoint): Boolean {
        val index = waypoints.indexOfFirst { it.id == waypoint.id }
        if (index < 0) return false
        
        waypoints[index] = waypoint
        saveHandler.saveAllWaypoints(waypoints)
        return true
    }
    
    /**
     * Gets all waypoints currently managed.
     * 
     * @return A list of all waypoints
     */
    fun getAllWaypoints(): List<Waypoint> {
        return waypoints.toList()
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
     * Gets a waypoint by its unique ID.
     * 
     * @param id The UUID of the waypoint to retrieve
     * @return The waypoint if found, null otherwise
     */
    fun getWaypoint(id: UUID): Waypoint? {
        return waypoints.find { it.id == id }
    }
    
    /**
     * Sets a waypoint as the current navigation target.
     *
     * @param name The name of the waypoint to navigate to
     * @return True if the waypoint was found and set as navigation target, false otherwise
     */
    fun setNavigationTarget(name: String): Boolean {
        val waypoint = waypoints.find { it.name == name } ?: return false
        navigationTarget = waypoint
        return true
    }
    
    /**
     * Clears the current navigation target.
     */
    fun clearNavigationTarget() {
        navigationTarget = null
    }
    
    /**
     * Gets the current navigation target waypoint, if any.
     *
     * @return The current navigation target waypoint, or null if none is set
     */
    fun getNavigationTarget(): Waypoint? {
        return navigationTarget
    }
    
    /**
     * Sets the waypoint list scroll position.
     *
     * @param position The scroll position to save
     */
    fun setWaypointListScrollPosition(position: Int) {
        waypointListScrollPosition = position
    }
    
    /**
     * Gets the saved waypoint list scroll position.
     *
     * @return The saved scroll position
     */
    fun getWaypointListScrollPosition(): Int {
        return waypointListScrollPosition
    }
    
    /**
     * Checks if the given waypoint is the current navigation target.
     *
     * @param name The name of the waypoint to check
     * @return True if the waypoint is the current navigation target, false otherwise
     */
    fun isNavigationTarget(name: String): Boolean {
        return navigationTarget?.name == name
    }
    
    /**
     * Checks if the player is within the deadzone of the current navigation target.
     * The player must be within the threshold distance in all three dimensions (X, Y, Z).
     *
     * @param playerPos The current position of the player
     * @return True if the player is within the deadzone, false otherwise or if no navigation target is set
     */
    fun isWithinDeadzone(playerPos: Vec3d): Boolean {
        val target = navigationTarget ?: return false
        val waypointPos = target.getPosition()
        
        // Check if the player is within the threshold distance in all three dimensions
        val withinX = Math.abs(playerPos.x - waypointPos.x) <= DEADZONE_THRESHOLD
        val withinY = Math.abs(playerPos.y - waypointPos.y) <= DEADZONE_THRESHOLD
        val withinZ = Math.abs(playerPos.z - waypointPos.z) <= DEADZONE_THRESHOLD
        
        return withinX && withinY && withinZ
    }
    
    /**
     * Gets the current world name.
     *
     * @return The name of the current world
     */
    fun getCurrentWorldName(): String {
        return currentWorldName
    }
    
    /**
     * Replaces all waypoints with the given list.
     * This is used for syncing with the server.
     * 
     * @param newWaypoints The new list of waypoints
     */
    fun replaceAllWaypoints(newWaypoints: List<Waypoint>) {
        waypoints = newWaypoints.toMutableList()
        saveHandler.saveAllWaypoints(waypoints)
    }
    
    /**
     * Represents a waypoint in the game world.
     * 
     * @property name The name of the waypoint
     * @property position The 3D position of the waypoint
     * @property color The color of the waypoint in RGB format
     * @property dimension The dimension of the waypoint
     * @property visible Whether the waypoint is currently visible
     * @property isShared Whether this waypoint is shared with other players on the server
     * @property owner UUID of the player who created this waypoint (null for local waypoints)
     * @property id Unique identifier for this waypoint, used for server synchronization
     */
    @Serializable
    data class Waypoint(
        var name: String,
        val position: SerializableVec3d,
        var color: Int,
        var dimension: String,
        var visible: Boolean,
        var isShared: Boolean = false,
        @Serializable(with = UUIDSerializer::class) var owner: UUID? = null,
        @Serializable(with = UUIDSerializer::class) var id: UUID = UUID.randomUUID()
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