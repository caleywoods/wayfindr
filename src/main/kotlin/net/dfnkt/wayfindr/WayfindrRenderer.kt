package net.dfnkt.wayfindr

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Vec3d
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import org.joml.Matrix4f
import org.joml.Quaternionf

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

            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.disableCull()

            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)

            val tessellator = Tessellator.getInstance()
            val bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR)

            matrices.translate(-baseSize/2, 0f, -baseSize/2)

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

            val builtBuffer = bufferBuilder.end()
            BufferRenderer.drawWithGlobalProgram(builtBuffer)

            RenderSystem.enableCull()
            RenderSystem.disableBlend()

            matrices.pop()
            
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
            
            val baseScale = 0.025f
            
            val minScale = 0.025f
            val maxScale = 0.12f
            
            val scaleFactor = Math.sqrt(distance / 5.0).coerceIn(1.0, 6.0)
            
            val dynamicScale = (baseScale * scaleFactor).toFloat().coerceIn(minScale, maxScale)
            
            matrices.scale(-dynamicScale, -dynamicScale, dynamicScale)
            
            val textWidth = textRenderer.getWidth(waypointName)
            
            val matrix4f = matrices.peek().positionMatrix
            
            textRenderer.draw(
                waypointName,
                -textWidth / 2f,
                0f,
                0xFFFFFF,
                true,
                matrix4f,
                client.bufferBuilders.entityVertexConsumers,
                net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH,
                0,
                15728880
            )
            
            matrices.pop()
        }
    }

}