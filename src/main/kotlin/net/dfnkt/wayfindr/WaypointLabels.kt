package net.dfnkt.wayfindr

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector4f

/**
 * Draws waypoint name labels on the HUD by projecting each waypoint's world position to
 * screen space.
 *
 * In-world text (`submitText` / `submitNameTag`) routes through entity feature-render
 * phases that are not flushed for submissions made from a mod's level-render event, so
 * the labels never appear. Instead we capture the world camera matrices during the level
 * render pass and project waypoint positions onto the 2D HUD, where drawing is reliable.
 */
object WaypointLabels {
    // Captured from the level render pass each frame (copied so later mutation is safe).
    @Volatile private var cameraPos: Vec3? = null
    @Volatile private var viewRotation: Matrix4f? = null
    @Volatile private var projection: Matrix4f? = null

    /** Called during the level render event to snapshot the current camera transform. */
    fun capture(camera: CameraRenderState) {
        cameraPos = camera.pos
        viewRotation = Matrix4f(camera.viewRotationMatrix)
        projection = Matrix4f(camera.projectionMatrix)
    }

    /** Called from the HUD element to draw a label at each visible waypoint. */
    fun render(context: GuiGraphicsExtractor) {
        val cam = cameraPos ?: return
        val view = viewRotation ?: return
        val proj = projection ?: return

        val client = Minecraft.getInstance()
        val font = client.font
        val screenWidth = client.window.guiScaledWidth
        val screenHeight = client.window.guiScaledHeight
        val maxDistance = WayfindrConfig.get().maxRenderDistance
        // Only label waypoints in the player's current dimension — otherwise nether
        // coords would project onto the overworld (and vice versa).
        val currentDimension = client.player?.level()?.dimension()?.identifier()?.toString() ?: return

        for (waypoint in WaypointManager.waypoints) {
            if (!waypoint.visible || waypoint.name.isEmpty()) continue
            if (waypoint.dimension != currentDimension) continue

            val pos = waypoint.position.toVec3d()
            val distance = cam.distanceTo(pos)
            if (distance > maxDistance) continue

            // World position relative to the camera, lifted a little above the beam base.
            val v = Vector4f(
                (pos.x - cam.x).toFloat(),
                (pos.y - cam.y + 2.3).toFloat(),
                (pos.z - cam.z).toFloat(),
                1.0f
            )
            view.transform(v)
            proj.transform(v)

            // Behind the camera → skip.
            if (v.w <= 0.05f) continue

            val ndcX = v.x / v.w
            val ndcY = v.y / v.w
            if (ndcX < -1f || ndcX > 1f || ndcY < -1f || ndcY > 1f) continue

            val screenX = (ndcX * 0.5f + 0.5f) * screenWidth
            val screenY = (0.5f - ndcY * 0.5f) * screenHeight

            val label = waypoint.name
            val textWidth = font.width(label)
            val x = (screenX - textWidth / 2f).toInt()
            val y = screenY.toInt()

            // Translucent backdrop for readability.
            context.fill(x - 2, y - 2, x + textWidth + 2, y + font.lineHeight, 0x80000000.toInt())
            context.text(font, label, x, y, 0xFFFFFFFF.toInt(), true)
        }
    }
}
