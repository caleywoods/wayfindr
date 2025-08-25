package net.dfnkt.wayfindr

import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.client.MinecraftClient

class WayfindrRenameScreen(
    private val parent: Screen,
    private val waypointName: String
) : Screen(Text.literal("Rename Waypoint")) {
    
    private lateinit var nameField: TextFieldWidget
    private val client = MinecraftClient.getInstance()
    
    override fun init() {
        super.init()
        
        val centerX = width / 2
        val centerY = height / 2
        
        // Create text field for the new name
        nameField = TextFieldWidget(textRenderer, centerX - 100, centerY - 20, 200, 20, Text.literal(""))
        nameField.setMaxLength(32)
        nameField.text = waypointName
        nameField.setFocused(true)
        addDrawableChild(nameField)
        
        // Add Save button
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Save")) {
                val newName = nameField.text.trim()
                if (newName.isNotEmpty() && newName != waypointName) {
                    val success = WaypointManager.renameWaypoint(waypointName, newName)
                    if (success) {
                        client?.player?.sendMessage(Text.literal("Renamed waypoint '$waypointName' to '$newName'"), false)
                    } else {
                        client?.player?.sendMessage(Text.literal("Failed to rename waypoint. Name may already be in use."), false)
                    }
                }
                client?.setScreen(parent)
            }
            .dimensions(centerX - 105, centerY + 30, 100, 20)
            .build()
        )
        
        // Add Cancel button
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Cancel")) {
                client?.setScreen(parent)
            }
            .dimensions(centerX + 5, centerY + 30, 100, 20)
            .build()
        )
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)
        
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF)
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Enter new name:"), width / 2, height / 2 - 40, 0xFFFFFF)
    }
    
    // override fun tick() {
    //     super.tick()
    // }
    
    override fun shouldPause(): Boolean = false
}
