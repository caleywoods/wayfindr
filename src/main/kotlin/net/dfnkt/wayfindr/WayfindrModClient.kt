package net.dfnkt.wayfindr

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.util.math.Vec3d
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient
import net.dfnkt.wayfindr.WayfindrRenderer.Companion.renderWaypointMarker
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.text.Text
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW
import kotlin.random.Random

object WayfindrModClient : ClientModInitializer {

    private val openWaypointMenu = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            "key.wayfindr.open_menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "category.wayfindr.general"
        )
    )
    
    private val quickAddWaypoint = KeyBindingHelper.registerKeyBinding(
        KeyBinding(
            "key.wayfindr.quick_add",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "category.wayfindr.general"
        )
    )

    override fun onInitializeClient() {
        WaypointManager.initializeWaypoints()
        
        val client = MinecraftClient.getInstance();
        HudRenderCallback.EVENT.register {drawContext, _ ->
            drawContext.drawText(client.textRenderer, "Wayfinder mod demo", 100, 200, 0xFFFFFFFFu.toInt(), true);
        }
        
        WorldRenderEvents.AFTER_TRANSLUCENT.register { context ->
            val matrixStack = context.matrixStack() ?: return@register
            val player = context.camera().pos

            for (waypoint in WaypointManager.waypoints) {
                val distance = player.distanceTo(waypoint.position.toVec3d())

                if (distance <= 200) {
                    renderWaypointMarker(matrixStack, waypoint.position.toVec3d(), player, waypoint.color, waypoint.name)
                }
            }
        }
        
        ClientTickEvents.END_CLIENT_TICK.register { mcClient ->
            while (openWaypointMenu.wasPressed()) {
                mcClient.setScreen(WayfindrGui())
            }
            
            while (quickAddWaypoint.wasPressed()) {
                val player = mcClient.player
                if (player != null) {
                    val waypointName = "Quick Waypoint ${WaypointManager.waypoints.size + 1}"
                    val position = WayfindrRaycast.getRaycastPosition(player)
                    
                    val randomColor = generateRandomColor()
                    
                    WaypointManager.addWaypoint(waypointName, position, randomColor)
                    player.sendMessage(Text.literal("Quick waypoint added at crosshair target!"), false)
                }
            }
        }
    }
    
    private fun generateRandomColor(): Int {
        val red = Random.nextInt(100, 256)
        val green = Random.nextInt(100, 256)
        val blue = Random.nextInt(100, 256)
        
        return (red shl 16) or (green shl 8) or blue
    }
}