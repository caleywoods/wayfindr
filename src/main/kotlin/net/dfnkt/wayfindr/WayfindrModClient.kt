package net.dfnkt.wayfindr

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Vec3d
import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient

class WayfindrModClient : ClientModInitializer {
    // Test waypoint at coordinates (0, 95, 0), adjust if you need it higher or lower
    private val testWaypoint = Vec3d(0.0, 95.0, 0.0)

    override fun onInitializeClient() {
        val client = MinecraftClient.getInstance();
        HudRenderCallback.EVENT.register {drawContext, _ ->
            // This could eventually show the waypoint name above the point
            drawContext.drawText(client.textRenderer, "Example test", 100, 200, 0xFFFFFFFFu.toInt(), true);
        }
        // Register our render event
        WorldRenderEvents.AFTER_TRANSLUCENT.register { context ->
            val matrixStack = context.matrixStack() ?: return@register
            val player = context.camera().pos
            val distance = player.distanceTo(testWaypoint)

            // Only render if player is within 100 blocks
            if (distance <= 100) {
                renderWaypointMarker(matrixStack, testWaypoint, player)
            }

        }
    }

    private fun renderWaypointMarker(
        matrices: MatrixStack,
        waypointPos: Vec3d,
        playerPos: Vec3d
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
        val red = 1.0f
        val green = 0.0f
        val blue = 0.0f
        val alpha = 0.8f

        // Set up rendering state
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableCull()

        // Set the shader program key correctly for position color rendering
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)

        val tessellator = Tessellator.getInstance()
        // Create a new BufferBuilder with the tessellator's allocator
        val bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)

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