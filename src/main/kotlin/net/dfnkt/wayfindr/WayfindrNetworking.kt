package net.dfnkt.wayfindr

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
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
    val WAYPOINT_SYNC_ID = Identifier.fromNamespaceAndPath(Wayfindr.MOD_ID, "waypoint_sync")
    val WAYPOINT_ADD_ID = Identifier.fromNamespaceAndPath(Wayfindr.MOD_ID, "waypoint_add")
    val WAYPOINT_UPDATE_ID = Identifier.fromNamespaceAndPath(Wayfindr.MOD_ID, "waypoint_update")
    val WAYPOINT_DELETE_ID = Identifier.fromNamespaceAndPath(Wayfindr.MOD_ID, "waypoint_delete")

    // Custom payload implementations
    data class WaypointSyncPayload(val data: String) : CustomPacketPayload {
        override fun type() = ID
        companion object {
            val ID = CustomPacketPayload.Type<WaypointSyncPayload>(WAYPOINT_SYNC_ID)
            val CODEC: StreamCodec<RegistryFriendlyByteBuf, WaypointSyncPayload> = StreamCodec.of(
                { buf, payload -> buf.writeUtf(payload.data) },
                { buf -> WaypointSyncPayload(buf.readUtf()) }
            )
        }
    }

    data class WaypointAddPayload(val data: String) : CustomPacketPayload {
        override fun type() = ID
        companion object {
            val ID = CustomPacketPayload.Type<WaypointAddPayload>(WAYPOINT_ADD_ID)
            val CODEC: StreamCodec<RegistryFriendlyByteBuf, WaypointAddPayload> = StreamCodec.of(
                { buf, payload -> buf.writeUtf(payload.data) },
                { buf -> WaypointAddPayload(buf.readUtf()) }
            )
        }
    }

    data class WaypointUpdatePayload(val data: String) : CustomPacketPayload {
        override fun type() = ID
        companion object {
            val ID = CustomPacketPayload.Type<WaypointUpdatePayload>(WAYPOINT_UPDATE_ID)
            val CODEC: StreamCodec<RegistryFriendlyByteBuf, WaypointUpdatePayload> = StreamCodec.of(
                { buf, payload -> buf.writeUtf(payload.data) },
                { buf -> WaypointUpdatePayload(buf.readUtf()) }
            )
        }
    }

    data class WaypointDeletePayload(val waypointId: String) : CustomPacketPayload {
        override fun type() = ID
        companion object {
            val ID = CustomPacketPayload.Type<WaypointDeletePayload>(WAYPOINT_DELETE_ID)
            val CODEC: StreamCodec<RegistryFriendlyByteBuf, WaypointDeletePayload> = StreamCodec.of(
                { buf, payload -> buf.writeUtf(payload.waypointId) },
                { buf -> WaypointDeletePayload(buf.readUtf()) }
            )
        }
    }

    /**
     * Initializes the networking system.
     * This should be called during mod initialization.
     */
    fun initialize() {
        logger.info("Initializing Wayfindr networking")

        // Register custom payload types for server-to-client (S2C) communication
        PayloadTypeRegistry.clientboundPlay().register(
            WaypointSyncPayload.ID,
            WaypointSyncPayload.CODEC
        )

        PayloadTypeRegistry.clientboundPlay().register(
            WaypointAddPayload.ID,
            WaypointAddPayload.CODEC
        )

        PayloadTypeRegistry.clientboundPlay().register(
            WaypointUpdatePayload.ID,
            WaypointUpdatePayload.CODEC
        )

        PayloadTypeRegistry.clientboundPlay().register(
            WaypointDeletePayload.ID,
            WaypointDeletePayload.CODEC
        )

        // Register custom payload types for client-to-server (C2S) communication
        PayloadTypeRegistry.serverboundPlay().register(
            WaypointAddPayload.ID,
            WaypointAddPayload.CODEC
        )

        PayloadTypeRegistry.serverboundPlay().register(
            WaypointUpdatePayload.ID,
            WaypointUpdatePayload.CODEC
        )

        PayloadTypeRegistry.serverboundPlay().register(
            WaypointDeletePayload.ID,
            WaypointDeletePayload.CODEC
        )

        logger.info("Registered all Wayfindr custom payload types")
    }
}
