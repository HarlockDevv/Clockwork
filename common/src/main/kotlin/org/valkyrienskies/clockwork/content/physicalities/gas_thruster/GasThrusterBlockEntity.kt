package org.valkyrienskies.clockwork.content.physicalities.gas_thruster

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkSoundScapes
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.GasPhysics.mixtureCapacity
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.clockwork.util.toJOMLD
import kotlin.math.*
import kotlin.random.Random

// VS2 physics listener removed - GasThruster applies forces to VS2 ships (disabled without VS2)
class GasThrusterBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state) {

    @Volatile
    var thrust = 0.0
    var velocity = 0.0

    val gasMassFlow = HashMap<GasType, Double>()
    val massPerParticle = 0.005

    override fun write(tag: CompoundTag, clientPacket: Boolean) {

        val compound = CompoundTag()
        for ((gas, mass) in gasMassFlow) {
            compound.putDouble(gas.resourceLocation.toString(), mass)
        }
        tag.put("GasMassFlow", compound)
        tag.putDouble("FlowRate",velocity)
        tag.putDouble("Thrust",thrust)
        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {

        super.read(tag, clientPacket)
        val compound = tag.get("GasMassFlow") as? CompoundTag ?: return
        for (key in compound.allKeys) {
            val gasType = GasTypeRegistry.getGasType(ResourceLocation(key)) ?: continue
            gasMassFlow[gasType] = compound.getDouble(key)
        }

        velocity = tag.getDouble("FlowRate")
        thrust = tag.getDouble("Thrust")

    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }

    override fun getDuctNodePosition(): DuctNodePos {
        if (level != null && !level!!.isClientSide()) {
            return blockPos.toDuctNodePos(level!!.dimension().location())
        }
        return blockPos.toDuctNodePos()
    }

    fun clientTick() {
        if (gasMassFlow.isEmpty() || velocity == 0.0) return

        // Handle audio
        val pitch = 60f / cbrt(velocity).toFloat()
        val scape = ClockworkSoundScapes.AmbienceGroup.THRUSTER
        ClockworkSoundScapes.play(scape, this.worldPosition, pitch)


        // Handle particles
        val ductNetwork = KelvinMod.KelvinClient
        for ((gas,mass) in gasMassFlow) {
            val particleCount = mass/massPerParticle
            val direction = blockState.getValue(BlockStateProperties.FACING)
            val speed = direction.normal.toJOMLD().mul(-cbrt(abs(velocity))/50)


            fun random() = Random.nextDouble(-0.35,0.35)
            val position = blockPos.toJOMLD().add(0.5, 0.5, 0.5)


            for (count in 1..particleCount.toInt()) {
                ductNetwork.createGasParticle(level as ClientLevel, gas, blockPos.toDuctNodePos(level!!.dimension().location()),
                    position.x+random(), position.y+random(), position.z+random(), speed.x, speed.y, speed.z)
            }
        }
    }

    fun clearMassFlow() {
        if (gasMassFlow.isEmpty()) return
        gasMassFlow.clear()
        velocity = 0.0
        thrust = 0.0
        sendData()
    }

    @OptIn(GameTickOnly::class)
    override fun tick() {
        super.tick()


        if (level!!.isClientSide) return clientTick()

        val ductnodepos = getDuctNodePosition()
        val kelvin = ClockworkMod.getKelvin()
        val node = kelvin.getNodeAt(ductnodepos) ?: return clearMassFlow()
        val gasMasses = kelvin.getGasMassAt(ductnodepos)

        if (gasMasses.values.sum() == 0.0) return clearMassFlow()

        // VS2 removed: use standard atmospheric pressure (101325 Pa)
        val airPressure = 101325.0
        val gasPressure = kelvin.getPressureAt(ductnodepos)
        val temp = kelvin.getTemperatureAt(ductnodepos)
        val avgSpecificHeat = mixtureCapacity(kelvin.getGasMassAt(ductnodepos))


        if (gasPressure < airPressure) return clearMassFlow()

        velocity = 0.0

        for (edge in node.nodeEdges) {
            velocity += edge.currentFlowRate
        }

        // AerodynamicUtils.UNIVERSAL_GAS_CONSTANT = 8.314 J/(mol·K)
        val maxFlowRate = (ClockworkConstants.Misc.DUCT_AREA * gasPressure / sqrt(temp)) * sqrt(avgSpecificHeat / 8.314) * ((avgSpecificHeat+1)/2).pow(-(avgSpecificHeat+1)/(2*(avgSpecificHeat-1)))
        val flowRate = min(maxFlowRate, velocity)

        for (gas in gasMasses) {
            velocity += flowRate/(gas.key.density*ClockworkConstants.Misc.DUCT_AREA)

            val gasMassLoss = max(flowRate*0.05, gas.value)

            gasMassFlow[gas.key] = gasMassLoss
            kelvin.removeGas(ductnodepos, gas.key, gasMassLoss)
        }

        thrust = (flowRate * velocity + (gasPressure-airPressure)) * ClockworkConfig.SERVER.gasThrusterForceMul
        sendData()
    }

    // VS2 physTick removed - ship force application requires Valkyrien Skies 2
    // override fun physTick(physShip: PhysShip?, physLevel: PhysLevel) { ... }



}
