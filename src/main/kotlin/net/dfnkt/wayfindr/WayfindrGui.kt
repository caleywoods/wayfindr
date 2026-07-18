package net.dfnkt.wayfindr

import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.client.Minecraft
import org.slf4j.LoggerFactory
import java.util.*

class WayfindrGui : Screen(Component.literal("Waypoint Manager")) {
    private val logger = LoggerFactory.getLogger("wayfindr")
    private var waypointButtons = mutableListOf<Button>()
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
    private lateinit var searchBox: EditBox
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
        this.searchBox = EditBox(
            this.font,
            10,
            22,
            paneWidth - 20,
            20,
            Component.literal("Search waypoints...")
        )
        this.searchBox.setResponder { text ->
            refreshWaypointList(RIGHT_PANE_Y)
        }
        addRenderableWidget(this.searchBox)
        setInitialFocus(this.searchBox)

        // Filter buttons
        addRenderableWidget(
            Button.builder(Component.literal(if (showPersonalWaypoints) "[x] Personal" else "[ ] Personal")) { button ->
                showPersonalWaypoints = !showPersonalWaypoints
                button.message = Component.literal(if (showPersonalWaypoints) "[x] Personal" else "[ ] Personal")
                refreshWaypointList(RIGHT_PANE_Y)
            }
                .bounds(10, height - FILTER_BUTTON_Y_OFFSET, paneWidth / 2 - 15, BUTTON_HEIGHT)
                .build()
        )

        addRenderableWidget(
            Button.builder(Component.literal(if (showSharedWaypoints) "[x] Shared" else "[ ] Shared")) { button ->
                showSharedWaypoints = !showSharedWaypoints
                button.message = Component.literal(if (showSharedWaypoints) "[x] Shared" else "[ ] Shared")
                refreshWaypointList(RIGHT_PANE_Y)
            }
                .bounds(paneWidth / 2 + 5, height - FILTER_BUTTON_Y_OFFSET, paneWidth / 2 - 15, BUTTON_HEIGHT)
                .build()
        )

        // Add waypoint button
        addRenderableWidget(
            Button.builder(Component.literal("+ Add Waypoint")) {
                val client = Minecraft.getInstance()
                val player = client.player
                if (player != null) {
                    val pos = player.position()
                    val name = "Waypoint ${WaypointManager.waypoints.size + 1}"
                    WaypointManager.addWaypoint(name, pos)
                    refreshWaypointList(RIGHT_PANE_Y)
                    selectWaypoint(WaypointManager.waypoints.last().id)
                }
            }
                .bounds(rightPaneX + 10, RIGHT_PANE_Y, paneWidth - 20, BUTTON_HEIGHT)
                .build()
        )

        // Settings button
        addRenderableWidget(
            Button.builder(Component.literal("Settings")) {
                minecraft?.setScreenAndShow(WayfindrConfigScreen(this))
            }
                .bounds(rightPaneX + 10, height - 40, (paneWidth / 2) - 15, BUTTON_HEIGHT)
                .build()
        )

        // Close button
        addRenderableWidget(
            Button.builder(Component.literal("Close")) { onClose() }
                .bounds(rightPaneX + (paneWidth / 2) + 5, height - 40, (paneWidth / 2) - 15, BUTTON_HEIGHT)
                .build()
        )

        refreshWaypointList(RIGHT_PANE_Y)
    }

    private fun refreshWaypointList(startY: Int) {
        waypointButtons.forEach { removeWidget(it) }
        waypointButtons.clear()

        val listAreaHeight = height - startY - (FILTER_BUTTON_Y_OFFSET + FILTER_BUTTON_MARGIN + BUTTON_HEIGHT)
        val maxVisibleWaypoints = maxOf(5, listAreaHeight / (BUTTON_HEIGHT + BUTTON_SPACING))

        val filteredWaypoints = getFilteredWaypoints()

        if (filteredWaypoints.isEmpty()) {
            val noWaypointsButton = Button.builder(Component.literal("No waypoints found")) {}
                .bounds(10 + SCROLLBAR_WIDTH + 4, startY + 10, paneWidth - 20 - SCROLLBAR_WIDTH - 4, BUTTON_HEIGHT)
                .build()
            addRenderableWidget(noWaypointsButton)
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
            val waypointButton = Button.builder(buttonText) {
                selectWaypoint(waypoint.id)
            }
                .bounds(10 + SCROLLBAR_WIDTH + 4, currentY, paneWidth - 60 - SCROLLBAR_WIDTH - 4, BUTTON_HEIGHT)
                .build()

            // Add a visibility indicator
            val visibilityIndicator = Button.builder(Component.literal(if (waypoint.visible) "1" else "0")) { button ->
                // Toggle visibility without selecting the waypoint
                val success = WaypointManager.toggleWaypointVisibility(waypoint.id)
                if (success) {
                    // Update the button text to reflect the new visibility state
                    val updatedWaypoint = WaypointManager.getWaypoint(waypoint.id)
                    button.message = Component.literal(if (updatedWaypoint?.visible == true) "1" else "0")

                    // If this waypoint is currently selected, update its details
                    if (selectedWaypoint?.id == waypoint.id) {
                        selectedWaypoint = updatedWaypoint
                        refreshWaypointDetails()
                    }
                }
            }
                .bounds(paneWidth - 50, currentY, 20, BUTTON_HEIGHT)
                .build()

            // Add a navigation guidance button
            val isNavigationTarget = WaypointManager.isNavigationTarget(waypoint.id)
            val navigationButton = Button.builder(Component.literal(if (isNavigationTarget) "*" else ">")) { button ->
                // Toggle navigation guidance without selecting the waypoint
                if (isNavigationTarget) {
                    WaypointManager.clearNavigationTarget()
                    button.message = Component.literal(">")
                } else {
                    WaypointManager.setNavigationTarget(waypoint.id)
                    // Update all navigation buttons to ensure only one is active
                    refreshWaypointList(RIGHT_PANE_Y)
                }

                // If this waypoint is currently selected, update its details
                if (selectedWaypoint?.id == waypoint.id) {
                    refreshWaypointDetails()
                }
            }
                .bounds(paneWidth - 30, currentY, 20, BUTTON_HEIGHT)
                .build()

            addRenderableWidget(waypointButton)
            addRenderableWidget(visibilityIndicator)
            addRenderableWidget(navigationButton)
            waypointButtons.add(waypointButton)
            waypointButtons.add(visibilityIndicator)
            waypointButtons.add(navigationButton)

            currentY += BUTTON_HEIGHT + BUTTON_SPACING
        }

        // If we had a selected waypoint, try to keep it selected
        selectedWaypoint?.let { selected ->
            if (filteredWaypoints.any { it.id == selected.id }) {
                selectWaypoint(selected.id)
            } else if (filteredWaypoints.isNotEmpty()) {
                selectWaypoint(filteredWaypoints[0].id)
            }
        }
    }

    /**
     * Creates formatted text for waypoint button with appropriate styling
     * based on whether it's shared or personal
     */
    private fun buildWaypointButtonText(waypoint: WaypointManager.Waypoint): Component {
        val prefix = if (waypoint.isShared) "[Shared] " else "[Personal] "
        return Component.literal(prefix + waypoint.name)
    }

    private fun selectWaypoint(id: UUID) {
        selectedWaypoint = WaypointManager.getWaypoint(id)
        refreshWaypointDetails()
    }

    private fun refreshWaypointDetails() {
        // Remove previous detail buttons
        for (child in children().toList()) {
            if (child is Button && child.x >= rightPaneX && !waypointButtons.contains(child)) {
                removeWidget(child)
            }
        }

        val waypoint = selectedWaypoint ?: return

        // Waypoint name
        val nameButton = Button.builder(Component.literal("Rename")) {
            minecraft?.setScreenAndShow(WayfindrRenameScreen(this, waypoint.id, waypoint.name))
        }
            .bounds(rightPaneX + 10, RIGHT_PANE_Y + 40, paneWidth - 20, BUTTON_HEIGHT)
            .build()
        addRenderableWidget(nameButton)

        // Visibility toggle
        val visibilityText = if (waypoint.visible) "Hide Waypoint" else "Show Waypoint"
        val visibilityButton = Button.builder(Component.literal(visibilityText)) {
            WaypointManager.toggleWaypointVisibility(waypoint.id)
            refreshWaypointDetails()
        }
            .bounds(rightPaneX + 10, RIGHT_PANE_Y + 70, paneWidth - 20, BUTTON_HEIGHT)
            .build()
        addRenderableWidget(visibilityButton)

        // Navigation guidance toggle
        val isNavigationTarget = WaypointManager.isNavigationTarget(waypoint.id)
        val navigationText = if (isNavigationTarget) "Stop Navigation" else "Navigate to Waypoint"
        val navigationButton = Button.builder(Component.literal(navigationText)) {
            if (isNavigationTarget) {
                WaypointManager.clearNavigationTarget()
            } else {
                WaypointManager.setNavigationTarget(waypoint.id)
                // Refresh the waypoint list to update navigation indicators
                refreshWaypointList(RIGHT_PANE_Y)
            }
            refreshWaypointDetails()
        }
            .bounds(rightPaneX + 10, RIGHT_PANE_Y + 100, paneWidth - 20, BUTTON_HEIGHT)
            .build()
        addRenderableWidget(navigationButton)

        // Shared status toggle (only if player owns the waypoint or it's personal)
        if (!waypoint.isShared || waypoint.owner == Minecraft.getInstance().player?.uuid) {
            val shareText = if (waypoint.isShared) "Make Personal" else "Share Waypoint"
            val shareButton = Button.builder(Component.literal(shareText)) {
                // Toggle shared status
                waypoint.isShared = !waypoint.isShared

                // If making it shared, set the owner
                if (waypoint.isShared) {
                    waypoint.owner = Minecraft.getInstance().player?.uuid

                    // Send to server if connected
                    if (Minecraft.getInstance().connection != null) {
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
                    if (Minecraft.getInstance().connection != null && waypoint.owner != null) {
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

                // Persist the toggled shared/personal state to disk. Without this the
                // change lives only in memory and can be lost on reload.
                WaypointManager.updateWaypoint(waypoint)

                refreshWaypointDetails()
                refreshWaypointList(RIGHT_PANE_Y)
            }
                .bounds(rightPaneX + 10, RIGHT_PANE_Y + 130, paneWidth - 20, BUTTON_HEIGHT)
                .build()
            addRenderableWidget(shareButton)
        }

        // Teleport button (if in creative mode)
        val client = Minecraft.getInstance()
        val yOffset = if (!waypoint.isShared || waypoint.owner == client.player?.uuid) 160 else 130

        if (client.player?.abilities?.instabuild == true) {
            val teleportButton = Button.builder(Component.literal("Teleport")) {
                val pos = waypoint.getPosition()
                val command = "tp ${pos.x.toInt()} ${pos.y.toInt()} ${pos.z.toInt()}"
                client.connection?.sendCommand(command)
            }
                .bounds(rightPaneX + 10, RIGHT_PANE_Y + yOffset, paneWidth - 20, BUTTON_HEIGHT)
                .build()
            addRenderableWidget(teleportButton)
        }

        // Delete button (only if player owns the waypoint or it's personal)
        if (!waypoint.isShared || waypoint.owner == client.player?.uuid) {
            val deleteYOffset = if (client.player?.abilities?.instabuild == true) yOffset + 30 else yOffset
            val deleteButton = Button.builder(Component.literal("Delete Waypoint")) {
                if (waypoint.isShared) {
                    // Send delete request to server if connected
                    if (Minecraft.getInstance().connection != null) {
                        val success = WayfindrNetworkClient.sendWaypointDeleteToServer(waypoint.id)
                        if (success) {
                            logger.info("Sent delete request to server for waypoint: ${waypoint.name}")
                        } else {
                            logger.error("Failed to send delete request to server for waypoint: ${waypoint.name}")
                        }
                    }

                    // Also remove locally
                    WaypointManager.removeWaypoint(waypoint.id)
                } else {
                    // Just remove locally
                    WaypointManager.removeWaypoint(waypoint.id)
                }
                selectedWaypoint = null
                refreshWaypointList(RIGHT_PANE_Y)
                refreshWaypointDetails()
            }
                .bounds(rightPaneX + 10, RIGHT_PANE_Y + deleteYOffset, paneWidth - 20, BUTTON_HEIGHT)
                .build()
            addRenderableWidget(deleteButton)
        }
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(context, mouseX, mouseY, delta)

        // Draw divider line
        context.fill(paneWidth, 0, paneWidth + 1, height, 0xFFAAAAAA.toInt())

        val filteredWaypoints = getFilteredWaypoints()

        // Draw scrollbar if needed
        if (filteredWaypoints.isNotEmpty()) {
            // Draw scrollbar background
            context.fill(10, scrollbarY, 10 + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, 0xFF333333.toInt())

            // Draw scrollbar handle
            val handleColor = if (isDraggingScrollbar) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt()
            context.fill(10, scrollbarHandleY, 10 + SCROLLBAR_WIDTH, scrollbarHandleY + scrollbarHandleHeight, handleColor)
        }

        // Draw title centered in the left pane
        context.centeredText(font, title, paneWidth / 2, 6, 0xFFFFFFFF.toInt())

        // Draw waypoint count with breakdown
        val personalCount = WaypointManager.waypoints.count { !it.isShared }
        val sharedCount = WaypointManager.waypoints.count { it.isShared }
        val waypointCountText = "${WaypointManager.waypoints.size} Waypoints ($personalCount Personal, $sharedCount Shared)"
        context.text(font, waypointCountText, 10, height - 20, 0xFFAAAAAA.toInt(), true)

        // Draw selected waypoint details
        selectedWaypoint?.let { waypoint ->
            // Draw waypoint name with shared/personal indicator
            val namePrefix = if (waypoint.isShared) "[Shared] " else "[Personal] "
            context.text(
                font,
                Component.literal(namePrefix + waypoint.name),
                rightPaneX + 10,
                RIGHT_PANE_Y + 10,
                0xFFFFFFFF.toInt(),
                true
            )

            // Draw coordinates
            val pos = waypoint.getPosition()
            val coordsText = "X: ${pos.x.toInt()}, Y: ${pos.y.toInt()}, Z: ${pos.z.toInt()}"
            context.text(
                font,
                coordsText,
                rightPaneX + 10,
                RIGHT_PANE_Y + 25,
                0xFFAAAAAA.toInt(),
                true
            )

            // Draw owner info if shared
            if (waypoint.isShared && waypoint.owner != null) {
                val ownerName = getPlayerNameFromUUID(waypoint.owner!!)
                val ownerText = "Owner: $ownerName"
                context.text(
                    font,
                    ownerText,
                    rightPaneX + 10,
                    RIGHT_PANE_Y + 25 + font.lineHeight + 2,
                    0xFFAAAAAA.toInt(),
                    true
                )
            }
        }
    }

    /**
     * Gets player name from UUID, or returns "Unknown Player" if not found
     */
    private fun getPlayerNameFromUUID(uuid: UUID): String {
        val client = Minecraft.getInstance()

        // Try to find in current player list
        client.connection?.onlinePlayers?.find { it.profile.id == uuid }?.let {
            return it.profile.name
        }

        // If player is local client
        if (client.player?.uuid == uuid) {
            return client.player?.name?.string ?: "Unknown Player"
        }

        return "Unknown Player"
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

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        // Check if click is on scrollbar
        if (event.button() == 0 && event.x() >= 10 && event.x() <= 10 + SCROLLBAR_WIDTH &&
            event.y() >= scrollbarHandleY && event.y() <= scrollbarHandleY + scrollbarHandleHeight) {
            isDraggingScrollbar = true
            lastMouseY = event.y()
            return true
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (event.button() == 0 && isDraggingScrollbar) {
            isDraggingScrollbar = false
            return true
        }
        return super.mouseReleased(event)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        if (isDraggingScrollbar) {
            val filteredWaypoints = getFilteredWaypoints()

            if (filteredWaypoints.isNotEmpty()) {
                val listAreaHeight = height - RIGHT_PANE_Y - (FILTER_BUTTON_Y_OFFSET + FILTER_BUTTON_MARGIN + BUTTON_HEIGHT)
                val maxVisibleWaypoints = maxOf(5, listAreaHeight / (BUTTON_HEIGHT + BUTTON_SPACING))

                if (filteredWaypoints.size > maxVisibleWaypoints) {
                    val scrollableHeight = scrollbarHeight - scrollbarHandleHeight
                    val deltaScroll = event.y() - lastMouseY

                    if (scrollableHeight > 0) {
                        val scrollRatio = deltaScroll / scrollableHeight
                        val scrollAmount = (scrollRatio * (filteredWaypoints.size - maxVisibleWaypoints)).toInt()

                        if (scrollAmount != 0) {
                            scrollOffset = (scrollOffset + scrollAmount).coerceIn(0, filteredWaypoints.size - maxVisibleWaypoints)
                            refreshWaypointList(RIGHT_PANE_Y)
                            lastMouseY = event.y()
                        }
                    }
                }
            }
            return true
        }
        return super.mouseDragged(event, dragX, dragY)
    }

    override fun isPauseScreen(): Boolean = false

    override fun onClose() {
        // Save scroll position to manager
        WaypointManager.setWaypointListScrollPosition(scrollOffset)
        super.onClose()
    }

    private fun getFilteredWaypoints(): List<WaypointManager.Waypoint> {
        var waypoints = WaypointManager.waypoints

        // Apply search filter
        if (this::searchBox.isInitialized && searchBox.value.isNotEmpty()) {
            waypoints = waypoints.filter {
                it.name.lowercase(Locale.getDefault()).contains(searchBox.value.lowercase(Locale.getDefault()))
            }.toMutableList()
        }

        // Apply shared/personal filters
        waypoints = waypoints.filter {
            (it.isShared && showSharedWaypoints) || (!it.isShared && showPersonalWaypoints)
        }.toMutableList()

        return waypoints
    }
}
