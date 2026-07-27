package net.dfnkt.wayfindr

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Minecraft
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import org.slf4j.LoggerFactory
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object WayfindrModClient : ClientModInitializer {

    private val logger = LoggerFactory.getLogger("wayfindr")

    private val NAVIGATION_LAYER_ID = Identifier.fromNamespaceAndPath(Wayfindr.MOD_ID, "navigation_layer")

    override fun onInitializeClient() {
        WayfindrKeybinds.initialize()

        WayfindrConfig.load()

        WaypointManager.initializeWaypoints()

        registerWorldChangeEvents()

        registerPlayerDeathHandler()

        WayfindrNetworkClient.initialize()

        // World-space waypoint beams via the 26.x submit-node rendering system.
        LevelRenderEvents.COLLECT_SUBMITS.register { context ->
            val poseStack = context.poseStack()
            val collector = context.submitNodeCollector()
            val cameraState = context.levelState().cameraRenderState
            val cameraPos = context.gameRenderer().mainCamera().position()
            val config = WayfindrConfig.get()

            // Snapshot the camera transform so the HUD can project waypoint name labels.
            WaypointLabels.capture(cameraState)

            // Only render beams for waypoints in the player's current dimension so
            // e.g. nether waypoints don't appear in the overworld at matching coords.
            val currentDimension = Minecraft.getInstance().player?.level()?.dimension()?.identifier()?.toString()

            // Compare squared distances to avoid a sqrt per waypoint, and read the
            // waypoint's raw coordinates directly to avoid allocating a Vec3 per
            // waypoint every frame (matters at hundreds of waypoints).
            val maxRenderSq = config.maxRenderDistance * config.maxRenderDistance

            for (waypoint in WaypointManager.waypoints) {
                if (!waypoint.visible) continue
                if (waypoint.dimension != currentDimension) continue

                val wp = waypoint.position
                val dx = wp.x - cameraPos.x
                val dy = wp.y - cameraPos.y
                val dz = wp.z - cameraPos.z
                if (dx * dx + dy * dy + dz * dz > maxRenderSq) continue

                WayfindrRenderer.renderWaypointMarker(poseStack, collector, cameraPos, wp.x, wp.y, wp.z, waypoint.color)
            }
        }

        // Navigation arrow HUD element.
        HudElementRegistry.addLast(
            NAVIGATION_LAYER_ID,
            HudElement { guiGraphics, _ ->
                val client = Minecraft.getInstance()
                if (!client.isPaused) {
                    WaypointLabels.render(guiGraphics)
                    WayfindrNavigationRenderer.render(guiGraphics)
                }
            }
        )

        ClientTickEvents.END_CLIENT_TICK.register { mcClient ->
            while (WayfindrKeybinds.OPEN_WAYPOINT_MENU.consumeClick()) {
                mcClient.setScreenAndShow(WayfindrGui())
            }

            if (WayfindrKeybinds.QUICK_ADD_WAYPOINT.consumeClick()) {
                val player = mcClient.player
                if (player != null) {
                    val waypointName = "Quick Waypoint ${WaypointManager.waypoints.size + 1}"
                    val position = WayfindrRaycast.getRaycastPosition(player)

                    val randomColor = WaypointColors.random()
                    val dimension = player.level().dimension().identifier().toString()

                    WaypointManager.addWaypoint(waypointName, position, randomColor, dimension = dimension)
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
            // Persist any debounced waypoint edits. Marshal onto the client thread so this
            // can't race with main-thread mutations regardless of which thread fires the
            // event — WaypointManager's state is only safe to touch from the client thread.
            client.execute { WaypointManager.flushPendingSaves() }
        }
    }

    /**
     * Registers a handler to create a waypoint when the player dies if enabled in config.
     */
    private fun registerPlayerDeathHandler() {
        var lastHealth = 20.0f
        var lastPosition = Minecraft.getInstance().player?.position()

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
                            WaypointManager.addWaypoint(
                                name = waypointName,
                                position = position,
                                color = 0xFF0000, // Red color for death waypoints
                                dimension = player.level().dimension().identifier().toString()
                            )

                            logger.info("Created death waypoint at ${position.x}, ${position.y}, ${position.z}")

                            // Show a message to the player
                            player.sendSystemMessage(
                                Component.literal("§c[Wayfindr]§r Created waypoint at your death location.")
                            )
                        }
                    }
                }

                // Update last health and position for next tick
                lastHealth = player.health
                lastPosition = player.position()
            } else {
                // Reset when player is null
                lastHealth = 20.0f
                lastPosition = null
            }
        }
    }

}
