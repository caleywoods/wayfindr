package net.dfnkt.wayfindr

import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.client.MinecraftClient
import net.dfnkt.wayfindr.WaypointManager

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

        setInitialFocus(nameField)
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)
        
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF)
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Enter new name:"), width / 2, height / 2 - 40, 0xFFFFFF)
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        // Handle Enter key to save
        if (keyCode == 257 || keyCode == 335) { // Enter or numpad Enter
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
            return true
        }
        
        // Handle Escape key to cancel
        if (keyCode == 256) { // Escape
            client?.setScreen(parent)
            return true
        }
        
        // Let the text field handle other keys if it's focused
        if (nameField.isFocused()) {
            return nameField.keyPressed(keyCode, scanCode, modifiers);
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
    
    override fun shouldPause(): Boolean = false
}
