package net.dfnkt.wayfindr

import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.client.MinecraftClient
import net.dfnkt.wayfindr.WaypointManager
import net.minecraft.util.math.MathHelper
import java.util.*

class WayfindrGui : Screen(Text.literal("Waypoint Manager")) {
    private var waypointButtons = mutableListOf<ButtonWidget>()
    private var scrollOffset = 0
    private val BUTTON_HEIGHT = 20
    private val BUTTON_SPACING = 2
    private val RIGHT_PANE_Y = 48
    
    // UI components
    private lateinit var searchBox: TextFieldWidget
    private var selectedWaypoint: WaypointManager.Waypoint? = null
    private var paneWidth = 0
    private var rightPaneX = 0
    
    override fun init() {
        super.init()
        
        // Calculate pane dimensions
        this.paneWidth = this.width / 2 - 8
        this.rightPaneX = this.width - this.paneWidth
        
        // Search box
        this.searchBox = TextFieldWidget(
            this.textRenderer,
            10,
            22,
            paneWidth - 20,
            20,
            Text.literal("Search waypoints...")
        )
        this.searchBox.setChangedListener { text ->
            refreshWaypointList(RIGHT_PANE_Y)
        }
        addDrawableChild(this.searchBox)
        setInitialFocus(this.searchBox)
        
        // Add waypoint button
        addDrawableChild(
            ButtonWidget.builder(Text.literal("+ Add Waypoint")) { 
                val client = MinecraftClient.getInstance()
                val player = client.player
                if (player != null) {
                    val pos = player.pos
                    val name = "Waypoint ${WaypointManager.waypoints.size + 1}"
                    WaypointManager.addWaypoint(name, pos)
                    refreshWaypointList(RIGHT_PANE_Y)
                    selectWaypoint(name)
                }
            }
                .dimensions(rightPaneX + 10, RIGHT_PANE_Y, paneWidth - 20, BUTTON_HEIGHT)
                .build()
        )
        
        // Settings button
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Settings")) { 
                client?.setScreen(WayfindrConfigScreen(this))
            }
                .dimensions(rightPaneX + 10, height - 40, (paneWidth / 2) - 15, BUTTON_HEIGHT)
                .build()
        )
        
        // Close button
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Close")) { close() }
                .dimensions(rightPaneX + (paneWidth / 2) + 5, height - 40, (paneWidth / 2) - 15, BUTTON_HEIGHT)
                .build()
        )
        
        refreshWaypointList(RIGHT_PANE_Y)
    }
    
    private fun refreshWaypointList(startY: Int) {
        waypointButtons.forEach { remove(it) }
        waypointButtons.clear()
        
        val listAreaHeight = height - startY - 60
        val maxVisibleWaypoints = maxOf(5, listAreaHeight / (BUTTON_HEIGHT + BUTTON_SPACING))
        
        // Filter waypoints based on search text
        val filteredWaypoints = if (this::searchBox.isInitialized && searchBox.text.isNotEmpty()) {
            WaypointManager.waypoints.filter { 
                it.name.lowercase(Locale.getDefault()).contains(searchBox.text.lowercase(Locale.getDefault())) 
            }
        } else {
            WaypointManager.waypoints
        }
        
        if (filteredWaypoints.isEmpty()) {
            val noWaypointsButton = ButtonWidget.builder(Text.literal("No waypoints found")) {}
                .dimensions(10, startY + 10, paneWidth - 20, BUTTON_HEIGHT)
                .build()
            addDrawableChild(noWaypointsButton)
            waypointButtons.add(noWaypointsButton)
            noWaypointsButton.active = false
            return
        }
        
        // Adjust scroll offset if needed
        if (scrollOffset >= filteredWaypoints.size) {
            scrollOffset = maxOf(0, filteredWaypoints.size - 1)
        }
        
        val visibleCount = minOf(maxVisibleWaypoints, filteredWaypoints.size)
        val endIndex = minOf(filteredWaypoints.size, scrollOffset + visibleCount)
        val startIndex = minOf(scrollOffset, filteredWaypoints.size - visibleCount).coerceAtLeast(0)
        
        val visibleWaypoints = if (filteredWaypoints.size > startIndex) {
            filteredWaypoints.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
        
        var currentY = startY + 10
        
        // Add waypoint list entries
        visibleWaypoints.forEach { waypoint ->
            // Create a container panel for each waypoint entry
            val waypointButton = ButtonWidget.builder(Text.literal(waypoint.name)) {
                selectWaypoint(waypoint.name)
            }
                .dimensions(10, currentY, paneWidth - 40, BUTTON_HEIGHT)
                .build()
            
            // Add a visibility indicator
            val visibilityIndicator = ButtonWidget.builder(Text.literal(if (waypoint.visible) "👁" else "🚫")) { button ->
                // Toggle visibility without selecting the waypoint
                val success = WaypointManager.toggleWaypointVisibility(waypoint.name)
                if (success) {
                    // Update the button text to reflect the new visibility state
                    val updatedWaypoint = WaypointManager.getWaypoint(waypoint.name)
                    button.message = Text.literal(if (updatedWaypoint?.visible == true) "👁" else "🚫")
                    
                    // If this waypoint is currently selected, update its details
                    if (selectedWaypoint?.name == waypoint.name) {
                        selectedWaypoint = updatedWaypoint
                        refreshWaypointDetails()
                    }
                }
            }
                .dimensions(paneWidth - 30, currentY, 20, BUTTON_HEIGHT)
                .build()
            
            addDrawableChild(waypointButton)
            addDrawableChild(visibilityIndicator)
            waypointButtons.add(waypointButton)
            waypointButtons.add(visibilityIndicator)
            
            currentY += BUTTON_HEIGHT + BUTTON_SPACING
        }
        
        // Add pagination controls if needed
        if (filteredWaypoints.size > maxVisibleWaypoints) {
            val paginationY = height - 60
            
            // Up button
            val upButton = ButtonWidget.builder(Text.literal("↑")) {
                if (scrollOffset > 0) scrollOffset--
                refreshWaypointList(startY)
            }
                .dimensions(10, paginationY, 30, BUTTON_HEIGHT)
                .build()
            
            addDrawableChild(upButton)
            waypointButtons.add(upButton)
            
            // Down button
            val downButton = ButtonWidget.builder(Text.literal("↓")) {
                if (scrollOffset < filteredWaypoints.size - maxVisibleWaypoints) scrollOffset++
                refreshWaypointList(startY)
            }
                .dimensions(paneWidth - 40, paginationY, 30, BUTTON_HEIGHT)
                .build()
            
            addDrawableChild(downButton)
            waypointButtons.add(downButton)
            
            // Scroll indicator
            val scrollText = "Showing ${startIndex + 1}-$endIndex of ${filteredWaypoints.size}"
            val scrollIndicator = ButtonWidget.builder(Text.literal(scrollText)) {}
                .dimensions(45, paginationY, paneWidth - 90, BUTTON_HEIGHT)
                .build()
            addDrawableChild(scrollIndicator)
            waypointButtons.add(scrollIndicator)
            scrollIndicator.active = false
        }
        
        // If we had a selected waypoint, try to keep it selected
        selectedWaypoint?.let { selected ->
            if (filteredWaypoints.any { it.name == selected.name }) {
                selectWaypoint(selected.name)
            } else if (filteredWaypoints.isNotEmpty()) {
                selectWaypoint(filteredWaypoints[0].name)
            }
        }
    }
    
    private fun selectWaypoint(name: String) {
        selectedWaypoint = WaypointManager.getWaypoint(name)
        refreshWaypointDetails()
    }
    
    private fun refreshWaypointDetails() {
        // Remove previous detail buttons
        for (child in children().toList()) {
            if (child is ButtonWidget && child.x >= rightPaneX && !waypointButtons.contains(child)) {
                remove(child)
            }
        }
        
        val waypoint = selectedWaypoint ?: return
        
        // Waypoint name
        val nameButton = ButtonWidget.builder(Text.literal("Rename")) {
            client?.setScreen(WayfindrRenameScreen(this, waypoint.name))
        }
            .dimensions(rightPaneX + 10, RIGHT_PANE_Y + 40, paneWidth - 20, BUTTON_HEIGHT)
            .build()
        addDrawableChild(nameButton)
        
        // Visibility toggle
        val visibilityText = if (waypoint.visible) "Hide Waypoint" else "Show Waypoint"
        val visibilityButton = ButtonWidget.builder(Text.literal(visibilityText)) {
            WaypointManager.toggleWaypointVisibility(waypoint.name)
            refreshWaypointDetails()
        }
            .dimensions(rightPaneX + 10, RIGHT_PANE_Y + 70, paneWidth - 20, BUTTON_HEIGHT)
            .build()
        addDrawableChild(visibilityButton)
        
        // Teleport button (if in creative mode)
        val client = MinecraftClient.getInstance()
        if (client.player?.abilities?.creativeMode == true) {
            val teleportButton = ButtonWidget.builder(Text.literal("Teleport")) {
                val pos = waypoint.getPosition()
                // client.player?.teleport(pos.x, pos.y, pos.z)
            }
                .dimensions(rightPaneX + 10, RIGHT_PANE_Y + 100, paneWidth - 20, BUTTON_HEIGHT)
                .build()
            addDrawableChild(teleportButton)
        }
        
        // Delete button
        val deleteButton = ButtonWidget.builder(Text.literal("Delete Waypoint")) {
            WaypointManager.removeWaypoint(waypoint.name)
            selectedWaypoint = null
            refreshWaypointList(RIGHT_PANE_Y)
            refreshWaypointDetails()
        }
            .dimensions(rightPaneX + 10, RIGHT_PANE_Y + 130, paneWidth - 20, BUTTON_HEIGHT)
            .build()
        addDrawableChild(deleteButton)
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        
        // Draw divider line
        context.fill(paneWidth, 0, paneWidth + 1, height, 0xFFAAAAAA.toInt())
        
        // Get filtered waypoints for rendering
        val filteredWaypoints = if (this::searchBox.isInitialized && searchBox.text.isNotEmpty()) {
            WaypointManager.waypoints.filter { 
                it.name.lowercase(Locale.getDefault()).contains(searchBox.text.lowercase(Locale.getDefault())) 
            }
        } else {
            WaypointManager.waypoints
        }
        
        super.render(context, mouseX, mouseY, delta)
        
        // Draw title centered in the left pane
        context.drawCenteredTextWithShadow(textRenderer, title, paneWidth / 2, 6, 0xFFFFFF)
        
        // Draw waypoint count
        val waypointCountText = "${WaypointManager.waypoints.size} Waypoints"
        context.drawTextWithShadow(textRenderer, waypointCountText, 10, height - 20, 0xAAAAAA)
        
        // Draw selected waypoint details
        selectedWaypoint?.let { waypoint ->
            // Draw waypoint name
            context.drawTextWithShadow(
                textRenderer, 
                Text.literal(waypoint.name), 
                rightPaneX + 10, 
                RIGHT_PANE_Y + 10, 
                0xFFFFFF
            )
            
            // Draw coordinates
            val pos = waypoint.getPosition()
            val coordsText = "X: ${pos.x.toInt()}, Y: ${pos.y.toInt()}, Z: ${pos.z.toInt()}"
            context.drawTextWithShadow(
                textRenderer, 
                coordsText, 
                rightPaneX + 10, 
                RIGHT_PANE_Y + 25, 
                0xAAAAAA
            )
        }
    }
    
    private fun getWaypointNameFromPosition(y: Int): String {
        // Calculate the visible waypoints based on current filters and scroll position
        val filteredWaypoints = if (this::searchBox.isInitialized && searchBox.text.isNotEmpty()) {
            WaypointManager.waypoints.filter { 
                it.name.lowercase(Locale.getDefault()).contains(searchBox.text.lowercase(Locale.getDefault())) 
            }
        } else {
            WaypointManager.waypoints
        }
        
        if (filteredWaypoints.isEmpty()) return ""
        
        val listAreaHeight = height - RIGHT_PANE_Y - 60
        val maxVisibleWaypoints = maxOf(5, listAreaHeight / (BUTTON_HEIGHT + BUTTON_SPACING))
        val visibleCount = minOf(maxVisibleWaypoints, filteredWaypoints.size)
        val endIndex = minOf(filteredWaypoints.size, scrollOffset + visibleCount)
        val startIndex = minOf(scrollOffset, filteredWaypoints.size - visibleCount).coerceAtLeast(0)
        
        val visibleWaypoints = if (filteredWaypoints.size > startIndex) {
            filteredWaypoints.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
        
        // Calculate which waypoint corresponds to the given y position
        val index = (y - (RIGHT_PANE_Y + 10)) / (BUTTON_HEIGHT + BUTTON_SPACING)
        
        if (index >= 0 && index < visibleWaypoints.size) {
            return visibleWaypoints[index].name
        }
        
        return ""
    }
    
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (mouseX < paneWidth) {
            // Scroll the waypoint list
            if (verticalAmount > 0 && scrollOffset > 0) {
                scrollOffset--
                refreshWaypointList(RIGHT_PANE_Y)
                return true
            } else if (verticalAmount < 0 && WaypointManager.waypoints.size > 0) {
                val listAreaHeight = height - RIGHT_PANE_Y - 60
                val maxVisibleWaypoints = maxOf(5, listAreaHeight / (BUTTON_HEIGHT + BUTTON_SPACING))
                
                if (scrollOffset < WaypointManager.waypoints.size - maxVisibleWaypoints) {
                    scrollOffset++
                    refreshWaypointList(RIGHT_PANE_Y)
                    return true
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }
    
    override fun shouldPause(): Boolean = false
}
