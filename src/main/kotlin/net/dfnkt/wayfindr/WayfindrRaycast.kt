package net.dfnkt.wayfindr

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext

object WayfindrRaycast {
    
    /**
     * Performs a raycast from the player's eye position in the direction they're looking
     * @param player The player to raycast from
     * @param maxDistance Maximum distance to raycast (uses config value by default)
     * @return The position where the raycast hits, or a position in front of the player if no hit
     */
    fun getRaycastPosition(player: PlayerEntity, maxDistance: Double = WayfindrConfig.get().maxRaycastDistance): Vec3d {
        val eyePos = player.eyePos
        val lookDirection = player.rotationVector
        val endPos = eyePos.add(lookDirection.multiply(maxDistance))
        
        // Perform the raycast
        val raycastContext = RaycastContext(
            eyePos,
            endPos,
            RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.NONE,
            player
        )
        
        val hitResult = player.world.raycast(raycastContext)
        
        return when (hitResult.type) {
            HitResult.Type.BLOCK -> {
                // Hit a block, place waypoint at the hit position
                val blockHit = hitResult as BlockHitResult
                blockHit.pos
            }
            HitResult.Type.MISS -> {
                // No block hit, place waypoint a reasonable distance in front of the player
                eyePos.add(lookDirection.multiply(10.0))
            }
            else -> {
                // Fallback to position in front of player
                eyePos.add(lookDirection.multiply(10.0))
            }
        }
    }
    
    /**
     * Gets a position a fixed distance in front of the player
     * @param player The player
     * @param distance Distance in front of the player (default 5 blocks)
     * @return Position in front of the player
     */
    fun getPositionInFrontOfPlayer(player: PlayerEntity, distance: Double = 5.0): Vec3d {
        val eyePos = player.eyePos
        val lookDirection = player.rotationVector
        return eyePos.add(lookDirection.multiply(distance))
    }
}
