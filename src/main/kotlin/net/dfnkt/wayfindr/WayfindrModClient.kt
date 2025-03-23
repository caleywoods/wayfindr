package net.dfnkt.wayfindr

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.util.math.Vec3d
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient
import net.dfnkt.wayfindr.WayfindrRenderer.Companion.renderWaypointMarker

class WayfindrModClient : ClientModInitializer {
    // Test waypoint at coordinates (0, 95, 0), adjust if you need it higher or lower
    private val testWaypoint = Vec3d(0.0, 95.0, 0.0)

    override fun onInitializeClient() {
        val client = MinecraftClient.getInstance();
        HudRenderCallback.EVENT.register {drawContext, _ ->
            // This could eventually show the waypoint name above the point
            drawContext.drawText(client.textRenderer, "Wayfinder mod demo", 100, 200, 0xFFFFFFFFu.toInt(), true);
        }
        // Register our render event
        WorldRenderEvents.AFTER_TRANSLUCENT.register { context ->
            val matrixStack = context.matrixStack() ?: return@register
            val player = context.camera().pos

            for (waypoint in WaypointManager.waypoints) {
                val distance = player.distanceTo(waypoint.position)

                // Only render if player is within 100 blocks
                if (distance <= 100) {
                    renderWaypointMarker(matrixStack, waypoint.position, player, waypoint.color)
                }
            }
        }
    }
}