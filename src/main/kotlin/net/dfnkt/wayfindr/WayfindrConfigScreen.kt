package net.dfnkt.wayfindr

import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import net.minecraft.client.gui.widget.SliderWidget
import kotlin.math.roundToInt

class WayfindrConfigScreen(private val parent: Screen?) : Screen(Text.literal("Wayfindr Configuration")) {
    private var maxRenderDistanceSlider: SliderWidget? = null
    private var maxRaycastDistanceSlider: SliderWidget? = null
    private var deathWaypointButton: ButtonWidget? = null
    
    private var config = WayfindrConfig.get()
    private var createDeathWaypoint = config.createDeathWaypoint
    
    override fun init() {
        maxRenderDistanceSlider = this.addDrawableChild(
            object : SliderWidget(
                width / 2 - 100, 
                height / 4, 
                200, 
                20, 
                Text.literal("Max Waypoint Render Distance: ${config.maxRenderDistance.roundToInt()}"), 
                (config.maxRenderDistance - 50.0) / 450.0
            ) {
                override fun updateMessage() {
                    message = Text.literal("Max Waypoint Render Distance: ${(50 + value * 450).roundToInt()}")
                }
                
                override fun applyValue() {
                }
            }
        )
        
        maxRaycastDistanceSlider = this.addDrawableChild(
            object : SliderWidget(
                width / 2 - 100, 
                height / 4 + 30, 
                200, 
                20, 
                Text.literal("Max Waypoint Placement Distance: ${config.maxRaycastDistance.roundToInt()}"), 
                (config.maxRaycastDistance - 50.0) / 450.0
            ) {
                override fun updateMessage() {
                    message = Text.literal("Max Waypoint Placement Distance: ${(50 + value * 450).roundToInt()}")
                }
                
                override fun applyValue() {
                }
            }
        )
        
        deathWaypointButton = this.addDrawableChild(
            ButtonWidget.builder(
                Text.literal("Death Waypoint: ${if (createDeathWaypoint) "Enabled" else "Disabled"}"),
                { button ->
                    createDeathWaypoint = !createDeathWaypoint
                    button.message = Text.literal("Death Waypoint: ${if (createDeathWaypoint) "Enabled" else "Disabled"}")
                }
            ).dimensions(width / 2 - 100, height / 4 + 120, 200, 20).build()
        )
        
        this.addDrawableChild(
            ButtonWidget.builder(
                Text.literal("Save"),
                { _ ->
                    saveConfig()
                    close()
                }
            ).dimensions(width / 2 - 100, height / 4 + 150, 95, 20).build()
        )
        
        this.addDrawableChild(
            ButtonWidget.builder(
                Text.literal("Cancel"),
                { _ ->
                    close()
                }
            ).dimensions(width / 2 + 5, height / 4 + 150, 95, 20).build()
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
    
    override fun close() {
        client?.setScreen(parent)
    }
    
    override fun render(context: net.minecraft.client.gui.DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(context, mouseX, mouseY, delta)
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF)
        
        context.drawTextWithShadow(textRenderer, 
            Text.literal("Configure Wayfindr mod settings"), 
            width / 2 - 100, height / 4 - 20, 0xAAAAAA)
        
        super.render(context, mouseX, mouseY, delta)
    }
}
