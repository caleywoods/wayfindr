package net.dfnkt.wayfindr

/*? if minecraft: <=1.21.4 {*/
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.VertexFormat
/*?} else*//*
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.render.RenderLayer
*/
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Vec3d

class WayfindrRenderer {
    companion object {
        fun renderWaypointMarker(
            matrices: MatrixStack,
            waypointPos: Vec3d,
            playerPos: Vec3d,
            color: Int = 0xFF0000,
            waypointName: String = ""
        ) {
            val distance = playerPos.distanceTo(waypointPos)
            matrices.push()

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

            val beamWidth = 0.2f
            val beamHeight = 50f
            val baseSize = 0.5f

            /*? if <=1.21.4 {*/
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.disableCull()
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)
            RenderSystem.enableDepthTest()
            RenderSystem.depthMask(true)
            /*?}*/

            val tessellator = Tessellator.getInstance()
            val bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)

            // Position the beam to start at waypoint level and extend upward
            matrices.translate(-baseSize/2, 0f, -baseSize/2)
            matrices.scale(beamWidth, beamHeight, beamWidth)

            // Adjust the vertices to make the beam start at the bottom and extend upward
            val matrix = matrices.peek().positionMatrix

            // Top face (Y+)
            bufferBuilder.vertex(matrix, -size, 1.0f, -size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, 1.0f, -size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, 1.0f, size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, -size, 1.0f, size).color(red, green, blue, alpha)

            // Bottom face (Y-)
            bufferBuilder.vertex(matrix, -size, 0.0f, size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, 0.0f, size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, 0.0f, -size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, -size, 0.0f, -size).color(red, green, blue, alpha)

            // North face (Z-)
            bufferBuilder.vertex(matrix, -size, 0.0f, -size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, 0.0f, -size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, 1.0f, -size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, -size, 1.0f, -size).color(red, green, blue, alpha)

            // South face (Z+)
            bufferBuilder.vertex(matrix, -size, 1.0f, size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, 1.0f, size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, 0.0f, size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, -size, 0.0f, size).color(red, green, blue, alpha)

            // West face (X-)
            bufferBuilder.vertex(matrix, -size, 0.0f, size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, -size, 0.0f, -size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, -size, 1.0f, -size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, -size, 1.0f, size).color(red, green, blue, alpha)

            // East face (X+)
            bufferBuilder.vertex(matrix, size, 1.0f, size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, 1.0f, -size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, 0.0f, -size).color(red, green, blue, alpha)
            bufferBuilder.vertex(matrix, size, 0.0f, size).color(red, green, blue, alpha)

            val builtBuffer = bufferBuilder.end()
            /*? if minecraft: <=1.21.4 {*/
            BufferRenderer.drawWithGlobalProgram(builtBuffer)
            /*?} else*//*
            RenderLayer.getDebugQuads().draw(builtBuffer)
            */

            matrices.pop()

            /*? if <=1.21.4 {*/
            // Restore state
            RenderSystem.enableCull()
            RenderSystem.disableBlend()
            /*?}*/

            if (waypointName.isNotEmpty()) {
                renderWaypointName(matrices, waypointPos, playerPos, waypointName, color, distance)
            }
        }

        private fun renderWaypointName(
            matrices: MatrixStack,
            waypointPos: Vec3d,
            playerPos: Vec3d,
            waypointName: String,
            @Suppress("UNUSED_PARAMETER") color: Int,
            distance: Double = playerPos.distanceTo(waypointPos)
        ) {
            val client = MinecraftClient.getInstance()
            val textRenderer = client.textRenderer

            matrices.push()

            matrices.translate(
                waypointPos.x - playerPos.x,
                waypointPos.y - playerPos.y + 2.0,
                waypointPos.z - playerPos.z
            )

            val camera = client.gameRenderer.camera

            val directionToCamera = Vec3d(
                camera.pos.x - waypointPos.x,
                camera.pos.y - waypointPos.y,
                camera.pos.z - waypointPos.z
            ).normalize()

            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180f))

            val yaw = Math.toDegrees(Math.atan2(directionToCamera.x, directionToCamera.z)).toFloat()
            val pitch = Math.toDegrees(Math.asin(directionToCamera.y)).toFloat()

            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(yaw))
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(pitch))

            val baseScale = 0.05f
            val minScale = 0.05f
            val maxScale = 0.25f

            val scaleFactor = Math.sqrt(distance / 3.0).coerceIn(1.0, 8.0)
            val dynamicScale = (baseScale * scaleFactor).toFloat().coerceIn(minScale, maxScale)

            matrices.scale(-dynamicScale, -dynamicScale, dynamicScale)

            val textWidth = textRenderer.getWidth(waypointName)
            val matrix4f = matrices.peek().positionMatrix

            val bgColor = 0x80000000.toInt()

            textRenderer.draw(
                waypointName,
                -textWidth / 2f,
                0f,
                0xFFFFFF,
                true,
                matrix4f,
                client.bufferBuilders.entityVertexConsumers,
                net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH,
                bgColor,
                15728880
            )

            matrices.pop()
        }
    }
}
