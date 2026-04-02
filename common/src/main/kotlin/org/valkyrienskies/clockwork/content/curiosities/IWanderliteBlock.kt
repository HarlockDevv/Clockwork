package org.valkyrienskies.clockwork.content.curiosities

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

interface IWanderliteBlock {

    // VS2 removed: addToShip/removeFromShip require VS2 LoadedServerShip and WanderShipControl attachment
    fun collectBlockPositions(worldIn: Level, pos: BlockPos, depth: Int, collectedPositions: MutableList<BlockPos> = mutableListOf()): MutableList<BlockPos> {
        // Base case: If the depth is 0, return an empty collection
        if (depth == 0) {
            return mutableListOf()
        }

        // Add the current position to the set
        collectedPositions.add(pos)

        // Create a set to store block positions for the current iteration
        val positionsInThisIteration = mutableListOf(pos)

        // Iterate through each direction
        for (direction in Direction.entries) {
            // Get the neighboring block position in the current direction
            val neighborPos = pos.relative(direction)
            // Check if the block position is valid and not already collected
            if (worldIn.isInWorldBounds(neighborPos) && !collectedPositions.contains(neighborPos) && worldIn.getBlockState(neighborPos).block is IWanderliteBlock) {
                // Recursively collect block positions for the neighboring block
                positionsInThisIteration.addAll(collectBlockPositions(worldIn, neighborPos, depth - 1, collectedPositions))
            }
        }

        // Return the set of collected positions for this iteration
        return positionsInThisIteration
    }

    fun shipifyBlock(level: ServerLevel, blockPos: BlockPos) {
        // VS2 removed: shipifyBlock requires VS2 ShipAssembler, wanderliteNodesToAdd, and ship transforms
    }

}
