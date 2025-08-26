package net.dfnkt.wayfindr

import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import net.minecraft.client.util.InputUtil
import net.minecraft.client.gui.widget.SliderWidget
import org.lwjgl.glfw.GLFW
import kotlin.math.roundToInt

class WayfindrConfigScreen(private val parent: Screen?) : Screen(Text.literal("Wayfindr Configuration")) {
    private var maxRenderDistanceSlider: SliderWidget? = null
    private var maxRaycastDistanceSlider: SliderWidget? = null
    private var openMenuKeyButton: ButtonWidget? = null
    private var quickAddKeyButton: ButtonWidget? = null
    
    private var listeningForKey = false
    private var currentKeyButton: ButtonWidget? = null
    
    private var config = WayfindrConfig.get()
    private var openMenuKey = config.openMenuKey
    private var quickAddKey = config.quickAddKey
    
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
        
        openMenuKeyButton = this.addDrawableChild(
            ButtonWidget.builder(
                Text.literal("Open Menu Key: ${getKeyName(openMenuKey)}"),
                { button ->
                    listeningForKey = true
                    currentKeyButton = button
                    button.message = Text.literal("Press a key...")
                }
            ).dimensions(width / 2 - 100, height / 4 + 60, 200, 20).build()
        )
        
        quickAddKeyButton = this.addDrawableChild(
            ButtonWidget.builder(
                Text.literal("Quick Add Key: ${getKeyName(quickAddKey)}"),
                { button ->
                    listeningForKey = true
                    currentKeyButton = button
                    button.message = Text.literal("Press a key...")
                }
            ).dimensions(width / 2 - 100, height / 4 + 90, 200, 20).build()
        )
        
        this.addDrawableChild(
            ButtonWidget.builder(
                Text.literal("Save"),
                { _ ->
                    saveConfig()
                    close()
                }
            ).dimensions(width / 2 - 100, height / 4 + 120, 95, 20).build()
        )
        
        this.addDrawableChild(
            ButtonWidget.builder(
                Text.literal("Cancel"),
                { _ ->
                    close()
                }
            ).dimensions(width / 2 + 5, height / 4 + 120, 95, 20).build()
        )
    }
    
    private fun saveConfig() {
        // For SliderWidget, we need to parse the value from the message text since 'value' is protected
        val renderSliderText = maxRenderDistanceSlider?.message?.string ?: ""
        val raycastSliderText = maxRaycastDistanceSlider?.message?.string ?: ""
        
        // Extract the numeric value from the text using regex
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
            openMenuKey = openMenuKey,
            quickAddKey = quickAddKey
        )
        
        WayfindrConfig.update(newConfig)
    }
    
    private fun getKeyName(keyCode: Int): String {
        return InputUtil.fromKeyCode(keyCode, 0).localizedText.string
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (listeningForKey) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listeningForKey = false
                updateKeyButtonText()
                return true
            }
            
            when (currentKeyButton) {
                openMenuKeyButton -> openMenuKey = keyCode
                quickAddKeyButton -> quickAddKey = keyCode
            }
            
            listeningForKey = false
            updateKeyButtonText()
            return true
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
    
    private fun updateKeyButtonText() {
        openMenuKeyButton?.message = Text.literal("Open Menu Key: ${getKeyName(openMenuKey)}")
        quickAddKeyButton?.message = Text.literal("Quick Add Key: ${getKeyName(quickAddKey)}")
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
