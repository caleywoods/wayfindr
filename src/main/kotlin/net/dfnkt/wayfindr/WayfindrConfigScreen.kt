package net.dfnkt.wayfindr

import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import net.minecraft.client.gui.components.AbstractSliderButton
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.roundToInt

class WayfindrConfigScreen(private val parent: Screen) : Screen(Component.literal("Wayfindr Configuration")) {
    private var maxRenderDistanceSlider: AbstractSliderButton? = null
    private var maxRaycastDistanceSlider: AbstractSliderButton? = null
    private var deathWaypointButton: Button? = null
    private var enableTeleportButton: Button? = null

    private var config = WayfindrConfig.get()
    private var createDeathWaypoint = config.createDeathWaypoint
    private var enableTeleport = config.enableTeleport

    override fun init() {
        // Responsive layout: controls stack from the top with a fixed row stride, and
        // Save/Cancel are anchored to the bottom of the screen so they are always
        // visible/clickable regardless of GUI scale (previously they used height/4 + 180,
        // which pushed them off the bottom on short screens).
        val controlX = width / 2 - 100
        val controlW = 200
        val rowStride = 24
        val top = 40
        var y = top

        maxRenderDistanceSlider = this.addRenderableWidget(
            object : AbstractSliderButton(
                controlX, y, controlW, 20,
                Component.literal("Max Waypoint Render Distance: ${config.maxRenderDistance.roundToInt()}"),
                (config.maxRenderDistance - 50.0) / 450.0
            ) {
                override fun updateMessage() {
                    message = Component.literal("Max Waypoint Render Distance: ${(50 + value * 450).roundToInt()}")
                }

                override fun applyValue() {
                }
            }
        )
        y += rowStride

        maxRaycastDistanceSlider = this.addRenderableWidget(
            object : AbstractSliderButton(
                controlX, y, controlW, 20,
                Component.literal("Max Waypoint Placement Distance: ${config.maxRaycastDistance.roundToInt()}"),
                (config.maxRaycastDistance - 50.0) / 450.0
            ) {
                override fun updateMessage() {
                    message = Component.literal("Max Waypoint Placement Distance: ${(50 + value * 450).roundToInt()}")
                }

                override fun applyValue() {
                }
            }
        )
        y += rowStride

        deathWaypointButton = this.addRenderableWidget(
            Button.builder(
                Component.literal("Death Waypoint: ${if (createDeathWaypoint) "Enabled" else "Disabled"}")
            ) { button ->
                createDeathWaypoint = !createDeathWaypoint
                button.message = Component.literal("Death Waypoint: ${if (createDeathWaypoint) "Enabled" else "Disabled"}")
            }.bounds(controlX, y, controlW, 20).build()
        )
        y += rowStride

        enableTeleportButton = this.addRenderableWidget(
            Button.builder(
                Component.literal("Teleport Button (ops): ${if (enableTeleport) "Enabled" else "Disabled"}")
            ) { button ->
                enableTeleport = !enableTeleport
                button.message = Component.literal("Teleport Button (ops): ${if (enableTeleport) "Enabled" else "Disabled"}")
            }.bounds(controlX, y, controlW, 20).build()
        )

        // Anchor Save/Cancel to the bottom; never let them sit under the last control.
        val bottomY = maxOf(height - 28, y + rowStride + 6)
        this.addRenderableWidget(
            Button.builder(Component.literal("Save")) {
                saveConfig()
                onClose()
            }.bounds(controlX, bottomY, 95, 20).build()
        )

        this.addRenderableWidget(
            Button.builder(Component.literal("Cancel")) {
                onClose()
            }.bounds(width / 2 + 5, bottomY, 95, 20).build()
        )
    }

    private fun saveConfig() {
        val renderSliderText = maxRenderDistanceSlider?.message?.string ?: ""
        val raycastSliderText = maxRaycastDistanceSlider?.message?.string ?: ""

        val renderDistanceRegex = "Max Waypoint Render Distance: (\\d+)".toRegex()
        val raycastDistanceRegex = "Max Waypoint Placement Distance: (\\d+)".toRegex()

        val renderSliderValue = renderSliderText.let { text ->
            renderDistanceRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull() ?: config.maxRenderDistance
        }

        val raycastSliderValue = raycastSliderText.let { text ->
            raycastDistanceRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull() ?: config.maxRaycastDistance
        }

        val newConfig = WayfindrConfig(
            maxRenderDistance = renderSliderValue,
            maxRaycastDistance = raycastSliderValue,
            openMenuKey = config.openMenuKey,
            quickAddKey = config.quickAddKey,
            createDeathWaypoint = createDeathWaypoint,
            enableTeleport = enableTeleport,
            sortMode = config.sortMode
        )

        WayfindrConfig.update(newConfig)
    }

    override fun onClose() {
        minecraft?.setScreenAndShow(parent)
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(context, mouseX, mouseY, delta)
        context.centeredText(font, title, width / 2, 12, 0xFFFFFFFF.toInt())
    }
}
