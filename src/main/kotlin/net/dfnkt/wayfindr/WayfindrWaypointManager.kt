package net.dfnkt.wayfindr

import net.minecraft.util.math.Vec3d
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Wrapper class for Vec3d serialization
@Serializable
data class SerializableVec3d(val x: Double, val y: Double, val z: Double) {
    constructor(vec3d: Vec3d) : this(vec3d.x, vec3d.y, vec3d.z)

    fun toVec3d(): Vec3d {
        return Vec3d(x, y, z)
    }
}

object WaypointManager {
    val waypoints = mutableListOf<Waypoint>()

    @Serializable
    data class Waypoint(
        val name: String,
        val position: SerializableVec3d, // Changed from Vec3d to SerializableVec3d
        val color: Int = 0xFF0000u.toInt(),
        val dimension: String = "minecraft:overworld",
    ) {
        // Constructor that accepts Vec3d
        constructor(name: String, position: Vec3d, color: Int = 0xFF0000u.toInt(), dimension: String = "minecraft:overworld") :
                this(name, SerializableVec3d(position), color, dimension)

        // Helper method to get the original Vec3d
        fun getPosition(): Vec3d {
            return position.toVec3d()
        }
    }

    fun addWaypoint(name: String, position: Vec3d): Waypoint {
        val waypoint = Waypoint(name, position)
        val jsonWaypoint = Json.encodeToString(waypoint)
        waypoints.add(waypoint)
        val saveManager = WayfindrSaveFileHandler()
        saveManager.saveWaypoint(jsonWaypoint)
        return waypoint
    }

    fun addWaypoint(name: String, position: Vec3d, color: Int): Waypoint {
        val waypoint = Waypoint(name, position, color)
        val jsonWaypoint = Json.encodeToString(waypoint)
        waypoints.add(waypoint)
        val saveManager = WayfindrSaveFileHandler()
        saveManager.saveWaypoint(jsonWaypoint)
        return waypoint
    }

    fun removeWaypoint(name: String): Boolean {
        return waypoints.removeIf { it.name == name }
    }

    fun getWaypoint(name: String): Waypoint? {
        return waypoints.find { it.name == name }
    }

    // Add method to load waypoints
    fun loadWaypoints(jsonString: String) {
        try {
            val loadedWaypoint = Json.decodeFromString<Waypoint>(jsonString)
            waypoints.add(loadedWaypoint)
        } catch (e: Exception) {
            println("Error loading waypoint: ${e.message}")
        }
    }
}