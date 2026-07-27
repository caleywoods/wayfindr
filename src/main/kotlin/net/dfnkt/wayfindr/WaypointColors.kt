package net.dfnkt.wayfindr

import kotlin.random.Random

/** Color helpers for waypoints. */
object WaypointColors {
    /**
     * A random, reasonably bright RGB color (each channel in [100, 255]) so newly created
     * waypoints are visually distinct rather than all defaulting to the same color.
     */
    fun random(): Int {
        val red = Random.nextInt(100, 256)
        val green = Random.nextInt(100, 256)
        val blue = Random.nextInt(100, 256)
        return (red shl 16) or (green shl 8) or blue
    }
}
