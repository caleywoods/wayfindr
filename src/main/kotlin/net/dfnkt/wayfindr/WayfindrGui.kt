package net.dfnkt.wayfindr

import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.client.MinecraftClient
import org.slf4j.LoggerFactory
import java.util.*

class WayfindrGui : Screen(Text.literal("Waypoint Manager")) {
    private val logger = LoggerFactory.getLogger("wayfindr")
    private var waypointButtons = mutableListOf<ButtonWidget>()
    private var scrollOffset = 0
    private val BUTTON_HEIGHT = 20
    private val BUTTON_SPACING = 2
    private val RIGHT_PANE_Y = 48
    
    // UI layout constants
    private val FILTER_BUTTON_Y_OFFSET = 50 // Distance from bottom of screen to filter buttons
    private val FILTER_BUTTON_MARGIN = 10 // Extra margin between scrollbar and filter buttons
    
    // Scrollbar properties
    private val SCROLLBAR_WIDTH = 6
    private var scrollbarHeight = 0
    private var scrollbarY = 0
    private var scrollbarHandleHeight = 0
    private var scrollbarHandleY = 0
    private var isDraggingScrollbar = false
    private var lastMouseY = 0.0
    
    // UI components
    private lateinit var searchBox: TextFieldWidget
    private var selectedWaypoint: WaypointManager.Waypoint? = null
    private var paneWidth = 0
    private var rightPaneX = 0
    
    // Filter options
    private var showPersonalWaypoints = true
    private var showSharedWaypoints = true
    
    override fun init() {
        super.init()
        
        // Restore scroll position from manager
        scrollOffset = WaypointManager.getWaypointListScrollPosition()
        
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
        
        // Filter buttons
        addDrawableChild(
            ButtonWidget.builder(Text.literal(if (showPersonalWaypoints) "✓ Personal" else "❌ Personal")) { button ->
                showPersonalWaypoints = !showPersonalWaypoints
                button.message = Text.literal(if (showPersonalWaypoints) "✓ Personal" else "❌ Personal")
                refreshWaypointList(RIGHT_PANE_Y)
            }
                .dimensions(10, height - FILTER_BUTTON_Y_OFFSET, paneWidth / 2 - 15, BUTTON_HEIGHT)
                .build()
        )
        
        addDrawableChild(
            ButtonWidget.builder(Text.literal(if (showSharedWaypoints) "✓ Shared" else "❌ Shared")) { button ->
                showSharedWaypoints = !showSharedWaypoints
                button.message = Text.literal(if (showSharedWaypoints) "✓ Shared" else "❌ Shared")
                refreshWaypointList(RIGHT_PANE_Y)
            }
                .dimensions(paneWidth / 2 + 5, height - FILTER_BUTTON_Y_OFFSET, paneWidth / 2 - 15, BUTTON_HEIGHT)
                .build()
        )
        
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
        
        val listAreaHeight = height - startY - (FILTER_BUTTON_Y_OFFSET + FILTER_BUTTON_MARGIN + BUTTON_HEIGHT)
        val maxVisibleWaypoints = maxOf(5, listAreaHeight / (BUTTON_HEIGHT + BUTTON_SPACING))
        
        val filteredWaypoints = getFilteredWaypoints()
        
        if (filteredWaypoints.isEmpty()) {
            val noWaypointsButton = ButtonWidget.builder(Text.literal("No waypoints found")) {}
                .dimensions(10 + SCROLLBAR_WIDTH + 4, startY + 10, paneWidth - 20 - SCROLLBAR_WIDTH - 4, BUTTON_HEIGHT)
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
        
        // Calculate scrollbar dimensions
        scrollbarY = startY + 10
        scrollbarHeight = listAreaHeight
        
        if (filteredWaypoints.size > maxVisibleWaypoints) {
            val totalContentHeight = filteredWaypoints.size * (BUTTON_HEIGHT + BUTTON_SPACING)
            val visibleRatio = listAreaHeight.toFloat() / totalContentHeight.toFloat()
            scrollbarHandleHeight = (scrollbarHeight * visibleRatio).toInt().coerceAtLeast(20)
            
            val scrollRatio = startIndex.toFloat() / (filteredWaypoints.size - visibleCount).toFloat()
            scrollbarHandleY = scrollbarY + ((scrollbarHeight - scrollbarHandleHeight) * scrollRatio).toInt()
        } else {
            // If all content fits, make the scrollbar handle fill the entire height
            scrollbarHandleHeight = scrollbarHeight
            scrollbarHandleY = scrollbarY
        }
        
        // Add waypoint list entries
        visibleWaypoints.forEach { waypoint ->
            // Create a container panel for each waypoint entry
            val buttonText = buildWaypointButtonText(waypoint)
            val waypointButton = ButtonWidget.builder(buttonText) {
                selectWaypoint(waypoint.name)
            }
                .dimensions(10 + SCROLLBAR_WIDTH + 4, currentY, paneWidth - 60 - SCROLLBAR_WIDTH - 4, BUTTON_HEIGHT)
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
                .dimensions(paneWidth - 50, currentY, 20, BUTTON_HEIGHT)
                .build()
                
            // Add a navigation guidance button
            val isNavigationTarget = WaypointManager.isNavigationTarget(waypoint.name)
            val navigationButton = ButtonWidget.builder(Text.literal(if (isNavigationTarget) "🧭" else "📍")) { button ->
                // Toggle navigation guidance without selecting the waypoint
                if (isNavigationTarget) {
                    WaypointManager.clearNavigationTarget()
                    button.message = Text.literal("📍")
                } else {
                    WaypointManager.setNavigationTarget(waypoint.name)
                    // Update all navigation buttons to ensure only one is active
                    refreshWaypointList(RIGHT_PANE_Y)
                }
                
                // If this waypoint is currently selected, update its details
                if (selectedWaypoint?.name == waypoint.name) {
                    refreshWaypointDetails()
                }
            }
                .dimensions(paneWidth - 30, currentY, 20, BUTTON_HEIGHT)
                .build()
            
            addDrawableChild(waypointButton)
            addDrawableChild(visibilityIndicator)
            addDrawableChild(navigationButton)
            waypointButtons.add(waypointButton)
            waypointButtons.add(visibilityIndicator)
            waypointButtons.add(navigationButton)
            
            currentY += BUTTON_HEIGHT + BUTTON_SPACING
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
    
    /**
     * Creates formatted text for waypoint button with appropriate styling
     * based on whether it's shared or personal
     */
    private fun buildWaypointButtonText(waypoint: WaypointManager.Waypoint): Text {
        val prefix = if (waypoint.isShared) "🌐 " else "🔒 "
        return Text.literal(prefix + waypoint.name)
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
        
        // Navigation guidance toggle
        val isNavigationTarget = WaypointManager.isNavigationTarget(waypoint.name)
        val navigationText = if (isNavigationTarget) "Stop Navigation 🧭" else "Navigate to Waypoint 📍"
        val navigationButton = ButtonWidget.builder(Text.literal(navigationText)) {
            if (isNavigationTarget) {
                WaypointManager.clearNavigationTarget()
            } else {
                WaypointManager.setNavigationTarget(waypoint.name)
                // Refresh the waypoint list to update navigation indicators
                refreshWaypointList(RIGHT_PANE_Y)
            }
            refreshWaypointDetails()
        }
            .dimensions(rightPaneX + 10, RIGHT_PANE_Y + 100, paneWidth - 20, BUTTON_HEIGHT)
            .build()
        addDrawableChild(navigationButton)
        
        // Shared status toggle (only if player owns the waypoint or it's personal)
        if (!waypoint.isShared || waypoint.owner == MinecraftClient.getInstance().player?.uuid) {
            val shareText = if (waypoint.isShared) "Make Personal 🔒" else "Share Waypoint 🌐"
            val shareButton = ButtonWidget.builder(Text.literal(shareText)) {
                // Toggle shared status
                waypoint.isShared = !waypoint.isShared
                
                // If making it shared, set the owner
                if (waypoint.isShared) {
                    waypoint.owner = MinecraftClient.getInstance().player?.uuid
                    
                    // Send to server if connected
                    if (MinecraftClient.getInstance().networkHandler != null) {
                        // Send the waypoint to the server for sharing
                        val success = WayfindrNetworkClient.sendWaypointToServer(waypoint)
                        if (success) {
                            logger.info("Shared waypoint with server: ${waypoint.name}")
                        } else {
                            logger.error("Failed to share waypoint with server: ${waypoint.name}")
                            // Revert the shared status if sending failed
                            waypoint.isShared = false
                            waypoint.owner = null
                        }
                    }
                } else {
                    // If making it personal, we need to delete it from the server if it was previously shared
                    if (MinecraftClient.getInstance().networkHandler != null && waypoint.owner != null) {
                        // Send delete request to server
                        val success = WayfindrNetworkClient.sendWaypointDeleteToServer(waypoint.id)
                        if (success) {
                            logger.info("Removed shared waypoint from server: ${waypoint.name}")
                        } else {
                            logger.error("Failed to remove shared waypoint from server: ${waypoint.name}")
                        }
                    }
                    
                    // Clear the owner
                    waypoint.owner = null
                }
                
                refreshWaypointDetails()
                refreshWaypointList(RIGHT_PANE_Y)
            }
                .dimensions(rightPaneX + 10, RIGHT_PANE_Y + 130, paneWidth - 20, BUTTON_HEIGHT)
                .build()
            addDrawableChild(shareButton)
        }
        
        // Teleport button (if in creative mode)
        val client = MinecraftClient.getInstance()
        val yOffset = if (!waypoint.isShared || waypoint.owner == client.player?.uuid) 160 else 130
        
        if (client.player?.abilities?.creativeMode == true) {
            val teleportButton = ButtonWidget.builder(Text.literal("Teleport")) {
                val pos = waypoint.getPosition()
                val command = "tp ${pos.x.toInt()} ${pos.y.toInt()} ${pos.z.toInt()}"
                client.networkHandler?.sendChatCommand(command)
            }
                .dimensions(rightPaneX + 10, RIGHT_PANE_Y + yOffset, paneWidth - 20, BUTTON_HEIGHT)
                .build()
            addDrawableChild(teleportButton)
        }
        
        // Delete button (only if player owns the waypoint or it's personal)
        if (!waypoint.isShared || waypoint.owner == client.player?.uuid) {
            val deleteYOffset = if (client.player?.abilities?.creativeMode == true) yOffset + 30 else yOffset
            val deleteButton = ButtonWidget.builder(Text.literal("Delete Waypoint")) {
                if (waypoint.isShared) {
                    // Send delete request to server if connected
                    if (MinecraftClient.getInstance().networkHandler != null) {
                        val success = WayfindrNetworkClient.sendWaypointDeleteToServer(waypoint.id)
                        if (success) {
                            logger.info("Sent delete request to server for waypoint: ${waypoint.name}")
                        } else {
                            logger.error("Failed to send delete request to server for waypoint: ${waypoint.name}")
                        }
                    }
                    
                    // Also remove locally
                    WaypointManager.removeWaypoint(waypoint.name)
                } else {
                    // Just remove locally
                    WaypointManager.removeWaypoint(waypoint.name)
                }
                selectedWaypoint = null
                refreshWaypointList(RIGHT_PANE_Y)
                refreshWaypointDetails()
            }
                .dimensions(rightPaneX + 10, RIGHT_PANE_Y + deleteYOffset, paneWidth - 20, BUTTON_HEIGHT)
                .build()
            addDrawableChild(deleteButton)
        }
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)
        
        // Draw divider line
        context.fill(paneWidth, 0, paneWidth + 1, height, 0xFFAAAAAA.toInt())
        
        val filteredWaypoints = getFilteredWaypoints()
        
        // Draw scrollbar if needed
        if (filteredWaypoints.size > 0) {
            // Draw scrollbar background
            context.fill(10, scrollbarY, 10 + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, 0xFF333333.toInt())
            
            // Draw scrollbar handle
            val handleColor = if (isDraggingScrollbar) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt()
            context.fill(10, scrollbarHandleY, 10 + SCROLLBAR_WIDTH, scrollbarHandleY + scrollbarHandleHeight, handleColor)
        }
        
        super.render(context, mouseX, mouseY, delta)
        
        // Draw title centered in the left pane
        context.drawCenteredTextWithShadow(textRenderer, title, paneWidth / 2, 6, 0xFFFFFF)
        
        // Draw waypoint count with breakdown
        val personalCount = WaypointManager.waypoints.count { !it.isShared }
        val sharedCount = WaypointManager.waypoints.count { it.isShared }
        val waypointCountText = "${WaypointManager.waypoints.size} Waypoints (${personalCount} Personal, ${sharedCount} Shared)"
        context.drawTextWithShadow(textRenderer, waypointCountText, 10, height - 20, 0xAAAAAA)
        
        // Draw selected waypoint details
        selectedWaypoint?.let { waypoint ->
            // Draw waypoint name with shared/personal indicator
            val namePrefix = if (waypoint.isShared) "🌐 " else "🔒 "
            context.drawTextWithShadow(
                textRenderer, 
                Text.literal(namePrefix + waypoint.name), 
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
            
            // Draw owner info if shared
            if (waypoint.isShared && waypoint.owner != null) {
                val ownerName = getPlayerNameFromUUID(waypoint.owner!!)
                val ownerText = "Owner: $ownerName"
                context.drawTextWithShadow(
                    textRenderer,
                    ownerText,
                    rightPaneX + 10,
                    RIGHT_PANE_Y + 25 + textRenderer.fontHeight + 2,
                    0xAAAAAA
                )
            }
        }
    }
    
    /**
     * Gets player name from UUID, or returns "Unknown Player" if not found
     */
    private fun getPlayerNameFromUUID(uuid: UUID): String {
        val client = MinecraftClient.getInstance()
        
        // Try to find in current player list
        client.networkHandler?.playerList?.find { it.profile.id == uuid }?.let {
            return it.profile.name
        }
        
        // If player is local client
        if (client.player?.uuid == uuid) {
            return client.player?.name?.string ?: "Unknown Player"
        }
        
        return "Unknown Player"
    }
    
    private fun getWaypointNameFromPosition(y: Int): String {
        val filteredWaypoints = getFilteredWaypoints()
        
        if (filteredWaypoints.isEmpty()) return ""
        
        val listAreaHeight = height - RIGHT_PANE_Y - (FILTER_BUTTON_Y_OFFSET + FILTER_BUTTON_MARGIN + BUTTON_HEIGHT)
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
                val listAreaHeight = height - RIGHT_PANE_Y - (FILTER_BUTTON_Y_OFFSET + FILTER_BUTTON_MARGIN + BUTTON_HEIGHT)
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
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Check if click is on scrollbar
        if (button == 0 && mouseX >= 10 && mouseX <= 10 + SCROLLBAR_WIDTH && 
            mouseY >= scrollbarHandleY && mouseY <= scrollbarHandleY + scrollbarHandleHeight) {
            isDraggingScrollbar = true
            lastMouseY = mouseY
            return true
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }
    
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && isDraggingScrollbar) {
            isDraggingScrollbar = false
            return true
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }
    
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (isDraggingScrollbar) {
            val filteredWaypoints = getFilteredWaypoints()
            val listAreaHeight = height - RIGHT_PANE_Y - (FILTER_BUTTON_Y_OFFSET + FILTER_BUTTON_MARGIN + BUTTON_HEIGHT)
            val maxVisibleWaypoints = maxOf(5, listAreaHeight / (BUTTON_HEIGHT + BUTTON_SPACING))
            
            if (filteredWaypoints.size > maxVisibleWaypoints) {
                val scrollableHeight = scrollbarHeight - scrollbarHandleHeight
                val deltaScroll = mouseY - lastMouseY
                
                if (scrollableHeight > 0) {
                    val scrollRatio = deltaScroll / scrollableHeight
                    val scrollAmount = (scrollRatio * (filteredWaypoints.size - maxVisibleWaypoints)).toInt()
                    
                    if (scrollAmount != 0) {
                        scrollOffset = (scrollOffset + scrollAmount).coerceIn(0, filteredWaypoints.size - maxVisibleWaypoints)
                        refreshWaypointList(RIGHT_PANE_Y)
                        lastMouseY = mouseY
                    }
                }
            }
            return true
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }
    
    override fun shouldPause(): Boolean = false
    
    override fun close() {
        // Save scroll position to manager
        WaypointManager.setWaypointListScrollPosition(scrollOffset)
        super.close()
    }
    
    private fun getFilteredWaypoints(): List<WaypointManager.Waypoint> {
        var waypoints = WaypointManager.waypoints
        
        // Apply search filter
        if (this::searchBox.isInitialized && searchBox.text.isNotEmpty()) {
            waypoints = waypoints.filter { 
                it.name.lowercase(Locale.getDefault()).contains(searchBox.text.lowercase(Locale.getDefault())) 
            }.toMutableList()
        }
        
        // Apply shared/personal filters
        waypoints = waypoints.filter {
            (it.isShared && showSharedWaypoints) || (!it.isShared && showPersonalWaypoints)
        }.toMutableList()
        
        return waypoints
    }
}
