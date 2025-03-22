package net.dfnkt.wayfindr

import net.minecraft.util.math.Vec3d

object WaypointManager {
    val waypoints = mutableListOf<Waypoint>()

    data class Waypoint(
        val name: String,
        val position: Vec3d,
        val color: Int = 0xFF0000u.toInt(),
        val dimension: String = "minecraft:overworld",
    )

    fun addWaypoint(name: String, position: Vec3d): Waypoint {
        val waypoint = Waypoint(name, position)
        waypoints.add(waypoint)
        return waypoint
    }

    fun removeWaypoint(name: String): Boolean {
        return waypoints.removeIf { it.name == name }
    }

    fun getWaypoint(name: String): Waypoint? {
        return waypoints.find { it.name == name }
    }
}