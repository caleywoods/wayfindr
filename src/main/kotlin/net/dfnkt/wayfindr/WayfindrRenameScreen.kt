package net.dfnkt.wayfindr

import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.client.Minecraft
import net.minecraft.client.input.KeyEvent
import java.util.UUID

class WayfindrRenameScreen(
    private val parent: Screen,
    private val waypointId: UUID,
    private val currentName: String
) : Screen(Component.literal("Rename Waypoint")) {

    private lateinit var nameField: EditBox
    private val mc = Minecraft.getInstance()

    override fun init() {
        super.init()

        val centerX = width / 2
        val centerY = height / 2

        // Create text field for the new name
        nameField = EditBox(font, centerX - 100, centerY - 20, 200, 20, Component.literal(""))
        nameField.setMaxLength(32)
        nameField.value = currentName
        addRenderableWidget(nameField)

        // Add Save button
        addRenderableWidget(
            Button.builder(Component.literal("Save")) {
                val newName = nameField.value.trim()
                if (newName.isNotEmpty() && newName != currentName) {
                    val success = WaypointManager.renameWaypoint(waypointId, newName)
                    if (success) {
                        mc.player?.sendSystemMessage(Component.literal("Renamed waypoint '$currentName' to '$newName'"))
                    } else {
                        mc.player?.sendSystemMessage(Component.literal("Failed to rename waypoint. Name may already be in use."))
                    }
                }
                mc.setScreenAndShow(parent)
            }
            .bounds(centerX - 105, centerY + 30, 100, 20)
            .build()
        )

        // Add Cancel button
        addRenderableWidget(
            Button.builder(Component.literal("Cancel")) {
                mc.setScreenAndShow(parent)
            }
            .bounds(centerX + 5, centerY + 30, 100, 20)
            .build()
        )

        setInitialFocus(nameField)
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(context, mouseX, mouseY, delta)

        context.centeredText(font, title, width / 2, 20, 0xFFFFFFFF.toInt())
        context.centeredText(font, Component.literal("Enter new name:"), width / 2, height / 2 - 40, 0xFFFFFFFF.toInt())
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val keyCode = event.key()
        // Handle Enter key to save
        if (keyCode == 257 || keyCode == 335) { // Enter or numpad Enter
            val newName = nameField.value.trim()
            if (newName.isNotEmpty() && newName != currentName) {
                val success = WaypointManager.renameWaypoint(waypointId, newName)
                if (success) {
                    mc.player?.sendSystemMessage(Component.literal("Renamed waypoint '$currentName' to '$newName'"))
                } else {
                    mc.player?.sendSystemMessage(Component.literal("Failed to rename waypoint. Name may already be in use."))
                }
            }
            mc.setScreenAndShow(parent)
            return true
        }

        // Handle Escape key to cancel
        if (keyCode == 256) { // Escape
            mc.setScreenAndShow(parent)
            return true
        }

        // Let the text field handle other keys if it's focused
        if (nameField.isFocused) {
            return nameField.keyPressed(event)
        }

        return super.keyPressed(event)
    }

    override fun isPauseScreen(): Boolean = false
}
