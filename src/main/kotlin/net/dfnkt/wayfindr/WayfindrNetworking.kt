package net.dfnkt.wayfindr

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry

/**
 * Handles network communication for waypoint synchronization between server and clients.
 * 
 * This class defines network channels for different waypoint operations and provides
 * utility methods for packet handling.
 */
object WayfindrNetworking {
    private val logger = LoggerFactory.getLogger("wayfindr")
    
    // Network channel identifiers
    val WAYPOINT_SYNC_ID = Identifier.of(Wayfindr.MOD_ID, "waypoint_sync")
    val WAYPOINT_ADD_ID = Identifier.of(Wayfindr.MOD_ID, "waypoint_add")
    val WAYPOINT_UPDATE_ID = Identifier.of(Wayfindr.MOD_ID, "waypoint_update")
    val WAYPOINT_DELETE_ID = Identifier.of(Wayfindr.MOD_ID, "waypoint_delete")
    
    // Custom payload implementations
    data class WaypointSyncPayload(val data: String) : CustomPayload {
        companion object {
            val ID = CustomPayload.Id<WaypointSyncPayload>(WAYPOINT_SYNC_ID)
            val CODEC = object : PacketCodec<PacketByteBuf, WaypointSyncPayload> {
                override fun encode(buf: PacketByteBuf, payload: WaypointSyncPayload) {
                    buf.writeString(payload.data)
                }
                
                override fun decode(buf: PacketByteBuf): WaypointSyncPayload {
                    return WaypointSyncPayload(buf.readString())
                }
            }
        }
        override fun getId() = ID
    }
    
    data class WaypointAddPayload(val data: String) : CustomPayload {
        companion object {
            val ID = CustomPayload.Id<WaypointAddPayload>(WAYPOINT_ADD_ID)
            val CODEC = object : PacketCodec<PacketByteBuf, WaypointAddPayload> {
                override fun encode(buf: PacketByteBuf, payload: WaypointAddPayload) {
                    buf.writeString(payload.data)
                }
                
                override fun decode(buf: PacketByteBuf): WaypointAddPayload {
                    return WaypointAddPayload(buf.readString())
                }
            }
        }
        override fun getId() = ID
    }
    
    data class WaypointUpdatePayload(val data: String) : CustomPayload {
        companion object {
            val ID = CustomPayload.Id<WaypointUpdatePayload>(WAYPOINT_UPDATE_ID)
            val CODEC = object : PacketCodec<PacketByteBuf, WaypointUpdatePayload> {
                override fun encode(buf: PacketByteBuf, payload: WaypointUpdatePayload) {
                    buf.writeString(payload.data)
                }
                
                override fun decode(buf: PacketByteBuf): WaypointUpdatePayload {
                    return WaypointUpdatePayload(buf.readString())
                }
            }
        }
        override fun getId() = ID
    }
    
    data class WaypointDeletePayload(val waypointId: String) : CustomPayload {
        companion object {
            val ID = CustomPayload.Id<WaypointDeletePayload>(WAYPOINT_DELETE_ID)
            val CODEC = object : PacketCodec<PacketByteBuf, WaypointDeletePayload> {
                override fun encode(buf: PacketByteBuf, payload: WaypointDeletePayload) {
                    buf.writeString(payload.waypointId)
                }
                
                override fun decode(buf: PacketByteBuf): WaypointDeletePayload {
                    return WaypointDeletePayload(buf.readString())
                }
            }
        }
        override fun getId() = ID
    }
    
    /**
     * Initializes the networking system.
     * This should be called during mod initialization.
     */
    fun initialize() {
        logger.info("Initializing Wayfindr networking")
        
        // Register custom payload types for server-to-client (S2C) communication
        PayloadTypeRegistry.playS2C().register(
            WaypointSyncPayload.ID,
            WaypointSyncPayload.CODEC
        )
        
        PayloadTypeRegistry.playS2C().register(
            WaypointAddPayload.ID,
            WaypointAddPayload.CODEC
        )
        
        PayloadTypeRegistry.playS2C().register(
            WaypointUpdatePayload.ID,
            WaypointUpdatePayload.CODEC
        )
        
        PayloadTypeRegistry.playS2C().register(
            WaypointDeletePayload.ID,
            WaypointDeletePayload.CODEC
        )
        
        // Register custom payload types for client-to-server (C2S) communication
        PayloadTypeRegistry.playC2S().register(
            WaypointAddPayload.ID,
            WaypointAddPayload.CODEC
        )
        
        PayloadTypeRegistry.playC2S().register(
            WaypointUpdatePayload.ID,
            WaypointUpdatePayload.CODEC
        )
        
        PayloadTypeRegistry.playC2S().register(
            WaypointDeletePayload.ID,
            WaypointDeletePayload.CODEC
        )
        
        logger.info("Registered all Wayfindr custom payload types")
    }
}
