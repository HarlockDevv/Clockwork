package org.valkyrienskies.clockwork.content.contraptions.phys.bearing

import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour
import com.simibubi.create.foundation.item.TooltipHelper
import com.simibubi.create.foundation.utility.ServerSpeedProvider
import net.createmod.catnip.math.AngleHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkMod.MOD_ID
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.platform.api.ContraptionController
import org.valkyrienskies.clockwork.platform.api.ContraptionController.LockedMode
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.clockwork.util.ClockworkConstants.Nbt.ORIGINAL_DIRECTION
import org.valkyrienskies.clockwork.util.minus
import org.valkyrienskies.clockwork.util.plus
import kotlin.math.*

//TODO move
fun getHingeRotation(localDir: Vector3dc, right: Vector3dc = Vector3d(1.0, 0.0, 0.0)): Quaterniond {
    if ((localDir - right).length() < 1e-5) { return Quaterniond() }
    val v1l = right.length()
    val v2l = localDir.length()
    val a = right.cross(localDir, Vector3d())
    val k = sqrt(v1l * v1l * v2l * v2l)
    val kCosTheta = right.dot(localDir)
    if (abs(kCosTheta / k + 1.0) < 1e-5) {
        val ort = right.let { it.orthogonalize(it, Vector3d()) }
        return Quaterniond(ort.x, ort.y, ort.z, 0.0).normalize()
    }
    return Quaterniond(a.x, a.y, a.z, k + kCosTheta).normalize()
}

// VS2 removed: PhysBearingBlockEntity is a no-op stub; all VS2 ship assembly/joint logic removed.
class PhysBearingBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) :
    GeneratingKineticBlockEntity(type, pos, state), IBearingBlockEntity, IDisplayAssemblyExceptions,
    ContraptionController {

    var assembleNextTick = false
    var movementMode: ScrollOptionBehaviour<LockedMode>? = null
    var isRunning = false
        private set
    var shiptraptionID = NO_SHIPTRAPTION_ID
        private set
    var targetAngle = 0f
        get() = field
        private set(v) { field = v }
    var disassembleWhenPossible = false
        private set

    private var lastException: AssemblyException? = null
    private var open = false
    private var originalDirection: Direction? = null
    private var clientAngleDiff = 0f
    private var prevAngle = 0f
    private var coreAngle = 0f
    private var previousCoreAngle = 0f
    private var opening = false
    private var openProgress = 0f
    private val openProgressMax = 70f
    private var inOutCorner = 0f
    private var cornerShrinking = false
    private var ticks = 0
    private var sequencedAngleLimit = -1.0f
    private var sequencedAngleProgress = 0f

    init {
        setLazyTickRate(3)
    }

    private fun movementModeChanged(value: Int) {
        if (level == null || level!!.isClientSide) return
        sendData()
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)
        movementMode = ScrollOptionBehaviour(
            LockedMode::class.java, Component.translatable("$MOD_ID.phys_bearing.rotation_mode"),
            this, movementModeSlot
        )
        movementMode!!.withCallback { movementModeChanged(it) }
        movementMode!!.requiresWrench()
        behaviours.add(movementMode!!)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        tag.putBoolean(ClockworkConstants.Nbt.RUNNING, isRunning)
        tag.putFloat(ClockworkConstants.Nbt.ANGLE, targetAngle)
        if (shiptraptionID != NO_SHIPTRAPTION_ID) tag.putLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID, shiptraptionID)
        if (originalDirection != null) tag.putInt(ORIGINAL_DIRECTION, originalDirection!!.ordinal)
        AssemblyException.write(tag, lastException)
        tag.putBoolean(ClockworkConstants.Nbt.OPEN, open)
        tag.putFloat(ClockworkConstants.Nbt.SEQUENCED_ANGLE_LIMIT, sequencedAngleLimit)
        tag.putFloat(ClockworkConstants.Nbt.SEQUENCED_ANGLE_PROGRESS, sequencedAngleProgress)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        val angleBefore = targetAngle
        open = tag.getBoolean(ClockworkConstants.Nbt.OPEN)
        isRunning = tag.getBoolean(ClockworkConstants.Nbt.RUNNING)
        targetAngle = tag.getFloat(ClockworkConstants.Nbt.ANGLE)
        lastException = AssemblyException.read(tag)
        if (tag.contains(ClockworkConstants.Nbt.SHIPTRAPTION_ID))
            shiptraptionID = tag.getLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID)
        if (tag.contains(ORIGINAL_DIRECTION))
            originalDirection = Direction.entries[tag.getInt(ORIGINAL_DIRECTION)]
        if (isRunning) {
            if (shiptraptionID == NO_SHIPTRAPTION_ID) {
                clientAngleDiff = AngleHelper.getShortestAngleDiff(angleBefore.toDouble(), targetAngle.toDouble())
                targetAngle = angleBefore
            }
        } else {
            shiptraptionID = NO_SHIPTRAPTION_ID
        }
        sequencedAngleLimit = tag.getFloat(ClockworkConstants.Nbt.SEQUENCED_ANGLE_LIMIT)
        sequencedAngleProgress = tag.getFloat(ClockworkConstants.Nbt.SEQUENCED_ANGLE_PROGRESS)
        super.read(tag, clientPacket)
    }

    override fun getInterpolatedAngle(partialTicks: Float): Float {
        var pt = partialTicks
        if (isVirtual) return Mth.lerp(pt + .5f, prevAngle, targetAngle)
        if (shiptraptionID == NO_SHIPTRAPTION_ID || !isRunning) pt = 0f
        return Mth.lerp(pt, targetAngle, targetAngle + angularSpeed)
    }

    fun getWingRotOffset(): Float = when {
        isRunning && open  -> openProgressMax
        isRunning          -> Mth.lerp(openProgress.toDouble(), 0.0, openProgressMax.toDouble()).toFloat()
        !isRunning && open -> Mth.lerp(openProgress.toDouble(), 1.0, openProgressMax.toDouble()).toFloat()
        else               -> 0f
    }

    fun getInterpolatedCoreAngle(partialTicks: Float): Float {
        previousCoreAngle = coreAngle
        coreAngle++
        if (coreAngle == 360f) coreAngle = 0f
        return if (isVirtual) Mth.lerp(partialTicks + .5f, previousCoreAngle, coreAngle)
        else Mth.lerp(partialTicks, coreAngle, coreAngle + 4f)
    }

    val angularSpeed: Float
        get() {
            var speed = convertToAngular(getSpeed())
            if (getSpeed() == 0f) speed = 0f
            if (level!!.isClientSide) {
                speed *= ServerSpeedProvider.get()
                speed += clientAngleDiff / 3f
            }
            return speed
        }

    fun getActualAngularSpeed(): Float {
        val dir = originalDirection ?: return 0f
        return convertToAngular(getSpeed()) * if (dir == Direction.WEST || dir == Direction.NORTH || dir == Direction.DOWN) 1 else -1
    }

    fun getRealisticAngularSpeed(): Float {
        val dir = originalDirection ?: return 0f
        return getSpeed() * 2f * PI.toFloat() / 60f * if (dir == Direction.WEST || dir == Direction.NORTH || dir == Direction.DOWN) 1 else -1
    }

    private fun tickAnimationLogic() {
        if (inOutCorner < 1 && !cornerShrinking) inOutCorner += 0.0075f
        else if (inOutCorner >= 1) cornerShrinking = true
        if (inOutCorner > 0 && cornerShrinking) inOutCorner -= 0.0075f
        else if (inOutCorner <= 0) cornerShrinking = false

        if (isRunning && !open && !opening) opening = true
        if (opening && isRunning && openProgress < 1f) openProgress += 0.05f
        else if (openProgress >= 1f) { opening = false; open = true; openProgress = 1f }
        if (open && !isRunning && openProgress > 0f) openProgress -= 0.05f
        else if (openProgress <= 0f) { open = false; openProgress = 0f }
    }

    override fun tick() {
        super.tick()
        prevAngle = targetAngle
        ticks++
        if (level!!.isClientSide) clientAngleDiff /= 2f
        tickAnimationLogic()
        if (!isRunning) return
        if (shiptraptionID == NO_SHIPTRAPTION_ID) targetAngle = 0f
    }

    override fun lazyTick() {
        super.lazyTick()
        if (shiptraptionID != NO_SHIPTRAPTION_ID && !level!!.isClientSide) sendData()
    }

    override fun onSpeedChanged(previousSpeed: Float) {
        sequencedAngleLimit = -1f
        sequencedAngleProgress = 0f
        if (sequenceContext != null && sequenceContext.instruction == SequencerInstructions.TURN_ANGLE)
            sequencedAngleLimit = sequenceContext.getEffectiveValue(theoreticalSpeed.toDouble()).toFloat()
        super.onSpeedChanged(previousSpeed)
    }

    override fun addToTooltip(tooltip: List<Component>, isPlayerSneaking: Boolean): Boolean {
        if (super.addToTooltip(tooltip, isPlayerSneaking)) return true
        if (isPlayerSneaking) return false
        if (getSpeed() == 0f) return false
        if (isRunning) return false
        if (blockState.block !is BearingBlock) return false
        val attachedState = level!!.getBlockState(worldPosition.relative(blockState.getValue(BearingBlock.FACING)))
        if (attachedState.canBeReplaced()) return false
        TooltipHelper.addHint(tooltip, "hint.empty_bearing")
        return true
    }

    // VS2 removed: assemble/disassemble require VS2 ship assembly (VSAssemblyEvents, assembleToShip, joints, etc.)
    fun assemble() {}
    fun disassemble() {}
    override fun destroy() {}

    // VS2 removed: actual angle requires VS2 ship transform lookup
    fun getActualAngle(): Double? = null

    override fun attach(contraption: ControlledContraptionEntity) {}
    override fun onStall() { if (!level!!.isClientSide) sendData() }
    override fun isValid(): Boolean = !isRemoved
    override fun isAttachedTo(contraption: AbstractContraptionEntity): Boolean = false
    override fun setAngle(forcedAngle: Float) { targetAngle = forcedAngle }
    override fun getLastAssemblyException(): AssemblyException? = lastException
    override fun getBlockPosition(): BlockPos = worldPosition
    override fun isWoodenTop(): Boolean = false

    companion object {
        const val NO_SHIPTRAPTION_ID: Long = -1
    }
}
