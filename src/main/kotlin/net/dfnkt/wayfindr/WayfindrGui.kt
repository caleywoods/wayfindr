package net.dfnkt.wayfindr

import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.client.MinecraftClient
import net.minecraft.util.math.Vec3d
import net.minecraft.client.gui.widget.GridWidget
import net.minecraft.client.gui.widget.SimplePositioningWidget

class WayfindrGui : Screen(Text.literal("Waypoint Manager")) {
    private var nameField: TextFieldWidget? = null
    private var colorField: TextFieldWidget? = null
    private var selectedWaypoint: WaypointManager.Waypoint? = null
    private var waypointButtons = mutableListOf<ButtonWidget>()
    
    override fun init() {
        super.init()
        
        val centerX = width / 2
        val startY = 40
        
        // Title (non-interactive)
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Waypoint Manager")) { }
                .dimensions(centerX - 100, 20, 200, 20)
                .build()
        ).active = false
        
        // Add waypoint section header
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Add New Waypoint")) { }
                .dimensions(centerX - 100, startY, 200, 20)
                .build()
        ).active = false
        
        // Name input field
        nameField = TextFieldWidget(textRenderer, centerX - 100, startY + 30, 200, 20, Text.literal("Waypoint Name"))
        nameField?.setPlaceholder(Text.literal("Enter waypoint name..."))
        addDrawableChild(nameField!!)
        
        // Color input field
        colorField = TextFieldWidget(textRenderer, centerX - 100, startY + 60, 200, 20, Text.literal("Color"))
        colorField?.setPlaceholder(Text.literal("red, blue, #FF0000..."))
        addDrawableChild(colorField!!)
        
        // Add waypoint buttons
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Add at Crosshair")) { addWaypointAtCrosshair() }
                .dimensions(centerX - 150, startY + 90, 140, 20)
                .build()
        )
        
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Add at Player")) { addWaypointAtPlayer() }
                .dimensions(centerX + 10, startY + 90, 140, 20)
                .build()
        )
        
        // Existing waypoints section header
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Existing Waypoints")) { }
                .dimensions(centerX - 100, startY + 130, 200, 20)
                .build()
        ).active = false
        
        // Populate waypoint list
        refreshWaypointList(startY + 160)
        
        // Close button
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Close")) { close() }
                .dimensions(centerX - 50, height - 40, 100, 20)
                .build()
        )
    }
    
    private fun refreshWaypointList(startY: Int) {
        waypointButtons.forEach { remove(it) }
        waypointButtons.clear()
        
        val centerX = width / 2
        var currentY = startY
        
        WaypointManager.waypoints.forEachIndexed { _, waypoint ->
            if (currentY + 25 < height - 60) { // Leave space for close button
                val waypointText = "${waypoint.name} (${waypoint.getPosition().x.toInt()}, ${waypoint.getPosition().y.toInt()}, ${waypoint.getPosition().z.toInt()})"
                
                val infoButton = ButtonWidget.builder(Text.literal(waypointText)) { 
                    selectedWaypoint = waypoint
                    nameField?.text = waypoint.name
                }
                    .dimensions(centerX - 150, currentY, 200, 20)
                    .build()
                
                val deleteButton = ButtonWidget.builder(Text.literal("Delete")) { 
                    deleteWaypoint(waypoint.name)
                }
                    .dimensions(centerX + 60, currentY, 60, 20)
                    .build()
                
                addDrawableChild(infoButton)
                addDrawableChild(deleteButton)
                waypointButtons.add(infoButton)
                waypointButtons.add(deleteButton)
                
                currentY += 25
            }
        }
    }
    
    private fun addWaypointAtCrosshair() {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val name = nameField?.text?.takeIf { it.isNotBlank() } ?: "Waypoint ${WaypointManager.waypoints.size + 1}"
        val colorText = colorField?.text?.takeIf { it.isNotBlank() } ?: "red"
        
        val position = WayfindrRaycast.getRaycastPosition(player)
        val color = parseColor(colorText)
        
        WaypointManager.addWaypoint(name, position, color)
        
        nameField?.text = ""
        colorField?.text = ""
        refreshWaypointList(200)
        
        player.sendMessage(Text.literal("Added waypoint '$name' at crosshair target"), false)
    }
    
    private fun addWaypointAtPlayer() {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val name = nameField?.text?.takeIf { it.isNotBlank() } ?: "Waypoint ${WaypointManager.waypoints.size + 1}"
        val colorText = colorField?.text?.takeIf { it.isNotBlank() } ?: "red"
        
        val position = Vec3d(player.x, player.y, player.z)
        val color = parseColor(colorText)
        
        WaypointManager.addWaypoint(name, position, color)
        
        nameField?.text = ""
        colorField?.text = ""
        refreshWaypointList(200)
        
        player.sendMessage(Text.literal("Added waypoint '$name' at your location"), false)
    }
    
    private fun deleteWaypoint(name: String) {
        WaypointManager.removeWaypoint(name)
        refreshWaypointList(200)
        
        val client = MinecraftClient.getInstance()
        client.player?.sendMessage(Text.literal("Deleted waypoint '$name'"), false)
    }
    
    private fun parseColor(colorArg: String): Int {
        return when (colorArg.lowercase()) {
            "red" -> 0xFF0000
            "green" -> 0x00FF00
            "blue" -> 0x0000FF
            "yellow" -> 0xFFFF00
            "purple" -> 0x800080
            "orange" -> 0xFFA500
            "white" -> 0xFFFFFF
            "black" -> 0x000000
            else -> {
                if (colorArg.startsWith("#")) {
                    try {
                        return colorArg.substring(1).toInt(16)
                    } catch (e: NumberFormatException) {
                        // Invalid hex color
                    }
                }
                0xFF0000 // Default to red if color not recognized
            }
        }
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)
        
        // Draw title
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 6, 0xFFFFFF)
        
        // Draw waypoint count
        val waypointCount = "Total waypoints: ${WaypointManager.waypoints.size}"
        context.drawTextWithShadow(textRenderer, waypointCount, 10, height - 20, 0xAAAAAA)
    }
    
    override fun shouldPause(): Boolean = false
}
