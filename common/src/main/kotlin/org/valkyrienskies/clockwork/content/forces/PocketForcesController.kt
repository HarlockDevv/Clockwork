package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import org.valkyrienskies.clockwork.ClockworkAugmentations
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.utilities.PocketForcesQueueable
import org.valkyrienskies.clockwork.util.ClockworkUtils.retrieveGasInfoFromPocket
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.connectivity.ConnectionStatus
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.util.pollUntilEmpty
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.DuctNetwork
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.DuctNetworkServer
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.GasPhysics.mixtureCapacity
import org.valkyrienskies.mod.api.positionToWorld
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.HashMap
import kotlin.math.*

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
// VS2 removed: ShipPhysicsListener requires VS2
class PocketForcesController {

    var dimensionId: DimensionId = "minecraft:dimension:minecraft:overworld"

    @JsonIgnore
    private val pocketQueue: ConcurrentLinkedQueue<PocketForcesQueueable> = ConcurrentLinkedQueue()


    // Todo: Implement serialization

    fun physTick(physShip: PhysShip, physLevel: PhysLevel) {
        val physShipImpl = physShip

        val buoyancyForce = calculateBuoyancyForce(physShip, physLevel)

        //ClockworkMod.LOGGER.info(physShip.mass.toString())

        buoyancyForce.forEach {
            //if (it.value > 1.0) println(it.value.toString() + "to" + it.key.toString())
            if (it.value.isFinite() && !it.value.isNaN()) { //just to be safe

                physShipImpl.applyWorldForceToModelPos(Vector3d(0.0, it.value, 0.0))
            }
        }
    }

    fun calculateBuoyancyForce(physShip: PhysShip, physLevel: PhysLevel): Map<Vector3dc, Double> {
        val physShipImpl = physShip

        var totalBuoyantForce = HashMap<Vector3dc, Double>()
        //if (dimensionMap[dimensionId] == null || dimensionMap[dimensionId]!!.maxY <= 0.0) return totalBuoyantForce



        pocketQueue.pollUntilEmpty {
            val yHeight = physShip.transform.shipToWorld.transformPosition(it.pocketCenter, Vector3d()).y()
            val atmoDensity = physLevel.aerodynamicUtils.getAirDensityForY(yHeight, this.dimensionId)

            val atmoGravity = physLevel.aerodynamicUtils.getAtmosphereForDimension(this.dimensionId).third
            val buoyantForce = it.pocketVolume * (atmoDensity - it.hotDensity) * atmoGravity * ClockworkConfig.SERVER.balloonForceMult
            totalBuoyantForce[it.pocketCenter] = max(buoyantForce, 0.0)
        }
        pocketQueue.clear()

        return totalBuoyantForce
    }

    fun gameTick(level: ServerLevel, id: Long) {
        // VS2 removed: gameTick requires VS2 shipObjectWorld, ClockworkAugmentations augmentations, and air component APIs
    }
    // VS2 removed: gameTick and getPocketCenter require VS2 shipObjectWorld and air component augmentations

    private fun getPocketCenter(level: ServerLevel, pos: Vector3i): Vector3d {
        // VS2 removed: requires VS2 shipObjectWorld air component augmentations
        return Vector3d(pos.x().toDouble(), pos.y().toDouble(), pos.z().toDouble())
    }

    companion object {
        fun getOrCreate(ship: LoadedServerShip): PocketForcesController? {
            if (ship.getAttachment(PocketForcesController::class.java) == null) {
                val controller = PocketForcesController()
                controller.dimensionId = ship.chunkClaimDimension
                ship.setAttachment(controller)
            }
            val controller = ship.getAttachment(PocketForcesController::class.java)
            controller!!.dimensionId = ship.chunkClaimDimension
            return controller
        }
    }
}
