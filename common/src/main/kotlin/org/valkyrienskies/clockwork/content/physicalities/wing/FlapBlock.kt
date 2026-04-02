package org.valkyrienskies.clockwork.content.physicalities.wing

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.util.blocktype.DyedWing

// VS2 removed: FlapBlock no longer implements org.valkyrienskies.mod.common.block.WingBlock.
// Aerodynamic flap functionality (getWing) requires VS2 runtime.
class FlapBlock(properties: Properties?) : DyedWing(properties) {
    override fun getNewState(state: BlockState?, level: Level?, pos: BlockPos?): BlockState? = state
}
