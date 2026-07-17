package net.dfnkt.wayfindr

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW

object WayfindrKeybinds {
    // Custom keybind category for Wayfindr
    private val CATEGORY: KeyMapping.Category by lazy {
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Wayfindr.MOD_ID, "general"))
    }

    // Use lazy initialization to ensure keybinds are only registered once
    val OPEN_WAYPOINT_MENU: KeyMapping by lazy {
        KeyMappingHelper.registerKeyMapping(
            KeyMapping(
                "key.wayfindr.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M, // Default to 'M' key
                CATEGORY
            )
        )
    }

    val QUICK_ADD_WAYPOINT: KeyMapping by lazy {
        KeyMappingHelper.registerKeyMapping(
            KeyMapping(
                "key.wayfindr.quick_add",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_N, // Default to 'N' key
                CATEGORY
            )
        )
    }

    // Call this method early in the mod initialization process
    fun initialize() {
        // Force initialization of the lazy properties
        OPEN_WAYPOINT_MENU
        QUICK_ADD_WAYPOINT
    }
}
