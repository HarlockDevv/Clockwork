package org.valkyrienskies.clockwork.util

import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.core.api.ships.ServerShip

// VS2 removed: ShipDestroyer requires VS2 ship networking and chunk management APIs
object ShipDestroyer {
    fun unfillShip(level: ServerLevel, ship: ServerShip) {
        // VS2 removed: ship disassembly requires VS2 activeChunksSet, relocateBlock, PacketStopChunkUpdates, etc.
    }
}
