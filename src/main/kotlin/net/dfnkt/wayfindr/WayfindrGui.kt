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

    // Proportional layout metrics — derived from the current viewport (width/height) so
    // the UI scales with the window and GUI scale instead of using fixed pixel offsets.
    // Each is clamped to a sensible pixel range ("within reason") so it stays usable at
    // both tiny and huge resolutions. Implemented as getters so they always reflect the
    // latest size, including after a resize.
    private val edgeMargin: Int get() = (width * 0.012f).toInt().coerceIn(6, 16)
    private val vGap: Int get() = (height * 0.012f).toInt().coerceIn(3, 10)
    private val titleY: Int get() = (height * 0.012f).toInt().coerceIn(4, 12)
    private val searchY: Int get() = titleY + font.lineHeight + vGap
    private val contentTop: Int get() = searchY + BUTTON_HEIGHT + vGap
    private val countTextY: Int get() = height - font.lineHeight - vGap
    private val bottomRowY: Int get() = countTextY - BUTTON_HEIGHT - vGap

    // Kept for the many call sites that pass the content-start Y around; it now tracks
    // the proportional content top rather than a fixed 48px.
    private val RIGHT_PANE_Y: Int get() = contentTop

    // Left-pane list geometry, shared between drawing, hit-testing and layout.
    private val scrollbarX: Int get() = edgeMargin
    private val listEntryX: Int get() = edgeMargin + SCROLLBAR_WIDTH + 4

    // Height available for the scrollable list, and how many entries fit in it.
    private val listAreaHeight: Int get() = (bottomRowY - vGap) - RIGHT_PANE_Y
    private fun maxVisibleWaypoints(): Int = maxOf(1, listAreaHeight / (BUTTON_HEIGHT + BUTTON_SPACING))

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
            edgeMargin,
            searchY,
            paneWidth - edgeMargin * 2,
            BUTTON_HEIGHT,
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
                .bounds(edgeMargin, bottomRowY, paneWidth / 2 - edgeMargin - vGap, BUTTON_HEIGHT)
                .build()
        )

        addRenderableWidget(
            Button.builder(Component.literal(if (showSharedWaypoints) "[x] Shared" else "[ ] Shared")) { button ->
                showSharedWaypoints = !showSharedWaypoints
                button.message = Component.literal(if (showSharedWaypoints) "[x] Shared" else "[ ] Shared")
                refreshWaypointList(RIGHT_PANE_Y)
            }
                .bounds(paneWidth / 2 + vGap, bottomRowY, paneWidth / 2 - edgeMargin - vGap, BUTTON_HEIGHT)
                .build()
        )

        // Default right-pane buttons (Add / Settings / Close). Shown whenever no
        // waypoint is selected; removed while a waypoint's details are open.
        addDefaultRightPaneButtons()

        refreshWaypointList(RIGHT_PANE_Y)
    }

    /**
     * Adds the right-pane buttons shown when no waypoint is selected: Add Waypoint,
     * Settings and Close. These are removed by [refreshWaypointDetails] when a waypoint
     * is selected and re-added when it is deselected.
     */
    private fun addDefaultRightPaneButtons() {
        addRenderableWidget(
            Button.builder(Component.literal("+ Add Waypoint")) {
                val client = Minecraft.getInstance()
                val player = client.player
                if (player != null) {
                    val pos = player.position()
                    val name = "Waypoint ${WaypointManager.waypoints.size + 1}"
                    val dimension = player.level().dimension().identifier().toString()
                    WaypointManager.addWaypoint(name, pos, dimension = dimension)
                    refreshWaypointList(RIGHT_PANE_Y)
                    selectWaypoint(WaypointManager.waypoints.last().id)
                }
            }
                .bounds(rightPaneX + edgeMargin, RIGHT_PANE_Y, paneWidth - edgeMargin * 2, BUTTON_HEIGHT)
                .build()
        )

        addRenderableWidget(
            Button.builder(Component.literal("Settings")) {
                minecraft?.setScreenAndShow(WayfindrConfigScreen(this))
            }
                .bounds(rightPaneX + edgeMargin, bottomRowY, paneWidth / 2 - edgeMargin - vGap, BUTTON_HEIGHT)
                .build()
        )

        addRenderableWidget(
            Button.builder(Component.literal("Close")) { onClose() }
                .bounds(rightPaneX + paneWidth / 2 + vGap, bottomRowY, paneWidth / 2 - edgeMargin - vGap, BUTTON_HEIGHT)
                .build()
        )
    }

    private fun refreshWaypointList(startY: Int) {
        waypointButtons.forEach { removeWidget(it) }
        waypointButtons.clear()

        val maxVisibleWaypoints = maxVisibleWaypoints()

        val filteredWaypoints = getFilteredWaypoints()

        if (filteredWaypoints.isEmpty()) {
            val noWaypointsButton = Button.builder(Component.literal("No waypoints found")) {}
                .bounds(listEntryX, startY + vGap, paneWidth - edgeMargin - listEntryX, BUTTON_HEIGHT)
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

        var currentY = startY + vGap

        // Calculate scrollbar dimensions
        scrollbarY = startY + vGap
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

        // Two 20px action buttons (visibility, navigation) sit at the right edge of the
        // left pane; the entry button fills the space between the list start and them.
        val navBtnX = paneWidth - edgeMargin - 20
        val visBtnX = navBtnX - 24
        val entryWidth = (visBtnX - 4) - listEntryX

        // Add waypoint list entries
        visibleWaypoints.forEach { waypoint ->
            // Create a container panel for each waypoint entry
            val buttonText = buildWaypointButtonText(waypoint)
            val waypointButton = Button.builder(buttonText) {
                // Clicking the already-selected waypoint deselects it, returning to the
                // default right pane (with the Settings button) without reopening.
                if (selectedWaypoint?.id == waypoint.id) {
                    selectedWaypoint = null
                    refreshWaypointDetails()
                } else {
                    selectWaypoint(waypoint.id)
                }
            }
                .bounds(listEntryX, currentY, entryWidth, BUTTON_HEIGHT)
                .build()

            // Add a visibility indicator
            val visibilityIndicator = Button.builder(Component.literal(if (waypoint.visible) "1" else "0")) {
                // Toggle visibility, then rebuild the list (and, via re-select, the
                // details pane) so both stay in sync regardless of where it was toggled.
                WaypointManager.toggleWaypointVisibility(waypoint.id)
                refreshWaypointList(RIGHT_PANE_Y)
            }
                .bounds(visBtnX, currentY, 20, BUTTON_HEIGHT)
                .build()

            // Add a navigation guidance button
            val isNavigationTarget = WaypointManager.isNavigationTarget(waypoint.id)
            val navigationButton = Button.builder(Component.literal(if (isNavigationTarget) "*" else ">")) {
                // Toggle navigation guidance, then rebuild the list (and details) so the
                // active indicator stays in sync across both panes.
                if (isNavigationTarget) {
                    WaypointManager.clearNavigationTarget()
                } else {
                    WaypointManager.setNavigationTarget(waypoint.id)
                }
                refreshWaypointList(RIGHT_PANE_Y)
            }
                .bounds(navBtnX, currentY, 20, BUTTON_HEIGHT)
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

        // No selection: show the default right-pane buttons (Add / Settings / Close).
        val waypoint = selectedWaypoint ?: run {
            addDefaultRightPaneButtons()
            return
        }

        val client = Minecraft.getInstance()
        val ownsOrPersonal = !waypoint.isShared || waypoint.owner == client.player?.uuid
        val teleportShown = WayfindrConfig.get().enableTeleport

        // Detail-pane buttons flow top-down from a running Y so they always pack
        // together without overlap or gaps, regardless of which optional buttons
        // (Share, Teleport, Delete) are present. The stride is computed from the
        // current screen height and the number of buttons actually shown, so the
        // layout stays on screen at any resolution / GUI scale — tighter on small
        // screens, roomier on large ones.
        val buttonCount = 3 + (if (ownsOrPersonal) 1 else 0) +
            (if (teleportShown) 1 else 0) + (if (ownsOrPersonal) 1 else 0)
        // Start below the name + coords (and owner, if shown) header lines; end above
        // the bottom Settings/Close row. Both bounds are proportional to the viewport.
        val headerLines = if (waypoint.isShared && waypoint.owner != null) 3 else 2
        val topY = contentTop + headerLines * (font.lineHeight + 1) + vGap
        val bottomLimit = bottomRowY - vGap
        val detailStride = ((bottomLimit - topY) / buttonCount).coerceIn(BUTTON_HEIGHT + 1, BUTTON_HEIGHT + 8)
        var detailY = topY

        // Waypoint name
        val nameButton = Button.builder(Component.literal("Rename")) {
            minecraft?.setScreenAndShow(WayfindrRenameScreen(this, waypoint.id, waypoint.name))
        }
            .bounds(rightPaneX + edgeMargin, detailY, paneWidth - edgeMargin * 2, BUTTON_HEIGHT)
            .build()
        addRenderableWidget(nameButton)
        detailY += detailStride

        // Visibility toggle
        val visibilityText = if (waypoint.visible) "Hide Waypoint" else "Show Waypoint"
        val visibilityButton = Button.builder(Component.literal(visibilityText)) {
            WaypointManager.toggleWaypointVisibility(waypoint.id)
            // Rebuild the list too so its "1"/"0" indicator matches this pane.
            refreshWaypointList(RIGHT_PANE_Y)
        }
            .bounds(rightPaneX + edgeMargin, detailY, paneWidth - edgeMargin * 2, BUTTON_HEIGHT)
            .build()
        addRenderableWidget(visibilityButton)
        detailY += detailStride

        // Navigation guidance toggle
        val isNavigationTarget = WaypointManager.isNavigationTarget(waypoint.id)
        val navigationText = if (isNavigationTarget) "Stop Navigation" else "Navigate to Waypoint"
        val navigationButton = Button.builder(Component.literal(navigationText)) {
            if (isNavigationTarget) {
                WaypointManager.clearNavigationTarget()
            } else {
                WaypointManager.setNavigationTarget(waypoint.id)
            }
            // Rebuild the list (and details, via re-select) so the nav indicator in
            // both panes reflects the change — including when stopping navigation.
            refreshWaypointList(RIGHT_PANE_Y)
        }
            .bounds(rightPaneX + edgeMargin, detailY, paneWidth - edgeMargin * 2, BUTTON_HEIGHT)
            .build()
        addRenderableWidget(navigationButton)
        detailY += detailStride

        // Shared status toggle (only if player owns the waypoint or it's personal)
        if (ownsOrPersonal) {
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
                .bounds(rightPaneX + edgeMargin, detailY, paneWidth - edgeMargin * 2, BUTTON_HEIGHT)
                .build()
            addRenderableWidget(shareButton)
            detailY += detailStride
        }

        // Teleport button — shown when enabled in config. Op status can't be reliably
        // detected client-side in 26.2, so the server enforces /tp permission at
        // execution time; a non-op who clicks it just gets a harmless "no permission".
        if (teleportShown) {
            val teleportButton = Button.builder(Component.literal("Teleport")) {
                val pos = waypoint.getPosition()
                val command = "tp ${pos.x.toInt()} ${pos.y.toInt()} ${pos.z.toInt()}"
                client.connection?.sendCommand(command)
            }
                .bounds(rightPaneX + edgeMargin, detailY, paneWidth - edgeMargin * 2, BUTTON_HEIGHT)
                .build()
            addRenderableWidget(teleportButton)
            detailY += detailStride
        }

        // Delete button (only if player owns the waypoint or it's personal)
        if (ownsOrPersonal) {
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
                .bounds(rightPaneX + edgeMargin, detailY, paneWidth - edgeMargin * 2, BUTTON_HEIGHT)
                .build()
            addRenderableWidget(deleteButton)
            detailY += detailStride
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
            context.fill(scrollbarX, scrollbarY, scrollbarX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, 0xFF333333.toInt())

            // Draw scrollbar handle
            val handleColor = if (isDraggingScrollbar) 0xFFFFFFFF.toInt() else 0xFFAAAAAA.toInt()
            context.fill(scrollbarX, scrollbarHandleY, scrollbarX + SCROLLBAR_WIDTH, scrollbarHandleY + scrollbarHandleHeight, handleColor)
        }

        // Draw title centered in the left pane
        context.centeredText(font, title, paneWidth / 2, titleY, 0xFFFFFFFF.toInt())

        // Draw waypoint count with breakdown
        val personalCount = WaypointManager.waypoints.count { !it.isShared }
        val sharedCount = WaypointManager.waypoints.count { it.isShared }
        val waypointCountText = "${WaypointManager.waypoints.size} Waypoints ($personalCount Personal, $sharedCount Shared)"
        context.text(font, waypointCountText, edgeMargin, countTextY, 0xFFAAAAAA.toInt(), true)

        // Draw selected waypoint details
        selectedWaypoint?.let { waypoint ->
            // Draw waypoint name with shared/personal indicator
            val namePrefix = if (waypoint.isShared) "[Shared] " else "[Personal] "
            val lineStep = font.lineHeight + 1
            context.text(
                font,
                Component.literal(namePrefix + waypoint.name),
                rightPaneX + edgeMargin,
                contentTop,
                0xFFFFFFFF.toInt(),
                true
            )

            // Draw coordinates
            val pos = waypoint.getPosition()
            val coordsText = "X: ${pos.x.toInt()}, Y: ${pos.y.toInt()}, Z: ${pos.z.toInt()}"
            context.text(
                font,
                coordsText,
                rightPaneX + edgeMargin,
                contentTop + lineStep,
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
                    rightPaneX + edgeMargin,
                    contentTop + lineStep * 2,
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
                val maxVisibleWaypoints = maxVisibleWaypoints()

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
        if (event.button() == 0 && event.x() >= scrollbarX && event.x() <= scrollbarX + SCROLLBAR_WIDTH &&
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
                val maxVisibleWaypoints = maxVisibleWaypoints()

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
