package net.dfnkt.wayfindr

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.lwjgl.glfw.GLFW
import java.io.File
import org.slf4j.LoggerFactory
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text

@Serializable
data class WayfindrConfig(
    val maxRenderDistance: Double = 200.0,
    val maxRaycastDistance: Double = 200.0,
    val openMenuKey: Int = GLFW.GLFW_KEY_M,
    val quickAddKey: Int = GLFW.GLFW_KEY_N,
    val createDeathWaypoint: Boolean = false
) {
    companion object {
        private val minecraftDir = File(System.getProperty("user.home"), ".minecraft")
        private val modDir = File(minecraftDir, "config/wayfindr")
        private val configFile = File(modDir, "config.json")
        private val MOD_ID = "wayfindr"
        private val logger = LoggerFactory.getLogger(MOD_ID)
        private val json = Json { prettyPrint = true }
        
        private var instance = WayfindrConfig()
        
        fun get(): WayfindrConfig {
            return instance
        }
        
        fun load() {
            try {
                if (configFile.exists()) {
                    val jsonContent = configFile.readText()
                    if (jsonContent.isNotBlank()) {
                        instance = json.decodeFromString(jsonContent)
                        logger.info("Loaded configuration from ${configFile.absolutePath}")
                    } else {
                        logger.info("Config file is empty, using defaults")
                        save()
                    }
                } else {
                    logger.info("No config file found at ${configFile.absolutePath}, creating default")
                    save()
                }
            } catch (e: Exception) {
                logger.error("Error loading config: ${e.message}")
                save()
            }
        }
        
        fun save() {
            try {
                if (!modDir.exists() && !modDir.mkdirs()) {
                    logger.error("Failed to create directory: ${modDir.absolutePath}")
                    return
                }
                
                configFile.writeText(json.encodeToString(instance))
                logger.info("Saved configuration to ${configFile.absolutePath}")
            } catch (e: Exception) {
                logger.error("Error saving config: ${e.message}")
            }
        }
        
        fun update(newConfig: WayfindrConfig) {
            val oldConfig = instance
            instance = newConfig
            save()
            
            // Notify that config has changed
            if (oldConfig.openMenuKey != newConfig.openMenuKey || 
                oldConfig.quickAddKey != newConfig.quickAddKey) {
                // Inform player that keybindings are managed via the Controls menu
                val client = MinecraftClient.getInstance()
                client.player?.sendMessage(
                    Text.literal("§6[Wayfindr]§r Keybindings are configured via Minecraft: Options > Controls > Key Binds (Wayfindr). Changes in this screen won't affect keybinds."), 
                    false
                )
            }
        }
    }
}
