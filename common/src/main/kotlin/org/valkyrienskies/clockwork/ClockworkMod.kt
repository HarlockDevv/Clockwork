package org.valkyrienskies.clockwork

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.logging.LogUtils
import com.simibubi.create.foundation.data.CreateRegistrate
import com.simibubi.create.foundation.item.ItemDescription
import com.simibubi.create.foundation.item.TooltipModifier
import dev.architectury.event.events.common.CommandRegistrationEvent
import dev.architectury.event.events.common.InteractionEvent
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.event.events.common.TickEvent
import dev.architectury.platform.Platform
import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.createmod.catnip.lang.FontHelper
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import org.slf4j.LoggerFactory
import org.valkyrienskies.clockwork.content.contraptions.flap.dual_link.DualLinkHandler
import org.valkyrienskies.clockwork.content.events.CollisionSoundEffectHandler
import org.valkyrienskies.clockwork.integration.cc.GenericPeripheralsCommon
import org.valkyrienskies.clockwork.util.ClockworkUtils
import org.valkyrienskies.clockwork.util.builder.ClockworkExpandedCreateRegistrate
import org.valkyrienskies.clockwork.util.gui.DuctStats
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.impl.DuctNetworkServer
import kotlin.math.roundToInt


object ClockworkMod {
    const val MOD_ID = "vs_clockwork"

    // versioning
    const val BUILD_VERSION = 1
    const val NETWORK_VERSION = 1
    const val NETWORK_VERSION_STR = NETWORK_VERSION.toString()

    val NETWORK_CHANNEL: ResourceLocation = asResource("main")

    val REGISTRATE: CreateRegistrate = ClockworkExpandedCreateRegistrate.create(MOD_ID).setTooltipModifierFactory { item ->
        ItemDescription . Modifier (item, FontHelper.Palette.STANDARD_CREATE)
        .andThen(TooltipModifier.mapNull(DuctStats.create(item)))
    }
    val MIXIN_LOGGER = LoggerFactory.getLogger("ClockworkMixins")
    val LOGGER = LogUtils.getLogger()

    private val TAB_REGISTRY = DeferredRegister.create(MOD_ID, Registries.CREATIVE_MODE_TAB)

    val BASE_CREATIVE_TAB: RegistrySupplier<CreativeModeTab> = TAB_REGISTRY.register("clockwork_main") {
        CreativeTabRegistry.create(Component.translatable("itemGroup.vs_clockwork")) {
            ItemStack(ClockworkItems.WANDERWAND.asItem())
        }
    }

    val PHYSICAL_CREATIVE_TAB: RegistrySupplier<CreativeModeTab> = TAB_REGISTRY.register("clockwork_physicalities") {
        CreativeTabRegistry.create(Component.translatable("itemGroup.vs_clockwork.physicalities")) {
            ItemStack(ClockworkBlocks.PHYSICS_INFUSER.asItem())
        }
    }

    val GAS_CREATIVE_TAB: RegistrySupplier<CreativeModeTab> = TAB_REGISTRY.register("clockwork_gasses") {
        CreativeTabRegistry.create(Component.translatable("itemGroup.vs_clockwork.gasses")) {
            ItemStack(ClockworkBlocks.AIR_COMPRESSOR.get())
        }
    }

    val BASE_CREATIVE_TABINFO: ResourceKey<CreativeModeTab> = BASE_CREATIVE_TAB.key
    val PHYSICAL_CREATIVE_TABINFO: ResourceKey<CreativeModeTab> = PHYSICAL_CREATIVE_TAB.key
    val GAS_CREATIVE_TABINFO: ResourceKey<CreativeModeTab> = GAS_CREATIVE_TAB.key

    @JvmStatic
    fun init() {
        ClockworkPackets.init()
        ClockworkTags.init()
        ClockworkRecipes.init()
        TAB_REGISTRY.register()

        // VS2 ship attachment registrations removed (no VS2 dependency)

        ClockworkWorldgen.register()

        ClockworkDamageTypes.init()

        //Register gas types
        ClockworkGasses.init()

        TickEvent.SERVER_LEVEL_POST.register {
            ClockworkUtils.tick(it)
            CollisionSoundEffectHandler.tick(it)
        }

        InteractionEvent.RIGHT_CLICK_BLOCK.register(InteractionEvent.RightClickBlock { player, hand, pos, face ->
            DualLinkHandler.handler(player, hand, pos, face)
        })

        if (Platform.isModLoaded("computercraft")) {
            GenericPeripheralsCommon.register()
        }

        KelvinMod.disableReactionJEI()
    }

    @JvmStatic
    fun getKelvin(): DuctNetworkServer {
        KelvinMod.Kelvin.solver = ClockworkConfig.SERVER.kelvinSolver.getSolver()
        return KelvinMod.getKelvin() as DuctNetworkServer
    }

    @JvmStatic
    fun asResource(path: String): ResourceLocation {
        return ResourceLocation(MOD_ID, path)
    }

}
