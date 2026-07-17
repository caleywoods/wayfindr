package net.dfnkt.wayfindr

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.world.phys.Vec3

/**
 * Renders waypoint markers (vertical beams) in the world using the 26.x submit-node
 * rendering system.
 *
 * The floating name labels are drawn separately on the HUD (see [WaypointLabels]) because
 * in-world text goes through entity feature-render phases that are not flushed for
 * submissions made from a mod's level-render event.
 */
class WayfindrRenderer {
    companion object {
        /**
         * Draws a vertical beam at the given waypoint position.
         *
         * @param poseStack   The level render pose stack (positioned at the camera).
         * @param collector   The submit-node collector for this frame.
         * @param cameraPos   The camera position in world space.
         * @param waypointPos The waypoint position in world space.
         * @param color       RGB color (0xRRGGBB).
         */
        fun renderWaypointMarker(
            poseStack: PoseStack,
            collector: SubmitNodeCollector,
            cameraPos: Vec3,
            waypointPos: Vec3,
            color: Int = 0xFF0000
        ) {
            val size = 0.5f
            val red = (color shr 16) and 0xFF
            val green = (color shr 8) and 0xFF
            val blue = color and 0xFF
            val alpha = 178 // ~0.7

            val beamWidth = 0.2f
            val beamHeight = 50f
            val baseSize = 0.5f

            poseStack.pushPose()

            // Translate from the camera to the waypoint position.
            poseStack.translate(
                waypointPos.x - cameraPos.x,
                waypointPos.y - cameraPos.y,
                waypointPos.z - cameraPos.z
            )

            poseStack.translate(-baseSize / 2.0, 0.0, -baseSize / 2.0)
            poseStack.scale(beamWidth, beamHeight, beamWidth)

            collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads()) { pose, vc ->
                // Top face (Y+)
                vc.addVertex(pose, -size, 1.0f, -size).setColor(red, green, blue, alpha)
                vc.addVertex(pose, size, 1.0f, -size).setColor(red, green, blue, alpha)
                vc.addVertex(pose, size, 1.0f, size).setColor(red, green, blue, alpha)
                vc.addVertex(pose, -size, 1.0f, size).setColor(red, green, blue, alpha)

                // Bottom face (Y-)
                vc.addVertex(pose, -size, 0.0f, size).setColor(red, green, blue, alpha)
                vc.addVertex(pose, size, 0.0f, size).setColor(red, green, blue, alpha)
                vc.addVertex(pose, size, 0.0f, -size).setColor(red, green, blue, alpha)
                vc.addVertex(pose, -size, 0.0f, -size).setColor(red, green, blue, alpha)

                // North face (Z-)
                vc.addVertex(pose, -size, 0.0f, -size).setColor(red, green, blue, alpha)
                vc.addVertex(pose, size, 0.0f, -size).setColor(red, green, blue, alpha)
                vc.addVertex(pose, size, 1.0f, -size).setColor(red, green, blue, alpha)
                vc.addVertex(pose, -size, 1.0f, -size).setColor(red, green, blue, alpha)

                // South face (Z+)
                vc.addVertex(pose, -size, 1.0f, size).setColor(red, green, blue, alpha)
                vc.addVertex(pose, size, 1.0f, size).setColor(red, green, blue, alpha)
                vc.addVertex(pose, size, 0.0f, size).setColor(red, green, blue, alpha)
                vc.addVertex(pose, -size, 0.0f, size).setColor(red, green, blue, alpha)

                // West face (X-)
                vc.addVertex(pose, -size, 0.0f, size).setColor(red, green, blue, alpha)
                vc.addVertex(pose, -size, 0.0f, -size).setColor(red, green, blue, alpha)
                vc.addVertex(pose, -size, 1.0f, -size).setColor(red, green, blue, alpha)
                vc.addVertex(pose, -size, 1.0f, size).setColor(red, green, blue, alpha)

                // East face (X+)
                vc.addVertex(pose, size, 1.0f, size).setColor(red, green, blue, alpha)
                vc.addVertex(pose, size, 1.0f, -size).setColor(red, green, blue, alpha)
                vc.addVertex(pose, size, 0.0f, -size).setColor(red, green, blue, alpha)
                vc.addVertex(pose, size, 0.0f, size).setColor(red, green, blue, alpha)
            }

            poseStack.popPose()
        }
    }
}
