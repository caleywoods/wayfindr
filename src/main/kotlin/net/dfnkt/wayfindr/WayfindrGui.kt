package net.dfnkt.wayfindr

import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.client.MinecraftClient

class WayfindrGui : Screen(Text.literal("Waypoint Manager")) {
    private var waypointButtons = mutableListOf<ButtonWidget>()
    private var scrollOffset = 0
    private val BUTTON_HEIGHT = 16 
    private val BUTTON_SPACING = 2 
    
    override fun init() {
        super.init()
        
        val centerX = width / 2
        val startY = 30 
        val waypointListY = startY
        refreshWaypointList(waypointListY)
        
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Settings")) { 
                client?.setScreen(WayfindrConfigScreen(this))
            }
                .dimensions(centerX - 100, height - 40, 95, 20)
                .build()
        )
        
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Close")) { close() }
                .dimensions(centerX + 5, height - 40, 95, 20)
                .build()
        )
    }
    
    private fun refreshWaypointList(startY: Int) {
        waypointButtons.forEach { remove(it) }
        waypointButtons.clear()
        
        val centerX = width / 2
        var currentY = startY
        
        val listAreaHeight = height - startY - 100 
        val calculatedMaxWaypoints = maxOf(3, listAreaHeight / (BUTTON_HEIGHT + BUTTON_SPACING))
        val maxVisibleWaypoints = minOf(6, calculatedMaxWaypoints) 
        if (WaypointManager.waypoints.isEmpty()) {
            val noWaypointsButton = ButtonWidget.builder(Text.literal("No waypoints found")) {}
                .dimensions(centerX - 100, currentY, 200, BUTTON_HEIGHT)
                .build()
            addDrawableChild(noWaypointsButton)
            waypointButtons.add(noWaypointsButton)
            noWaypointsButton.active = false
            return
        }
        
        currentY += BUTTON_HEIGHT + BUTTON_SPACING
        
        if (scrollOffset >= WaypointManager.waypoints.size) {
            scrollOffset = maxOf(0, WaypointManager.waypoints.size - 1)
        }
        
        val visibleCount = minOf(maxVisibleWaypoints, WaypointManager.waypoints.size)
        val endIndex = minOf(WaypointManager.waypoints.size, scrollOffset + visibleCount)
        val startIndex = minOf(scrollOffset, WaypointManager.waypoints.size - visibleCount)
        
        val visibleWaypoints = if (WaypointManager.waypoints.size > startIndex) {
            WaypointManager.waypoints.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
        
        visibleWaypoints.forEach { waypoint ->
            val waypointText = "${waypoint.name} (${waypoint.getPosition().x.toInt()}, ${waypoint.getPosition().y.toInt()}, ${waypoint.getPosition().z.toInt()})"
            println("[Wayfindr] Adding button for waypoint: ${waypoint.name} at Y=$currentY")
            
            val visibilityButtonText = if (waypoint.visible) "Hide" else "Show"
            val visibilityButton = ButtonWidget.builder(Text.literal(visibilityButtonText)) {
                WaypointManager.toggleWaypointVisibility(waypoint.name)
                refreshWaypointList(startY)
            }
                .dimensions(centerX - 210, currentY, 50, BUTTON_HEIGHT)
                .build()
            
            val infoButton = ButtonWidget.builder(Text.literal(waypointText)) {}
                .dimensions(centerX - 150, currentY, 250, BUTTON_HEIGHT)
                .build()
            infoButton.active = false
            
            val deleteButton = ButtonWidget.builder(Text.literal("Delete")) { 
                deleteWaypoint(waypoint.name)
            }
                .dimensions(centerX + 110, currentY, 60, BUTTON_HEIGHT)
                .build()
            
            addDrawableChild(visibilityButton)
            addDrawableChild(infoButton)
            addDrawableChild(deleteButton)
            waypointButtons.add(visibilityButton)
            waypointButtons.add(infoButton)
            waypointButtons.add(deleteButton)
            
            currentY += BUTTON_HEIGHT + BUTTON_SPACING
        }
        
        if (WaypointManager.waypoints.size > 0) {
            val paginationY = height - 80
            
            if (WaypointManager.waypoints.size > maxVisibleWaypoints) {
                val leftButton = ButtonWidget.builder(Text.literal("←")) {
                    if (scrollOffset > 0) scrollOffset--
                    refreshWaypointList(startY)
                }
                    .dimensions(centerX - 130, paginationY, 30, BUTTON_HEIGHT)
                    .build()
                
                addDrawableChild(leftButton)
                waypointButtons.add(leftButton)
            }
            
            val scrollText = "Showing ${startIndex + 1}-$endIndex of ${WaypointManager.waypoints.size}"
            val scrollIndicator = ButtonWidget.builder(Text.literal(scrollText)) {}
                .dimensions(centerX - 100, paginationY, 200, BUTTON_HEIGHT)
                .build()
            addDrawableChild(scrollIndicator)
            waypointButtons.add(scrollIndicator)
            scrollIndicator.active = false
            
            if (WaypointManager.waypoints.size > maxVisibleWaypoints) {
                val rightButton = ButtonWidget.builder(Text.literal("→")) {
                    if (scrollOffset < WaypointManager.waypoints.size - maxVisibleWaypoints) scrollOffset++
                    refreshWaypointList(startY)
                }
                    .dimensions(centerX + 100, paginationY, 30, BUTTON_HEIGHT)
                    .build()
                
                addDrawableChild(rightButton)
                waypointButtons.add(rightButton)
            }
        }
    }
    
    private fun deleteWaypoint(name: String) {
        WaypointManager.removeWaypoint(name)
        refreshWaypointList(30)
        val client = MinecraftClient.getInstance()
        client.player?.sendMessage(Text.literal("Deleted waypoint '$name'"), false)
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)
        
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 6, 0xFFFFFF)
        
        if (WaypointManager.waypoints.isNotEmpty()) {
            val waypointCountText = "${WaypointManager.waypoints.size} Waypoints"
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(waypointCountText), width / 2, 35, 0xFFFFFF)
        }
        
        val totalWaypointCount = "Total waypoints: ${WaypointManager.waypoints.size}"
        context.drawTextWithShadow(textRenderer, totalWaypointCount, 10, height - 20, 0xAAAAAA)
    }
    
    override fun shouldPause(): Boolean = false
}
