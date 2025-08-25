package net.dfnkt.wayfindr

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.util.math.Vec3d
import net.minecraft.client.MinecraftClient
import net.dfnkt.wayfindr.WayfindrRenderer.Companion.renderWaypointMarker
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.text.Text
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW
import kotlin.random.Random
import org.slf4j.LoggerFactory
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback

object WayfindrModClient : ClientModInitializer {

    private lateinit var openWaypointMenu: KeyBinding
    private lateinit var quickAddWaypoint: KeyBinding
    
    private fun registerKeybindings() {
        val config = WayfindrConfig.get()
        
        openWaypointMenu = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.wayfindr.open_menu",
                InputUtil.Type.KEYSYM,
                config.openMenuKey,
                "category.wayfindr.general"
            )
        )
        
        quickAddWaypoint = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.wayfindr.quick_add",
                InputUtil.Type.KEYSYM,
                config.quickAddKey,
                "category.wayfindr.general"
            )
        )
    }
    
    override fun onInitializeClient() {
        WayfindrConfig.load()
        
        registerKeybindings()
        
        WaypointManager.initializeWaypoints()
        
        WorldRenderEvents.AFTER_TRANSLUCENT.register { context ->
            val matrixStack = context.matrixStack() ?: return@register
            val player = context.camera().pos
            val config = WayfindrConfig.get()
            
            for (waypoint in WaypointManager.waypoints) {
                if (!waypoint.visible) continue
                
                val distance = player.distanceTo(waypoint.position.toVec3d())
                
                if (distance <= config.maxRenderDistance) {
                    WayfindrRenderer.renderWaypointMarker(matrixStack, waypoint.position.toVec3d(), player, waypoint.color, waypoint.name)
                }
            }
        }
        
        HudRenderCallback.EVENT.register { drawContext, tickDelta ->
            val client = MinecraftClient.getInstance()
            if (client.currentScreen == null && !client.isPaused) {
                WayfindrNavigationRenderer.render(drawContext)
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