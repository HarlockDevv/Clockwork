package org.valkyrienskies.clockwork.util

import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.*
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.joml.Vector3i
import org.joml.Vector3ic
import org.joml.primitives.AABBi
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.SelectedAreaToolkit
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.INodeBlockEntity
import org.valkyrienskies.clockwork.util.toJOMLD
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.HashMap


object ClockworkUtils {

    // VS2 removed: wanderliteNodesToAdd required ship attachment (WanderShipControl) from VS2
    // val wanderliteNodesToAdd: HashMap<BlockPos, Double> = HashMap()

    @JvmStatic
    fun tick(level: ServerLevel) {
        // VS2 removed: wanderlite node registration required getLoadedShipManagingPos and WanderShipControl
    }

    fun getDuctNodePos(blockPos: BlockPos, level: Level?): DuctNodePos {
        if (level == null) return DuctNodePos(blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble())
        val be = level.getBlockEntity(blockPos) as? INodeBlockEntity

        if (be != null) return be.getDuctNodePosition()
        return DuctNodePos(blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble(), level.dimension().location())
    }

//    @JvmStatic
//    fun DenseBlockPosSet.toBlockPosSet(): Set<BlockPos> {
//        val set = mutableSetOf<BlockPos>()
//        this.forEach { x, y, z ->
//            set.add(BlockPos(x, y, z))
//        }
//        return set
//    }
//
//    @JvmStatic
//    fun Set<BlockPos>.toBoundedVoxelSet(): BoundedVoxelSet {
//        var minBound: Vector3ic? = null
//        var maxBound: Vector3ic? = null
//        this.forEach { bp ->
//            val pos = bp.toJOML()
//            if (minBound == null) {
//                minBound = pos
//                maxBound = pos
//            } else {
//                minBound = Vector3i(
//                    Math.min(minBound!!.x(), pos.x),
//                    Math.min(minBound!!.y(), pos.y),
//                    Math.min(minBound!!.z(), pos.z)
//                )
//                maxBound = Vector3i(
//                    Math.max(maxBound!!.x(), pos.x),
//                    Math.max(maxBound!!.y(), pos.y),
//                    Math.max(maxBound!!.z(), pos.z)
//                )
//            }
//        }
//        if (minBound == null || maxBound == null) {
//            ClockworkMod.LOGGER.warn("Tried to convert empty DenseBlockPosSet to BoundedVoxelSet!")
//            return BoundedVoxelSet(HashSet(), Vector3i(0, 0, 0), Vector3i(0, 0, 0))
//        }
//        return BoundedVoxelSet(this, minBound, maxBound)
//    }
//
//    @JvmStatic
//    fun DenseBlockPosSet.toBoundedVoxelSet(): BoundedVoxelSet {
//        var minBound: Vector3ic? = null
//        var maxBound: Vector3ic? = null
//        this.forEach { x, y, z ->
//            val pos = Vector3i(x, y, z)
//            if (minBound == null) {
//                minBound = pos
//                maxBound = pos
//            } else {
//                minBound = Vector3i(
//                    Math.min(minBound!!.x(), pos.x),
//                    Math.min(minBound!!.y(), pos.y),
//                    Math.min(minBound!!.z(), pos.z)
//                )
//                maxBound = Vector3i(
//                    Math.max(maxBound!!.x(), pos.x),
//                    Math.max(maxBound!!.y(), pos.y),
//                    Math.max(maxBound!!.z(), pos.z)
//                )
//            }
//        }
//        if (minBound == null || maxBound == null) {
//            ClockworkMod.LOGGER.warn("Tried to convert empty DenseBlockPosSet to BoundedVoxelSet!")
//            return BoundedVoxelSet(HashSet(), Vector3i(0, 0, 0), Vector3i(0, 0, 0))
//        }
//        return BoundedVoxelSet(this.toBlockPosSet(), minBound, maxBound)
//    }
//
//    @JvmStatic
//    fun StructureTemplate.fillFromDenseBlockPosSet(level: ServerLevel, set: DenseBlockPosSet) {
//        this.fillFromVoxelSet(level, set.toBoundedVoxelSet())
//    }
//
//    @JvmStatic
//    fun assembleFromDenseBlockSet(level: ServerLevel, set: DenseBlockPosSet, static: Boolean): ServerShip? {
//        val voxelSet = set.toBoundedVoxelSet()
//        val ship = StructureTemplate().let {
//            it.fillFromVoxelSet(level, voxelSet)
//            it.placeAsShip(level, BlockPos.containing(level.toWorldCoordinates(voxelSet.min.toBlockPos())), true)
//        } ?: return null
//
//        //sorry mungus i had to copy this
//        cleanupOriginalBlocks(level, voxelSet) {
//            ship.isStatic = static
//        }
//
//        return ship
//    }
//
//    @JvmStatic
//    fun assembleFromBlockSet(level: ServerLevel, set: Set<BlockPos>, static: Boolean): ServerShip? {
//        val voxelSet = set.toBoundedVoxelSet()
//        val ship = StructureTemplate().let {
//            it.fillFromVoxelSet(level, voxelSet)
//            it.placeAsShip(level, BlockPos.containing(level.toWorldCoordinates(voxelSet.min.toBlockPos())), true)
//        } ?: return null
//
//        //sorry mungus i had to copy this
//        cleanupOriginalBlocks(level, voxelSet) {
//            ship.isStatic = static
//        }
//
//        return ship
//    }
//
//
//    private fun cleanupOriginalBlocks(level: ServerLevel, voxelSet: BoundedVoxelSet, whenComplete: () -> Unit) {
//        voxelSet.voxels.forEach { pos ->
//            val be = level.getBlockEntity(pos)
//            if (be != null) {
//                level.removeBlockEntity(pos)
//            }
//            level.setBlock(pos, VLib.GHOST_BLOCK.defaultBlockState(), 0)
//        }
//
//        level.scheduleCallback(4) {
//            voxelSet.voxels.forEach { pos ->
//                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS)
//            }
//
//            whenComplete.invoke()
//        }
//    }

    @JvmStatic
    fun writeVec3(vec: Vec3): ListTag {
        val tag = ListTag()
        tag.add(DoubleTag.valueOf(vec.x))
        tag.add(DoubleTag.valueOf(vec.y))
        tag.add(DoubleTag.valueOf(vec.z))
        return tag
    }

    @JvmStatic
    fun readVec3(tag: ListTag): Vec3 {
        return Vec3(tag.getDouble(0), tag.getDouble(1), tag.getDouble(2))
    }

    fun fromNormal(x: Int, y: Int, z: Int): Direction {
        return BY_NORMAL[BlockPos.asLong(x, y, z)] as Direction
    }

    private val BY_NORMAL: Long2ObjectMap<Direction> =
        Arrays.stream(Direction.values())
            .collect(
                Collectors.toMap(
                    { direction -> BlockPos(direction.normal).asLong() },
                    { direction -> direction },
                    { _, _ -> throw IllegalArgumentException("Duplicate keys") },
                    { Long2ObjectOpenHashMap() }
                )
            )

    fun writeAABBi(bb: AABBic): ListTag {
        val bbtag = ListTag()
        bbtag.add(FloatTag.valueOf(bb.minX().toFloat()))
        bbtag.add(FloatTag.valueOf(bb.minY().toFloat()))
        bbtag.add(FloatTag.valueOf(bb.minZ().toFloat()))
        bbtag.add(FloatTag.valueOf(bb.maxX().toFloat()))
        bbtag.add(FloatTag.valueOf(bb.maxY().toFloat()))
        bbtag.add(FloatTag.valueOf(bb.maxZ().toFloat()))
        return bbtag
    }

    fun readAABBi(bbtag: ListTag?): AABBic? {
        if (bbtag == null || bbtag.isEmpty()) return null
        return AABBi(
            bbtag.getFloat(0).toInt(),
            bbtag.getFloat(1).toInt(),
            bbtag.getFloat(2).toInt(),
            bbtag.getFloat(3).toInt(),
            bbtag.getFloat(4).toInt(),
            bbtag.getFloat(5).toInt()
        )
    }

    fun writeVector3i(vec: Vector3ic): ListTag {
        val tag = ListTag()
        tag.add(IntTag.valueOf(vec.x()))
        tag.add(IntTag.valueOf(vec.y()))
        tag.add(IntTag.valueOf(vec.z()))
        return tag
    }

    fun readVector3i(tag: ListTag): Vector3ic {
        return Vector3i(tag.getInt(0), tag.getInt(1), tag.getInt(2))
    }

    fun readVector3i(buf: FriendlyByteBuf): Vector3ic {
        return Vector3i(buf.readInt(), buf.readInt(), buf.readInt())
    }

    fun CompoundTag.getVector3d(prefix: String): Vector3d? {
        return if (
            !this.contains(prefix + "x") ||
            !this.contains(prefix + "y") ||
            !this.contains(prefix + "z")
        ) {
            null
        } else {
            Vector3d(
                this.getDouble(prefix + "x"),
                this.getDouble(prefix + "y"),
                this.getDouble(prefix + "z")
            )
        }
    }

    fun writeVector3i(buf: FriendlyByteBuf, vector3f: Vector3ic) {
        buf.writeInt(vector3f.x())
        buf.writeInt(vector3f.y())
        buf.writeInt(vector3f.z())
    }

    fun loadArea(nbt: CompoundTag?): SelectedAreaToolkit {
        // VS2 removed: used VSJacksonUtil.defaultMapper (VSCore) + Jackson for JSON serialization
        return SelectedAreaToolkit()
    }

    fun saveArea(nbt: CompoundTag, area: SelectedAreaToolkit?): CompoundTag {
        // VS2 removed: used VSJacksonUtil.defaultMapper (VSCore) + Jackson for JSON serialization
        return nbt
    }

    fun retrieveGasInfoFromPocket(pos: Vector3ic, level: ServerLevel): Pair<HashMap<GasType, Double>, Double> {
        // VS2 removed: getAirComponentAugmentation requires VS2 shipObjectWorld and dimensionId
        return Pair(HashMap(), 0.0)
    }

    fun getRealPos(level: Level?, blockPos: BlockPos): Vector3d {
        // VS2 removed: getShipManagingBlock requires VS2 vsApi; return block center directly
        return blockPos.toJOMLD().add(0.5, 0.5, 0.5)
    }

    // VS2 removed: getAirComponentsInChunkClaim and getSolidComponentsInChunkClaim required VS2 shipObjectWorld
}
