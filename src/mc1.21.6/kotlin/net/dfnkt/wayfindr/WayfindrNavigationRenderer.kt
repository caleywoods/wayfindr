package net.dfnkt.wayfindr

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.math.Vec3d
import kotlin.math.atan2
import kotlin.math.sqrt
import org.joml.Matrix4f
import net.minecraft.util.math.RotationAxis

/**
 * Renders the navigation arrow and distance information for the currently selected waypoint.
 * Version-specific implementation for Minecraft 1.21.6
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
     * @param context The draw context for rendering
     */
    fun render(context: DrawContext) {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        
        // Check if we have a navigation target
        val targetWaypoint = WaypointManager.getNavigationTarget() ?: return
        
        // Only render if the player is in the same dimension as the waypoint
        if (player.world.registryKey.value.toString() != targetWaypoint.dimension) {
            return
        }
        
        val playerPos = player.pos
        
        // Check if player has reached the waypoint (within deadzone)
        if (WaypointManager.isWithinDeadzone(playerPos)) {
            // Player has reached the waypoint, turn off navigation
            WaypointManager.clearNavigationTarget()
            
            // Display a message to the player
            player.sendMessage(net.minecraft.text.Text.literal("You have reached your waypoint: ${targetWaypoint.name}"), true)
            return
        }
        
        // Calculate direction to waypoint
        val direction = calculateDirection(playerPos, targetWaypoint.getPosition(), player.yaw)
        
        // Calculate distance to waypoint
        val distance = calculateDistance(playerPos, targetWaypoint.getPosition())
        
        // Render the arrow
        renderArrow(context, direction)
        
        // Render the distance
        renderDistance(context, distance)
    }
    
    /**
     * Calculates the direction angle to the waypoint relative to the player's current orientation.
     * 
     * @param playerPos The player's position
     * @param waypointPos The waypoint's position
     * @param playerYaw The player's current yaw (horizontal rotation)
     * @return The angle in degrees to the waypoint, relative to the player's orientation
     */
    private fun calculateDirection(playerPos: Vec3d, waypointPos: Vec3d, playerYaw: Float): Float {
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
     * 
     * @param playerPos The player's position
     * @param waypointPos The waypoint's position
     * @return The distance to the waypoint in blocks
     */
    private fun calculateDistance(playerPos: Vec3d, waypointPos: Vec3d): Double {
        val dx = waypointPos.x - playerPos.x
        val dy = waypointPos.y - playerPos.y
        val dz = waypointPos.z - playerPos.z
        
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
    
    /**
     * Renders the navigation arrow on the screen.
     * 
     * @param context The draw context for rendering
     * @param direction The direction angle to the waypoint in degrees
     */
    private fun renderArrow(context: DrawContext, direction: Float) {
        val client = MinecraftClient.getInstance()
        val screenWidth = client.window.scaledWidth
        val screenHeight = client.window.scaledHeight
        
        // Position the arrow at the top center of the screen
        val centerX = screenWidth / 2
        val centerY = ARROW_SIZE + ARROW_PADDING
        
        val matrices = context.matrices
        // Save the current matrix state using the Matrix4f stack
        val matrixStack = matrices.peek().positionMatrix
        val savedMatrix = Matrix4f(matrixStack)
        
        // Move to the center position
        matrices.translate(centerX.toFloat(), centerY.toFloat(), 0f)
        
        // Rotate to point in the correct direction using the RotationAxis helper
        // Add 180 degrees to fix the direction issue
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(direction + 180f))
        
        // Draw a smooth arrow with glow effect
        val arrowSize = ARROW_SIZE * 0.8f
        
        // Draw glow effect first (larger triangle with semi-transparent color)
        drawSmoothTriangle(
            context,
            0f, -arrowSize / 2 - 2,                // Top point
            -arrowSize / 2 - 2, arrowSize / 2 + 2, // Bottom left
            arrowSize / 2 + 2, arrowSize / 2 + 2,  // Bottom right
            ARROW_GLOW_COLOR
        )
        
        // Draw the main arrow as a single white triangle
        drawSmoothTriangle(
            context,
            0f, -arrowSize / 2,                // Top point
            -arrowSize / 2, arrowSize / 2,     // Bottom left
            arrowSize / 2, arrowSize / 2,      // Bottom right
            ARROW_COLOR
        )
        
        // Restore the previous matrix state by setting it back
        matrices.loadIdentity()
        matrices.multiplyPositionMatrix(savedMatrix)
    }
    
    /**
     * Helper method to draw a smooth anti-aliased triangle.
     */
    private fun drawSmoothTriangle(
        context: DrawContext,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        color: Int
    ) {
        // Calculate bounding box
        val minX = minOf(x1, x2, x3).toInt() - 1
        val maxX = maxOf(x1, x2, x3).toInt() + 1
        val minY = minOf(y1, y2, y3).toInt() - 1
        val maxY = maxOf(y1, y2, y3).toInt() + 1
        
        // Extract color components
        val alpha = (color shr 24) and 0xFF
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        
        // For each pixel in the bounding box
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                // Perform supersampling anti-aliasing
                var coverage = 0f
                val step = 1.0f / ANTI_ALIASING_SAMPLES
                
                // Sample multiple points within the pixel
                for (sy in 0 until ANTI_ALIASING_SAMPLES) {
                    for (sx in 0 until ANTI_ALIASING_SAMPLES) {
                        val sampleX = x + (sx + 0.5f) * step
                        val sampleY = y + (sy + 0.5f) * step
                        
                        // Check if the sample point is inside the triangle
                        if (isPointInTriangle(sampleX, sampleY, x1, y1, x2, y2, x3, y3)) {
                            coverage += 1.0f / (ANTI_ALIASING_SAMPLES * ANTI_ALIASING_SAMPLES)
                        }
                    }
                }
                
                // Only draw if there's some coverage
                if (coverage > 0) {
                    // Calculate the final alpha based on coverage
                    val finalAlpha = (alpha * coverage).toInt().coerceIn(0, 255)
                    val finalColor = (finalAlpha shl 24) or (red shl 16) or (green shl 8) or blue
                    
                    // Draw the pixel
                    context.fill(x, y, x + 1, y + 1, finalColor)
                }
            }
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
     * Darkens a color by the given factor
     */
    private fun darkenColor(color: Int, factor: Float): Int {
        val a = (color shr 24) and 0xFF
        val r = ((color shr 16) and 0xFF) * factor
        val g = ((color shr 8) and 0xFF) * factor
        val b = (color and 0xFF) * factor
        
        return (a shl 24) or ((r.toInt() and 0xFF) shl 16) or ((g.toInt() and 0xFF) shl 8) or (b.toInt() and 0xFF)
    }
    
    /**
     * Lightens a color by the given factor
     */
    private fun lightenColor(color: Int, factor: Float): Int {
        val a = (color shr 24) and 0xFF
        val r = Math.min(255f, ((color shr 16) and 0xFF) * factor)
        val g = Math.min(255f, ((color shr 8) and 0xFF) * factor)
        val b = Math.min(255f, (color and 0xFF) * factor)
        
        return (a shl 24) or ((r.toInt() and 0xFF) shl 16) or ((g.toInt() and 0xFF) shl 8) or (b.toInt() and 0xFF)
    }
    
    /**
     * Helper method to draw a line with thickness
     */
    private fun drawLine(
        context: DrawContext,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        color: Int,
        thickness: Int
    ) {
        val dx = x2 - x1
        val dy = y2 - y1
        val length = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        
        if (length < 0.0001f) return
        
        val dirX = dx / length
        val dirY = dy / length
        val perpX = -dirY
        val perpY = dirX
        
        val halfThickness = thickness / 2f
        
        // Draw a thick line as a quad
        drawTriangle(
            context,
            x1 + perpX * halfThickness, y1 + perpY * halfThickness,
            x1 - perpX * halfThickness, y1 - perpY * halfThickness,
            x2 + perpX * halfThickness, y2 + perpY * halfThickness,
            color
        )
        
        drawTriangle(
            context,
            x2 + perpX * halfThickness, y2 + perpY * halfThickness,
            x1 - perpX * halfThickness, y1 - perpY * halfThickness,
            x2 - perpX * halfThickness, y2 - perpY * halfThickness,
            color
        )
    }
    
    /**
     * Helper method to draw a triangle.
     */
    private fun drawTriangle(
        context: DrawContext,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        color: Int
    ) {
        // Draw filled triangle using multiple horizontal lines
        val minY = minOf(y1, y2, y3).toInt()
        val maxY = maxOf(y1, y2, y3).toInt()
        
        for (y in minY..maxY) {
            val yf = y.toFloat()
            
            // Calculate intersections with each edge
            val intersections = mutableListOf<Float>()
            
            // Edge 1-2
            addIntersection(x1, y1, x2, y2, yf, intersections)
            
            // Edge 2-3
            addIntersection(x2, y2, x3, y3, yf, intersections)
            
            // Edge 3-1
            addIntersection(x3, y3, x1, y1, yf, intersections)
            
            // Draw horizontal line if we have exactly 2 intersections
            if (intersections.size >= 2) {
                intersections.sort()
                context.fill(
                    intersections[0].toInt(),
                    y,
                    intersections[1].toInt() + 1, // +1 to ensure the pixel is included
                    y + 1,
                    color
                )
            }
        }
    }
    
    /**
     * Helper method to find the intersection of a horizontal line with a line segment.
     */
    private fun addIntersection(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        y: Float,
        intersections: MutableList<Float>
    ) {
        // Check if the horizontal line at y intersects with the line segment
        if ((y1 <= y && y2 >= y) || (y1 >= y && y2 <= y)) {
            // Avoid division by zero
            if (y1 != y2) {
                val x = x1 + (x2 - x1) * (y - y1) / (y2 - y1)
                intersections.add(x)
            }
        }
    }
    
    /**
     * Renders the distance information on the screen.
     * 
     * @param context The draw context for rendering
     * @param distance The distance to the waypoint in blocks
     */
    private fun renderDistance(context: DrawContext, distance: Double) {
        val client = MinecraftClient.getInstance()
        val screenWidth = client.window.scaledWidth
        
        // Format the distance text
        val distanceText = "${distance.toInt()} blocks"
        
        // Calculate text position (centered below the arrow)
        val textWidth = client.textRenderer.getWidth(distanceText)
        val textX = (screenWidth - textWidth) / 2
        val textY = ARROW_SIZE + ARROW_PADDING * 2
        
        // Draw the text
        context.drawText(client.textRenderer, distanceText, textX, textY, DISTANCE_COLOR, true)
    }
}
