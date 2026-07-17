package net.dfnkt.wayfindr

import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.ClipContext

object WayfindrRaycast {

    /**
     * Performs a raycast from the player's eye position in the direction they're looking
     * @param player The player to raycast from
     * @param maxDistance Maximum distance to raycast (uses config value by default)
     * @return The position where the raycast hits, or a position in front of the player if no hit
     */
    fun getRaycastPosition(player: Player, maxDistance: Double = WayfindrConfig.get().maxRaycastDistance): Vec3 {
        val eyePos = player.eyePosition
        val lookDirection = player.getViewVector(1.0f)
        val endPos = eyePos.add(lookDirection.scale(maxDistance))

        // Perform the raycast
        val clipContext = ClipContext(
            eyePos,
            endPos,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            player
        )

        val hitResult = player.level().clip(clipContext)

        return when (hitResult.type) {
            HitResult.Type.BLOCK -> {
                // Hit a block, place waypoint at the hit position
                val blockHit = hitResult as BlockHitResult
                blockHit.location
            }
            HitResult.Type.MISS -> {
                // No block hit, place waypoint a reasonable distance in front of the player
                eyePos.add(lookDirection.scale(10.0))
            }
            else -> {
                // Fallback to position in front of player
                eyePos.add(lookDirection.scale(10.0))
            }
        }
    }

    /**
     * Gets a position a fixed distance in front of the player
     * @param player The player
     * @param distance Distance in front of the player (default 5 blocks)
     * @return Position in front of the player
     */
    fun getPositionInFrontOfPlayer(player: Player, distance: Double = 5.0): Vec3 {
        val eyePos = player.eyePosition
        val lookDirection = player.getViewVector(1.0f)
        return eyePos.add(lookDirection.scale(distance))
    }
}
