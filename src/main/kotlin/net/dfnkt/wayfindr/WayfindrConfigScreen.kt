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

    private var config = WayfindrConfig.get()
    private var createDeathWaypoint = config.createDeathWaypoint

    override fun init() {
        maxRenderDistanceSlider = this.addRenderableWidget(
            object : AbstractSliderButton(
                width / 2 - 100,
                height / 4,
                200,
                20,
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

        maxRaycastDistanceSlider = this.addRenderableWidget(
            object : AbstractSliderButton(
                width / 2 - 100,
                height / 4 + 30,
                200,
                20,
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

        deathWaypointButton = this.addRenderableWidget(
            Button.builder(
                Component.literal("Death Waypoint: ${if (createDeathWaypoint) "Enabled" else "Disabled"}")
            ) { button ->
                createDeathWaypoint = !createDeathWaypoint
                button.message = Component.literal("Death Waypoint: ${if (createDeathWaypoint) "Enabled" else "Disabled"}")
            }.bounds(width / 2 - 100, height / 4 + 120, 200, 20).build()
        )

        this.addRenderableWidget(
            Button.builder(Component.literal("Save")) {
                saveConfig()
                onClose()
            }.bounds(width / 2 - 100, height / 4 + 150, 95, 20).build()
        )

        this.addRenderableWidget(
            Button.builder(Component.literal("Cancel")) {
                onClose()
            }.bounds(width / 2 + 5, height / 4 + 150, 95, 20).build()
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
            createDeathWaypoint = createDeathWaypoint
        )

        WayfindrConfig.update(newConfig)
    }

    override fun onClose() {
        minecraft?.setScreenAndShow(parent)
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(context, mouseX, mouseY, delta)
        context.centeredText(font, title, width / 2, 20, 0xFFFFFF)

        context.text(font,
            Component.literal("Configure Wayfindr mod settings"),
            width / 2 - 100, height / 4 - 20, 0xAAAAAA, false)
    }
}
