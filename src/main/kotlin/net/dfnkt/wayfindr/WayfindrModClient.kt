package net.dfnkt.wayfindr

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.MinecraftClient
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import kotlin.random.Random
import org.slf4j.LoggerFactory
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.lwjgl.glfw.GLFW

object WayfindrModClient : ClientModInitializer {

    private lateinit var openWaypointMenu: KeyBinding
    private lateinit var quickAddWaypoint: KeyBinding
    private val logger = LoggerFactory.getLogger("wayfindr")
    
    private fun registerKeybindings() {
        openWaypointMenu = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.wayfindr.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M, // Default 'M'
                "category.wayfindr.general"
            )
        )

        quickAddWaypoint = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.wayfindr.quick_add",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N, // Default 'N'
                "category.wayfindr.general"
            )
        )
    }
    
    override fun onInitializeClient() {
        WayfindrConfig.load()
        
        registerKeybindings()
        
        WaypointManager.initializeWaypoints()
        
        // Register world change events to reload waypoints
        registerWorldChangeEvents()
        
        // Register player death event handler
        registerPlayerDeathHandler()
        
        // Initialize network handlers for waypoint synchronization
        WayfindrNetworkClient.initialize()
        
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
        
        HudRenderCallback.EVENT.register { drawContext, _ ->
            val client = MinecraftClient.getInstance()
            if (client.currentScreen == null && !client.isPaused) {
                WayfindrNavigationRenderer.render(drawContext)
            }
        }
        
        ClientTickEvents.END_CLIENT_TICK.register { mcClient ->
            while (openWaypointMenu.wasPressed()) {
                mcClient.setScreen(WayfindrGui())
            }
            
            if (quickAddWaypoint.wasPressed()) {
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
    
    /**
     * Registers event handlers for world changes to reload waypoints.
     */
    private fun registerWorldChangeEvents() {
        // When joining a server or singleplayer world
        ClientPlayConnectionEvents.JOIN.register { handler, sender, client ->
            logger.info("Joined world, reloading waypoints")
            WaypointManager.loadWaypointsForCurrentWorld()
        }
        
        // When disconnecting from a server or singleplayer world
        ClientPlayConnectionEvents.DISCONNECT.register { handler, client ->
            logger.info("Disconnected from world")
        }
    }
    
    /**
     * Registers a handler to create a waypoint when the player dies if enabled in config.
     */
    private fun registerPlayerDeathHandler() {
        var lastHealth = 20.0f
        var lastPosition = MinecraftClient.getInstance().player?.pos
        
        ClientTickEvents.START_CLIENT_TICK.register { client ->
            val player = client.player
            
            if (player != null) {
                // Check if player just died (health went from > 0 to 0)
                if (lastHealth > 0 && player.health <= 0) {
                    logger.info("Player died, checking if death waypoint should be created")
                    
                    // Create a death waypoint if enabled in config
                    if (WayfindrConfig.get().createDeathWaypoint) {
                        // Use the last known position since the player's position might be reset on death
                        lastPosition?.let { position ->
                            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
                            val waypointName = "Death Point $timestamp"
                            
                            // Create the waypoint with a red color
                            val deathWaypoint = WaypointManager.addWaypoint(
                                name = waypointName,
                                position = position,
                                color = 0xFF0000, // Red color for death waypoints
                                dimension = player.world.registryKey.value.toString()
                            )
                            
                            logger.info("Created death waypoint at ${position.x}, ${position.y}, ${position.z}")
                            
                            // Show a message to the player
                            player.sendMessage(
                                net.minecraft.text.Text.literal("§c[Wayfindr]§r Created waypoint at your death location."),
                                false
                            )
                        }
                    }
                }
                
                // Update last health and position for next tick
                lastHealth = player.health
                lastPosition = player.pos
            } else {
                // Reset when player is null
                lastHealth = 20.0f
                lastPosition = null
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