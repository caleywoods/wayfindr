package net.dfnkt.wayfindr

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Vec3d

class WayfindrRenderer {
    companion object {
        fun renderWaypointMarker(
            matrices: MatrixStack,
            waypointPos: Vec3d,
            playerPos: Vec3d,
            color: Int = 0xFF0000
        ) {
            // Save the current transformation state
            matrices.push()

            // Move to waypoint position relative to player
            matrices.translate(
                waypointPos.x - playerPos.x,
                waypointPos.y - playerPos.y,
                waypointPos.z - playerPos.z
            )

            val size = 0.5f
            val red = ((color shr 16) and 0xFF) / 255f
            val green = ((color shr 8) and 0xFF) / 255f
            val blue = (color and 0xFF) / 255f
            val alpha = 0.7f

            val beamWidth = 0.2f  // Width of the beam (X and Z axes)
            val beamHeight = 50f  // Height of the beam (Y axis)
            val baseSize = 0.5f   // Size of the base cube

            // Set up rendering state
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.disableCull()

            // Set the shader program key correctly for position color rendering
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)

            val tessellator = Tessellator.getInstance()
            // Create a new BufferBuilder with the tessellator's allocator
            val bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)

            // center the beam horizontally at the waypoint position
            matrices.translate(-baseSize/2, 0f, -baseSize/2)

            // Scale to create a tall and thin beam
            matrices.scale(beamWidth, beamHeight, beamWidth)

            val matrix = matrices.peek().positionMatrix

            // Top face (Y+)
            bufferBuilder.vertex(matrix, -size, size, -size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, size, -size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, size, size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, -size, size, size).color(red, green, blue, alpha)

            // Bottom face (Y-)
            bufferBuilder.vertex(matrix, -size, -size, size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, -size, size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, -size, -size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, -size, -size, -size).color(red, green, blue, alpha)

            // North face (Z-)
            bufferBuilder.vertex(matrix, -size, -size, -size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, -size, -size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, size, -size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, -size, size, -size).color(red, green, blue, alpha)

            // South face (Z+)
            bufferBuilder.vertex(matrix, -size, size, size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, size, size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, -size, size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, -size, -size, size).color(red, green, blue, alpha)

            // West face (X-)
            bufferBuilder.vertex(matrix, -size, -size, size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, -size, -size, -size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, -size, size, -size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, -size, size, size).color(red, green, blue, alpha)

            // East face (X+)
            bufferBuilder.vertex(matrix, size, size, size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, size, -size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, -size, -size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, -size, size).color(red, green, blue, alpha)

            // Draw all vertices
            val builtBuffer = bufferBuilder.end()
            BufferRenderer.drawWithGlobalProgram(builtBuffer)

            // Restore rendering state
            RenderSystem.enableCull()
            RenderSystem.disableBlend()

            // Restore the transformation state
            matrices.pop()
        }
    }

}