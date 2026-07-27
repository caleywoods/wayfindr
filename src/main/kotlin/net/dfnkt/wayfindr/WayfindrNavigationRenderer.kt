package net.dfnkt.wayfindr

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Renders the navigation arrow and distance information for the currently selected waypoint.
 */
object WayfindrNavigationRenderer {
    private const val ARROW_SIZE = 14
    private const val ARROW_PADDING = 10
    private const val ARROW_COLOR = 0xFFFFFFFF.toInt()
    private const val ARROW_GLOW_COLOR = 0x99FFFFFF.toInt() // Semi-transparent white for glow
    private const val DISTANCE_COLOR = 0xFFFFFFFF.toInt()
    private const val ANTI_ALIASING_SAMPLES = 8

    /**
     * Renders the navigation arrow and distance information on the screen.
     *
     * @param context The GUI graphics context for rendering
     */
    fun render(context: GuiGraphicsExtractor) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return

        // Check if we have a navigation target
        val targetWaypoint = WaypointManager.getNavigationTarget() ?: return

        // Only render if the player is in the same dimension as the waypoint
        if (player.level().dimension().identifier().toString() != targetWaypoint.dimension) {
            return
        }

        val playerPos = player.position()

        // Check if player has reached the waypoint (within deadzone)
        if (WaypointManager.isWithinDeadzone(playerPos)) {
            // Player has reached the waypoint, turn off navigation
            WaypointManager.clearNavigationTarget()

            // Display a message to the player
            player.sendSystemMessage(Component.literal("You have reached your waypoint: ${targetWaypoint.name}"))
            return
        }

        // Calculate direction to waypoint
        val direction = calculateDirection(playerPos, targetWaypoint.getPosition(), player.yRot)

        // Calculate distance to waypoint
        val distance = calculateDistance(playerPos, targetWaypoint.getPosition())

        // Render the arrow
        renderArrow(context, direction)

        // Render the distance
        renderDistance(context, distance)
    }

    /**
     * Calculates the direction angle to the waypoint relative to the player's current orientation.
     */
    private fun calculateDirection(playerPos: Vec3, waypointPos: Vec3, playerYaw: Float): Float {
        val dx = waypointPos.x - playerPos.x
        val dz = waypointPos.z - playerPos.z

        // Calculate the angle to the waypoint in the world
        val angleToWaypoint = Math.toDegrees(atan2(dz, dx)).toFloat()

        // Adjust for player's rotation
        var relativeAngle = angleToWaypoint - playerYaw + 90

        // Normalize to 0-360 degrees
        while (relativeAngle > 360) relativeAngle -= 360
        while (relativeAngle < 0) relativeAngle += 360

        return relativeAngle
    }

    /**
     * Calculates the distance to the waypoint.
     */
    private fun calculateDistance(playerPos: Vec3, waypointPos: Vec3): Double {
        val dx = waypointPos.x - playerPos.x
        val dy = waypointPos.y - playerPos.y
        val dz = waypointPos.z - playerPos.z

        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Renders the navigation arrow on the screen.
     */
    private fun renderArrow(context: GuiGraphicsExtractor, direction: Float) {
        val client = Minecraft.getInstance()
        val screenWidth = client.window.guiScaledWidth

        // Position the arrow at the top center of the screen
        val centerX = screenWidth / 2
        val centerY = ARROW_SIZE + ARROW_PADDING

        val pose = context.pose()
        pose.pushMatrix()

        // Move to the center position
        pose.translate(centerX.toFloat(), centerY.toFloat())

        // Rotate to point in the correct direction (add 180 to fix orientation)
        pose.rotate(Math.toRadians((direction + 180f).toDouble()).toFloat())

        // Replay the pre-baked anti-aliased arrow (glow + arrow). The pose rotation above
        // orients it; the pixel coverage itself was computed once (see [arrowRuns]).
        for (run in arrowRuns) {
            context.fill(run.xStart, run.y, run.xEnd, run.y + 1, run.color)
        }

        pose.popMatrix()
    }

    /** A horizontal run of identically-colored pixels in the baked arrow (xEnd exclusive). */
    private data class ArrowRun(val y: Int, val xStart: Int, val xEnd: Int, val color: Int)

    /**
     * The arrow is a fixed shape; only its on-screen rotation changes (applied via the pose
     * matrix). So we rasterize the anti-aliased glow + arrow triangles exactly once, coalesce
     * each row into runs, and replay those every frame. Keeps the smooth edges the AA gave us
     * without re-rasterizing 64 samples/pixel every frame.
     */
    private val arrowRuns: List<ArrowRun> by lazy { buildArrowRuns() }

    private fun buildArrowRuns(): List<ArrowRun> {
        val arrowSize = ARROW_SIZE * 0.8f
        val runs = ArrayList<ArrowRun>()
        // Glow first (drawn under), then the main arrow on top — same order as before.
        rasterizeTriangleRuns(runs,
            0f, -arrowSize / 2 - 2, -arrowSize / 2 - 2, arrowSize / 2 + 2, arrowSize / 2 + 2, arrowSize / 2 + 2,
            ARROW_GLOW_COLOR)
        rasterizeTriangleRuns(runs,
            0f, -arrowSize / 2, -arrowSize / 2, arrowSize / 2, arrowSize / 2, arrowSize / 2,
            ARROW_COLOR)
        return runs
    }

    /** Coverage-samples a triangle (as the old drawSmoothTriangle did) into coalesced row runs. */
    private fun rasterizeTriangleRuns(
        out: MutableList<ArrowRun>,
        x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float,
        color: Int
    ) {
        val minX = minOf(x1, x2, x3).toInt() - 1
        val maxX = maxOf(x1, x2, x3).toInt() + 1
        val minY = minOf(y1, y2, y3).toInt() - 1
        val maxY = maxOf(y1, y2, y3).toInt() + 1

        val alpha = (color shr 24) and 0xFF
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        val step = 1.0f / ANTI_ALIASING_SAMPLES
        val sampleWeight = 1.0f / (ANTI_ALIASING_SAMPLES * ANTI_ALIASING_SAMPLES)

        for (y in minY..maxY) {
            var runStart = minX
            var runColor = 0 // 0 == transparent, breaks a run
            for (x in minX..maxX) {
                var coverage = 0f
                for (sy in 0 until ANTI_ALIASING_SAMPLES) {
                    for (sx in 0 until ANTI_ALIASING_SAMPLES) {
                        val sampleX = x + (sx + 0.5f) * step
                        val sampleY = y + (sy + 0.5f) * step
                        if (isPointInTriangle(sampleX, sampleY, x1, y1, x2, y2, x3, y3)) {
                            coverage += sampleWeight
                        }
                    }
                }
                val pxColor = if (coverage > 0f) {
                    val finalAlpha = (alpha * coverage).toInt().coerceIn(0, 255)
                    (finalAlpha shl 24) or (red shl 16) or (green shl 8) or blue
                } else 0

                if (pxColor != runColor) {
                    if (runColor != 0) out.add(ArrowRun(y, runStart, x, runColor))
                    runColor = pxColor
                    runStart = x
                }
            }
            if (runColor != 0) out.add(ArrowRun(y, runStart, maxX + 1, runColor))
        }
    }

    /**
     * Helper method to check if a point is inside a triangle.
     */
    private fun isPointInTriangle(
        px: Float, py: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float
    ): Boolean {
        // Compute barycentric coordinates
        val denominator = ((y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3))
        if (denominator == 0f) return false

        val a = ((y2 - y3) * (px - x3) + (x3 - x2) * (py - y3)) / denominator
        val b = ((y3 - y1) * (px - x3) + (x1 - x3) * (py - y3)) / denominator
        val c = 1 - a - b

        // Check if point is inside triangle
        return a >= 0f && a <= 1f && b >= 0f && b <= 1f && c >= 0f && c <= 1f
    }

    /**
     * Renders the distance information on the screen.
     */
    private fun renderDistance(context: GuiGraphicsExtractor, distance: Double) {
        val client = Minecraft.getInstance()
        val screenWidth = client.window.guiScaledWidth

        // Format the distance text
        val distanceText = "${distance.toInt()} blocks"

        // Calculate text position (centered below the arrow)
        val textWidth = client.font.width(distanceText)
        val textX = (screenWidth - textWidth) / 2
        val textY = ARROW_SIZE + ARROW_PADDING * 2

        // Draw the text
        context.text(client.font, distanceText, textX, textY, DISTANCE_COLOR, true)
    }
}
