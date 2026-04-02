package org.valkyrienskies.clockwork.content.physicalities.spinoff_bearing

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.DOWN
import net.minecraft.core.Direction.EAST
import net.minecraft.core.Direction.NORTH
import net.minecraft.core.Direction.SOUTH
import net.minecraft.core.Direction.UP
import net.minecraft.core.Direction.WEST
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.DirectionalBlock.FACING
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.physicalities.extendon.ExtendonBlockEntity
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.clockwork.util.toJOMLD

// VS2 removed: SpinoffBearing requires VS2 revolute joints between ships
class SpinoffBearingBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos,
    state
) {

    var partner : SpinoffBearingBlockEntity? = null
    @Volatile
    var partnerPos : BlockPos? = null
    @Volatile
    var partnerFacing : Direction? = null
    @Volatile
    var isLeader: Boolean = false

    @Volatile
    var isConnected : Boolean = false
    @Volatile
    var jointId : Int = -1

    val position : BlockPos
        get() = this.worldPosition
    val facing: Direction
        get() = blockState.getValue(BlockStateProperties.FACING)

    @Volatile
    var shouldVerifyConnection: Boolean = false
    @Volatile
    var shouldRemoveJoint: Boolean = false
    var shouldVerifyPartner: Boolean = false

    var reconnectDelay: Int = 0
    var canConnect : Boolean = true

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        if (clientPacket) {
            super.write(tag, clientPacket)
        }
        if (partnerPos != null) {
            tag.putInt("partnerX", partnerPos!!.x)
            tag.putInt("partnerY", partnerPos!!.y)
            tag.putInt("partnerZ", partnerPos!!.z)
        }
        tag.putInt("jointId", jointId)
        tag.putBoolean("isLeader", isLeader)
        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        if (tag.contains("partnerX") && tag.contains("partnerY") && tag.contains("partnerZ")) {
            val x = tag.getInt("partnerX")
            val y = tag.getInt("partnerY")
            val z = tag.getInt("partnerZ")
            partnerPos = BlockPos(x, y, z)
        }
        if (tag.contains("isLeader")) {
            isLeader = tag.getBoolean("isLeader")
        }
        jointId = tag.getInt("jointId")
        shouldVerifyPartner = true
    }

    override fun tick() {
        super.tick()
        // VS2 removed: SpinoffBearing connection logic requires VS2 ship joints (clipIncludeShips, getLoadedShipManagingPos, VSRevoluteJoint)
    }

    // VS2 removed: physTick requires VS2 VsiPhysLevel and VSRevoluteJoint

    fun disconnect() {
        this.shouldRemoveJoint = true
        partner = null
        partnerPos = null
        partnerFacing = null
        reconnectDelay = 10
    }

    override fun destroy() {
        // VS2 removed: joint removal requires VS2 gtpa
        partner?.disconnect()
        this.disconnect()
        super.destroy()
    }

    private fun Direction.getQuaternion(): Quaterniondc {
        return when (this) {
            DOWN -> {
                Quaterniond(AxisAngle4d(Math.PI, Vector3d(1.0, 0.0, 0.0)))
            }
            NORTH -> {
                Quaterniond(AxisAngle4d(Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)))).normalize()
            }
            EAST -> {
                Quaterniond(AxisAngle4d(0.5 * Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)))).normalize()
            }
            SOUTH -> {
                Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0))).normalize()
            }
            WEST -> {
                Quaterniond(AxisAngle4d(1.5 * Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)))).normalize()
            }
            UP -> {
                // Do nothing
                Quaterniond()
            }
            else -> {
                // This should be impossible, but have this here just in case
                Quaterniond()
            }
        }
    }

    override fun addBehaviours(behaviours: List<BlockEntityBehaviour?>?) {
    }
}
