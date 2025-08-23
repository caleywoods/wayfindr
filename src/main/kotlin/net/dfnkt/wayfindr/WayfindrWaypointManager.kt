package net.dfnkt.wayfindr

import net.minecraft.util.math.Vec3d
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
        val position: SerializableVec3d,
        val color: Int = 0xFF0000u.toInt(),
        val dimension: String = "minecraft:overworld",
    ) {
        constructor(name: String, position: Vec3d, color: Int = 0xFF0000u.toInt(), dimension: String = "minecraft:overworld") :
                this(name, SerializableVec3d(position), color, dimension)

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
        val removed = waypoints.removeIf { it.name == name }
        if (removed) {
            val saveManager = WayfindrSaveFileHandler()
            saveManager.saveAllWaypoints(waypoints)
        }
        return removed
    }

    fun getWaypoint(name: String): Waypoint? {
        return waypoints.find { it.name == name }
    }

    fun initializeWaypoints() {
        println("[Wayfindr] Starting to initialize waypoints")
        val saveManager = WayfindrSaveFileHandler()
        val savedWaypoints = saveManager.loadWaypoints()
        waypoints.clear()
        waypoints.addAll(savedWaypoints)
        println("[Wayfindr] Loaded ${savedWaypoints.size} waypoints from save file")
        
        savedWaypoints.forEach { waypoint ->
            println("[Wayfindr] Loaded waypoint: ${waypoint.name} at ${waypoint.position.x}, ${waypoint.position.y}, ${waypoint.position.z}")
        }
    }

    fun loadWaypoints(waypointList: List<Waypoint>) {
        waypoints.clear()
        waypoints.addAll(waypointList)
    }
}