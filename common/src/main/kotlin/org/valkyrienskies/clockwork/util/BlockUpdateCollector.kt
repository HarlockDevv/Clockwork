package org.valkyrienskies.clockwork.util

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState

object BlockUpdateCollector {
    fun onSetBlock(sLevel: ServerLevel, pos: BlockPos, state: BlockState) {
        // VS2 removed: getLoadedShipManagingPos requires VS2 runtime.
        // Without VS2 there are no ships, so ship is always null and nothing here would run.
    }
}
