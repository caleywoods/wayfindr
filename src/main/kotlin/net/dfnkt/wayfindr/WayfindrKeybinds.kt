package net.dfnkt.wayfindr

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

object WayfindrKeybinds {
    // Use lazy initialization to ensure keybinds are only registered once
    val OPEN_WAYPOINT_MENU: KeyBinding by lazy {
        KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.wayfindr.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M, // Default to 'M' key
                "category.wayfindr.general"
            )
        )
    }
    
    val QUICK_ADD_WAYPOINT: KeyBinding by lazy {
        KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.wayfindr.quick_add",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N, // Default to 'N' key
                "category.wayfindr.general"
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
